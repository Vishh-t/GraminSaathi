package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.VillageCsvParser;
import com.graminsaathi.model.Village;
import com.graminsaathi.repository.VillageRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Reader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class VillageService {

    static final int MIN_QUERY_LENGTH = 2;
    static final int DEFAULT_LIMIT = 10;
    static final int MAX_LIMIT = 20;
    private static final int IMPORT_BATCH = 1000;
    static final String DEMO_SEED_SOURCE = "demo_data.json (demo seed)";

    private final VillageRepository villageRepository;
    private final EntityManager entityManager;

    public record ImportSummary(int inserted, int skippedExisting, int skippedInvalid) {}

    /**
     * Autocomplete. Queries shorter than {@link #MIN_QUERY_LENGTH} (after normalizing) return nothing,
     * so a one-letter keystroke never scans the whole table. {@code state} may be null/blank for any state.
     */
    @Transactional(readOnly = true)
    public List<Village> search(String query, String state, Integer limit) {
        String q = normalize(query);
        if (q.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        String stateFilter = state == null ? "" : state.trim();
        return villageRepository.search(q, stateFilter, PageRequest.of(0, size));
    }

    /** Lower-case, strip diacritics, keep only letters/digits/single spaces. Also removes LIKE wildcards. */
    public static String normalize(String s) {
        if (s == null) return "";
        String decomposed = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return decomposed.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Insert-only, idempotent import: a row is skipped if its LGD code already exists, or (when it has no
     * LGD code) if the same name+district+state already exists. Existing rows are never modified here -
     * refreshing figures belongs to the later feature-ETL step, not to a re-run of an import.
     *
     * <p>Streams the CSV instead of loading it fully (see {@link VillageCsvParser}), and flushes+clears the
     * persistence context after every batch. Without the clear, Hibernate keeps every saved {@code Village}
     * attached to this one transaction's session for its whole duration - for a national-scale file
     * (500k+ rows) that alone would grow to hundreds of MB of heap even though the Java-level batch list is
     * only ever {@value #IMPORT_BATCH} rows. The two dedup sets below stay in memory on purpose: they're
     * cheap (strings, not entities) next to one DB round-trip per row, which a 500k-row import over a
     * network connection cannot afford.
     */
    @Transactional
    public ImportSummary importCsv(Reader reader, String defaultSource) throws IOException {
        Set<String> knownLgd = new HashSet<>(villageRepository.findAllLgdCodes());
        Set<String> knownKeys = new HashSet<>(villageRepository.findAllNameKeys());

        List<Village> batch = new ArrayList<>(IMPORT_BATCH);
        AtomicInteger inserted = new AtomicInteger();
        AtomicInteger skippedExisting = new AtomicInteger();

        int skippedInvalid = VillageCsvParser.parse(reader, r -> {
            String key = nameKey(r.name(), r.district(), r.state());
            boolean exists = r.lgdCode() != null ? knownLgd.contains(r.lgdCode()) : knownKeys.contains(key);
            if (exists) {
                skippedExisting.incrementAndGet();
                return;
            }
            if (r.lgdCode() != null) knownLgd.add(r.lgdCode());
            knownKeys.add(key);

            batch.add(Village.builder()
                    .lgdCode(r.lgdCode())
                    .name(r.name())
                    .nameNormalized(normalize(r.name()))
                    .block(r.block())
                    .district(r.district())
                    .state(r.state())
                    .latitude(r.latitude())
                    .longitude(r.longitude())
                    .population2011(r.population2011())
                    .households2011(r.households2011())
                    .source(r.source() != null ? r.source() : defaultSource)
                    .build());
            inserted.incrementAndGet();
            if (batch.size() >= IMPORT_BATCH) {
                villageRepository.saveAll(batch);
                entityManager.flush();
                entityManager.clear();
                batch.clear();
            }
        });

        if (!batch.isEmpty()) {
            villageRepository.saveAll(batch);
            entityManager.flush();
            entityManager.clear();
        }
        return new ImportSummary(inserted.get(), skippedExisting.get(), skippedInvalid);
    }

    /**
     * Makes sure the hand-curated demo villages exist in the master table, so autocomplete can find them
     * before a real Census/LGD extract is loaded. Insert-only and idempotent (matched by name+district+state).
     * Census population/households stay null: demo_data.json only carries 5 km-radius figures, which are a
     * different quantity, and unknowns are never filled with guesses. lgd_code stays null until verified.
     *
     * @return how many villages were newly inserted
     */
    @Transactional
    public int seedDemoVillages(List<DemoData.VillageData> demoVillages) {
        if (demoVillages == null || demoVillages.isEmpty()) {
            return 0;
        }
        Set<String> knownKeys = new HashSet<>(villageRepository.findAllNameKeys());
        List<Village> toInsert = new ArrayList<>();
        for (DemoData.VillageData d : demoVillages) {
            if (d.getVillageName() == null || d.getDistrict() == null || d.getState() == null) {
                continue;
            }
            if (!knownKeys.add(nameKey(d.getVillageName(), d.getDistrict(), d.getState()))) {
                continue;
            }
            toInsert.add(Village.builder()
                    .name(d.getVillageName())
                    .nameNormalized(normalize(d.getVillageName()))
                    .block(d.getBlock())
                    .district(d.getDistrict())
                    .state(d.getState())
                    .latitude(d.getLatitude())
                    .longitude(d.getLongitude())
                    .source(DEMO_SEED_SOURCE)
                    .build());
        }
        if (!toInsert.isEmpty()) {
            villageRepository.saveAll(toInsert);
        }
        return toInsert.size();
    }

    /** Must stay in sync with the key built in {@code VillageRepository#findAllNameKeys}. */
    static String nameKey(String name, String district, String state) {
        return normalize(name) + "|" + district.toLowerCase(Locale.ROOT) + "|" + state.toLowerCase(Locale.ROOT);
    }
}
