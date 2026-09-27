package com.graminsaathi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A candidate business type the discovery/recommendation engine scores a village and an applicant
 * against - the production replacement for {@link com.graminsaathi.data.DemoData.BusinessCategoryData}.
 * See Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, "data-independent steps" pass.
 *
 * <p>Seeded from {@code data/business_categories_seed.json} (currently the same 4 categories as the old
 * demo file, carried over verbatim) by {@link com.graminsaathi.data.BusinessCategoryDataInitializer}.
 * Adding a category is a JSON edit + one startup run, never a code change.
 *
 * <p>Two groups of fields:
 * <ul>
 *   <li><b>Reference economics</b> (cost, revenue, working capital, ...) - unchanged from the old demo
 *       model, still category-average figures, not village-specific.</li>
 *   <li><b>Fit requirements</b> ({@link #fitRequirementRulesJson}, {@link #hardFilterRulesJson}) - NEW.
 *       Both are JsonLogic, evaluated against {@code {"applicant": {...}}} by the same
 *       {@link com.graminsaathi.service.SchemeEligibilityService} already used for scheme eligibility,
 *       so no new rule engine was needed. {@code hardFilterRulesJson} is pass/fail (e.g. "must have
 *       land"); {@code fitRequirementRulesJson} is a weighted list scored by
 *       {@link com.graminsaathi.service.PersonFitScoreService} (e.g. "owns a loom" contributes partial
 *       fit, doesn't disqualify). Both stay null-safe: a category with no rules yet passes everyone.</li>
 * </ul>
 *
 * <p><b>Not included on purpose:</b> anything keyed on competitor/coverage data - that lives on
 * {@link VillageFeatures} and isn't populated yet (see the build log's "what's left in step 2" section).
 * Amenities are a partial exception: {@link #requiresElectricity} IS read now, by
 * {@link com.graminsaathi.service.HardFilterService#evaluate} against
 * {@link VillageFeatures#getHasElectricity()} - but that village-side column is itself still unpopulated
 * for every real row, so the filter passes everyone until an amenities data source is wired in. Water and
 * road requirement flags don't exist here yet even though the matching {@code village_features} columns
 * do; add {@code requiresWaterSupply}/{@code requiresAllWeatherRoad} here when there's a reason to.
 */
@Entity
@Table(name = "business_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

    /** manufacturing, services, trading, agri_allied, technology, tourism, green_energy, logistics - same vocabulary as {@code ApplicantProfile.sector}. */
    private String sector;

    @Column(name = "sub_sector")
    private String subSector;

    // ---- Reference economics (category-average, not village-specific) -----------------------------

    @Column(name = "reference_project_cost")
    private Double referenceProjectCost;

    @Column(name = "reference_cost_range_low")
    private Double referenceCostRangeLow;

    @Column(name = "reference_cost_range_high")
    private Double referenceCostRangeHigh;

    @Column(name = "working_capital_months")
    private Integer workingCapitalMonths;

    @Column(name = "reference_monthly_revenue")
    private Double referenceMonthlyRevenue;

    @Column(name = "reference_monthly_operating_cost")
    private Double referenceMonthlyOperatingCost;

    @Column(name = "secondary_opportunity")
    private String secondaryOpportunity;

    @Column(name = "source_note", length = 1000)
    private String sourceNote;

    @Column(name = "single_buyer_dependency_risk")
    private String singleBuyerDependencyRisk;

    @Column(name = "primary_input_dependency_note", length = 1000)
    private String primaryInputDependencyNote;

    // ---- Fit / filter requirements (NEW - data-independent, usable with today's data) --------------

    /** Minimum land the applicant needs to hold for this category to make sense at all. Null = not land-dependent. */
    @Column(name = "min_land_holding_acres")
    private Double minLandHoldingAcres;

    /** Hard floor on the applicant's available margin capital as a fraction of {@link #referenceProjectCost}, e.g. 0.10 for the standard 10% margin. Null = use the caller's own default. */
    @Column(name = "min_margin_capital_fraction")
    private Double minMarginCapitalFraction;

    /**
     * JsonLogic, pass/fail, evaluated against {@code applicant.*} (same vocabulary as scheme eligibility
     * rules - see {@link com.graminsaathi.dto.request.ApplicantProfile}). Null/blank = no hard requirement,
     * everyone passes. Stored as TEXT (like {@link User#getApplicantProfileJson()}) rather than a jsonb
     * column type, so no extra Hibernate type dependency is needed for a field this small.
     */
    @Column(name = "hard_filter_rules_json", columnDefinition = "TEXT")
    private String hardFilterRulesJson;

    /**
     * JSON array of {@code {"description": "...", "rule": <JsonLogic>, "weight": <0-1>}} objects, scored
     * (not pass/fail) by {@link com.graminsaathi.service.PersonFitScoreService}. Weights should sum to 1.0
     * across a category's rules; the service normalizes if they don't. Null/blank = the fit score falls
     * back to capital adequacy alone.
     */
    @Column(name = "fit_requirement_rules_json", columnDefinition = "TEXT")
    private String fitRequirementRulesJson;

    /**
     * Declares a future amenity dependency; NOT read by anything yet (see class Javadoc). Recorded now so
     * the category data doesn't need a second editing pass once {@code village_features} gains an
     * amenities column group.
     */
    @Column(name = "requires_electricity")
    private Boolean requiresElectricity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
