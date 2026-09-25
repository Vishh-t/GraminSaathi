package com.graminsaathi.service;

import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.dto.request.DiscoverRequest;
import com.graminsaathi.model.BusinessCategory;
import com.graminsaathi.model.Village;
import com.graminsaathi.repository.BusinessCategoryRepository;
import com.graminsaathi.repository.VillageFeaturesRepository;
import com.graminsaathi.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Rewritten for the real-data DiscoveryService (villages/village_features/business_categories +
 * HardFilterService/PersonFitScoreService/DemandSupplyScoreService/FinancialRangeService) - see
 * Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, "Discovery wiring" section. The old version mocked
 * DemoDataLoader/FeasibilityScoreService, which DiscoveryService no longer depends on.
 */
@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock
    private VillageRepository villageRepository;
    @Mock
    private VillageFeaturesRepository villageFeaturesRepository;
    @Mock
    private BusinessCategoryRepository businessCategoryRepository;
    @Mock
    private HardFilterService hardFilterService;
    @Mock
    private PersonFitScoreService personFitScoreService;
    @Mock
    private DemandSupplyScoreService demandSupplyScoreService;
    @Mock
    private FinancialRangeService financialRangeService;

    private DiscoveryService discoveryService;

    private final ApplicantProfile applicant = ApplicantProfile.builder().build();

    @BeforeEach
    void setUp() {
        discoveryService = new DiscoveryService(villageRepository, villageFeaturesRepository,
                businessCategoryRepository, hardFilterService, personFitScoreService,
                demandSupplyScoreService, financialRangeService);
    }

    private static FinancialRangeService.FinancialRangeResult emptyFinancialRange() {
        FinancialRangeService.MonthlyRange zero = new FinancialRangeService.MonthlyRange(0, 0, 0);
        return new FinancialRangeService.FinancialRangeResult(zero, zero, zero, 1.0);
    }

    private static DemandSupplyScoreService.ScoreResult noDataYet() {
        return new DemandSupplyScoreService.ScoreResult(
                DemandSupplyScoreService.Confidence.INSUFFICIENT_NO_POPULATION,
                null, null, null, null, "No population projection for this village yet.");
    }

    @Test
    void testDiscoveryReturnsSortedByPersonFitScore() {
        Village village = Village.builder().id(1L).name("Ghoti").nameNormalized("ghoti").build();
        when(villageRepository.findFirstByNameNormalized("ghoti")).thenReturn(Optional.of(village));
        when(villageFeaturesRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessCategory dairy = BusinessCategory.builder().categoryName("Dairy")
                .referenceProjectCost(1000000.0).referenceMonthlyRevenue(45000.0).referenceMonthlyOperatingCost(31500.0).build();
        BusinessCategory tailoring = BusinessCategory.builder().categoryName("Tailoring")
                .referenceProjectCost(250000.0).referenceMonthlyRevenue(18000.0).referenceMonthlyOperatingCost(12600.0).build();

        when(businessCategoryRepository.findAll()).thenReturn(List.of(dairy, tailoring));
        when(hardFilterService.evaluate(any(), any(), any())).thenReturn(HardFilterService.FilterResult.pass());
        when(demandSupplyScoreService.calculate(any(), any(), any())).thenReturn(noDataYet());
        when(financialRangeService.calculate(any(), any())).thenReturn(emptyFinancialRange());

        when(personFitScoreService.calculate(eq(dairy), any(), anyDouble())).thenReturn(
                new PersonFitScoreService.FitResult(85, "Strong fit", 85.0, null, 50.0, List.of()));
        when(personFitScoreService.calculate(eq(tailoring), any(), anyDouble())).thenReturn(
                new PersonFitScoreService.FitResult(45, "Moderate fit", 45.0, null, 50.0, List.of()));

        DiscoveryService.DiscoveryResult result = discoveryService.discover(
                new DiscoverRequest("Ghoti", 100000.0), applicant
        );

        assertEquals(2, result.businesses().size());
        assertEquals("Dairy", result.businesses().get(0).categoryName());
        assertEquals("Tailoring", result.businesses().get(1).categoryName());
    }

    @Test
    void testAffordabilityFlagWithinBudget() {
        Village village = Village.builder().id(1L).name("Ghoti").nameNormalized("ghoti").build();
        when(villageRepository.findFirstByNameNormalized("ghoti")).thenReturn(Optional.of(village));
        when(villageFeaturesRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessCategory category = BusinessCategory.builder().categoryName("Test")
                .referenceProjectCost(50000.0).referenceMonthlyRevenue(10000.0).referenceMonthlyOperatingCost(5000.0).build();
        when(businessCategoryRepository.findAll()).thenReturn(List.of(category));
        when(hardFilterService.evaluate(eq(category), any(), any())).thenReturn(HardFilterService.FilterResult.pass());
        when(demandSupplyScoreService.calculate(eq(village), any(), eq(category))).thenReturn(noDataYet());
        when(financialRangeService.calculate(eq(category), any())).thenReturn(emptyFinancialRange());
        when(personFitScoreService.calculate(eq(category), any(), anyDouble())).thenReturn(
                new PersonFitScoreService.FitResult(60, "Moderate fit", 60.0, null, 50.0, List.of()));

        // margin = 100000 -> project_cost = 1000000 >= 50000
        DiscoveryService.DiscoveryResult result = discoveryService.discover(
                new DiscoverRequest("Ghoti", 100000.0), applicant
        );

        assertEquals("Within budget", result.businesses().get(0).affordabilityFlag());
    }

    @Test
    void testAffordabilityFlagMayRequirePhasing() {
        Village village = Village.builder().id(1L).name("Ghoti").nameNormalized("ghoti").build();
        when(villageRepository.findFirstByNameNormalized("ghoti")).thenReturn(Optional.of(village));
        when(villageFeaturesRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessCategory category = BusinessCategory.builder().categoryName("Test")
                .referenceProjectCost(2000000.0).referenceMonthlyRevenue(10000.0).referenceMonthlyOperatingCost(5000.0).build();
        when(businessCategoryRepository.findAll()).thenReturn(List.of(category));
        when(hardFilterService.evaluate(eq(category), any(), any())).thenReturn(HardFilterService.FilterResult.pass());
        when(demandSupplyScoreService.calculate(eq(village), any(), eq(category))).thenReturn(noDataYet());
        when(financialRangeService.calculate(eq(category), any())).thenReturn(emptyFinancialRange());
        when(personFitScoreService.calculate(eq(category), any(), anyDouble())).thenReturn(
                new PersonFitScoreService.FitResult(60, "Moderate fit", 60.0, null, 50.0, List.of()));

        // margin = 10000 -> project_cost = 100000 < 2000000
        DiscoveryService.DiscoveryResult result = discoveryService.discover(
                new DiscoverRequest("Ghoti", 10000.0), applicant
        );

        assertEquals("May require phasing", result.businesses().get(0).affordabilityFlag());
    }

    @Test
    void testVillageNotFound() {
        when(villageRepository.findFirstByNameNormalized("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> discoveryService.discover(
                new DiscoverRequest("Unknown", 10000.0), applicant
        ));
    }
}
