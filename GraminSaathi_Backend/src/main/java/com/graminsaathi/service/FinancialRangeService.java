package com.graminsaathi.service;

import com.graminsaathi.model.BusinessCategory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Step 5 - financial ranges (Project_Docs/GraminSaathi_Recommendation_Engine_Plan.md, section 2.6):
 * "Reference revenue is a category average, so scale it by state consumption level and show
 * pessimistic, base and optimistic cases."
 *
 * <p>Entirely data-independent as written: the category reference figures already exist
 * ({@link BusinessCategory}, carried over from the old PMEGP-anchored demo data), and the spread
 * constants below are a simplifying assumption documented so they're easy to find and replace, not
 * hidden in the middle of a formula.
 *
 * <p><b>What's stubbed, not missing:</b> the plan's "state consumption level" scaling factor. The real
 * version of that is village HCE spend for this category vs. a national average (needs the same
 * {@code hce_category_spend} table step 3 reads - see {@link DemandSupplyScoreService}, not yet seeded).
 * {@link #calculate} takes that factor as an {@code Optional<Double>} parameter rather than looking it up
 * itself, so callers pass {@code Optional.empty()} today (falls back to 1.0, i.e. unscaled category
 * average) and can pass the real ratio later with zero change to this class.
 */
@Service
public class FinancialRangeService {

    /** How far below/above the base case the pessimistic/optimistic cases sit. Revisit once real peer-benchmark variance data (not just the single-point averages already in {@code BusinessCategory}) exists to calibrate this. */
    private static final double PESSIMISTIC_FACTOR = 0.70;
    private static final double OPTIMISTIC_FACTOR = 1.30;

    public record MonthlyRange(double pessimistic, double base, double optimistic) {}

    public record FinancialRangeResult(
            MonthlyRange revenue,
            MonthlyRange operatingCost,
            MonthlyRange profit,
            double stateConsumptionMultiplierUsed
    ) {}

    public FinancialRangeResult calculate(BusinessCategory category, Optional<Double> stateConsumptionMultiplier) {
        double multiplier = stateConsumptionMultiplier.filter(m -> m > 0).orElse(1.0);

        double baseRevenue = safe(category.getReferenceMonthlyRevenue()) * multiplier;
        double baseCost = safe(category.getReferenceMonthlyOperatingCost()) * multiplier;

        MonthlyRange revenue = range(baseRevenue);
        MonthlyRange operatingCost = range(baseCost);
        MonthlyRange profit = new MonthlyRange(
                round(revenue.pessimistic() - operatingCost.optimistic()), // worst revenue against worst (highest) cost
                round(revenue.base() - operatingCost.base()),
                round(revenue.optimistic() - operatingCost.pessimistic()) // best revenue against best (lowest) cost
        );

        return new FinancialRangeResult(revenue, operatingCost, profit, multiplier);
    }

    private MonthlyRange range(double base) {
        return new MonthlyRange(round(base * PESSIMISTIC_FACTOR), round(base), round(base * OPTIMISTIC_FACTOR));
    }

    private double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
