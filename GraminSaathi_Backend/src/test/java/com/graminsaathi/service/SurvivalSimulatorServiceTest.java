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
class SurvivalSimulatorServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;

    @Mock
    private FinancialCalculatorService financialCalculatorService;

    private SurvivalSimulatorService survivalSimulatorService;

    @BeforeEach
    void setUp() {
        survivalSimulatorService = new SurvivalSimulatorService(demoDataLoader, financialCalculatorService);
    }

    @Test
    void testBaseCaseSurvives() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setReferenceMonthlyRevenue(45000.0);
        category.setReferenceMonthlyOperatingCost(31500.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        FinancialCalculatorService.FinancialResult financial = new FinancialCalculatorService.FinancialResult(
                100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                0.065/12, 5000.0, 94500.0, 100000, 90000, 0.0,
                List.of(), null, 42.0, 39.9, 44.1, 40.74, 35.0, "breakeven note"
        );

        when(financialCalculatorService.calculate(any())).thenReturn(financial);

        SurvivalSimulatorService.SimulationResult result = survivalSimulatorService.simulate(
                new SimulateRequest("Ghoti", "Dairy", 10000.0, 0.0, 0.0)
        );

        assertEquals(24, result.cashCurve().size());
        assertNull(result.deficitMonth());
        assertTrue(result.verdict().contains("survives comfortably"));
    }

    @Test
    void testRevenueShockCausesDeficit() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setReferenceMonthlyRevenue(45000.0);
        category.setReferenceMonthlyOperatingCost(31500.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        FinancialCalculatorService.FinancialResult financial = new FinancialCalculatorService.FinancialResult(
                100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                0.065/12, 5000.0, 94500.0, 100000, 90000, 0.0,
                List.of(), null, 42.0, 39.9, 44.1, 40.74, 35.0, "breakeven note"
        );

        when(financialCalculatorService.calculate(any())).thenReturn(financial);

        // 50% revenue shock: 45000 * 0.5 = 22500 revenue, cost 31500, EMI 5000 after moratorium
        // Net = 22500 - 31500 - 5000 = -14000/month after moratorium
        SurvivalSimulatorService.SimulationResult result = survivalSimulatorService.simulate(
                new SimulateRequest("Ghoti", "Dairy", 10000.0, -0.5, 0.0)
        );

        assertNotNull(result.deficitMonth());
        assertTrue(result.deficitMonth() <= 10);
        assertTrue(result.verdict().contains("deficit"));
    }

    @Test
    void testCostShockCausesDeficit() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setReferenceMonthlyRevenue(45000.0);
        category.setReferenceMonthlyOperatingCost(31500.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        FinancialCalculatorService.FinancialResult financial = new FinancialCalculatorService.FinancialResult(
                100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                0.065/12, 5000.0, 94500.0, 100000, 90000, 0.0,
                List.of(), null, 42.0, 39.9, 44.1, 40.74, 35.0, "breakeven note"
        );

        when(financialCalculatorService.calculate(any())).thenReturn(financial);

        // 100% cost shock: cost = 63000, revenue = 45000
        SurvivalSimulatorService.SimulationResult result = survivalSimulatorService.simulate(
                new SimulateRequest("Ghoti", "Dairy", 10000.0, 0.0, 1.0)
        );

        assertNotNull(result.deficitMonth());
    }

    @Test
    void testCashCurveHas24Entries() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setReferenceMonthlyRevenue(45000.0);
        category.setReferenceMonthlyOperatingCost(31500.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        FinancialCalculatorService.FinancialResult financial = new FinancialCalculatorService.FinancialResult(
                100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                0.065/12, 5000.0, 94500.0, 100000, 90000, 0.0,
                List.of(), null, 42.0, 39.9, 44.1, 40.74, 35.0, "breakeven note"
        );

        when(financialCalculatorService.calculate(any())).thenReturn(financial);

        SurvivalSimulatorService.SimulationResult result = survivalSimulatorService.simulate(
                new SimulateRequest("Ghoti", "Dairy", 10000.0, 0.0, 0.0)
        );

        assertEquals(24, result.cashCurve().size());
        for (int i = 0; i < 24; i++) {
            assertEquals(i + 1, result.cashCurve().get(i).month());
        }
    }

    @Test
    void testMoratoriumPeriodNoEmi() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setReferenceMonthlyRevenue(10000.0);
        category.setReferenceMonthlyOperatingCost(5000.0);

        when(demoDataLoader.getBusinessCategory("Test")).thenReturn(category);

        FinancialCalculatorService.FinancialResult financial = new FinancialCalculatorService.FinancialResult(
                100000, 90000, "Micro Finance Scheme", 0.065, 3, 3, 33,
                0.065/12, 5000.0, 94500.0, 100000, 90000, 0.0,
                List.of(), null, 42.0, 39.9, 44.1, 40.74, 35.0, "breakeven note"
        );

        when(financialCalculatorService.calculate(any(AnalyzeRequest.class))).thenReturn(financial);

        SurvivalSimulatorService.SimulationResult result = survivalSimulatorService.simulate(
                new SimulateRequest("Ghoti", "Test", 10000.0, 0.0, 0.0)
        );

        // First 3 months (moratorium) should not have EMI, so cash decline is less steep
        // Month 0: initial = workingCapital = 94500
        // Month 1: +5000 (revenue - cost) = 99500
        // Month 2: +5000 = 104500
        // Month 3: +5000 = 109500 (still no EMI)
        // Month 4: +5000 - 5000 (EMI) = 109500 (flat)
        // So cash at month 3 > month 0
        assertTrue(result.cashCurve().get(3).cumulativeCash() > result.cashCurve().get(0).cumulativeCash());
        
        // After moratorium, EMI kicks in
        assertTrue(result.cashCurve().get(4).cumulativeCash() <= result.cashCurve().get(3).cumulativeCash());
    }
}