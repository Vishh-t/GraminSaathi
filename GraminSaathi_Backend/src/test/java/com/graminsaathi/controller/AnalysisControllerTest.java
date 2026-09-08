package com.graminsaathi.controller;

import com.graminsaathi.dto.request.AnalyzeRequest;
import com.graminsaathi.dto.request.SimulateRequest;
import com.graminsaathi.dto.response.AnalyzeResponse;
import com.graminsaathi.service.FinancialCalculatorService;
import com.graminsaathi.service.FeasibilityScoreService;
import com.graminsaathi.service.DscrService;
import com.graminsaathi.service.SurvivalSimulatorService;
import com.graminsaathi.service.BusinessCombinationService;
import com.graminsaathi.service.EvidenceService;
import com.graminsaathi.service.AgentCommitteeService;
import com.graminsaathi.service.BusinessHealthScoreService;
import com.graminsaathi.service.SupplyChainRiskService;
import com.graminsaathi.service.PeerBenchmarkService;
import com.graminsaathi.service.RoadmapMilestoneService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FinancialCalculatorService financialCalculatorService;
    @MockBean
    private FeasibilityScoreService feasibilityScoreService;
    @MockBean
    private DscrService dscrService;
    @MockBean
    private SurvivalSimulatorService survivalSimulatorService;
    @MockBean
    private BusinessCombinationService businessCombinationService;
    @MockBean
    private EvidenceService evidenceService;
    @MockBean
    private AgentCommitteeService agentCommitteeService;
    @MockBean
    private BusinessHealthScoreService businessHealthScoreService;
    @MockBean
    private SupplyChainRiskService supplyChainRiskService;
    @MockBean
    private PeerBenchmarkService peerBenchmarkService;
    @MockBean
    private RoadmapMilestoneService roadmapMilestoneService;

    @Test
    void analyze() throws Exception {
        // Setup minimal mocks for all services
        when(financialCalculatorService.calculate(any(AnalyzeRequest.class))).thenReturn(
                new FinancialCalculatorService.FinancialResult(
                        100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                        0.065/12, 5000.0, 94500.0, 100000, 90000, 0.0,
                        List.of(), null, 42.0, 39.9, 44.1, 40.74, 35.0, "breakeven"
                )
        );

        when(feasibilityScoreService.calculate(anyString(), anyString())).thenReturn(
                new FeasibilityScoreService.FeasibilityResult(85, "High opportunity", 2, 8420, 4210.0)
        );

        when(dscrService.calculate(anyString(), anyDouble())).thenReturn(
                new DscrService.DscrResult(2.7, "Healthy", 13500.0, 5000.0)
        );

        when(survivalSimulatorService.simulate(any(SimulateRequest.class))).thenReturn(
                new SurvivalSimulatorService.SimulationResult(List.of(), "survives", null)
        );

        when(businessCombinationService.getCombination(anyString())).thenReturn(
                new BusinessCombinationService.CombinationResult("Vermicompost")
        );

        when(evidenceService.generateEvidence(any(AnalyzeRequest.class))).thenReturn(
                new EvidenceService.EvidenceResult(List.of())
        );

        when(agentCommitteeService.evaluate(any(AnalyzeRequest.class))).thenReturn(
                new AgentCommitteeService.CommitteeResult(
                        new AgentCommitteeService.AgentOpinion("Market", "Good", "Basis"),
                        new AgentCommitteeService.AgentOpinion("Finance", "Good", "Basis"),
                        new AgentCommitteeService.AgentOpinion("Risk", "Good", "Basis"),
                        "VIABLE", "Reason"
                )
        );

        when(businessHealthScoreService.calculate(any())).thenReturn(
                new BusinessHealthScoreService.HealthScoreResult(
                        80, "🟢 Proceed",
                        new BusinessHealthScoreService.SubScore("Market", 80, "Good"),
                        new BusinessHealthScoreService.SubScore("Capital", 100, "Good"),
                        new BusinessHealthScoreService.SubScore("Profit", 60, "Good"),
                        new BusinessHealthScoreService.SubScore("Cash", 100, "Good"),
                        new BusinessHealthScoreService.SubScore("Supply", 70, "Good"),
                        new BusinessHealthScoreService.SubScore("Season", 70, "Good")
                )
        );

        when(supplyChainRiskService.getRisk(any())).thenReturn(
                new SupplyChainRiskService.RiskResult("Low", "Note")
        );

        when(peerBenchmarkService.getBenchmark(any())).thenReturn(
                new PeerBenchmarkService.BenchmarkResult(24, 41000.0, 78, "Disclaimer")
        );

        when(roadmapMilestoneService.getRoadmap(any())).thenReturn(
                new RoadmapMilestoneService.RoadmapResult(List.of(
                        new RoadmapMilestoneService.Milestone(1, "Test")
                ))
        );

        AnalyzeRequest request = new AnalyzeRequest("Ghoti", "Dairy", 10000.0);

        mockMvc.perform(post("/api/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financial.projectCost").value(100000))
                .andExpect(jsonPath("$.feasibility.opportunityScore").value(85))
                .andExpect(jsonPath("$.committee.finalVerdict").value("VIABLE"));
    }
}