package com.graminsaathi.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
public class DemoData {
    private List<VillageData> villages;
    @JsonProperty("business_categories")
    private List<BusinessCategoryData> businessCategories;

    @Data
    public static class VillageData {
        @JsonProperty("village_name")
        private String villageName;
        private String block;
        private String district;
        private String state;
        private Double latitude;
        private Double longitude;
        @JsonProperty("population_5km_radius")
        private Integer population5kmRadius;
        @JsonProperty("households_5km_radius")
        private Integer households5kmRadius;
        @JsonProperty("business_data")
        private Map<String, BusinessData> businessData;
    }

    @Data
    public static class BusinessData {
        @JsonProperty("competitor_count")
        private Integer competitorCount;
        @JsonProperty("avg_local_price")
        private Double avgLocalPrice;
    }

    @Data
    public static class BusinessCategoryData {
        @JsonProperty("category_name")
        private String categoryName;
        /** Scheme-vocabulary sector (manufacturing, services, trading, agri_allied, ...) used to pick real financing schemes. */
        private String sector;
        @JsonProperty("sub_sector")
        private String subSector;
        @JsonProperty("reference_project_cost")
        private Double referenceProjectCost;
        @JsonProperty("reference_cost_range")
        private List<Double> referenceCostRange;
        @JsonProperty("working_capital_months")
        private Integer workingCapitalMonths;
        @JsonProperty("reference_monthly_revenue")
        private Double referenceMonthlyRevenue;
        @JsonProperty("reference_monthly_operating_cost")
        private Double referenceMonthlyOperatingCost;
        @JsonProperty("secondary_opportunity")
        private String secondaryOpportunity;
        @JsonProperty("source_note")
        private String sourceNote;
        @JsonProperty("single_buyer_dependency_risk")
        private String singleBuyerDependencyRisk;
        @JsonProperty("primary_input_dependency_note")
        private String primaryInputDependencyNote;
        @JsonProperty("peer_benchmark")
        private PeerBenchmark peerBenchmark;
        @JsonProperty("roadmap_milestones")
        private List<RoadmapMilestone> roadmapMilestones;
    }

    @Data
    public static class PeerBenchmark {
        @JsonProperty("sample_size")
        private Integer sampleSize;
        @JsonProperty("avg_monthly_revenue_after_6_months")
        private Double avgMonthlyRevenueAfter6Months;
        @JsonProperty("pct_still_operating_after_1_year")
        private Integer pctStillOperatingAfter1Year;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoadmapMilestone {
        private Integer month;
        private String milestone;
    }
}