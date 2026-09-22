package com.graminsaathi.dto.response;

import lombok.Data;

import java.util.List;

/**
 * One eligible scheme returned by {@code POST /api/schemes/match} - carries everything the
 * browse-page card AND its detail view need (handoff section 6 display rules: a ₹ figure for
 * flat/capped types, an effective interest rate for rate-reduction, a cover % for guarantees -
 * never summed into one total), so the frontend never has to make a second per-scheme call for
 * the detail view (status doc section 6, Phase C note: "no new backend fields needed").
 */
@Data
public class SchemeMatchItemResponse {

    private String schemeId;
    private String name;
    /** "central" or "state". */
    private String level;
    /** Null for central schemes. */
    private String state;
    private String ministry;
    private String implementingAgency;
    private String type;
    private String subType;

    /** Computed via {@link com.graminsaathi.service.BenefitCalculator} against the request's known amounts. */
    private BenefitResponse benefit;
    /** Which special_category_enhancement applied (if any) - null when none did. */
    private EnhancementResponse enhancementApplied;

    /** Percent, own or estimated. Null for non-loan schemes. */
    private Double interestRate;
    /** True when {@link #interestRate} was worked out (bank rate minus subvention), not stated by the scheme. */
    private Boolean rateEstimated;
    private Double tenureYears;
    private Integer moratoriumMonths;
    /** Null = no stated minimum/maximum. */
    private Long loanAmountMin;
    private Long loanAmountMax;

    private String eligibilityHuman;
    /** Document names only - the dataset's mandatory/mandatory_if/source detail is dropped for this view. */
    private List<String> documentsRequired;
    private List<String> applicationSteps;
    private List<String> applicationChannel;
    private String portalUrl;
    private String applicationFormUrl;
    private List<String> pros;
    private List<String> cons;
    private Integer processingTimeDays;

    /** Mirrors {@link com.graminsaathi.service.BenefitCalculator.BenefitResult} field-for-field. */
    @Data
    public static class BenefitResponse {
        private String calculationType;
        private Double amount;
        private Double effectiveInterestRatePct;
        private Double guaranteeCoverPct;
        private String displayText;
        /** Only populated for calculationType == "composite". */
        private List<BenefitResponse> components;
    }

    /** Mirrors {@link com.graminsaathi.service.SchemeMatchingService.EnhancementApplied} field-for-field. */
    @Data
    public static class EnhancementResponse {
        private String key;
        private Double subsidyPct;
        private String note;
    }
}
