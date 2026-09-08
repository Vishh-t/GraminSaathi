package com.graminsaathi.dto.response;

import lombok.Data;

@Data
public class HealthScoreResponse {
    private int overallScore;
    private String recommendation;
    private SubScore marketDemand;
    private SubScore capitalAdequacy;
    private SubScore profitability;
    private SubScore cashFlow;
    private SubScore supplyRisk;
    private SubScore seasonality;

    @Data
    public static class SubScore {
        private String name;
        private int score;
        private String description;
    }
}