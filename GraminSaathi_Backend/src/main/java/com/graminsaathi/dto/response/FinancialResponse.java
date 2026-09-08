package com.graminsaathi.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class FinancialResponse {
    private double projectCost;
    private double loanAmount;
    private String schemeName;
    private double interestRateAnnual;
    private int tenureYears;
    private int moratoriumMonths;
    private int repaymentMonths;
    private double monthlyRate;
    private double emi;
    private double workingCapitalEstimate;
    private double recommendedProjectCost;
    private double recommendedLoanAmount;
    private double bufferAmount;
    private List<SchemeComparisonResponse> schemeComparison;
    private String workingCapitalWarning;
    private double localAveragePrice;
    private Double recommendedPriceLow;
    private Double recommendedPriceHigh;
    private Double recommendedLaunchPrice;
    private Double breakevenPrice;
    private String breakevenNote;
}