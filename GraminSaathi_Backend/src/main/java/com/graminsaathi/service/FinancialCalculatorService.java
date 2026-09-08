package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.data.SchemesDataLoader;
import com.graminsaathi.data.SchemesReference;
import com.graminsaathi.dto.request.AnalyzeRequest;
import com.graminsaathi.dto.response.SchemeComparisonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialCalculatorService {

    private final DemoDataLoader demoDataLoader;
    private final SchemesDataLoader schemesDataLoader;

    public record FinancialResult(
            double projectCost,
            double loanAmount,
            String schemeName,
            double interestRateAnnual,
            int tenureYears,
            int moratoriumMonths,
            int repaymentMonths,
            double monthlyRate,
            double emi,
            double workingCapitalEstimate,
            double recommendedProjectCost,
            double recommendedLoanAmount,
            double bufferAmount,
            List<SchemeComparisonResponse> schemeComparison,
            String workingCapitalWarning,
            double localAveragePrice,
            Double recommendedPriceLow,
            Double recommendedPriceHigh,
            Double recommendedLaunchPrice,
            Double breakevenPrice,
            String breakevenNote
    ) {}

    public FinancialResult calculate(AnalyzeRequest request) {
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(request.getBusinessCategory());
        if (category == null) {
            throw new IllegalArgumentException("Invalid business category: " + request.getBusinessCategory());
        }

        double availableMarginCapital = request.getAvailableMarginCapital();

        double projectCost = availableMarginCapital / 0.10;
        double loanAmount = projectCost * 0.90;

        SchemesReference.Scheme primaryScheme = schemesDataLoader.getPrimaryScheme(projectCost);
        if (primaryScheme == null) {
            throw new IllegalArgumentException("Project cost exceeds the ₹50 lakh scheme ceiling.");
        }

        String schemeName = primaryScheme.getSchemeName();
        double interestRateAnnual = primaryScheme.getInterestRate();
        int tenureYears = primaryScheme.getTenureYears();
        int moratoriumMonths = primaryScheme.getMoratoriumMonths();

        int repaymentMonths = (tenureYears * 12) - moratoriumMonths;
        double monthlyRate = interestRateAnnual / 12;

        double emi = calculateEmi(loanAmount, monthlyRate, repaymentMonths);

        double workingCapitalEstimate = category.getReferenceMonthlyOperatingCost() * category.getWorkingCapitalMonths();

        double recommendedProjectCost = Math.min(projectCost, category.getReferenceProjectCost());
        double recommendedLoanAmount = recommendedProjectCost * 0.90;
        double bufferAmount = loanAmount - recommendedLoanAmount;

        List<SchemeComparisonResponse> schemeComparison = buildSchemeComparison(projectCost, primaryScheme);

        String workingCapitalWarning = checkWorkingCapitalWarning(
                availableMarginCapital, recommendedProjectCost, workingCapitalEstimate, category.getWorkingCapitalMonths()
        );

        Double avgLocalPrice = null;
        DemoData.BusinessData businessData = demoDataLoader.getBusinessData(request.getVillageName(), request.getBusinessCategory());
        if (businessData != null) {
            avgLocalPrice = businessData.getAvgLocalPrice();
        }

        Double recommendedPriceLow = null;
        Double recommendedPriceHigh = null;
        Double recommendedLaunchPrice = null;
        Double breakevenPrice = null;
        String breakevenNote = null;

        if (avgLocalPrice != null && avgLocalPrice > 0) {
            recommendedPriceLow = round(avgLocalPrice * 0.95, 2);
            recommendedPriceHigh = round(avgLocalPrice * 1.05, 2);
            recommendedLaunchPrice = round(avgLocalPrice * 0.97, 2);

            double estimatedMonthlyUnitsSold = category.getReferenceMonthlyRevenue() / avgLocalPrice;
            if (estimatedMonthlyUnitsSold > 0) {
                breakevenPrice = round((category.getReferenceMonthlyOperatingCost() + emi) / estimatedMonthlyUnitsSold, 2);
                breakevenNote = String.format(
                        "At the current sales volume, your minimum viable price is ₹%.2f — your planned price of ₹%.2f gives you a ₹%.2f per-unit safety margin.",
                        breakevenPrice, avgLocalPrice, avgLocalPrice - breakevenPrice
                );
            }
        } else {
            breakevenNote = "Not applicable — mixed product pricing.";
        }

        return new FinancialResult(
                projectCost, loanAmount, schemeName, interestRateAnnual, tenureYears,
                moratoriumMonths, repaymentMonths, monthlyRate, emi,
                workingCapitalEstimate, recommendedProjectCost, recommendedLoanAmount,
                bufferAmount, schemeComparison, workingCapitalWarning,
                avgLocalPrice != null ? avgLocalPrice : 0,
                recommendedPriceLow, recommendedPriceHigh, recommendedLaunchPrice,
                breakevenPrice, breakevenNote
        );
    }

    private double calculateEmi(double principal, double monthlyRate, int months) {
        if (months <= 0) return 0;
        if (monthlyRate == 0) return principal / months;

        double factor = Math.pow(1 + monthlyRate, months);
        return principal * monthlyRate * factor / (factor - 1);
    }

    private List<SchemeComparisonResponse> buildSchemeComparison(double projectCost, SchemesReference.Scheme primaryScheme) {
        List<SchemeComparisonResponse> comparisons = new ArrayList<>();

        for (SchemesReference.Scheme scheme : schemesDataLoader.getApplicableSchemes(projectCost)) {
            int repaymentMonths = (scheme.getTenureYears() * 12) - scheme.getMoratoriumMonths();
            double monthlyRate = scheme.getInterestRate() / 12;
            double emi = calculateEmi(projectCost * 0.90, monthlyRate, repaymentMonths);

            SchemeComparisonResponse resp = new SchemeComparisonResponse();
            resp.setSchemeName(scheme.getSchemeName());
            resp.setInterestRate(round(scheme.getInterestRate() * 100, 2));
            resp.setTenureYears(scheme.getTenureYears());
            resp.setMoratoriumMonths(scheme.getMoratoriumMonths());
            resp.setEmi(round(emi, 2));
            resp.setAgency(scheme.getAgency());
            resp.setSubsidyNote(scheme.getSubsidyNote());
            resp.setPrimary(scheme.getSchemeName().equals(primaryScheme.getSchemeName()));
            comparisons.add(resp);
        }

        return comparisons;
    }

    private String checkWorkingCapitalWarning(double availableMarginCapital, double recommendedProjectCost,
                                               double workingCapitalEstimate, int workingCapitalMonths) {
        double marginRequired = recommendedProjectCost * 0.10;
        double remainingAfterMargin = availableMarginCapital - marginRequired;

        if (remainingAfterMargin < workingCapitalEstimate) {
            return String.format(
                    "⚠️ Your margin covers the 10%% contribution, but you have no separate buffer for the first %d months of operating costs (~₹%.0f). Consider starting with the recommended lower project size, or arranging a small additional buffer before applying.",
                    workingCapitalMonths, workingCapitalEstimate
            );
        }
        return null;
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}