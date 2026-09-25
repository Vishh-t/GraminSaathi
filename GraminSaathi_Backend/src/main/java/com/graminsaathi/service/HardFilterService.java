package com.graminsaathi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.model.BusinessCategory;
import com.graminsaathi.model.VillageFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Pass/fail eligibility for a business category before any scoring runs - step 4's hard-filter half
 * (Project_Docs/GraminSaathi_Recommendation_Engine_Plan.md, section 2.4: "a flour mill needs electricity,
 * dairy needs fodder and water").
 *
 * <p>Two independent checks, both must pass:
 * <ol>
 *   <li><b>Applicant-answer filter</b> ({@link BusinessCategory#getHardFilterRulesJson()}) - reuses
 *       {@link SchemeEligibilityService}'s JsonLogic evaluator against {@code applicant.*}. Fully
 *       data-independent, works today. A blank/null rule always passes (no category in the current seed
 *       sets one - see {@link BusinessCategory} javadoc for why).</li>
 *   <li><b>Amenity filter</b> ({@link BusinessCategory#getRequiresElectricity()} vs.
 *       {@link VillageFeatures#getHasElectricity()}) - fails only when the category requires electricity
 *       AND the village is positively known not to have it; a village with no amenities row yet (every
 *       real village today - see {@link VillageFeatures} javadoc) or an unset category flag always passes.
 *       Water and road connectivity have columns on {@link VillageFeatures} too but no corresponding
 *       {@code requires*} flag on {@link BusinessCategory} yet, so they're not checked here.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HardFilterService {

    private final SchemeEligibilityService schemeEligibilityService;
    private final ObjectMapper objectMapper;

    public record FilterResult(boolean passed, String failureReason) {
        public static FilterResult pass() {
            return new FilterResult(true, null);
        }
        public static FilterResult fail(String reason) {
            return new FilterResult(false, reason);
        }
    }

    public FilterResult evaluate(BusinessCategory category, ApplicantProfile applicant, VillageFeatures villageFeatures) {
        FilterResult applicantResult = passesApplicantFilter(category, applicant);
        if (!applicantResult.passed()) {
            return applicantResult;
        }
        return passesAmenityFilter(category, villageFeatures);
    }

    private FilterResult passesApplicantFilter(BusinessCategory category, ApplicantProfile applicant) {
        String rulesJson = category.getHardFilterRulesJson();
        if (rulesJson == null || rulesJson.isBlank()) {
            return FilterResult.pass();
        }
        try {
            JsonNode rule = objectMapper.readTree(rulesJson);
            boolean matched = schemeEligibilityService.matches(rule, applicant != null ? applicant : new ApplicantProfile());
            return matched ? FilterResult.pass()
                    : FilterResult.fail("Does not meet " + category.getCategoryName() + "'s eligibility requirements");
        } catch (Exception e) {
            log.warn("Could not parse hard_filter_rules_json for category {}: {}", category.getCategoryName(), e.getMessage());
            return FilterResult.pass(); // a broken rule should never silently block every applicant
        }
    }

    /**
     * Checks electricity only - see class javadoc for why water/roads aren't checked yet. Null-safe in
     * both directions: an unset {@code requiresElectricity} flag or a missing/unknown amenities row always
     * passes. Only fails when the category positively requires electricity AND the village positively
     * doesn't have it.
     */
    private FilterResult passesAmenityFilter(BusinessCategory category, VillageFeatures villageFeatures) {
        if (!Boolean.TRUE.equals(category.getRequiresElectricity())) {
            return FilterResult.pass();
        }
        if (villageFeatures == null || villageFeatures.getHasElectricity() == null) {
            return FilterResult.pass();
        }
        return villageFeatures.getHasElectricity()
                ? FilterResult.pass()
                : FilterResult.fail(category.getCategoryName() + " requires electricity, which this village doesn't have");
    }
}
