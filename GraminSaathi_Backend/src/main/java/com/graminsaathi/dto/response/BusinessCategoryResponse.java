package com.graminsaathi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessCategoryResponse {
    private String categoryName;
    private Double referenceProjectCost;
    private List<Double> referenceCostRange;
    private Integer workingCapitalMonths;
    private Double referenceMonthlyRevenue;
    private Double referenceMonthlyOperatingCost;
    private String secondaryOpportunity;
    private String sourceNote;
}