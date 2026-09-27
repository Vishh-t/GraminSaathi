package com.graminsaathi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.model.BusinessCategory;
import com.graminsaathi.model.Village;
import com.graminsaathi.model.VillageFeatures;
import com.graminsaathi.repository.BusinessCategoryRepository;
import com.graminsaathi.repository.VillageFeaturesRepository;
import com.graminsaathi.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Rewritten 2026-09-27 for the real-data FeasibilityScoreService (villages/village_features/
 * business_categories) - see Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, "part 7". The old version
 * mocked DemoDataLoader, which this service no longer depends on. Uses a real ObjectMapper (not mocked)
 * since it's exercised for real against competitorCountsJson, matching how the service actually parses it.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeasibilityScoreServiceTest {

    @Mock
    private VillageRepository villageRepository;
    @Mock
    private VillageFeaturesRepository villageFeaturesRepository;
    @Mock
    private BusinessCategoryRepository businessCategoryRepository;

    private FeasibilityScoreService feasibilityScoreService;

    @BeforeEach
    void setUp() {
        feasibilityScoreService = new FeasibilityScoreService(
                villageRepository, villageFeaturesRepository, businessCategoryRepository, new ObjectMapper());
    }

    private static Village village(String name, long id) {
        return Village.builder().id(id).name(name).nameNormalized(VillageService.normalize(name)).build();
    }

    private static BusinessCategory category(String name) {
        return BusinessCategory.builder().categoryName(name).build();
    }

    private void stub(Village v, BusinessCategory c, Integer population, String competitorCountsJson) {
        when(villageRepository.findFirstByNameNormalized(v.getNameNormalized())).thenReturn(Optional.of(v));
        when(businessCategoryRepository.findByCategoryNameIgnoreCase(c.getCategoryName())).thenReturn(Optional.of(c));
        VillageFeatures features = VillageFeatures.builder()
                .villageId(v.getId())
                .populationProjected(population)
                .competitorCountsJson(competitorCountsJson)
                .build();
        when(villageFeaturesRepository.findById(v.getId())).thenReturn(Optional.of(features));
    }

    @Test
    void testHighOpportunity() {
        Village v = village("Ghoti", 1L);
        BusinessCategory c = category("Dairy");
        stub(v, c, 8420, "{\"Dairy\":2}");

        FeasibilityScoreService.FeasibilityResult result = feasibilityScoreService.calculate("Ghoti", "Dairy");

        assertEquals(2, result.competitorCount());
        assertEquals(8420, result.population5kmRadius());
        assertEquals(4210.0, result.demandRatio(), 0.01);
        assertTrue(result.opportunityScore() >= 70);
        assertEquals("High opportunity", result.label());
    }

    @Test
    void testModerateOpportunity() {
        Village v = village("Ghoti", 1L);
        BusinessCategory c = category("Tailoring");
        stub(v, c, 8420, "{\"Tailoring\":4}");

        FeasibilityScoreService.FeasibilityResult result = feasibilityScoreService.calculate("Ghoti", "Tailoring");

        assertTrue(result.opportunityScore() >= 40 && result.opportunityScore() < 70);
        assertEquals("Moderate opportunity", result.label());
    }

    @Test
    void testLowOpportunity() {
        Village v = village("Peddapuram", 2L);
        BusinessCategory c = category("Retail / Kirana Store");
        stub(v, c, 12300, "{\"Retail / Kirana Store\":21}");

        FeasibilityScoreService.FeasibilityResult result = feasibilityScoreService.calculate("Peddapuram", "Retail / Kirana Store");

        assertTrue(result.opportunityScore() < 40);
        assertEquals("Low opportunity (saturated)", result.label());
    }

    @Test
    void testCompetitorCountMinimumOne() {
        Village v = village("Test", 3L);
        BusinessCategory c = category("Test");
        stub(v, c, 1000, "{\"Test\":0}");

        FeasibilityScoreService.FeasibilityResult result = feasibilityScoreService.calculate("Test", "Test");

        assertEquals(1, result.competitorCount());
    }

    @Test
    void testNoFeaturesRowTreatedAsZeroPopulationAndCompetitors() {
        // A real village that hasn't been through step 2a/2b yet - no village_features row at all.
        // Null-safe: 0 population, competitor floor of 1 (never a crash, never "Village not found").
        Village v = village("Unbackfilled", 4L);
        BusinessCategory c = category("Dairy");
        when(villageRepository.findFirstByNameNormalized(v.getNameNormalized())).thenReturn(Optional.of(v));
        when(businessCategoryRepository.findByCategoryNameIgnoreCase("Dairy")).thenReturn(Optional.of(c));
        when(villageFeaturesRepository.findById(v.getId())).thenReturn(Optional.empty());

        FeasibilityScoreService.FeasibilityResult result = feasibilityScoreService.calculate("Unbackfilled", "Dairy");

        assertEquals(0, result.population5kmRadius());
        assertEquals(1, result.competitorCount());
        assertEquals("Low opportunity (saturated)", result.label());
    }

    @Test
    void testVillageNotFound() {
        when(villageRepository.findFirstByNameNormalized(VillageService.normalize("Unknown"))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> feasibilityScoreService.calculate("Unknown", "Dairy"));
    }

    @Test
    void testInvalidCategoryThrows() {
        Village v = village("Ghoti", 1L);
        when(villageRepository.findFirstByNameNormalized(v.getNameNormalized())).thenReturn(Optional.of(v));
        when(businessCategoryRepository.findByCategoryNameIgnoreCase("Nope")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> feasibilityScoreService.calculate("Ghoti", "Nope"));
    }
}
