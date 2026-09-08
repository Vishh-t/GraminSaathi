package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.SimulateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SurvivalSimulatorService {

    private final DemoDataLoader demoDataLoader;
    private final FinancialCalculatorService financialCalculatorService;

    public record SimulationPoint(int month, double cumulativeCash) {}

    public record SimulationResult(
            List<SimulationPoint> cashCurve,
            String verdict,
            Integer deficitMonth
    ) {}

    public SimulationResult simulate(SimulateRequest request) {
        FinancialCalculatorService.FinancialResult financial = financialCalculatorService.calculate(
                new com.graminsaathi.dto.request.AnalyzeRequest(
                        request.getVillageName(),
                        request.getBusinessCategory(),
                        request.getAvailableMarginCapital()
                )
        );

        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(request.getBusinessCategory());
        if (category == null) {
            throw new IllegalArgumentException("Invalid business category: " + request.getBusinessCategory());
        }

        double revenueShockPct = request.getRevenueShockPct() != null ? request.getRevenueShockPct() : 0.0;
        double costShockPct = request.getCostShockPct() != null ? request.getCostShockPct() : 0.0;

        double adjustedMonthlyRevenue = category.getReferenceMonthlyRevenue() * (1 + revenueShockPct);
        double adjustedMonthlyCost = category.getReferenceMonthlyOperatingCost() * (1 + costShockPct);

        double cumulativeCash = financial.workingCapitalEstimate();
        Integer deficitMonth = null;
        List<SimulationPoint> cashCurve = new ArrayList<>();

        for (int month = 1; month <= 24; month++) {
            double emiThisMonth = month > financial.moratoriumMonths() ? financial.emi() : 0;
            double netCashFlow = adjustedMonthlyRevenue - adjustedMonthlyCost - emiThisMonth;
            cumulativeCash += netCashFlow;

            cashCurve.add(new SimulationPoint(month, cumulativeCash));

            if (cumulativeCash < 0 && deficitMonth == null) {
                deficitMonth = month;
            }
        }

        String verdict;
        if (deficitMonth == null) {
            verdict = "Business survives comfortably across the 24-month simulated period";
        } else {
            verdict = String.format("Cash-flow deficit likely from Month %d under this scenario", deficitMonth);
        }

        return new SimulationResult(cashCurve, verdict, deficitMonth);
    }
}