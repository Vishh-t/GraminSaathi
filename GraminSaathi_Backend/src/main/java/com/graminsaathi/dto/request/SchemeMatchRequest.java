package com.graminsaathi.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code POST /api/schemes/match} (status doc section 6, Phase B): an applicant profile
 * to run every scheme's eligibility rules against, optional browse filters the frontend's filter
 * controls apply on top, and optional known amounts (project cost / loan amount) so
 * {@link com.graminsaathi.service.BenefitCalculator} can compute a real ₹/rate figure instead of
 * a formula description. Every field is optional - an empty body still returns the schemes with
 * no eligibility gating (i.e. only the always-true rules) and no computed amounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemeMatchRequest {

    private ApplicantProfile applicant;

    // ---- Browse filters (SchemeMatchingService.MatchFilters) ------------------------------
    /** "central" or "state". */
    private String level;
    private String sector;
    private String state;
    private String calculationType;

    // ---- Known amounts (BenefitCalculator.BenefitInput) -----------------------------------
    private Double actualCost;
    private Double loanAmount;
}
