package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.DiscoverRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;

    @Mock
    private FeasibilityScoreService feasibilityScoreService;

    @Mock
    private FinancialCalculatorService financialCalculatorService;

    private DiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryService = new DiscoveryService(demoDataLoader, feasibilityScoreService, financialCalculatorService);
    }

    @Test
    void testDiscoveryReturnsSortedBusinesses() {
        DemoData.VillageData village = new DemoData.VillageData();
        village.setVillageName("Ghoti");

        when(demoDataLoader.getVillage("Ghoti")).thenReturn(village);

        DemoData.BusinessCategoryData dairy = new DemoData.BusinessCategoryData();
        dairy.setCategoryName("Dairy");
        dairy.setReferenceProjectCost(1000000.0);
        dairy.setReferenceMonthlyRevenue(45000.0);
        dairy.setReferenceMonthlyOperatingCost(31500.0);

        DemoData.BusinessCategoryData tailoring = new DemoData.BusinessCategoryData();
        tailoring.setCategoryName("Tailoring");
        tailoring.setReferenceProjectCost(250000.0);
        tailoring.setReferenceMonthlyRevenue(18000.0);
        tailoring.setReferenceMonthlyOperatingCost(12600.0);

        when(demoDataLoader.getAllBusinessCategories()).thenReturn(List.of(dairy, tailoring));
        when(demoDataLoader.getBusinessData("Ghoti", "Dairy")).thenReturn(createBusinessData(2, 42.0));
        when(demoDataLoader.getBusinessData("Ghoti", "Tailoring")).thenReturn(createBusinessData(11, 250.0));

        when(feasibilityScoreService.calculate("Ghoti", "Dairy")).thenReturn(
                new FeasibilityScoreService.FeasibilityResult(85, "High opportunity", 2, 8420, 4210.0)
        );
        when(feasibilityScoreService.calculate("Ghoti", "Tailoring")).thenReturn(
                new FeasibilityScoreService.FeasibilityResult(45, "Moderate opportunity", 11, 8420, 765.0)
        );

        DiscoveryService.DiscoveryResult result = discoveryService.discover(
                new DiscoverRequest("Ghoti", 100000.0)
        );

        assertEquals(2, result.businesses().size());
        assertEquals("Dairy", result.businesses().get(0).categoryName());
        assertEquals("Tailoring", result.businesses().get(1).categoryName());
    }

    @Test
    void testAffordabilityFlagWithinBudget() {
        DemoData.VillageData village = new DemoData.VillageData();
        village.setVillageName("Ghoti");
        when(demoDataLoader.getVillage("Ghoti")).thenReturn(village);

        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Test");
        category.setReferenceProjectCost(50000.0);
        category.setReferenceMonthlyRevenue(10000.0);
        category.setReferenceMonthlyOperatingCost(5000.0);

        when(demoDataLoader.getAllBusinessCategories()).thenReturn(List.of(category));
        when(demoDataLoader.getBusinessData("Ghoti", "Test")).thenReturn(createBusinessData(5, 100.0));
        when(feasibilityScoreService.calculate("Ghoti", "Test")).thenReturn(
                new FeasibilityScoreService.FeasibilityResult(60, "Moderate opportunity", 5, 10000, 2000.0)
        );

        // margin = 100000 -> project_cost = 1000000 >= 50000
        DiscoveryService.DiscoveryResult result = discoveryService.discover(
                new DiscoverRequest("Ghoti", 100000.0)
        );

        assertEquals("Within budget", result.businesses().get(0).affordabilityFlag());
    }

    @Test
    void testAffordabilityFlagMayRequirePhasing() {
        DemoData.VillageData village = new DemoData.VillageData();
        village.setVillageName("Ghoti");
        when(demoDataLoader.getVillage("Ghoti")).thenReturn(village);

        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Test");
        category.setReferenceProjectCost(2000000.0);
        category.setReferenceMonthlyRevenue(10000.0);
        category.setReferenceMonthlyOperatingCost(5000.0);

        when(demoDataLoader.getAllBusinessCategories()).thenReturn(List.of(category));
        when(demoDataLoader.getBusinessData("Ghoti", "Test")).thenReturn(createBusinessData(5, 100.0));
        when(feasibilityScoreService.calculate("Ghoti", "Test")).thenReturn(
                new FeasibilityScoreService.FeasibilityResult(60, "Moderate opportunity", 5, 10000, 2000.0)
        );

        // margin = 10000 -> project_cost = 100000 < 2000000
        DiscoveryService.DiscoveryResult result = discoveryService.discover(
                new DiscoverRequest("Ghoti", 10000.0)
        );

        assertEquals("May require phasing", result.businesses().get(0).affordabilityFlag());
    }

    @Test
    void testVillageNotFound() {
        when(demoDataLoader.getVillage("Unknown")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> discoveryService.discover(
                new DiscoverRequest("Unknown", 10000.0)
        ));
    }

    private DemoData.BusinessData createBusinessData(int competitors, Double price) {
        DemoData.BusinessData data = new DemoData.BusinessData();
        data.setCompetitorCount(competitors);
        data.setAvgLocalPrice(price);
        return data;
    }
}