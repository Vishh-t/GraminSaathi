package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.AnalyzeRequest;
import com.graminsaathi.dto.request.SimulateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentCommitteeServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;
    @Mock
    private FinancialCalculatorService financialCalculatorService;
    @Mock
    private FeasibilityScoreService feasibilityScoreService;
    @Mock
    private DscrService dscrService;
    @Mock
    private SurvivalSimulatorService survivalSimulatorService;

    private AgentCommitteeService agentCommitteeService;

    @BeforeEach
    void setUp() {
        agentCommitteeService = new AgentCommitteeService(demoDataLoader, financialCalculatorService, feasibilityScoreService, dscrService, survivalSimulatorService);
    }

    @Test
    void testViableVerdict() {
        setupMocks(85, 2.7, null);

        AgentCommitteeService.CommitteeResult result = agentCommitteeService.evaluate(
                new AnalyzeRequest("Ghoti", "Dairy", 10000.0)
        );

        assertEquals("VIABLE", result.finalVerdict());
        assertTrue(result.verdictReason().contains("Strong market opportunity"));
        assertEquals("Market Agent", result.marketAgent().agent());
        assertEquals("Finance Agent", result.financeAgent().agent());
        assertEquals("Risk Agent", result.riskAgent().agent());
    }

    @Test
    void testViableWithModifications() {
        setupMocks(50, 1.2, null);

        AgentCommitteeService.CommitteeResult result = agentCommitteeService.evaluate(
                new AnalyzeRequest("Ghoti", "Dairy", 10000.0)
        );

        assertEquals("VIABLE WITH MODIFICATIONS", result.finalVerdict());
        assertTrue(result.verdictReason().contains("Adequate market opportunity"));
    }

    @Test
    void testNotRecommended() {
        setupMocks(30, 0.8, 5);

        AgentCommitteeService.CommitteeResult result = agentCommitteeService.evaluate(
                new AnalyzeRequest("Ghoti", "Dairy", 10000.0)
        );

        assertEquals("NOT RECOMMENDED AS STRUCTURED", result.finalVerdict());
        assertTrue(result.verdictReason().contains("Insufficient"));
    }

    @Test
    void testMarketAgentOpinions() {
        setupMocks(85, 2.7, null);
        AgentCommitteeService.CommitteeResult result = agentCommitteeService.evaluate(
                new AnalyzeRequest("Ghoti", "Dairy", 10000.0)
        );
        assertTrue(result.marketAgent().opinion().contains("Strong local demand"));

        setupMocks(50, 2.7, null);
        result = agentCommitteeService.evaluate(new AnalyzeRequest("Ghoti", "Dairy", 10000.0));
        assertTrue(result.marketAgent().opinion().contains("Moderate opportunity"));

        setupMocks(30, 2.7, null);
        result = agentCommitteeService.evaluate(new AnalyzeRequest("Ghoti", "Dairy", 10000.0));
        assertTrue(result.marketAgent().opinion().contains("Saturated market"));
    }

    @Test
    void testFinanceAgentOpinions() {
        setupMocks(50, 2.5, null);
        AgentCommitteeService.CommitteeResult result = agentCommitteeService.evaluate(
                new AnalyzeRequest("Ghoti", "Dairy", 10000.0)
        );
        assertTrue(result.financeAgent().opinion().contains("Excellent debt coverage"));

        setupMocks(50, 1.5, null);
        result = agentCommitteeService.evaluate(new AnalyzeRequest("Ghoti", "Dairy", 10000.0));
        assertTrue(result.financeAgent().opinion().contains("Adequate debt coverage"));

        setupMocks(50, 0.8, null);
        result = agentCommitteeService.evaluate(new AnalyzeRequest("Ghoti", "Dairy", 10000.0));
        assertTrue(result.financeAgent().opinion().contains("Insufficient debt coverage"));
    }

    @Test
    void testRiskAgentOpinions() {
        setupMocks(50, 2.0, null);
        AgentCommitteeService.CommitteeResult result = agentCommitteeService.evaluate(
                new AnalyzeRequest("Ghoti", "Dairy", 10000.0)
        );
        assertTrue(result.riskAgent().opinion().contains("positive cash flow"));

        setupMocks(50, 2.0, 10);
        result = agentCommitteeService.evaluate(new AnalyzeRequest("Ghoti", "Dairy", 10000.0));
        assertTrue(result.riskAgent().opinion().contains("deficit"));
    }

    private void setupMocks(int opportunityScore, double dscrValue, Integer deficitMonth) {
        FinancialCalculatorService.FinancialResult financial = new FinancialCalculatorService.FinancialResult(
                100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                0.065/12, 5000.0, 94500.0, 100000, 90000, 0.0,
                List.of(), null, 42.0, 39.9, 44.1, 40.74, 35.0, "breakeven note"
        );
        when(financialCalculatorService.calculate(any(AnalyzeRequest.class))).thenReturn(financial);

        FeasibilityScoreService.FeasibilityResult feasibility = new FeasibilityScoreService.FeasibilityResult(
                opportunityScore, opportunityScore >= 70 ? "High opportunity" : (opportunityScore >= 40 ? "Moderate opportunity" : "Low opportunity (saturated)"),
                2, 8420, 4210.0
        );
        when(feasibilityScoreService.calculate(anyString(), anyString())).thenReturn(feasibility);

        DscrService.DscrResult dscr = new DscrService.DscrResult(dscrValue, dscrValue >= 2.0 ? "Healthy" : (dscrValue >= 1.0 ? "Moderate" : "Risky"), 10000.0, 5000.0);
        when(dscrService.calculate(anyString(), anyDouble())).thenReturn(dscr);

        SurvivalSimulatorService.SimulationResult survival = new SurvivalSimulatorService.SimulationResult(
                List.of(), deficitMonth == null ? "survives" : "deficit", deficitMonth
        );
        when(survivalSimulatorService.simulate(any(SimulateRequest.class))).thenReturn(survival);
    }
}