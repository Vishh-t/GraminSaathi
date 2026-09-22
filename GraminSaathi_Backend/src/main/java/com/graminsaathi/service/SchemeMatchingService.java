package com.graminsaathi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.model.Scheme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Finds ALL schemes (not just the 47 loan-type ones {@link FinancingSchemeService} handles) an
 * applicant is eligible for, across every {@code calculation_type} - the backbone of the
 * full-schemes-browse feature (status doc section 6, Phase B).
 *
 * <p>Deliberately separate from {@link FinancingSchemeService} rather than extending it: that service's
 * pre-filters were written and tested (`FinancingSchemeServiceTest`) around loan-shaped schemes only
 * (interest rate + tenure required upfront, a loan amount always known). This service runs against the
 * other 151 grant/subsidy/guarantee schemes too, where neither of those is true and a project cost or
 * loan amount is often not known yet (e.g. browsing before entering one) - see {@link
 * com.graminsaathi.service.BenefitCalculator.BenefitInput}'s own null-safety for the same reason. The
 * two share the same handful of one-line filter rules on purpose (state/area/sector availability) -
 * kept duplicated rather than extracted, since {@link FinancingSchemeService} is settled/tested code
 * this feature shouldn't risk touching without a separate go-ahead (see the age/business-stage note
 * below for why that matters).
 */
@Service
@RequiredArgsConstructor
public class SchemeMatchingService {

    private final SchemeDataService schemeDataService;
    private final SchemeEligibilityService eligibilityService;
    private final BenefitCalculator benefitCalculator;

    /** Optional narrowing the caller (the browse page's filter controls) can apply on top of eligibility. */
    public record MatchFilters(String level, String sector, String state, String calculationType) {
        public static final MatchFilters NONE = new MatchFilters(null, null, null, null);
    }

    /** Which special-category enhancement (if any) applied, and its own subsidy_pct/note for display. */
    public record EnhancementApplied(String key, Double subsidyPct, String note) {}

    /** One eligible scheme with its resolved enhancement (if any) and computed benefit. */
    public record SchemeMatch(Scheme scheme, EnhancementApplied enhancementApplied, BenefitCalculator.BenefitResult benefit) {}

    /**
     * Every scheme this applicant is eligible for, filtered and enriched but NOT ranked - see
     * {@link #findEligibleRanked} for the ordered version the browse page actually wants.
     */
    public List<SchemeMatch> findEligible(ApplicantProfile applicant, MatchFilters filters, BenefitCalculator.BenefitInput input) {
        ApplicantProfile safeApplicant = applicant != null ? applicant : new ApplicantProfile();
        MatchFilters f = filters != null ? filters : MatchFilters.NONE;
        BenefitCalculator.BenefitInput safeInput = input != null ? input : BenefitCalculator.BenefitInput.EMPTY;

        return schemeDataService.getAllSchemes().stream()
                .filter(SchemeMatchingService::isOpen)
                .filter(s -> matchesLevelFilter(s, f.level()))
                .filter(s -> matchesSectorFilter(s, f.sector()))
                .filter(s -> matchesStateFilter(s, f.state()))
                .filter(s -> matchesCalculationTypeFilter(s, f.calculationType()))
                .filter(s -> availableInState(s, safeApplicant.getState()))
                .filter(s -> matchesArea(s, safeApplicant.getRuralUrban()))
                .filter(s -> matchesSector(s, safeApplicant.getSector()))
                .filter(s -> matchesTargetApplicant(s, safeApplicant))
                .filter(s -> matchesAge(s, safeApplicant.getAge()))
                .filter(s -> matchesBusinessStage(s, safeApplicant.getBusinessStage()))
                .filter(s -> loanWithinRange(s, safeInput.loanAmount()))
                .filter(s -> eligibilityService.isEligible(s, safeApplicant))
                .map(s -> toMatch(s, safeApplicant, safeInput))
                .toList();
    }

    /**
     * {@link #findEligible} sorted for display. Benefit ₹ figures aren't comparable across
     * calculation_types (a flat grant vs. a guarantee-cover % vs. an interest-rate cut - status doc
     * section 6's "ranking design decision"), so this groups by kind rather than forcing one global
     * number: computed-₹ matches first (highest amount first - a real chance to see cash), then
     * interest-rate cuts (lowest effective rate first), then guarantee covers (highest cover % first),
     * then everything else (equity/text-only/composite-with-no-amount) in dataset order. The frontend
     * still does its own "best 5-7 + see more" slicing on top of this.
     */
    public List<SchemeMatch> findEligibleRanked(ApplicantProfile applicant, MatchFilters filters, BenefitCalculator.BenefitInput input) {
        return findEligible(applicant, filters, input).stream()
                .sorted(Comparator.comparingInt(SchemeMatchingService::rankTier)
                        .thenComparing(SchemeMatchingService::rankValue, Comparator.reverseOrder()))
                .toList();
    }

    private SchemeMatch toMatch(Scheme scheme, ApplicantProfile applicant, BenefitCalculator.BenefitInput input) {
        EnhancementApplied enhancement = resolveEnhancement(scheme, applicant);
        BenefitCalculator.BenefitResult benefit = benefitCalculator.calculate(scheme, input);
        return new SchemeMatch(scheme, enhancement, benefit);
    }

