package com.graminsaathi.dto.response;

import lombok.Data;

@Data
public class GoalSeekResponse {
    private double estimatedProjectCost;
    private double estimatedMarginRequired;
    private double estimatedLoanRequired;
    private double requiredMonthlyRevenue;
    private String note;
}