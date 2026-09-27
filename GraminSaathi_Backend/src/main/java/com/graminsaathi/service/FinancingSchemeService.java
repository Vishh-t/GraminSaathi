package com.graminsaathi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.data.DemoData;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.model.Scheme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Finds real government financing schemes (from the schemes dataset) that can supply loan terms
 * - interest rate, tenure, moratorium - for the business analysis flow.
 *
 * <p>The analysis request always carries a village, a business category and margin capital, and may
 * carry applicant details (caste category, gender, income, ...). {@link #resolveApplicant} combines
 * them: a <em>baseline</em> applicant built from the village and category, overlaid with whatever the
 * client supplied. Schemes that need a fact nobody supplied (caste, gender, income, education, ...) are
 * not eligible and are left out; they show up once those details are provided.
 */
@Service
@RequiredArgsConstructor
public class FinancingSchemeService {

    /** Age assumed for the baseline applicant - most schemes gate on a minimum (and sometimes maximum) age. */
    static final int BASELINE_AGE = 30;

    /** Facts about the business itself: the village and the business category decide these, not the applicant. */
    private static final List<String> BUSINESS_FACT_KEYS = List.of("state", "sector", "sub_sector");

    private final SchemeDataService schemeDataService;
    private final SchemeEligibilityService eligibilityService;
    private final ObjectMapper objectMapper;

    /**
     * Applicant facts that the analysis flow already knows: the village's state, that it is rural,
     * a new (greenfield) business, and the business category's sector.
     */
    public ApplicantProfile baselineProfile(DemoData.VillageData village, DemoData.BusinessCategoryData category) {
        return baselineProfile(village != null ? village.getState() : null, category);
    }

    /**
     * Same as {@link #baselineProfile(DemoData.VillageData, DemoData.BusinessCategoryData)} but for a
     * caller that already has a real {@code Village} entity (not the demo-data shape) - added 2026-09-27
     * so {@link FinancialCalculatorService} can pass the real village's state without needing a
     * DemoDataLoader lookup, which returns null for any village outside the ~4-village demo set.
     */
    public ApplicantProfile baselineProfile(String villageState, DemoData.BusinessCategoryData category) {
        return ApplicantProfile.builder()
                .age(BASELINE_AGE)
                .state(villageState)
                .ruralUrban("rural")
                .businessStage("greenfield")
                .sector(category != null ? category.getSector() : null)
                .subSector(category != null ? category.getSubSector() : null)
                .build();
    }

    /**
     * The applicant to look schemes up for: the baseline overlaid with the client-supplied facts
     * (anything the client sent wins over a baseline default such as age 30 or a greenfield business).
     * The exceptions are the business facts - state, sector, sub-sector - which the village and category
     * decide whenever they know them; the client only fills those in when the baseline has none.
     */
    public ApplicantProfile resolveApplicant(DemoData.VillageData village,
                                             DemoData.BusinessCategoryData category,
                                             ApplicantProfile provided) {
        return resolveApplicant(village != null ? village.getState() : null, category, provided);
    }

    /** Same as {@link #resolveApplicant(DemoData.VillageData, DemoData.BusinessCategoryData, ApplicantProfile)} but for a real village's state string - see {@link #baselineProfile(String, DemoData.BusinessCategoryData)}. */
    public ApplicantProfile resolveApplicant(String villageState,
                                             DemoData.BusinessCategoryData category,
                                             ApplicantProfile provided) {
        ApplicantProfile baseline = baselineProfile(villageState, category);
        if (provided == null) {
            return baseline;
        }

        Map<String, Object> baselineFacts = toMap(baseline);
        Map<String, Object> merged = toMap(baseline);
        toMap(provided).forEach((key, value) -> {
            if (value != null) {
                merged.put(key, value);
            }
        });
        for (String key : BUSINESS_FACT_KEYS) {
            if (baselineFacts.get(key) != null) {
                merged.put(key, baselineFacts.get(key));
            }
        }
        return objectMapper.convertValue(merged, ApplicantProfile.class);
    }

    /** Every scheme in the dataset that carries an interest rate and a repayment tenure of at least a year. */
    public List<Scheme> getFinancingSchemes() {
        return schemeDataService.getAllSchemes().stream()
                .filter(FinancingSchemeService::hasRepaymentTerms)
                .toList();
    }

    /**
     * Financing schemes this applicant is eligible for and that can lend {@code loanAmount},
     * in dataset order (ranking is the caller's job).
     */
    public List<Scheme> findFinancingCandidates(double loanAmount, ApplicantProfile applicant) {
        return getFinancingSchemes().stream()
                .filter(s -> availableInState(s, applicant.getState()))
                .filter(s -> matchesArea(s, applicant.getRuralUrban()))
                .filter(s -> matchesSector(s, applicant.getSector()))
                .filter(s -> loanWithinRange(s, loanAmount))
                .filter(s -> eligibilityService.isEligible(s, applicant))
                .toList();
    }

    private Map<String, Object> toMap(ApplicantProfile profile) {
        return objectMapper.convertValue(profile, new TypeReference<Map<String, Object>>() {});
    }

    /** Fractional tenures such as 2.5 years are fine; anything shorter than a year is not a term loan. */
    private static boolean hasRepaymentTerms(Scheme s) {
        Double tenure = s.getTenureYears();
        return s.getInterestRate() != null && tenure != null && tenure >= 1;
    }

    /** Central schemes are open everywhere; state schemes only in their own state. */
    private static boolean availableInState(Scheme s, String state) {
        return s.getState() == null || s.getState().equalsIgnoreCase(state);
    }

    private static boolean matchesArea(Scheme s, String ruralUrban) {
        String area = s.getRuralUrban();
        return ruralUrban == null || area == null || "both".equalsIgnoreCase(area) || area.equalsIgnoreCase(ruralUrban);
    }

    private static boolean matchesSector(Scheme s, String sector) {
        List<String> sectors = s.getTargetSector();
        return sector == null || sectors == null || sectors.isEmpty() || sectors.contains(sector);
    }

    /** The dataset writes a max of 0 (87 schemes) for "no stated limit", not for "lends nothing". */
    private static boolean loanWithinRange(Scheme s, double loanAmount) {
        Scheme.LoanAmountRange range = s.getLoanAmountRange();
        if (range == null) {
            return true;
        }
        boolean aboveMin = range.getMin() == null || loanAmount >= range.getMin();
        boolean belowMax = range.getMax() == null || range.getMax() <= 0 || loanAmount <= range.getMax();
        return aboveMin && belowMax;
    }
}
