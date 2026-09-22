package com.graminsaathi.controller;

import com.graminsaathi.dto.request.AnalyzeRequest;
import com.graminsaathi.dto.response.*;
import com.graminsaathi.model.User;
import com.graminsaathi.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalysisController {

    private final FinancialCalculatorService financialCalculatorService;
    private final FeasibilityScoreService feasibilityScoreService;
    private final DscrService dscrService;
    private final SurvivalSimulatorService survivalSimulatorService;
    private final BusinessCombinationService businessCombinationService;
    private final EvidenceService evidenceService;
    private final AgentCommitteeService agentCommitteeService;
    private final BusinessHealthScoreService businessHealthScoreService;
    private final SupplyChainRiskService supplyChainRiskService;
    private final PeerBenchmarkService peerBenchmarkService;
    private final RoadmapMilestoneService roadmapMilestoneService;
    private final ProfileService profileService;

    /**
     * When the caller is logged in, their saved "ask once" applicant profile (ProfileService) is merged
     * underneath whatever {@code applicant} facts this request sends - request facts win, the saved
     * profile fills any gaps. So once someone has answered the intake form a single time, every later
     * analysis (any village, any category) automatically sees their real eligibility, with nothing extra
     * for the frontend to resend. Anonymous callers are unaffected (user is null - no merge happens).
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResponse> analyze(@RequestBody AnalyzeRequest request,
                                                     @AuthenticationPrincipal User user) {
        if (user != null) {
            request.setApplicant(profileService.merge(profileService.getApplicantProfile(user), request.getApplicant()));
        }

        FinancialCalculatorService.FinancialResult financial = financialCalculatorService.calculate(request);
        FeasibilityScoreService.FeasibilityResult feasibility = feasibilityScoreService.calculate(request.getVillageName(), request.getBusinessCategory());
        DscrService.DscrResult dscr = dscrService.calculate(request.getBusinessCategory(), financial.emi());
        SurvivalSimulatorService.SimulationResult survival = survivalSimulatorService.simulate(
                new com.graminsaathi.dto.request.SimulateRequest(
                        request.getVillageName(), request.getBusinessCategory(),
                        request.getAvailableMarginCapital(), 0.0, 0.0
                )
        );
        BusinessCombinationService.CombinationResult combination = businessCombinationService.getCombination(request.getBusinessCategory());
        EvidenceService.EvidenceResult evidence = evidenceService.generateEvidence(request);
        AgentCommitteeService.CommitteeResult committee = agentCommitteeService.evaluate(request);
        BusinessHealthScoreService.HealthScoreResult health = businessHealthScoreService.calculate(request);
        SupplyChainRiskService.RiskResult supplyRisk = supplyChainRiskService.getRisk(request.getBusinessCategory());
        PeerBenchmarkService.BenchmarkResult benchmark = peerBenchmarkService.getBenchmark(request.getBusinessCategory());
        RoadmapMilestoneService.RoadmapResult roadmap = roadmapMilestoneService.getRoadmap(request.getBusinessCategory());

        AnalyzeResponse response = buildResponse(financial, feasibility, dscr, survival, combination, evidence, committee, health, supplyRisk, benchmark, roadmap);
        return ResponseEntity.ok(response);
    }

    private AnalyzeResponse buildResponse(FinancialCalculatorService.FinancialResult financial,
                                           FeasibilityScoreService.FeasibilityResult feasibility,
                                           DscrService.DscrResult dscr,
                                           SurvivalSimulatorService.SimulationResult survival,
                                           BusinessCombinationService.CombinationResult combination,
                                           EvidenceService.EvidenceResult evidence,
                                           AgentCommitteeService.CommitteeResult committee,
                                           BusinessHealthScoreService.HealthScoreResult health,
                                           SupplyChainRiskService.RiskResult supplyRisk,
                                           PeerBenchmarkService.BenchmarkResult benchmark,
                                           RoadmapMilestoneService.RoadmapResult roadmap) {
        AnalyzeResponse response = new AnalyzeResponse();

        response.setFinancial(buildFinancialResponse(financial));
        response.setFeasibility(buildFeasibilityResponse(feasibility));
        response.setDscr(buildDscrResponse(dscr));
        response.setSurvival(buildSurvivalResponse(survival));
        response.setCombination(buildCombinationResponse(combination));
        response.setEvidence(buildEvidenceResponse(evidence));
        response.setCommittee(buildCommitteeResponse(committee));
        response.setHealthScore(buildHealthScoreResponse(health));
        response.setSupplyRisk(buildSupplyRiskResponse(supplyRisk));
        response.setPeerBenchmark(buildPeerBenchmarkResponse(benchmark));
        response.setRoadmap(buildRoadmapResponse(roadmap));

        return response;
    }

    private FinancialResponse buildFinancialResponse(FinancialCalculatorService.FinancialResult f) {
        FinancialResponse r = new FinancialResponse();
        r.setProjectCost(f.projectCost());
        r.setLoanAmount(f.loanAmount());
        r.setSchemeName(f.schemeName());
        r.setInterestRateAnnual(f.interestRateAnnual());
        r.setTenureYears(f.tenureYears());
        r.setMoratoriumMonths(f.moratoriumMonths());
        r.setRepaymentMonths(f.repaymentMonths());
        r.setMonthlyRate(f.monthlyRate());
        r.setEmi(f.emi());
        r.setWorkingCapitalEstimate(f.workingCapitalEstimate());
        r.setRecommendedProjectCost(f.recommendedProjectCost());
        r.setRecommendedLoanAmount(f.recommendedLoanAmount());
        r.setBufferAmount(f.bufferAmount());
        r.setSchemeComparison(f.schemeComparison());
        r.setWorkingCapitalWarning(f.workingCapitalWarning());
        r.setLocalAveragePrice(f.localAveragePrice());
        r.setRecommendedPriceLow(f.recommendedPriceLow());
        r.setRecommendedPriceHigh(f.recommendedPriceHigh());
        r.setRecommendedLaunchPrice(f.recommendedLaunchPrice());
        r.setBreakevenPrice(f.breakevenPrice());
        r.setBreakevenNote(f.breakevenNote());
        return r;
    }

    private FeasibilityResponse buildFeasibilityResponse(FeasibilityScoreService.FeasibilityResult f) {
        FeasibilityResponse r = new FeasibilityResponse();
        r.setOpportunityScore(f.opportunityScore());
        r.setLabel(f.label());
        r.setCompetitorCount(f.competitorCount());
        r.setPopulation5kmRadius(f.population5kmRadius());
        r.setDemandRatio(f.demandRatio());
        return r;
    }

    private DscrResponse buildDscrResponse(DscrService.DscrResult d) {
        DscrResponse r = new DscrResponse();
        r.setDscr(d.dscr());
        r.setLabel(d.label());
        r.setMonthlyNetOperatingIncome(d.monthlyNetOperatingIncome());
        r.setEmi(d.emi());
        return r;
    }

    private SurvivalResponse buildSurvivalResponse(SurvivalSimulatorService.SimulationResult s) {
        SurvivalResponse r = new SurvivalResponse();
        r.setCashCurve(s.cashCurve().stream()
                .map(p -> new SurvivalResponse.CashPoint(p.month(), p.cumulativeCash()))
                .toList());
        r.setVerdict(s.verdict());
        r.setDeficitMonth(s.deficitMonth());
        return r;
    }

    private CombinationResponse buildCombinationResponse(BusinessCombinationService.CombinationResult c) {
        CombinationResponse r = new CombinationResponse();
        r.setSecondaryOpportunity(c.secondaryOpportunity());
        return r;
    }

    private EvidenceResponse buildEvidenceResponse(EvidenceService.EvidenceResult e) {
        EvidenceResponse r = new EvidenceResponse();
        r.setEvidence(e.evidence().stream()
                .map(item -> new EvidenceResponse.EvidenceItem(item.claim(), item.source(), item.details()))
                .toList());
        return r;
    }

    private CommitteeResponse buildCommitteeResponse(AgentCommitteeService.CommitteeResult c) {
        CommitteeResponse r = new CommitteeResponse();
        r.setMarketAgent(buildAgentOpinion(c.marketAgent()));
        r.setFinanceAgent(buildAgentOpinion(c.financeAgent()));
        r.setRiskAgent(buildAgentOpinion(c.riskAgent()));
        r.setFinalVerdict(c.finalVerdict());
        r.setVerdictReason(c.verdictReason());
        return r;
    }

    private CommitteeResponse.AgentOpinion buildAgentOpinion(AgentCommitteeService.AgentOpinion a) {
        CommitteeResponse.AgentOpinion r = new CommitteeResponse.AgentOpinion();
        r.setAgent(a.agent());
        r.setOpinion(a.opinion());
        r.setBasis(a.basis());
        return r;
    }

    private HealthScoreResponse buildHealthScoreResponse(BusinessHealthScoreService.HealthScoreResult h) {
        HealthScoreResponse r = new HealthScoreResponse();
        r.setOverallScore(h.overallScore());
        r.setRecommendation(h.recommendation());
        r.setMarketDemand(buildSubScore(h.marketDemand()));
        r.setCapitalAdequacy(buildSubScore(h.capitalAdequacy()));
        r.setProfitability(buildSubScore(h.profitability()));
        r.setCashFlow(buildSubScore(h.cashFlow()));
        r.setSupplyRisk(buildSubScore(h.supplyRisk()));
        r.setSeasonality(buildSubScore(h.seasonality()));
        return r;
    }

    private HealthScoreResponse.SubScore buildSubScore(BusinessHealthScoreService.SubScore s) {
        HealthScoreResponse.SubScore r = new HealthScoreResponse.SubScore();
        r.setName(s.name());
        r.setScore(s.score());
        r.setDescription(s.description());
        return r;
    }

    private SupplyRiskResponse buildSupplyRiskResponse(SupplyChainRiskService.RiskResult s) {
        SupplyRiskResponse r = new SupplyRiskResponse();
        r.setRiskLevel(s.riskLevel());
        r.setNote(s.note());
        return r;
    }

    private PeerBenchmarkResponse buildPeerBenchmarkResponse(PeerBenchmarkService.BenchmarkResult b) {
        PeerBenchmarkResponse r = new PeerBenchmarkResponse();
        r.setSampleSize(b.sampleSize());
        r.setAvgMonthlyRevenueAfter6Months(b.avgMonthlyRevenueAfter6Months());
        r.setPctStillOperatingAfter1Year(b.pctStillOperatingAfter1Year());
        r.setDisclaimer(b.disclaimer());
        return r;
    }

    private RoadmapResponse buildRoadmapResponse(RoadmapMilestoneService.RoadmapResult r) {
        RoadmapResponse response = new RoadmapResponse();
        response.setMilestones(r.milestones().stream()
                .map(m -> new RoadmapResponse.Milestone(m.month(), m.milestone()))
                .toList());
        return response;
    }
}