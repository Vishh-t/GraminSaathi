package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.AnalyzeRequest;
import com.graminsaathi.service.FinancialCalculatorService;
import com.graminsaathi.service.FeasibilityScoreService;
import com.graminsaathi.service.DscrService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessHealthScoreService {

    private final DemoDataLoader demoDataLoader;
    private final FinancialCalculatorService financialCalculatorService;
    private final FeasibilityScoreService feasibilityScoreService;
    private final DscrService dscrService;

    public record SubScore(String name, int score, String description) {}

    public record HealthScoreResult(
            int overallScore,
            String recommendation,
            SubScore marketDemand,
            SubScore capitalAdequacy,
            SubScore profitability,
            SubScore cashFlow,
            SubScore supplyRisk,
            SubScore seasonality
    ) {}

    public HealthScoreResult calculate(AnalyzeRequest request) {
        FinancialCalculatorService.FinancialResult financial = financialCalculatorService.calculate(request);
        FeasibilityScoreService.FeasibilityResult feasibility = feasibilityScoreService.calculate(request.getVillageName(), request.getBusinessCategory());
        DscrService.DscrResult dscr = dscrService.calculate(request.getBusinessCategory(), financial.emi());

        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(request.getBusinessCategory());
        DemoData.BusinessData businessData = demoDataLoader.getBusinessData(request.getVillageName(), request.getBusinessCategory());

        int marketDemandScore = feasibility.opportunityScore();

        double marginRequired = financial.recommendedProjectCost() * 0.10;
        int capitalAdequacyScore = request.getAvailableMarginCapital() >= marginRequired
                ? 100
                : (int) Math.round((request.getAvailableMarginCapital() / marginRequired) * 100);

        double netMarginPct = (category.getReferenceMonthlyRevenue() - category.getReferenceMonthlyOperatingCost())
                / category.getReferenceMonthlyRevenue() * 100;
        int profitabilityScore = (int) Math.round(Math.min(100, netMarginPct * 2));

        int cashFlowScore;
        if (dscr.dscr() >= 2.0) cashFlowScore = 100;
        else if (dscr.dscr() >= 1.0) cashFlowScore = 60;
        else cashFlowScore = 25;

        int competitorCount = businessData != null ? businessData.getCompetitorCount() : 0;
        int supplyRiskScore = Math.max(20, 100 - (competitorCount * 3));

        int seasonalityScore = 70;

        int overallScore = (int) Math.round(
                marketDemandScore * 0.25 +
                capitalAdequacyScore * 0.15 +
                profitabilityScore * 0.20 +
                cashFlowScore * 0.20 +
                supplyRiskScore * 0.10 +
                seasonalityScore * 0.10
        );

        String recommendation;
        if (overallScore >= 75) recommendation = "🟢 Proceed";
        else if (overallScore >= 50) recommendation = "🟡 Proceed with modifications";
        else recommendation = "🔴 Do not finance as structured";

        return new HealthScoreResult(
                overallScore,
                recommendation,
                new SubScore("Market Demand", marketDemandScore, feasibility.label()),
                new SubScore("Capital Adequacy", capitalAdequacyScore,
                        request.getAvailableMarginCapital() >= marginRequired ? "Sufficient margin capital" : "Insufficient margin capital"),
                new SubScore("Profitability", profitabilityScore,
                        String.format("Net margin: %.1f%%", netMarginPct)),
                new SubScore("Cash Flow", cashFlowScore,
                        String.format("DSCR: %.2f (%s)", dscr.dscr(), dscr.label())),
                new SubScore("Supply Risk", supplyRiskScore,
                        String.format("Competitors: %d", competitorCount)),
                new SubScore("Seasonality", seasonalityScore, "Moderate seasonality assumed (placeholder)")
        );
    }
}