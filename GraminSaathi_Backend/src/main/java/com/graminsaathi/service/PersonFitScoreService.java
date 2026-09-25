package com.graminsaathi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.model.BusinessCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * "Can this person actually run it?" - step 4's person-fit half of the plan (Project_Docs/
 * GraminSaathi_Recommendation_Engine_Plan.md, section 2.5). Entirely data-independent: everything it
 * reads (the applicant's own answers, a category's reference figures and fit rules) already exists
 * today, so this needed no coordinates, competitor counts, or coverage data to build.
 *
 * <p>Reuses {@link SchemeEligibilityService}'s JsonLogic evaluator for {@link BusinessCategory#getFitRequirementRulesJson()}
 * rather than a new rule engine - same {@code applicant.*} vocabulary as scheme eligibility.
 *
 * <p>Three weighted pillars, each 0-100:
 * <ul>
 *   <li><b>Capital adequacy</b> (always scored) - available margin capital vs. the category's required
 *       margin ({@code referenceProjectCost * minMarginCapitalFraction}, default 10%).</li>
 *   <li><b>Land adequacy</b> (only when the category declares {@code minLandHoldingAcres}) - scaled
 *       toward the minimum; unknown ({@code applicant.landHoldingAcres} null) scores neutral (50), not 0,
 *       since a missing answer isn't evidence of unfitness.</li>
 *   <li><b>Requirement-rule match</b> - the category's weighted {@code fitRequirementRulesJson} list,
 *       each rule contributing its weight x100 if it matches, 0 if not; weights are normalized if they
 *       don't sum to 1. No rules configured -> neutral (50).</li>
 * </ul>
 * Capital is fixed at 35% weight; land (when applicable) takes 15%, leaving requirement rules the
 * remainder (65%, or 50% when a land requirement is also present).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonFitScoreService {

    private static final double CAPITAL_WEIGHT = 0.35;
    private static final double LAND_WEIGHT_WHEN_APPLICABLE = 0.15;
    private static final double NEUTRAL_SCORE = 50.0;

    private final SchemeEligibilityService schemeEligibilityService;
    private final ObjectMapper objectMapper;

    public record RuleOutcome(String description, boolean matched, double weight) {}

    public record FitResult(
            int overallScore,
            String label,
            double capitalScore,
            Double landScore,
            double requirementScore,
            List<RuleOutcome> ruleOutcomes
    ) {}

    public FitResult calculate(BusinessCategory category, ApplicantProfile applicant, double availableMarginCapital) {
        ApplicantProfile safeApplicant = applicant != null ? applicant : new ApplicantProfile();

        double capitalScore = capitalAdequacy(category, availableMarginCapital);

        boolean hasLandRequirement = category.getMinLandHoldingAcres() != null;
        Double landScore = hasLandRequirement ? landAdequacy(category, safeApplicant) : null;

        List<RuleOutcome> ruleOutcomes = evaluateRules(category, safeApplicant);
        double requirementScore = requirementScore(ruleOutcomes);

        double landWeight = hasLandRequirement ? LAND_WEIGHT_WHEN_APPLICABLE : 0.0;
        double requirementWeight = 1.0 - CAPITAL_WEIGHT - landWeight;

        double overall = CAPITAL_WEIGHT * capitalScore
                + landWeight * (landScore != null ? landScore : 0)
                + requirementWeight * requirementScore;

        int overallScore = (int) Math.round(clamp(overall));
        String label;
        if (overallScore >= 70) {
            label = "Strong fit";
        } else if (overallScore >= 40) {
            label = "Moderate fit";
        } else {
            label = "Weak fit";
        }

        return new FitResult(overallScore, label, round(capitalScore), landScore != null ? round(landScore) : null,
                round(requirementScore), ruleOutcomes);
    }

    private double capitalAdequacy(BusinessCategory category, double availableMarginCapital) {
        if (category.getReferenceProjectCost() == null || category.getReferenceProjectCost() <= 0) {
            return NEUTRAL_SCORE;
        }
        double marginFraction = category.getMinMarginCapitalFraction() != null
                ? category.getMinMarginCapitalFraction() : 0.10;
        double requiredMargin = category.getReferenceProjectCost() * marginFraction;
        if (requiredMargin <= 0) {
            return 100.0;
        }
        return clamp((availableMarginCapital / requiredMargin) * 100.0);
    }

    private double landAdequacy(BusinessCategory category, ApplicantProfile applicant) {
        if (applicant.getLandHoldingAcres() == null) {
            return NEUTRAL_SCORE; // not provided - don't penalize, just don't credit either
        }
        double min = category.getMinLandHoldingAcres();
        if (min <= 0) {
            return 100.0;
        }
        return clamp((applicant.getLandHoldingAcres() / min) * 100.0);
    }

    /** Parses {@code [{"description","rule","weight"}, ...]}; malformed or absent JSON yields an empty list (caller treats that as neutral). */
    private List<RuleOutcome> evaluateRules(BusinessCategory category, ApplicantProfile applicant) {
        List<RuleOutcome> outcomes = new ArrayList<>();
        String rulesJson = category.getFitRequirementRulesJson();
        if (rulesJson == null || rulesJson.isBlank()) {
            return outcomes;
        }
        try {
            JsonNode rules = objectMapper.readTree(rulesJson);
            if (!rules.isArray()) {
                return outcomes;
            }
            for (JsonNode entry : rules) {
                String description = entry.path("description").asText("Requirement");
                double weight = entry.path("weight").asDouble(0);
                JsonNode rule = entry.get("rule");
                boolean matched = schemeEligibilityService.matches(rule, applicant);
                outcomes.add(new RuleOutcome(description, matched, weight));
            }
        } catch (Exception e) {
            log.warn("Could not parse fit_requirement_rules_json for category {}: {}", category.getCategoryName(), e.getMessage());
        }
        return outcomes;
    }

    private double requirementScore(List<RuleOutcome> outcomes) {
        if (outcomes.isEmpty()) {
            return NEUTRAL_SCORE;
        }
        double totalWeight = outcomes.stream().mapToDouble(RuleOutcome::weight).sum();
        if (totalWeight <= 0) {
            return NEUTRAL_SCORE;
        }
        double matchedWeight = outcomes.stream().filter(RuleOutcome::matched).mapToDouble(RuleOutcome::weight).sum();
        return clamp((matchedWeight / totalWeight) * 100.0);
    }

    private double clamp(double value) {
        return Math.min(100.0, Math.max(0.0, value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
