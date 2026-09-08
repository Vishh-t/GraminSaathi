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
class EvidenceServiceTest {

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

    private EvidenceService evidenceService;

    @BeforeEach
    void setUp() {
        evidenceService = new EvidenceService(demoDataLoader, financialCalculatorService, feasibilityScoreService, dscrService, survivalSimulatorService);
    }

    @Test
    void testGenerateEvidence() {
        DemoData.VillageData village = new DemoData.VillageData();
        village.setVillageName("Ghoti");
        village.setPopulation5kmRadius(8420);

        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Dairy");
        category.setReferenceProjectCost(1000000.0);
        category.setReferenceMonthlyRevenue(45000.0);
        category.setReferenceMonthlyOperatingCost(31500.0);
        category.setWorkingCapitalMonths(3);
        category.setSecondaryOpportunity("Vermicompost");
        category.setSourceNote("Test source");
        category.setPrimaryInputDependencyNote("Fodder note");
        category.setSingleBuyerDependencyRisk("Medium");
        DemoData.PeerBenchmark peer = new DemoData.PeerBenchmark();
        peer.setSampleSize(24);
        peer.setAvgMonthlyRevenueAfter6Months(41000.0);
        peer.setPctStillOperatingAfter1Year(78);
        category.setPeerBenchmark(peer);
        List<DemoData.RoadmapMilestone> milestones = List.of(
                new DemoData.RoadmapMilestone(1, "Test milestone")
        );
        category.setRoadmapMilestones(milestones);

        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(2);
        businessData.setAvgLocalPrice(42.0);

        when(demoDataLoader.getVillage("Ghoti")).thenReturn(village);
        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);
        when(demoDataLoader.getBusinessData("Ghoti", "Dairy")).thenReturn(businessData);

        FinancialCalculatorService.FinancialResult financial = new FinancialCalculatorService.FinancialResult(
                100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                0.065/12, 5000.0, 94500.0, 100000, 90000, 0.0,
                List.of(), null, 42.0, 39.9, 44.1, 40.74, 35.0, "breakeven note"
        );
        when(financialCalculatorService.calculate(any(AnalyzeRequest.class))).thenReturn(financial);

        FeasibilityScoreService.FeasibilityResult feasibility = new FeasibilityScoreService.FeasibilityResult(
                85, "High opportunity", 2, 8420, 4210.0
        );
        when(feasibilityScoreService.calculate(anyString(), anyString())).thenReturn(feasibility);

        DscrService.DscrResult dscr = new DscrService.DscrResult(2.7, "Healthy", 13500.0, 5000.0);
        when(dscrService.calculate(anyString(), anyDouble())).thenReturn(dscr);

        SurvivalSimulatorService.SimulationResult survival = new SurvivalSimulatorService.SimulationResult(
                List.of(), "survives", null
        );
        when(survivalSimulatorService.simulate(any(SimulateRequest.class))).thenReturn(survival);

        EvidenceService.EvidenceResult result = evidenceService.generateEvidence(
                new AnalyzeRequest("Ghoti", "Dairy", 10000.0)
        );

        assertFalse(result.evidence().isEmpty());
        assertTrue(result.evidence().size() >= 10);

        // Check specific evidence items exist
        assertTrue(result.evidence().stream().anyMatch(e -> e.claim().contains("Project Cost")));
        assertTrue(result.evidence().stream().anyMatch(e -> e.claim().contains("Scheme")));
        assertTrue(result.evidence().stream().anyMatch(e -> e.claim().contains("EMI")));
        assertTrue(result.evidence().stream().anyMatch(e -> e.claim().contains("Opportunity Score")));
        assertTrue(result.evidence().stream().anyMatch(e -> e.claim().contains("DSCR")));
        assertTrue(result.evidence().stream().anyMatch(e -> e.claim().contains("Survival")));
    }

    @Test
    void testEvidenceRetailNoPrice() {
        DemoData.VillageData village = new DemoData.VillageData();
        village.setVillageName("Ghoti");
        village.setPopulation5kmRadius(8420);

        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Retail / Kirana Store");
        category.setReferenceProjectCost(500000.0);
        category.setReferenceMonthlyRevenue(35000.0);
        category.setReferenceMonthlyOperatingCost(24500.0);
        category.setWorkingCapitalMonths(2);
        category.setSecondaryOpportunity("Bill payment");
        category.setSourceNote("Test");
        category.setPrimaryInputDependencyNote("Diversify");
        category.setSingleBuyerDependencyRisk("Low");
        DemoData.PeerBenchmark peer = new DemoData.PeerBenchmark();
        peer.setSampleSize(30);
        peer.setAvgMonthlyRevenueAfter6Months(32000.0);
        peer.setPctStillOperatingAfter1Year(82);
        category.setPeerBenchmark(peer);
        category.setRoadmapMilestones(List.of(new DemoData.RoadmapMilestone(1, "Test")));

        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(14);
        businessData.setAvgLocalPrice(null);

        when(demoDataLoader.getVillage("Ghoti")).thenReturn(village);
        when(demoDataLoader.getBusinessCategory("Retail / Kirana Store")).thenReturn(category);
        when(demoDataLoader.getBusinessData("Ghoti", "Retail / Kirana Store")).thenReturn(businessData);

        FinancialCalculatorService.FinancialResult financial = new FinancialCalculatorService.FinancialResult(
                100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                0.065/12, 5000.0, 49000.0, 100000, 90000, 0.0,
                List.of(), null, 0.0, null, null, null, null, "Not applicable — mixed product pricing."
        );
        when(financialCalculatorService.calculate(any(AnalyzeRequest.class))).thenReturn(financial);

        FeasibilityScoreService.FeasibilityResult feasibility = new FeasibilityScoreService.FeasibilityResult(
                30, "Low opportunity (saturated)", 14, 8420, 601.0
        );
        when(feasibilityScoreService.calculate(anyString(), anyString())).thenReturn(feasibility);

        DscrService.DscrResult dscr = new DscrService.DscrResult(1.5, "Moderate", 10500.0, 7000.0);
        when(dscrService.calculate(anyString(), anyDouble())).thenReturn(dscr);

        SurvivalSimulatorService.SimulationResult survival = new SurvivalSimulatorService.SimulationResult(
                List.of(), "survives", null
        );
        when(survivalSimulatorService.simulate(any(SimulateRequest.class))).thenReturn(survival);

        EvidenceService.EvidenceResult result = evidenceService.generateEvidence(
                new AnalyzeRequest("Ghoti", "Retail / Kirana Store", 10000.0)
        );

        // Should have evidence about no local price
        // For Retail, breakevenPrice is null so breakevenNote is not added to evidence
        // But other evidence should still be generated
        assertFalse(result.evidence().isEmpty());
        assertTrue(result.evidence().stream().anyMatch(e -> e.claim().contains("Opportunity Score")));
    }
}