package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DscrServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;

    private DscrService dscrService;

    @BeforeEach
    void setUp() {
        dscrService = new DscrService(demoDataLoader);
    }

    @Test
    void testHealthyDscr() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setReferenceMonthlyRevenue(45000.0);
        category.setReferenceMonthlyOperatingCost(31500.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        DscrService.DscrResult result = dscrService.calculate("Dairy", 5000.0);

        assertEquals(13500.0, result.monthlyNetOperatingIncome(), 0.01);
        assertEquals(2.7, result.dscr(), 0.01);
        assertEquals("Healthy", result.label());
        assertEquals(5000.0, result.emi(), 0.01);
    }

    @Test
    void testModerateDscr() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setReferenceMonthlyRevenue(18000.0);
        category.setReferenceMonthlyOperatingCost(12600.0);

        when(demoDataLoader.getBusinessCategory("Tailoring")).thenReturn(category);

        DscrService.DscrResult result = dscrService.calculate("Tailoring", 6000.0);

        assertEquals(5400.0, result.monthlyNetOperatingIncome(), 0.01);
        assertEquals(0.9, result.dscr(), 0.01);
        assertEquals("Risky", result.label());
    }

    @Test
    void testZeroEmi() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setReferenceMonthlyRevenue(45000.0);
        category.setReferenceMonthlyOperatingCost(31500.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        DscrService.DscrResult result = dscrService.calculate("Dairy", 0.0);

        assertEquals(Double.MAX_VALUE, result.dscr());
        assertEquals("Healthy", result.label());
    }

    @Test
    void testInvalidCategory() {
        when(demoDataLoader.getBusinessCategory("Invalid")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> dscrService.calculate("Invalid", 5000.0));
    }
}