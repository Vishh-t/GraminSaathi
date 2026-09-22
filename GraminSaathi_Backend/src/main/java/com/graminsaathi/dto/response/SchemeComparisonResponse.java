package com.graminsaathi.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SchemeComparisonResponse {
    /** Stable id from the dataset (e.g. "central-mudra-kishor") - not shown to the user, only for a future "learn more" link/key. */
    private String schemeId;
    private String schemeName;
    /** Percent (6.5 = 6.5%). An estimate when {@link #rateEstimated} is true. */
    private Double interestRate;
    private Integer tenureYears;
    private Integer moratoriumMonths;
    private Double emi;
    private String agency;
    private String subsidyNote;
    /**
     * Without @JsonProperty, Lombok's isPrimary() getter makes Jackson emit the JSON key as
     * "primary" (it strips the "is" prefix from boolean getters) - confirmed 2026-09-22 by
     * ReferenceDataControllerTest. Pinned here so the JSON key matches the field name the
     * frontend actually expects (isPrimary), instead of relying on the frontend to know this.
     */
    @JsonProperty("isPrimary")
    private boolean isPrimary;

    /** Everything repaid to the bank over the loan (instalments only), before any subsidy. Null on the plain scheme list. */
    private Double totalRepayment;
    /** Capital / margin-money subsidy the scheme gives on this loan; null when there is none we can count. */
    private Double estimatedSubsidy;
    /** totalRepayment minus estimatedSubsidy: what the options are ranked by. Null on the plain scheme list. */
    private Double netCost;
    /** True when the rate was worked out (bank rate minus the state's interest subvention), not stated by the scheme. */
    private Boolean rateEstimated;
    /** True when the EMI fits within the business's monthly net operating income (DSCR of at least 1). */
    private Boolean affordable;

    /** Plain-language eligibility summary from the dataset (e.g. "Rural women, SHG members, age 18-45"). Null if the dataset has none. */
    private String eligibilityHuman;
    /** Document names only (the dataset's mandatory/mandatory_if/source detail is dropped for this summary view). */
    private List<String> documentsRequired;
    private List<String> applicationSteps;
    /** Where to actually apply. Null if the dataset has no URL for this scheme. */
    private String portalUrl;
}
