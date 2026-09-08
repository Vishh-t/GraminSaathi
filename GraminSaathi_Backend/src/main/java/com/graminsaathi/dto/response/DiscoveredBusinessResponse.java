package com.graminsaathi.dto.response;

import lombok.Data;

@Data
public class DiscoveredBusinessResponse {
    private String categoryName;
    private int opportunityScore;
    private String opportunityLabel;
    private String affordabilityFlag;
    private double referenceProjectCost;
    private double referenceMonthlyRevenue;
    private double referenceMonthlyOperatingCost;
}