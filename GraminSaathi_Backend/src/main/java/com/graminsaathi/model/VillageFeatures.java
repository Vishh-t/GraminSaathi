package com.graminsaathi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Precomputed, per-village scoring inputs - one row per {@link Village}, rebuilt by the offline ETL
 * (see Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, step 2) so the API never has to compute or
 * fetch these at request time.
 *
 * <p>Keyed by {@code villageId} (a plain column, deliberately NOT a JPA association, so a lazy proxy of
 * {@link Village} can never leak into an API response). Every group of fields carries its own source and
 * as-of date, so anything shown to a user can be labelled "estimated, from X, as of Y".
 *
 * <p>Step 2a fills the population group. The competitor-counts (2b) and coverage-confidence (2c) groups
 * below exist as columns already (added in the "data-independent steps" pass, see the build log) but stay
 * null until their ETL jobs run - both are blocked on {@link Village#getLatitude()}/{@link Village#getLongitude()},
 * which are still null for every real (non-demo) row. Nothing currently writes to them; the read side
 * (step 3 demand-supply scoring) is written to treat null here as "no data yet", not zero.
 *
 * <p>The amenities group (power/water/roads) exists as columns too (added this session) and is read by
 * {@link com.graminsaathi.service.HardFilterService#evaluate} for electricity today, but nothing writes to
 * it yet - no data source is wired in. Same null-safe convention: a village with no amenities row never
 * fails a filter, it just passes unfiltered until real data lands.
 */
@Entity
@Table(
        name = "village_features",
        indexes = {
                @Index(name = "idx_village_features_population", columnList = "population_projected")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VillageFeatures {

    @Id
    @Column(name = "village_id")
    private Long villageId;

    // ---- Population group (step 2a) ---------------------------------------------------------------

    /** Census 2011 population projected to {@code projectionYear}. An ESTIMATE - always label it so. */
    @Column(name = "population_projected")
    private Integer populationProjected;

    /** Census 2011 households scaled by the same factor (assumes household size stays constant). */
    @Column(name = "households_projected")
    private Integer householdsProjected;

    @Column(name = "projection_year")
    private Integer projectionYear;

    /** Decadal growth rate (percent) used for the projection - national default or a state override. */
    @Column(name = "growth_rate_decadal_pct")
    private Double growthRateDecadalPct;

    @Column(name = "growth_rate_source", length = 255)
    private String growthRateSource;

    /** Census 2011 people per household (from the raw 2011 figures, not projected). Null if unknown. */
    @Column(name = "avg_household_size")
    private Double avgHouseholdSize;

    /** Where the underlying Census figures came from (copied from {@link Village#getSource()}). */
    @Column(name = "population_source", length = 255)
    private String populationSource;

    /** Reference date of the underlying figures (Census 2011 reference date, 1 March 2011). */
    @Column(name = "population_as_of")
    private LocalDate populationAsOf;

    // ---- Competitor-counts group (step 2b - NOT populated yet, see class javadoc) ------------------

    /**
     * JSON map of {@code {category_name: count}} within the scoring radius. TEXT rather than a fixed
     * column per category since the category list is open-ended (see {@link BusinessCategory}) and this
     * avoids a migration every time one is added. Null until the 2b ETL job runs.
     */
    @Column(name = "competitor_counts_json", columnDefinition = "TEXT")
    private String competitorCountsJson;

    @Column(name = "competitor_counts_source", length = 255)
    private String competitorCountsSource;

    @Column(name = "competitor_counts_as_of")
    private LocalDate competitorCountsAsOf;

    // ---- Coverage-confidence group (step 2c - NOT populated yet, see class javadoc) ----------------

    /** OSM building count within the scoring radius, vs. an external population-based building estimate for the same radius. */
    @Column(name = "osm_building_count")
    private Integer osmBuildingCount;

    @Column(name = "expected_building_count")
    private Integer expectedBuildingCount;

    /** osmBuildingCount / expectedBuildingCount, 0-1+. Low values mean OSM likely undercounts this area - widen uncertainty, never raise confidence past what this implies. */
    @Column(name = "coverage_confidence_ratio")
    private Double coverageConfidenceRatio;

    /** low / medium / high, derived from {@link #coverageConfidenceRatio} by whatever thresholds the 2c job settles on. */
    @Column(name = "coverage_confidence_label", length = 16)
    private String coverageConfidenceLabel;

    @Column(name = "coverage_confidence_source", length = 255)
    private String coverageConfidenceSource;

    @Column(name = "coverage_confidence_as_of")
    private LocalDate coverageConfidenceAsOf;

    // ---- Amenities group (power/water/roads - step 4 hard-filter input, NOT populated yet) --------

    /** Whether the village has electricity access. Null = unknown - a hard filter must treat this as "pass", never "fail". */
    @Column(name = "has_electricity")
    private Boolean hasElectricity;

    /** Whether the village has piped/protected water supply. Not read by any filter yet - no category field declares a water requirement (see {@link BusinessCategory}). */
    @Column(name = "has_piped_water_supply")
    private Boolean hasPipedWaterSupply;

    /** Whether the village has all-weather road connectivity. Not read by any filter yet, same reason as water. */
    @Column(name = "has_all_weather_road")
    private Boolean hasAllWeatherRoad;

    @Column(name = "amenities_source", length = 255)
    private String amenitiesSource;

    @Column(name = "amenities_as_of")
    private LocalDate amenitiesAsOf;

    @Column(name = "features_updated_at", nullable = false)
    private LocalDateTime featuresUpdatedAt;
}
