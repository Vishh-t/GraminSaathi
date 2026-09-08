package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.AnalyzeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessHealthScoreServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;
    @Mock
    private FinancialCalculatorService financialCalculatorService;
    @Mock
    private FeasibilityScoreService feasibilityScoreService;
    @Mock
    private DscrService dscrService;

    private BusinessHealthScoreService businessHealthScoreService;

    @BeforeEach
    void setUp() {
        businessHealthScoreService = new BusinessHealthScoreService(demoDataLoader, financialCalculatorService, feasibilityScoreService, dscrService);
    }

    @Test
    void testProceedRecommendation() {
        setupMocks(80, true, 2.5, 2, 45000, 31500);

        BusinessHealthScoreService.HealthScoreResult result = businessHealthScoreService.calculate(
                new AnalyzeRequest("Ghoti", "Dairy", 200000.0)
        );

        assertEquals("🟢 Proceed", result.recommendation());
        assertTrue(result.overallScore() >= 75);
        assertEquals(80, result.marketDemand().score());
        assertEquals(100, result.capitalAdequacy().score());
        assertEquals(100, result.cashFlow().score());
    }

    @Test
    void testProceedWithModifications() {
        setupMocks(50, true, 1.5, 10, 35000, 24500);

        BusinessHealthScoreService.HealthScoreResult result = businessHealthScoreService.calculate(
                new AnalyzeRequest("Ghoti", "Retail / Kirana Store", 50000.0)
        );

        assertEquals("🟡 Proceed with modifications", result.recommendation());
        assertTrue(result.overallScore() >= 50 && result.overallScore() < 75);
    }

    @Test
    void testDoNotFinance() {
        // Use a financial result with higher recommended project cost to trigger low capital adequacy
        FinancialCalculatorService.FinancialResult financial = new FinancialCalculatorService.FinancialResult(
                100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                0.065/12, 5000.0, 94500.0, 500000, 450000, 0.0, // higher recommended project cost
                List.of(), null, 42.0, 39.9, 44.1, 40.74, 35.0, "breakeven note"
        );
        when(financialCalculatorService.calculate(any(AnalyzeRequest.class))).thenReturn(financial);

        FeasibilityScoreService.FeasibilityResult feasibility = new FeasibilityScoreService.FeasibilityResult(
                20, "Low", 15, 8420, 4210.0
        );
        when(feasibilityScoreService.calculate(anyString(), anyString())).thenReturn(feasibility);

        DscrService.DscrResult dscr = new DscrService.DscrResult(0.8, "Risky", 5400.0, 5000.0);
        when(dscrService.calculate(anyString(), anyDouble())).thenReturn(dscr);

        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setReferenceMonthlyRevenue(18000.0);
        category.setReferenceMonthlyOperatingCost(12600.0);
        when(demoDataLoader.getBusinessCategory(anyString())).thenReturn(category);

        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(15);
        when(demoDataLoader.getBusinessData(anyString(), anyString())).thenReturn(businessData);

        BusinessHealthScoreService.HealthScoreResult result = businessHealthScoreService.calculate(
                new AnalyzeRequest("Ghoti", "Tailoring", 10000.0)
        );

        assertEquals("🔴 Do not finance as structured", result.recommendation());
        assertTrue(result.overallScore() < 50);
    }

    @Test
    void testAllSubScoresPresent() {
        setupMocks(60, true, 1.8, 5, 45000, 31500);

        BusinessHealthScoreService.HealthScoreResult result = businessHealthScoreService.calculate(
                new AnalyzeRequest("Ghoti", "Dairy", 100000.0)
        );

        assertNotNull(result.marketDemand());
        assertNotNull(result.capitalAdequacy());
        assertNotNull(result.profitability());
        assertNotNull(result.cashFlow());
        assertNotNull(result.supplyRisk());
        assertNotNull(result.seasonality());

        // All should have different scores for a real case
        assertTrue(result.marketDemand().score() >= 0 && result.marketDemand().score() <= 100);
        assertTrue(result.capitalAdequacy().score() >= 0 && result.capitalAdequacy().score() <= 100);
        assertTrue(result.profitability().score() >= 0 && result.profitability().score() <= 100);
        assertTrue(result.cashFlow().score() >= 0 && result.cashFlow().score() <= 100);
        assertTrue(result.supplyRisk().score() >= 20 && result.supplyRisk().score() <= 100);
        assertEquals(70, result.seasonality().score());
    }

    private void setupMocks(int opportunityScore, boolean sufficientMargin, double dscrValue, int competitors, double revenue, double opCost) {
        FinancialCalculatorService.FinancialResult financial = new FinancialCalculatorService.FinancialResult(
                100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                0.065/12, 5000.0, 94500.0, 100000, 90000, 0.0,
                List.of(), null, 42.0, 39.9, 44.1, 40.74, 35.0, "breakeven note"
        );
        when(financialCalculatorService.calculate(any(AnalyzeRequest.class))).thenReturn(financial);

        FeasibilityScoreService.FeasibilityResult feasibility = new FeasibilityScoreService.FeasibilityResult(
                opportunityScore, opportunityScore >= 70 ? "High" : (opportunityScore >= 40 ? "Moderate" : "Low"),
                competitors, 8420, 4210.0
        );
        when(feasibilityScoreService.calculate(anyString(), anyString())).thenReturn(feasibility);

        DscrService.DscrResult dscr = new DscrService.DscrResult(dscrValue, dscrValue >= 2.0 ? "Healthy" : (dscrValue >= 1.0 ? "Moderate" : "Risky"), revenue - opCost, 5000.0);
        when(dscrService.calculate(anyString(), anyDouble())).thenReturn(dscr);

        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setReferenceMonthlyRevenue(revenue);
        category.setReferenceMonthlyOperatingCost(opCost);
        when(demoDataLoader.getBusinessCategory(anyString())).thenReturn(category);

        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(competitors);
        when(demoDataLoader.getBusinessData(anyString(), anyString())).thenReturn(businessData);

        // Margin check
        double recommendedProjectCost = 100000.0;
        double marginRequired = recommendedProjectCost * 0.10;
        double availableMargin = sufficientMargin ? marginRequired + 50000 : marginRequired - 10000;
        // We can't easily mock the margin check, so we'll rely on the actual calculation
    }
}