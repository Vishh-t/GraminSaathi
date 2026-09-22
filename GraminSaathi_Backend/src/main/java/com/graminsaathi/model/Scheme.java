package com.graminsaathi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * One government scheme record from {@code data/all_schemes_enriched.json}.
 *
 * <p>Plain POJO (not a JPA entity) - the dataset is held in memory by
 * {@link com.graminsaathi.service.SchemeDataService}. Not to be confused with the legacy
 * {@code SchemesReference.Scheme} (the 4 loan-bracket schemes used by the analysis flow).
 *
 * <p>{@code benefit} and {@code eligibilityRules} stay as {@link JsonNode} because their shape
 * varies per scheme (JsonLogic rules, per-scheme enhancement blocks, composite components).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Scheme {

    private String schemeId;
    private String name;
    /** "central" or "state". */
    private String level;
    /** Null for central schemes. Full state name for state schemes. */
    private String state;
    private String ministry;
    private String implementingAgency;
    private String type;
    private String subType;

    /**
     * {subsidy_pct, max_subsidy_amount, on_loan_upto, special_category_enhancement,
     * calculation_type, calculation_type_confidence, calculation_components?}
     */
    private JsonNode benefit;

    /** Percent (e.g. 11 means 11%), NOT a fraction. Null when not applicable. */
    private Double interestRate;
    /**
     * True when {@code interestRate} is an estimate, not a figure from the dataset. Some bank loans have no
     * fixed rate (the bank sets it and the state pays back part of the interest), so the loader fills in
     * assumed bank rate minus that subvention from {@code data/scheme_rate_overrides.json}.
     * Null for every scheme that carries its own rate.
     */
    private Boolean rateEstimated;
    private String effectiveInterestRateNote;
    private LoanAmountRange loanAmountRange;
    private Double tenureYears;
    private Integer moratoriumMonths;

    /** JsonLogic, evaluated against {"applicant": {...}}. */
    private JsonNode eligibilityRules;
    private String eligibilityHuman;

    private List<String> targetApplicant;
    private List<String> targetSector;
    /** rural, urban or both. */
    private String ruralUrban;
    private Boolean greenfieldOnly;
    private Boolean existingUnitAllowed;
    private Integer minAge;
    private Integer maxAge;

    /** Each item: {name, mandatory?, mandatory_if?, source?}. */
    private List<JsonNode> documentsRequired;
    private List<String> applicationChannel;
    private List<String> applicationSteps;
    private String portalUrl;
    private String applicationFormUrl;
    private List<String> nodalAgencies;

    /** "open" for every scheme in the current dataset. */
    private String status;
    private String lastVerified;
    private List<String> sourceUrls;
    private List<String> flags;
    private List<String> pros;
    private List<String> cons;
    /** {helpline, email}. */
    private JsonNode contactInfo;
    private Integer processingTimeDays;

    @Data
    public static class LoanAmountRange {
        private Long min;
        /** Null = no upper limit. */
        private Long max;
    }
}
