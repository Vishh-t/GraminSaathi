package com.graminsaathi.service;

import com.graminsaathi.model.Village;
import com.graminsaathi.model.VillageFeatures;
import com.graminsaathi.repository.VillageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the {@code village_features} table (see Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, step 2).
 * Step 2a only: population/households group, via {@link PopulationProjector}. Competitor counts (2b) and
 * OSM coverage confidence (2c) are separate, later passes over the same table - each is its own nullable
 * column group so re-running one never disturbs the others (see {@link VillageFeatures}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VillageFeaturesService {

    private static final int BATCH = 1000;
    private static final int PROGRESS_EVERY_BATCHES = 10;

    /**
     * Upsert of the population column-group only. ON CONFLICT means re-runs overwrite just these columns
     * and never touch the 2b/2c columns. Written as plain SQL (not {@code repository.saveAll}) because
     * {@code village_features} has an assigned id: Spring Data treats such an entity as "not new" and
     * calls {@code merge()}, which fires one SELECT per row before every insert.
     */
    private static final String UPSERT_SQL = """
            INSERT INTO village_features (
                village_id, population_projected, households_projected, projection_year,
                growth_rate_decadal_pct, growth_rate_source, avg_household_size,
                population_source, population_as_of, features_updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (village_id) DO UPDATE SET
                population_projected    = EXCLUDED.population_projected,
                households_projected    = EXCLUDED.households_projected,
                projection_year         = EXCLUDED.projection_year,
                growth_rate_decadal_pct = EXCLUDED.growth_rate_decadal_pct,
                growth_rate_source      = EXCLUDED.growth_rate_source,
                avg_household_size      = EXCLUDED.avg_household_size,
                population_source       = EXCLUDED.population_source,
                population_as_of        = EXCLUDED.population_as_of,
                features_updated_at     = EXCLUDED.features_updated_at
            """;

    private final VillageRepository villageRepository;
    private final PopulationProjector populationProjector;
    private final JdbcTemplate jdbcTemplate;

    public record PopulationRunSummary(int processed, int projected, int skippedNoPopulation) {}

    /**
     * Pages through every {@link Village}, projects population/households to {@code year}, and
     * upserts the population column-group of that village's {@link VillageFeatures} row (villageId is
     * the shared key, so re-running this - e.g. with a later year, or after adding a state override CSV
     * - overwrites only the population fields and never touches step 2b/2c columns already in place).
     * A village with no Census 2011 population gets no row from this pass; a later pass (e.g. once 2b/2c
     * exist) may still create one for it.
     *
     * <p>Deliberately NOT one big transaction and NOT going through Hibernate for the writes: each batch
     * is a single JDBC batch upsert that commits on its own, so nothing accumulates in a session or in
     * one giant transaction, and an interrupted run keeps everything already written (re-running is safe).
     * Reads use keyset (seek) pagination on {@code id}, so every page is a straight primary-key seek.
     */
    public PopulationRunSummary runPopulationProjection(int year) {
        long totalVillages = villageRepository.count();
        int totalBatches = (int) Math.ceil(totalVillages / (double) BATCH);
        Instant startedAt = Instant.now();
        log.info("Village features: starting population pass for year {} over {} villages ({} batches of {})",
                year, totalVillages, totalBatches, BATCH);

        refreshPlannerStats();

        int processed = 0;
        int projected = 0;
        int skipped = 0;
        int batchNum = 0;
        long lastId = 0L;
        long readNanos = 0L;
        long writeNanos = 0L;
        LocalDateTime now = LocalDateTime.now();
        PageRequest limitOnly = PageRequest.of(0, BATCH);

        while (true) {
            long t0 = System.nanoTime();
            List<Village> villages = villageRepository.findByIdGreaterThanOrderByIdAsc(lastId, limitOnly);
            readNanos += System.nanoTime() - t0;
            if (villages.isEmpty()) {
                break;
            }

            List<VillageFeatures> batch = new ArrayList<>(villages.size());
            for (Village v : villages) {
                processed++;
                PopulationProjector.Projection p =
                        populationProjector.project(v.getPopulation2011(), v.getHouseholds2011(), v.getState(), year);
                if (p == null) {
                    skipped++;
                    continue;
                }
                batch.add(VillageFeatures.builder()
                        .villageId(v.getId())
                        .populationProjected(p.population())
                        .householdsProjected(p.households())
                        .projectionYear(p.projectionYear())
                        .growthRateDecadalPct(p.decadalGrowthPct())
                        .growthRateSource(p.growthSource())
                        .avgHouseholdSize(p.avgHouseholdSize())
                        .populationSource(v.getSource())
                        .populationAsOf(PopulationProjector.CENSUS_REFERENCE_DATE)
                        .featuresUpdatedAt(now)
                        .build());
                projected++;
            }
            if (!batch.isEmpty()) {
                long t1 = System.nanoTime();
                upsert(batch);
                writeNanos += System.nanoTime() - t1;
            }

            lastId = villages.get(villages.size() - 1).getId();
            batchNum++;
            boolean isLastBatch = villages.size() < BATCH;
            if (batchNum % PROGRESS_EVERY_BATCHES == 0 || isLastBatch) {
                double pct = totalVillages == 0 ? 100.0 : 100.0 * processed / totalVillages;
                Duration elapsed = Duration.between(startedAt, Instant.now());
                log.info("Village features: population pass progress - {}/{} villages ({}%), batch {}/{}, "
                                + "elapsed {}s (read {}s, write {}s)",
                        processed, totalVillages, String.format("%.1f", pct), batchNum, totalBatches,
                        elapsed.toSeconds(), readNanos / 1_000_000_000L, writeNanos / 1_000_000_000L);
            }

            if (isLastBatch) {
                break;
            }
        }

        log.info("Village features (population, year={}): processed={}, projected={}, skippedNoPopulation={}",
                year, processed, projected, skipped);
        return new PopulationRunSummary(processed, projected, skipped);
    }

    void upsert(List<VillageFeatures> rows) {
        jdbcTemplate.batchUpdate(UPSERT_SQL, rows, rows.size(), (ps, f) -> {
            ps.setObject(1, f.getVillageId());
            ps.setObject(2, f.getPopulationProjected());
            ps.setObject(3, f.getHouseholdsProjected());
            ps.setObject(4, f.getProjectionYear());
            ps.setObject(5, f.getGrowthRateDecadalPct());
            ps.setObject(6, f.getGrowthRateSource());
            ps.setObject(7, f.getAvgHouseholdSize());
            ps.setObject(8, f.getPopulationSource());
            ps.setObject(9, f.getPopulationAsOf());
            ps.setObject(10, f.getFeaturesUpdatedAt());
        });
    }

    /**
     * The villages table was just bulk-imported, so Postgres may not have planner statistics for it yet
     * (autovacuum analyzes lazily). Stale stats can turn the {@code id > ? ORDER BY id LIMIT n} seek into
     * a much worse plan. Best-effort: a failure here is logged and ignored.
     */
    private void refreshPlannerStats() {
        try {
            jdbcTemplate.execute("ANALYZE villages");
        } catch (Exception e) {
            log.warn("Village features: ANALYZE villages failed (continuing)", e);
        }
    }
}
