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

@Service
@RequiredArgsConstructor
public class AgentCommitteeService {

    private final DemoDataLoader demoDataLoader;
    private final FinancialCalculatorService financialCalculatorService;
    private final FeasibilityScoreService feasibilityScoreService;
    private final DscrService dscrService;
    private final SurvivalSimulatorService survivalSimulatorService;

    public record AgentOpinion(String agent, String opinion, String basis) {}

    public record CommitteeResult(
            AgentOpinion marketAgent,
            AgentOpinion financeAgent,
            AgentOpinion riskAgent,
            String finalVerdict,
            String verdictReason
    ) {}

    public CommitteeResult evaluate(AnalyzeRequest request) {
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

        AgentOpinion marketAgent = buildMarketOpinion(feasibility);
        AgentOpinion financeAgent = buildFinanceOpinion(dscr);
        AgentOpinion riskAgent = buildRiskOpinion(survival);

        String finalVerdict;
        String verdictReason;

        boolean viable = feasibility.opportunityScore() >= 60 && dscr.dscr() >= 1.5 && survival.deficitMonth() == null;
        boolean viableWithMods = feasibility.opportunityScore() >= 40 && dscr.dscr() >= 1.0;

        if (viable) {
            finalVerdict = "VIABLE";
            verdictReason = "Strong market opportunity, healthy cash flow coverage, and business survives stress scenarios.";
        } else if (viableWithMods) {
            finalVerdict = "VIABLE WITH MODIFICATIONS";
            verdictReason = "Adequate market opportunity and cash flow coverage, but consider reducing project size or adding buffers.";
        } else {
            finalVerdict = "NOT RECOMMENDED AS STRUCTURED";
            verdictReason = "Insufficient market opportunity, weak cash flow coverage, or high risk of cash-flow deficit.";
        }

        return new CommitteeResult(marketAgent, financeAgent, riskAgent, finalVerdict, verdictReason);
    }

    private AgentOpinion buildMarketOpinion(FeasibilityScoreService.FeasibilityResult feasibility) {
        String opinion;
        if (feasibility.opportunityScore() >= 70) {
            opinion = "Strong local demand with limited competition — this village has genuine need for this business.";
        } else if (feasibility.opportunityScore() >= 40) {
            opinion = "Moderate opportunity — demand exists but competition is noticeable. Differentiation will be key.";
        } else {
            opinion = "Saturated market — many existing players. Consider a different location or business type.";
        }
        return new AgentOpinion(
                "Market Agent",
                opinion,
                String.format("Opportunity Score: %d (%s) — Population: %d, Competitors: %d",
                        feasibility.opportunityScore(), feasibility.label(),
                        feasibility.population5kmRadius(), feasibility.competitorCount())
        );
    }

    private AgentOpinion buildFinanceOpinion(DscrService.DscrResult dscr) {
        String opinion;
        if (dscr.dscr() >= 2.0) {
            opinion = "Excellent debt coverage — net operating income comfortably covers EMI with room to spare.";
        } else if (dscr.dscr() >= 1.0) {
            opinion = "Adequate debt coverage — EMI is covered but little buffer for cost increases or revenue dips.";
        } else {
            opinion = "Insufficient debt coverage — EMI exceeds net operating income. High default risk.";
        }
        return new AgentOpinion(
                "Finance Agent",
                opinion,
                String.format("DSCR: %.2f (%s) — Net operating income: ₹%.0f, EMI: ₹%.0f",
                        dscr.dscr(), dscr.label(), dscr.monthlyNetOperatingIncome(), dscr.emi())
        );
    }

    private AgentOpinion buildRiskOpinion(SurvivalSimulatorService.SimulationResult survival) {
        String opinion;
        if (survival.deficitMonth() == null) {
            opinion = "Business maintains positive cash flow throughout 24 months even under base assumptions.";
        } else {
            opinion = String.format("Cash-flow deficit projected from Month %d — business may need additional working capital or revenue improvements.", survival.deficitMonth());
        }
        return new AgentOpinion(
                "Risk Agent",
                opinion,
                String.format("Base-case simulation: %s", survival.verdict())
        );
    }
}