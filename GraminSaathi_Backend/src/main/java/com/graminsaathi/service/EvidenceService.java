package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.AnalyzeRequest;
import com.graminsaathi.service.FinancialCalculatorService;
import com.graminsaathi.service.FeasibilityScoreService;
import com.graminsaathi.service.DscrService;
import com.graminsaathi.service.SurvivalSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final DemoDataLoader demoDataLoader;
    private final FinancialCalculatorService financialCalculatorService;
    private final FeasibilityScoreService feasibilityScoreService;
    private final DscrService dscrService;
    private final SurvivalSimulatorService survivalSimulatorService;

    public record EvidenceItem(String claim, String source, String details) {}

    public record EvidenceResult(List<EvidenceItem> evidence) {}

    public EvidenceResult generateEvidence(AnalyzeRequest request) {
        List<EvidenceItem> evidence = new ArrayList<>();

        FinancialCalculatorService.FinancialResult financial = financialCalculatorService.calculate(request);
        FeasibilityScoreService.FeasibilityResult feasibility = feasibilityScoreService.calculate(request.getVillageName(), request.getBusinessCategory());
        DscrService.DscrResult dscr = dscrService.calculate(request.getBusinessCategory(), financial.emi());

        SurvivalSimulatorService.SimulationResult survival = survivalSimulatorService.simulate(
                new com.graminsaathi.dto.request.SimulateRequest(
                        request.getVillageName(),
                        request.getBusinessCategory(),
                        request.getAvailableMarginCapital(),
                        0.0, 0.0
                )
        );

        DemoData.VillageData village = demoDataLoader.getVillage(request.getVillageName());
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(request.getBusinessCategory());
        DemoData.BusinessData businessData = demoDataLoader.getBusinessData(request.getVillageName(), request.getBusinessCategory());

        evidence.add(new EvidenceItem(
                "Project Cost & Loan Amount",
                "Financial Calculator (Section 5)",
                String.format("Margin capital Rs. %.0f -> Project cost Rs. %.0f (10%% margin) -> Loan Rs. %.0f (90%%)",
                        request.getAvailableMarginCapital(), financial.projectCost(), financial.loanAmount())
        ));

        evidence.add(new EvidenceItem(
                "Scheme Assignment",
                "Scheme Lookup Table (Section 5 + Addendum 17)",
                String.format("Project cost Rs. %.0f falls in %s: %.1f%% interest, %d years, %d-month moratorium",
                        financial.projectCost(), financial.schemeName(), financial.interestRateAnnual() * 100,
                        financial.tenureYears(), financial.moratoriumMonths())
        ));

        evidence.add(new EvidenceItem(
                "EMI Calculation",
                "Standard EMI Formula (Section 5)",
                String.format("P=Rs. %.0f, r=%.4f/month, n=%d months -> EMI=Rs. %.0f",
                        financial.loanAmount(), financial.monthlyRate(), financial.repaymentMonths(), financial.emi())
        ));

        evidence.add(new EvidenceItem(
                "Recommended (Optimal) Loan",
                "Reference Project Cost Cap (Section 5)",
                String.format("Reference project cost for %s: Rs. %.0f -> Recommended project cost: Rs. %.0f -> Recommended loan: Rs. %.0f (Buffer: Rs. %.0f)",
                        request.getBusinessCategory(), category.getReferenceProjectCost(),
                        financial.recommendedProjectCost(), financial.recommendedLoanAmount(), financial.bufferAmount())
        ));

        evidence.add(new EvidenceItem(
                "Working Capital Estimate",
                "Category Reference Data (Section 5)",
                String.format("%d months x Rs. %.0f/month operating cost = Rs. %.0f",
                        category.getWorkingCapitalMonths(), category.getReferenceMonthlyOperatingCost(), financial.workingCapitalEstimate())
        ));

        evidence.add(new EvidenceItem(
                "Opportunity Score",
                "Local Opportunity Score Formula (Section 6)",
                String.format("Population (5km): %d, Competitors: %d -> Demand ratio: %.1f -> Score: %d (%s)",
                        feasibility.population5kmRadius(), feasibility.competitorCount(),
                        feasibility.demandRatio(), feasibility.opportunityScore(), feasibility.label())
        ));

        evidence.add(new EvidenceItem(
                "DSCR (Debt Service Coverage Ratio)",
                "DSCR Formula (Section 7)",
                String.format("Monthly net operating income: Rs. %.0f, EMI: Rs. %.0f -> DSCR: %.2f (%s)",
                        dscr.monthlyNetOperatingIncome(), dscr.emi(), dscr.dscr(), dscr.label())
        ));

        evidence.add(new EvidenceItem(
                "Survival Simulation (Base Case)",
                "24-Month Cash Flow Model (Section 8)",
                String.format("Working capital: Rs. %.0f, Monthly revenue: Rs. %.0f, Monthly cost: Rs. %.0f, EMI after moratorium: Rs. %.0f -> %s",
                        financial.workingCapitalEstimate(), category.getReferenceMonthlyRevenue(),
                        category.getReferenceMonthlyOperatingCost(), financial.emi(), survival.verdict())
        ));

        if (businessData != null && businessData.getAvgLocalPrice() != null) {
            evidence.add(new EvidenceItem(
                    "Local Price Intelligence",
                    "Cached Village Business Data (Addendum 19)",
                    String.format("Local average price for %s: Rs. %.2f -> Recommended launch price: Rs. %.2f",
                            request.getBusinessCategory(), businessData.getAvgLocalPrice(),
                            financial.recommendedLaunchPrice())
            ));
        }

        if (financial.breakevenPrice() != null) {
            evidence.add(new EvidenceItem(
                    "Failure Boundary (Breakeven Price)",
                    "Counterfactual Calculator (Addendum 21)",
                    financial.breakevenNote()
            ));
        }

        evidence.add(new EvidenceItem(
                "Supply Chain Risk Note",
                "Static Category Advisory (Addendum 22)",
                category.getPrimaryInputDependencyNote()
        ));

        evidence.add(new EvidenceItem(
                "Peer Benchmark (Illustrative)",
                "Sample Cohort Data (Addendum 23)",
                String.format("Sample size: %d, Avg revenue after 6 months: Rs. %.0f, %% operating after 1 year: %d%%",
                        category.getPeerBenchmark().getSampleSize(),
                        category.getPeerBenchmark().getAvgMonthlyRevenueAfter6Months(),
                        category.getPeerBenchmark().getPctStillOperatingAfter1Year())
        ));

        return new EvidenceResult(evidence);
    }
}