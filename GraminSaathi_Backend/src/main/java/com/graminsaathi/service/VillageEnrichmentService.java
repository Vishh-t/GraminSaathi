package com.graminsaathi.service;

import com.graminsaathi.data.VillageCsvParser;
import com.graminsaathi.repository.VillageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Backfills columns onto EXISTING {@code villages} rows from an enrichment CSV, matched by identifiers
 * already on the row - never by re-running the insert-only importer, which can only add new rows and
 * would just skip everything that already exists (see {@link VillageService#importCsv}).
 *
 * <p>Same write pattern as {@link VillageFeaturesService}: plain JDBC batch UPDATE, no Hibernate in the
 * write path (avoids the merge()-per-row problem - see RECOMMENDATION_ENGINE_BUILD_LOG.md), committed per
 * batch so an interrupted run keeps its progress. Unmatched rows are counted, never guessed at.
 *
 * <p>Both backfills reuse {@link VillageCsvParser} - its {@code Row} already carries every column either
 * one needs (lgd_code, name/district/state, latitude/longitude/source, the four pc11_* codes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VillageEnrichmentService {

    private static final int BATCH = 1000;

    private static final String UPDATE_PC11_SQL = """
            UPDATE villages
            SET pc11_state_code = ?, pc11_district_code = ?, pc11_subdistrict_code = ?, pc11_village_code = ?
            WHERE id = ?
            """;

    private static final String UPDATE_COORDINATES_SQL = """
            UPDATE villages
            SET latitude = ?, longitude = ?, coordinates_source = ?, coordinates_as_of = ?
            WHERE id = ?
            """;

    private final VillageRepository villageRepository;
    private final JdbcTemplate jdbcTemplate;

    public record BackfillSummary(int matched, int unmatched, int skippedInvalid) {}

    /**
     * Backfills {@code pc11_*_code} onto existing villages from a CSV produced by
     * {@code village_processor.py} (must include the pc11_state_code/pc11_district_code/
     * pc11_subdistrict_code/pc11_village_code columns added for this purpose). Matches by lgd_code first,
     * else name+district+state. Rows that match neither are counted and skipped - never inserted, never
     * guessed at.
     */
    public BackfillSummary backfillPc11Codes(Reader reader) throws IOException {
        Map<String, Long> idByLgdCode = loadIdByLgdCode();
        Map<String, Long> idByNameKey = loadIdByNameKey();
        log.info("Village enrichment (pc11): loaded {} lgd-code keys and {} name keys for matching",
                idByLgdCode.size(), idByNameKey.size());

        List<Object[]> batch = new ArrayList<>(BATCH);
        int[] matched = {0};
        int[] unmatched = {0};

        int skippedInvalid = VillageCsvParser.parse(reader, r -> {
            if (r.pc11StateCode() == null && r.pc11DistrictCode() == null
                    && r.pc11SubdistrictCode() == null && r.pc11VillageCode() == null) {
                unmatched[0]++;
                return;
            }
            Long id = matchByLgdThenName(r, idByLgdCode, idByNameKey);
            if (id == null) {
                unmatched[0]++;
                return;
            }
            matched[0]++;
            batch.add(new Object[]{r.pc11StateCode(), r.pc11DistrictCode(), r.pc11SubdistrictCode(), r.pc11VillageCode(), id});
            if (batch.size() >= BATCH) {
                flush(UPDATE_PC11_SQL, batch);
            }
        });
        if (!batch.isEmpty()) {
            flush(UPDATE_PC11_SQL, batch);
        }

        log.info("Village enrichment (pc11): backfill complete - matched={}, unmatched={}, skippedInvalid={}",
                matched[0], unmatched[0], skippedInvalid);
        return new BackfillSummary(matched[0], unmatched[0], skippedInvalid);
    }

    /**
     * Backfills {@code latitude}/{@code longitude}/{@code coordinates_source} onto existing villages from
     * a CSV with the same columns {@link VillageCsvParser} understands, plus a {@code source} value per row
     * (e.g. "shrug_pc11" for the primary match, "osm_place" for the name-matched fallback - see
     * {@code match_shrug_coordinates.py}). {@code asOf} is one date for the whole run (like the population
     * pass takes one target year), since one extraction run has one "as of" date regardless of how many
     * rows it produces.
     *
     * <p>Matches by pc11 code first (most precise - phase 1's primary path via SHRUG), then lgd_code, then
     * name+district+state (the OSM-name fallback path, which has no pc11 code to match on).
     */
    public BackfillSummary backfillCoordinates(Reader reader, LocalDate asOf) throws IOException {
        Map<String, Long> idByPc11 = loadIdByPc11();
        Map<String, Long> idByLgdCode = loadIdByLgdCode();
        Map<String, Long> idByNameKey = loadIdByNameKey();
        log.info("Village enrichment (coordinates): loaded {} pc11 keys, {} lgd-code keys, {} name keys for matching",
                idByPc11.size(), idByLgdCode.size(), idByNameKey.size());
        Date sqlAsOf = Date.valueOf(asOf);

        List<Object[]> batch = new ArrayList<>(BATCH);
        int[] matched = {0};
        int[] unmatched = {0};

        int skippedInvalid = VillageCsvParser.parse(reader, r -> {
            if (r.latitude() == null || r.longitude() == null) {
                unmatched[0]++;
                return;
            }
            Long id = null;
            String pc11 = pc11Key(r.pc11StateCode(), r.pc11DistrictCode(), r.pc11SubdistrictCode(), r.pc11VillageCode());
            if (pc11 != null) {
                id = idByPc11.get(pc11);
            }
            if (id == null) {
                id = matchByLgdThenName(r, idByLgdCode, idByNameKey);
            }
            if (id == null) {
                unmatched[0]++;
                return;
            }
            matched[0]++;
            String source = r.source() != null ? r.source() : "unknown";
            batch.add(new Object[]{r.latitude(), r.longitude(), source, sqlAsOf, id});
            if (batch.size() >= BATCH) {
                flush(UPDATE_COORDINATES_SQL, batch);
            }
        });
        if (!batch.isEmpty()) {
            flush(UPDATE_COORDINATES_SQL, batch);
        }

        log.info("Village enrichment (coordinates): backfill complete - matched={}, unmatched={}, skippedInvalid={}",
                matched[0], unmatched[0], skippedInvalid);
        return new BackfillSummary(matched[0], unmatched[0], skippedInvalid);
    }

    private Long matchByLgdThenName(VillageCsvParser.Row r, Map<String, Long> idByLgdCode, Map<String, Long> idByNameKey) {
        Long id = r.lgdCode() != null ? idByLgdCode.get(r.lgdCode()) : null;
        if (id == null) {
            id = idByNameKey.get(nameKey(VillageService.normalize(r.name()), r.district(), r.state()));
        }
        return id;
    }

    private Map<String, Long> loadIdByLgdCode() {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : villageRepository.findIdsByLgdCode()) {
            map.put((String) row[1], (Long) row[0]);
        }
        return map;
    }

    private Map<String, Long> loadIdByNameKey() {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : villageRepository.findIdNameDistrictState()) {
            map.put(nameKey((String) row[1], (String) row[2], (String) row[3]), (Long) row[0]);
        }
        return map;
    }

    private Map<String, Long> loadIdByPc11() {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : villageRepository.findIdsByPc11Codes()) {
            String key = pc11Key((String) row[1], (String) row[2], (String) row[3], (String) row[4]);
            if (key != null) {
                map.put(key, (Long) row[0]);
            }
        }
        return map;
    }

    private void flush(String sql, List<Object[]> batch) {
        jdbcTemplate.batchUpdate(sql, batch);
        batch.clear();
    }

    /** Already-normalized name + district + state, same convention as {@code VillageService#nameKey}. */
    private static String nameKey(String normalizedName, String district, String state) {
        return normalizedName + "|" + district.toLowerCase(Locale.ROOT) + "|" + state.toLowerCase(Locale.ROOT);
    }

    /** Null if any of the four PC11 parts is missing - a partial code is not a usable key. */
    private static String pc11Key(String state, String district, String subdistrict, String village) {
        if (state == null || district == null || subdistrict == null || village == null) {
            return null;
        }
        return state + "-" + district + "-" + subdistrict + "-" + village;
    }
}
