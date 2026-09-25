package com.graminsaathi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.model.BusinessCategory;
import com.graminsaathi.model.HceCategorySpend;
import com.graminsaathi.model.Village;
import com.graminsaathi.model.VillageFeatures;
import com.graminsaathi.repository.HceCategorySpendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Step 3 - demand-supply gap scoring (Project_Docs/GraminSaathi_Recommendation_Engine_Plan.md, section
 * 2.2). Reads real data where it exists and returns an explicit "not enough data yet" result where it
 * doesn't, rather than guessing - see {@link Confidence}.
 *
 * <p><b>Demand</b> = {@code VillageFeatures.householdsProjected} (real, from step 2a) x
 * {@link HceCategorySpend#getMonthlyHouseholdSpend()} (schema exists, not yet seeded - see that class).
 * <p><b>Supply</b> = the category's competitor count for this village (from {@link VillageFeatures#getCompetitorCountsJson()},
 * step 2b - not populated until coordinates land) x {@link BusinessCategory#getReferenceMonthlyRevenue()}
 * (the "typical revenue per shop" the plan calls for; real PMEGP-anchored figures already exist per category).
 * <p><b>Gap</b> = demand - supply, expressed as {@link ScoreResult#marketScore()}: the percentage of
 * demand currently unmet (100 = fully unmet, 0 = fully served or oversaturated).
 *
 * <p>Nothing here needed the friend's coordinate file to be written; only competitor counts (step 2b) and
 * the HCES seed do, and both fail gracefully (null, not zero) until they exist. Once they're populated,
 * this service starts returning real scores with no code change - only {@link #findHouseholdSpend} and
 * {@link #findCompetitorCount} needed to already handle the "present" case correctly, which they do.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandSupplyScoreService {

    private final HceCategorySpendRepository hceCategorySpendRepository;
    private final ObjectMapper objectMapper;

    public enum Confidence {
        /** Households (step 2a) missing for this village - shouldn't happen for any village that has gone through the population pass. */
        INSUFFICIENT_NO_POPULATION,
        /** Households known, but either household spend (HCES) or competitor counts (2b) or both are missing. */
        LOW_PARTIAL_DATA,
        /** Both demand and supply inputs present. Will be downgraded further once step 2c (coverage confidence) exists - see class javadoc. */
        MEDIUM_FULL_DATA
    }

    public record ScoreResult(
            Confidence confidence,
            Integer marketScore,
            Double monthlyDemandRupees,
            Double monthlySupplyRupees,
            Double gapRupees,
            String explanation
    ) {}

    public ScoreResult calculate(Village village, VillageFeatures features, BusinessCategory category) {
        if (features == null || features.getHouseholdsProjected() == null) {
            return new ScoreResult(Confidence.INSUFFICIENT_NO_POPULATION, null, null, null, null,
                    "No population projection for this village yet.");
        }

        Optional<Double> householdSpend = findHouseholdSpend(village, category);
        Optional<Integer> competitorCount = findCompetitorCount(features, category);

        if (householdSpend.isEmpty() || competitorCount.isEmpty()) {
            StringBuilder missing = new StringBuilder("Waiting on: ");
            if (householdSpend.isEmpty()) missing.append("household spend data (HCES)");
            if (householdSpend.isEmpty() && competitorCount.isEmpty()) missing.append(", ");
            if (competitorCount.isEmpty()) missing.append("competitor counts (step 2b)");
            return new ScoreResult(Confidence.LOW_PARTIAL_DATA, null, null, null, null, missing.toString());
        }

        double demand = features.getHouseholdsProjected() * householdSpend.get();
        double supply = competitorCount.get() * safeRevenue(category);
        double gap = demand - supply;

        int marketScore = demand > 0
                ? (int) Math.round(clamp((gap / demand) * 100.0))
                : 0;

        return new ScoreResult(Confidence.MEDIUM_FULL_DATA, marketScore,
                round(demand), round(supply), round(gap),
                String.format("Estimated monthly demand Rs. %.0f vs. supply Rs. %.0f from %d mapped competitor(s).",
                        demand, supply, competitorCount.get()));
    }

    private Optional<Double> findHouseholdSpend(Village village, BusinessCategory category) {
        if (village == null || village.getState() == null) {
            return Optional.empty();
        }
        return hceCategorySpendRepository
                .findByStateIgnoreCaseAndCategoryNameIgnoreCase(village.getState(), category.getCategoryName())
                .map(HceCategorySpend::getMonthlyHouseholdSpend);
    }

    private Optional<Integer> findCompetitorCount(VillageFeatures features, BusinessCategory category) {
        String json = features.getCompetitorCountsJson();
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode map = objectMapper.readTree(json);
            JsonNode count = map.get(category.getCategoryName());
            return (count == null || count.isNull()) ? Optional.empty() : Optional.of(count.asInt());
        } catch (Exception e) {
            log.warn("Could not parse competitor_counts_json for village {}: {}", features.getVillageId(), e.getMessage());
            return Optional.empty();
        }
    }

    private double safeRevenue(BusinessCategory category) {
        return category.getReferenceMonthlyRevenue() != null ? category.getReferenceMonthlyRevenue() : 0.0;
    }

    private double clamp(double value) {
        return Math.min(100.0, Math.max(0.0, value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
