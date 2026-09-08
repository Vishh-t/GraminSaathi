package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.data.SchemesDataLoader;
import com.graminsaathi.data.SchemesReference;
import com.graminsaathi.dto.request.AnalyzeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyDouble;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinancialCalculatorServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;

    @Mock
    private SchemesDataLoader schemesDataLoader;

    private FinancialCalculatorService financialCalculatorService;

    @BeforeEach
    void setUp() {
        financialCalculatorService = new FinancialCalculatorService(demoDataLoader, schemesDataLoader);
    }

    @Test
    void testMicroFinanceSchemeCalculation() {
        // Given: margin = 10,000 -> project_cost = 100,000 (Micro Finance Scheme)
        DemoData.BusinessCategoryData category = createMockCategory("Dairy", 1000000, 31500, 45000, 3);
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(2);
        businessData.setAvgLocalPrice(42.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);
        when(demoDataLoader.getBusinessData("Ghoti", "Dairy")).thenReturn(businessData);

        SchemesReference.Scheme microScheme = createMockScheme("Micro Finance Scheme", 0, 140000, 0.065, 3, 3);
        when(schemesDataLoader.getPrimaryScheme(100000)).thenReturn(microScheme);
        when(schemesDataLoader.getApplicableSchemes(100000)).thenReturn(List.of(microScheme));

        AnalyzeRequest request = new AnalyzeRequest();
        request.setVillageName("Ghoti");
        request.setBusinessCategory("Dairy");
        request.setAvailableMarginCapital(10000.0);

        // When
        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request);

        // Then
        assertEquals(100000.0, result.projectCost(), 0.01);
        assertEquals(90000.0, result.loanAmount(), 0.01);
        assertEquals("Micro Finance Scheme", result.schemeName());
        assertEquals(0.065, result.interestRateAnnual(), 0.0001);
        assertEquals(3, result.tenureYears());
        assertEquals(3, result.moratoriumMonths());
        assertEquals(33, result.repaymentMonths());
        
        // EMI calculation: P=90000, r=0.065/12, n=33
        double monthlyRate = 0.065 / 12;
        double expectedEmi = 90000 * monthlyRate * Math.pow(1 + monthlyRate, 33) / (Math.pow(1 + monthlyRate, 33) - 1);
        assertEquals(expectedEmi, result.emi(), 0.01);

        assertEquals(94500.0, result.workingCapitalEstimate(), 0.01); // 31500 * 3
        assertEquals(100000.0, result.recommendedProjectCost(), 0.01); // min(100000, 1000000)
        assertEquals(90000.0, result.recommendedLoanAmount(), 0.01);
        assertEquals(0.0, result.bufferAmount(), 0.01);
    }

    @Test
    void testTermLoanSchemeCalculation() {
        // Given: margin = 500,000 -> project_cost = 5,000,000 (Term Loan Scheme)
        DemoData.BusinessCategoryData category = createMockCategory("Dairy", 1000000, 31500, 45000, 3);
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(2);
        businessData.setAvgLocalPrice(42.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);
        when(demoDataLoader.getBusinessData("Ghoti", "Dairy")).thenReturn(businessData);

        SchemesReference.Scheme termScheme = createMockScheme("Term Loan Scheme", 140001, 5000000, 0.08, 7, 6);
        when(schemesDataLoader.getPrimaryScheme(5000000)).thenReturn(termScheme);
        when(schemesDataLoader.getApplicableSchemes(5000000)).thenReturn(List.of(termScheme));

        AnalyzeRequest request = new AnalyzeRequest();
        request.setVillageName("Ghoti");
        request.setBusinessCategory("Dairy");
        request.setAvailableMarginCapital(500000.0);

        // When
        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request);

        // Then
        assertEquals(5000000.0, result.projectCost(), 0.01);
        assertEquals(4500000.0, result.loanAmount(), 0.01);
        assertEquals("Term Loan Scheme", result.schemeName());
        assertEquals(0.08, result.interestRateAnnual(), 0.0001);
        assertEquals(7, result.tenureYears());
        assertEquals(6, result.moratoriumMonths());
        assertEquals(78, result.repaymentMonths()); // 7*12 - 6

        assertEquals(1000000.0, result.recommendedProjectCost(), 0.01); // min(5000000, 1000000)
        assertEquals(900000.0, result.recommendedLoanAmount(), 0.01);
        assertEquals(3600000.0, result.bufferAmount(), 0.01); // 4500000 - 900000
    }

    @Test
    void testProjectCostExceedsCeiling() {
        DemoData.BusinessCategoryData category = createMockCategory("Dairy", 1000000, 31500, 45000, 3);
        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);
        when(schemesDataLoader.getPrimaryScheme(6000000)).thenReturn(null);

        AnalyzeRequest request = new AnalyzeRequest();
        request.setVillageName("Ghoti");
        request.setBusinessCategory("Dairy");
        request.setAvailableMarginCapital(600000.0);

        assertThrows(IllegalArgumentException.class, () -> financialCalculatorService.calculate(request));
    }

    @Test
    void testWorkingCapitalWarning() {
        DemoData.BusinessCategoryData category = createMockCategory("Dairy", 1000000, 31500, 45000, 3);
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(2);
        businessData.setAvgLocalPrice(42.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);
        when(demoDataLoader.getBusinessData("Ghoti", "Dairy")).thenReturn(businessData);

        SchemesReference.Scheme microScheme = createMockScheme("Micro Finance Scheme", 0, 140000, 0.065, 3, 3);
        when(schemesDataLoader.getPrimaryScheme(100000)).thenReturn(microScheme);
        when(schemesDataLoader.getApplicableSchemes(100000)).thenReturn(List.of(microScheme));

        // Margin = 10,000, project_cost = 100,000, recommended = 100,000
        // margin_required = 10,000, remaining = 0, working_capital = 94,500
        // 0 < 94,500 -> warning should be generated
        AnalyzeRequest request = new AnalyzeRequest();
        request.setVillageName("Ghoti");
        request.setBusinessCategory("Dairy");
        request.setAvailableMarginCapital(10000.0);

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request);

        assertNotNull(result.workingCapitalWarning());
        assertTrue(result.workingCapitalWarning().contains("⚠️"));
        assertTrue(result.workingCapitalWarning().contains("94500"));
    }

    @Test
    void testNoWorkingCapitalWarningWhenSufficientMargin() {
        DemoData.BusinessCategoryData category = createMockCategory("Dairy", 1000000, 31500, 45000, 3);
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(2);
        businessData.setAvgLocalPrice(42.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);
        when(demoDataLoader.getBusinessData("Ghoti", "Dairy")).thenReturn(businessData);

        SchemesReference.Scheme microScheme = createMockScheme("Micro Finance Scheme", 0, 140000, 0.065, 3, 3);
        when(schemesDataLoader.getPrimaryScheme(anyDouble())).thenReturn(microScheme);
        when(schemesDataLoader.getApplicableSchemes(anyDouble())).thenReturn(List.of(microScheme));

        // Margin = 14,000, project_cost = 140,000 (Micro Finance max), recommended = 140,000
        // margin_required = 14,000, remaining = 0, working_capital = 94,500
        // 0 < 94,500 -> warning IS generated

        // Margin = 100,000, project_cost = 1,000,000, recommended = 1,000,000
        // margin_required = 100,000, remaining = 0, working_capital = 94,500
        // 0 < 94,500 -> warning IS generated

        // We need remaining >= 94,500, so margin >= 100,000 + 94,500 = 194,500
        // project_cost = 1,945,000, recommended = 1,000,000 (capped), margin_required = 100,000, remaining = 94,500
        // That equals working_capital so no warning
        AnalyzeRequest request = new AnalyzeRequest();
        request.setVillageName("Ghoti");
        request.setBusinessCategory("Dairy");
        request.setAvailableMarginCapital(194500.0); // remaining = 194500 - 100000 = 94500 = working_capital

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request);

        assertNull(result.workingCapitalWarning());
    }

    @Test
    void testLocalPriceIntelligence() {
        DemoData.BusinessCategoryData category = createMockCategory("Dairy", 1000000, 31500, 45000, 3);
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(2);
        businessData.setAvgLocalPrice(42.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);
        when(demoDataLoader.getBusinessData("Ghoti", "Dairy")).thenReturn(businessData);

        SchemesReference.Scheme microScheme = createMockScheme("Micro Finance Scheme", 0, 140000, 0.065, 3, 3);
        when(schemesDataLoader.getPrimaryScheme(100000)).thenReturn(microScheme);
        when(schemesDataLoader.getApplicableSchemes(100000)).thenReturn(List.of(microScheme));

        AnalyzeRequest request = new AnalyzeRequest();
        request.setVillageName("Ghoti");
        request.setBusinessCategory("Dairy");
        request.setAvailableMarginCapital(10000.0);

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request);

        assertEquals(42.0, result.localAveragePrice(), 0.01);
        assertEquals(39.9, result.recommendedPriceLow(), 0.01); // 42 * 0.95
        assertEquals(44.1, result.recommendedPriceHigh(), 0.01); // 42 * 1.05
        assertEquals(40.74, result.recommendedLaunchPrice(), 0.01); // 42 * 0.97

        // Breakeven: (31500 + emi) / (45000/42) = (31500 + emi) / 1071.43
        double emi = result.emi();
        double expectedBreakeven = (31500 + emi) / (45000 / 42);
        assertEquals(expectedBreakeven, result.breakevenPrice(), 0.01);
    }

    @Test
    void testRetailCategoryNoLocalPrice() {
        DemoData.BusinessCategoryData category = createMockCategory("Retail / Kirana Store", 500000, 24500, 35000, 2);
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(14);
        businessData.setAvgLocalPrice(null);

        when(demoDataLoader.getBusinessCategory("Retail / Kirana Store")).thenReturn(category);
        when(demoDataLoader.getBusinessData("Ghoti", "Retail / Kirana Store")).thenReturn(businessData);

        SchemesReference.Scheme microScheme = createMockScheme("Micro Finance Scheme", 0, 140000, 0.065, 3, 3);
        when(schemesDataLoader.getPrimaryScheme(100000)).thenReturn(microScheme);
        when(schemesDataLoader.getApplicableSchemes(100000)).thenReturn(List.of(microScheme));

        AnalyzeRequest request = new AnalyzeRequest();
        request.setVillageName("Ghoti");
        request.setBusinessCategory("Retail / Kirana Store");
        request.setAvailableMarginCapital(10000.0);

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request);

        assertEquals(0.0, result.localAveragePrice());
        assertNull(result.recommendedPriceLow());
        assertNull(result.recommendedPriceHigh());
        assertNull(result.recommendedLaunchPrice());
        assertNull(result.breakevenPrice());
        assertEquals("Not applicable — mixed product pricing.", result.breakevenNote());
    }

    private DemoData.BusinessCategoryData createMockCategory(String name, double refProjectCost, double refMonthlyOpCost, double refMonthlyRevenue, int workingCapitalMonths) {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName(name);
        category.setReferenceProjectCost(refProjectCost);
        category.setReferenceMonthlyOperatingCost(refMonthlyOpCost);
        category.setReferenceMonthlyRevenue(refMonthlyRevenue);
        category.setWorkingCapitalMonths(workingCapitalMonths);
        category.setSecondaryOpportunity("Test opportunity");
        category.setSourceNote("Test source");
        return category;
    }

    private SchemesReference.Scheme createMockScheme(String name, double minCost, double maxCost, double interestRate, int tenureYears, int moratoriumMonths) {
        SchemesReference.Scheme scheme = new SchemesReference.Scheme();
        scheme.setSchemeName(name);
        scheme.setMinCost(minCost);
        scheme.setMaxCost(maxCost);
        scheme.setInterestRate(interestRate);
        scheme.setTenureYears(tenureYears);
        scheme.setMoratoriumMonths(moratoriumMonths);
        scheme.setAgency("Test Agency");
        return scheme;
    }
}