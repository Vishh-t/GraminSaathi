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
class SupplyChainRiskServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;

    private SupplyChainRiskService supplyChainRiskService;

    @BeforeEach
    void setUp() {
        supplyChainRiskService = new SupplyChainRiskService(demoDataLoader);
    }

    @Test
    void testDairyRisk() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Dairy");
        category.setSingleBuyerDependencyRisk("Medium");
        category.setPrimaryInputDependencyNote("Fodder supply can be seasonal");

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        SupplyChainRiskService.RiskResult result = supplyChainRiskService.getRisk("Dairy");

        assertEquals("Medium", result.riskLevel());
        assertEquals("Fodder supply can be seasonal", result.note());
    }

    @Test
    void testTailoringRisk() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Tailoring");
        category.setSingleBuyerDependencyRisk("Low");
        category.setPrimaryInputDependencyNote("Fabric suppliers generally available");

        when(demoDataLoader.getBusinessCategory("Tailoring")).thenReturn(category);

        SupplyChainRiskService.RiskResult result = supplyChainRiskService.getRisk("Tailoring");

        assertEquals("Low", result.riskLevel());
    }

    @Test
    void testInvalidCategory() {
        when(demoDataLoader.getBusinessCategory("Invalid")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> supplyChainRiskService.getRisk("Invalid"));
    }
}