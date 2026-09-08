package com.graminsaathi.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SchemesReference {
    private List<Scheme> schemes;

    @Data
    public static class Scheme {
        @JsonProperty("scheme_name")
        private String schemeName;
        @JsonProperty("min_cost")
        private Double minCost;
        @JsonProperty("max_cost")
        private Double maxCost;
        @JsonProperty("interest_rate")
        private Double interestRate;
        @JsonProperty("tenure_years")
        private Integer tenureYears;
        @JsonProperty("moratorium_months")
        private Integer moratoriumMonths;
        private String agency;
        @JsonProperty("subsidy_note")
        private String subsidyNote;
    }
}