    /**
     * Which special_category_enhancement key (if any) applies, evaluated the same way as the main
     * eligibility rule. Verified against every scheme in the dataset before writing this: PMEGP is the
     * ONLY scheme with more than one high-confidence enhancement condition, and its 3 conditions
     * (general_rural / special_category_urban / special_category_rural) are already mutually exclusive
     * on disk - the "true for everyone" data bug the handoff doc flagged (finding 6) is NOT present in
     * the current dataset, so no data fix was needed here. Kept defensive anyway: if more than one
     * condition somehow evaluates true for some future scheme, the highest subsidy_pct wins.
     */
    private EnhancementApplied resolveEnhancement(Scheme scheme, ApplicantProfile applicant) {
        JsonNode benefit = scheme.getBenefit();
        if (benefit == null) return null;
        JsonNode enhancements = benefit.path("special_category_enhancement");
        if (!enhancements.isObject()) return null;

        EnhancementApplied best = null;
        Iterator<String> keys = enhancements.fieldNames();
        while (keys.hasNext()) {
            String key = keys.next();
            JsonNode entry = enhancements.path(key);
            JsonNode condition = entry.path("condition");
            if (!eligibilityService.matches(condition.isMissingNode() ? null : condition, applicant)) {
                continue;
            }
            JsonNode pctNode = entry.path("subsidy_pct");
            Double pct = pctNode.isMissingNode() || pctNode.isNull() ? null : pctNode.asDouble();
            String note = entry.path("note").asText(null);
            if (best == null || (pct != null && (best.subsidyPct() == null || pct > best.subsidyPct()))) {
                best = new EnhancementApplied(key, pct, note);
            }
        }
        return best;
    }

    // ---- ranking ---------------------------------------------------------------------------------

    private static int rankTier(SchemeMatch match) {
        BenefitCalculator.BenefitResult b = match.benefit();
        if (b.amount() != null) return 0;
        if (b.effectiveInterestRatePct() != null) return 1;
        if (b.guaranteeCoverPct() != null) return 2;
        return 3;
    }

    /** Sorted descending within a tier, so a lower interest rate needs to be negated to still sort "best first". */
    private static double rankValue(SchemeMatch match) {
        BenefitCalculator.BenefitResult b = match.benefit();
        if (b.amount() != null) return b.amount();
        if (b.effectiveInterestRatePct() != null) return -b.effectiveInterestRatePct();
        if (b.guaranteeCoverPct() != null) return b.guaranteeCoverPct();
        return 0;
    }

    // ---- pre-filters -------------------------------------------------------------------------------
    // The applicant-facing ones (availableInState / matchesArea / matchesSector / loanWithinRange) mirror
    // FinancingSchemeService's rules exactly, by design (see class note on why they're not shared code).

    private static boolean isOpen(Scheme s) {
        return s.getStatus() == null || "open".equalsIgnoreCase(s.getStatus());
    }

    private static boolean matchesLevelFilter(Scheme s, String level) {
        return level == null || level.equalsIgnoreCase(s.getLevel());
    }

    private static boolean matchesSectorFilter(Scheme s, String sector) {
        if (sector == null) return true;
        List<String> sectors = s.getTargetSector();
        return sectors != null && sectors.stream().anyMatch(sc -> sc.equalsIgnoreCase(sector));
    }

    private static boolean matchesStateFilter(Scheme s, String state) {
        return state == null || s.getState() == null || s.getState().equalsIgnoreCase(state);
    }

    private static boolean matchesCalculationTypeFilter(Scheme s, String calculationType) {
        if (calculationType == null) return true;
        JsonNode benefit = s.getBenefit();
        return benefit != null && calculationType.equalsIgnoreCase(benefit.path("calculation_type").asText(null));
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

    /** Permissive both ways - only excludes when the scheme states a restricted list AND the applicant states a type outside it. */
    private static boolean matchesTargetApplicant(Scheme s, ApplicantProfile applicant) {
        List<String> targets = s.getTargetApplicant();
        String enterpriseType = applicant.getEnterpriseType();
        return targets == null || targets.isEmpty() || enterpriseType == null
                || targets.stream().anyMatch(t -> t.equalsIgnoreCase(enterpriseType));
    }

    /**
     * NOT checked anywhere before this service existed. Data findings: 223/230 records carry min_age,
     * 58 carry max_age, and 115 of those never repeat the constraint inside eligibility_rules - so the
     * loan-comparison flow has silently never enforced it, relying on the baseline applicant's fixed
     * age of 30 usually happening to fall in range. Permissive when the applicant's age isn't known.
     */
    private static boolean matchesAge(Scheme s, Integer age) {
        if (age == null) return true;
        if (s.getMinAge() != null && age < s.getMinAge()) return false;
        if (s.getMaxAge() != null && age > s.getMaxAge()) return false;
        return true;
    }

    /**
     * {@code greenfieldOnly} and {@code existingUnitAllowed} are the same fact stated twice in the
     * dataset (verified: always exact opposites - 79 greenfield-only, 151 open to existing units too),
     * so only {@code greenfieldOnly} is read. Also not checked anywhere before this service.
     */
    private static boolean matchesBusinessStage(Scheme s, String businessStage) {
        if (businessStage == null || !Boolean.TRUE.equals(s.getGreenfieldOnly())) return true;
        return "greenfield".equalsIgnoreCase(businessStage);
    }

    /** Same "max: 0 means no stated limit" rule as FinancingSchemeService - only applied when a loan amount is actually known. */
    private static boolean loanWithinRange(Scheme s, Double loanAmount) {
        if (loanAmount == null) return true;
        Scheme.LoanAmountRange range = s.getLoanAmountRange();
        if (range == null) return true;
        boolean aboveMin = range.getMin() == null || loanAmount >= range.getMin();
        boolean belowMax = range.getMax() == null || range.getMax() <= 0 || loanAmount <= range.getMax();
        return aboveMin && belowMax;
    }
}
