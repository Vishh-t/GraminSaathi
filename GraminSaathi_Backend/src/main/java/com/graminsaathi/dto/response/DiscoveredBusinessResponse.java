package com.graminsaathi.dto.response;

import lombok.Data;

@Data
public class DiscoveredBusinessResponse {
    private String categoryName;

    // Step 3 - demand-supply market score (DemandSupplyScoreService). Score/explanation are null until
    // the underlying data (HCES spend, step 2b competitor counts) is populated - see marketConfidence.
    private Integer marketScore;
    private String marketConfidence;
    private String marketExplanation;

    // Step 4 - person-fit score (PersonFitScoreService). Always computable today.
    private int personFitScore;
    private String personFitLabel;

    private String affordabilityFlag;

    private double referenceProjectCost;
    private double referenceMonthlyRevenue;
    private double referenceMonthlyOperatingCost;

    // Step 5 - financial ranges (FinancialRangeService).
    private FinancialRange financialRange;

    @Data
    public static class MonthlyRange {
        private double pessimistic;
        private double base;
        private double optimistic;

        public MonthlyRange(double pessimistic, double base, double optimistic) {
            this.pessimistic = pessimistic;
            this.base = base;
            this.optimistic = optimistic;
        }
    }

    @Data
    public static class FinancialRange {
        private MonthlyRange revenue;
        private MonthlyRange operatingCost;
        private MonthlyRange profit;
        private double stateConsumptionMultiplierUsed;
    }
}
