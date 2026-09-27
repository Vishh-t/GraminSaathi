package com.graminsaathi.service;

import com.graminsaathi.data.MarketSignalsCsvParser;
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

    /**
     * Upsert of the competitor-counts (2b) and coverage-confidence (2c) column groups only on
     * {@code village_features} - same ON CONFLICT shape as {@link VillageFeaturesService}'s population
     * upsert, so this never disturbs the population or amenities groups on a row that already exists.
     *
     * <p>{@code compute_village_market_signals.py} and {@code compute_coverage_confidence.py} are two
     * SEPARATE Python passes, run at different times, whose CSVs carry different, non-overlapping subsets
     * of these columns (the first has competitor counts + osm_building_count; the second has
     * expected_building_count/ratio/label, computed FROM the first pass's osm_building_count but written
     * to a different file). Every SET clause below is therefore {@code COALESCE(EXCLUDED.x, village_features.x)}
     * rather than a bare {@code EXCLUDED.x} - a column absent from whichever CSV is being backfilled right
     * now (parsed as null by {@link com.graminsaathi.data.MarketSignalsCsvParser}) falls back to whatever
     * the row already has, instead of overwriting an earlier pass's value with null. This also means running
     * either script's output twice, in either order, is safe and idempotent - never a data-loss risk.
     */
    private static final String UPSERT_MARKET_SIGNALS_SQL = """
            INSERT INTO village_features (
                village_id, competitor_counts_json, competitor_counts_source, competitor_counts_as_of,
                osm_building_count, expected_building_count, coverage_confidence_ratio, coverage_confidence_label,
                coverage_confidence_source, coverage_confidence_as_of, features_updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (village_id) DO UPDATE SET
                competitor_counts_json     = COALESCE(EXCLUDED.competitor_counts_json, village_features.competitor_counts_json),
                competitor_counts_source   = COALESCE(EXCLUDED.competitor_counts_source, village_features.competitor_counts_source),
                competitor_counts_as_of    = COALESCE(EXCLUDED.competitor_counts_as_of, village_features.competitor_counts_as_of),
                osm_building_count         = COALESCE(EXCLUDED.osm_building_count, village_features.osm_building_count),
                expected_building_count    = COALESCE(EXCLUDED.expected_building_count, village_features.expected_building_count),
                coverage_confidence_ratio  = COALESCE(EXCLUDED.coverage_confidence_ratio, village_features.coverage_confidence_ratio),
                coverage_confidence_label  = COALESCE(EXCLUDED.coverage_confidence_label, village_features.coverage_confidence_label),
                coverage_confidence_source = COALESCE(EXCLUDED.coverage_confidence_source, village_features.coverage_confidence_source),
                coverage_confidence_as_of  = COALESCE(EXCLUDED.coverage_confidence_as_of, village_features.coverage_confidence_as_of),
                features_updated_at        = EXCLUDED.features_updated_at
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

    /**
     * Backfills the competitor-counts (2b) and osm-building-count (2c-partial) column groups onto
     * {@code village_features} from {@code compute_village_market_signals.py}'s output. Matched by
     * {@code lgd_code} only (that script's output always carries one, since it's itself downstream of
     * {@code resolve_osm_places.py}'s coordinates match, which requires lgd_code to have succeeded) -
     * no lgd-then-name fallback here, unlike {@link #backfillCoordinates}, since a row with no lgd_code
     * is unusable as a join key regardless.
     *
     * <p>Writes straight to {@code village_features}, not {@code villages} - this is a {@code village_id}
     * upsert (see {@link VillageFeaturesService#upsert}), not a {@code villages} row UPDATE, since
     * competitor counts/building counts are scoring inputs that live on the features table by design.
     */
    public BackfillSummary backfillMarketSignals(Reader reader) throws IOException {
        Map<String, Long> idByLgdCode = loadIdByLgdCode();
        log.info("Village enrichment (market signals): loaded {} lgd-code keys for matching", idByLgdCode.size());

        List<Object[]> batch = new ArrayList<>(BATCH);
        int[] matched = {0};
        int[] unmatched = {0};
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        int skippedInvalid = MarketSignalsCsvParser.parse(reader, r -> {
            Long id = idByLgdCode.get(r.lgdCode());
            if (id == null) {
                unmatched[0]++;
                return;
            }
            matched[0]++;
            batch.add(new Object[]{
                    id, r.competitorCountsJson(), r.competitorCountsSource(),
                    r.competitorCountsAsOf() != null ? Date.valueOf(r.competitorCountsAsOf()) : null,
                    r.osmBuildingCount(), r.expectedBuildingCount(), r.coverageConfidenceRatio(), r.coverageConfidenceLabel(),
                    r.coverageConfidenceSource(),
                    r.coverageConfidenceAsOf() != null ? Date.valueOf(r.coverageConfidenceAsOf()) : null,
                    java.sql.Timestamp.valueOf(now)
            });
            if (batch.size() >= BATCH) {
                flush(UPSERT_MARKET_SIGNALS_SQL, batch);
            }
        });
        if (!batch.isEmpty()) {
            flush(UPSERT_MARKET_SIGNALS_SQL, batch);
        }

        log.info("Village enrichment (market signals): backfill complete - matched={}, unmatched={}, skippedInvalid={}",
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
