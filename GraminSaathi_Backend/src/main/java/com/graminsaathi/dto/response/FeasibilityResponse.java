package com.graminsaathi.dto.response;

import lombok.Data;

@Data
public class FeasibilityResponse {
    private int opportunityScore;
    private String label;
    private int competitorCount;
    private int population5kmRadius;
    private double demandRatio;
}