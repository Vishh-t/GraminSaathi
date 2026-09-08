package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeasibilityScoreServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;

    private FeasibilityScoreService feasibilityScoreService;

    @BeforeEach
    void setUp() {
        feasibilityScoreService = new FeasibilityScoreService(demoDataLoader);
    }

    @Test
    void testHighOpportunity() {
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(2);
        businessData.setAvgLocalPrice(42.0);

        DemoData.VillageData village = new DemoData.VillageData();
        village.setPopulation5kmRadius(8420);

        when(demoDataLoader.getBusinessData("Ghoti", "Dairy")).thenReturn(businessData);
        when(demoDataLoader.getVillage("Ghoti")).thenReturn(village);

        FeasibilityScoreService.FeasibilityResult result = feasibilityScoreService.calculate("Ghoti", "Dairy");

        assertEquals(2, result.competitorCount());
        assertEquals(8420, result.population5kmRadius());
        assertEquals(4210.0, result.demandRatio(), 0.01);
        assertTrue(result.opportunityScore() >= 70);
        assertEquals("High opportunity", result.label());
    }

    @Test
    void testModerateOpportunity() {
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(4);
        businessData.setAvgLocalPrice(250.0);

        DemoData.VillageData village = new DemoData.VillageData();
        village.setPopulation5kmRadius(8420);

        when(demoDataLoader.getBusinessData("Ghoti", "Tailoring")).thenReturn(businessData);
        when(demoDataLoader.getVillage("Ghoti")).thenReturn(village);

        FeasibilityScoreService.FeasibilityResult result = feasibilityScoreService.calculate("Ghoti", "Tailoring");

        assertTrue(result.opportunityScore() >= 40 && result.opportunityScore() < 70);
        assertEquals("Moderate opportunity", result.label());
    }

    @Test
    void testLowOpportunity() {
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(21);
        businessData.setAvgLocalPrice(null);

        DemoData.VillageData village = new DemoData.VillageData();
        village.setPopulation5kmRadius(12300);

        when(demoDataLoader.getBusinessData("Peddapuram", "Retail / Kirana Store")).thenReturn(businessData);
        when(demoDataLoader.getVillage("Peddapuram")).thenReturn(village);

        FeasibilityScoreService.FeasibilityResult result = feasibilityScoreService.calculate("Peddapuram", "Retail / Kirana Store");

        assertTrue(result.opportunityScore() < 40);
        assertEquals("Low opportunity (saturated)", result.label());
    }

    @Test
    void testCompetitorCountMinimumOne() {
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(0);
        businessData.setAvgLocalPrice(42.0);

        DemoData.VillageData village = new DemoData.VillageData();
        village.setPopulation5kmRadius(1000);

        when(demoDataLoader.getBusinessData("Test", "Test")).thenReturn(businessData);
        when(demoDataLoader.getVillage("Test")).thenReturn(village);

        FeasibilityScoreService.FeasibilityResult result = feasibilityScoreService.calculate("Test", "Test");

        assertEquals(1, result.competitorCount());
    }

    @Test
    void testVillageNotFound() {
        when(demoDataLoader.getVillage("Unknown")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> feasibilityScoreService.calculate("Unknown", "Dairy"));
    }
}