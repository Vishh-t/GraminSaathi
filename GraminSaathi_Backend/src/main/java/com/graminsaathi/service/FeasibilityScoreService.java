package com.graminsaathi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.model.BusinessCategory;
import com.graminsaathi.model.Village;
import com.graminsaathi.model.VillageFeatures;
import com.graminsaathi.repository.BusinessCategoryRepository;
import com.graminsaathi.repository.VillageFeaturesRepository;
import com.graminsaathi.repository.VillageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Local opportunity score (Section 6) - population vs. competitor density for one business category
 * at one village. Rewired 2026-09-27 (see Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, "part 7")
 * off the real Village/VillageFeatures/BusinessCategory tables - previously ran entirely on
 * DemoDataLoader's ~4 hardcoded villages, which threw "Village not found" for every real Census village
 * (blocking the whole Analysis page, since AnalysisController/EvidenceService/AgentCommitteeService/
 * BusinessHealthScoreService all call this for a village name that now comes from the real search table).
 *
 * <p>Population = {@link VillageFeatures#getPopulationProjected()} (step 2a). Competitor count = this
 * category's entry in {@link VillageFeatures#getCompetitorCountsJson()} (step 2b) - both null-safe:
 * a village with no features row yet, or a category with no mapped competitors, scores as population 0 /
 * competitors treated as the old demo code's floor of 1 (to avoid a divide-by-zero) rather than throwing,
 * same convention {@link DemandSupplyScoreService} already uses for the richer Discovery-flow score.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeasibilityScoreService {

    private static final double REFERENCE_HIGH = 4000.0;

    private final VillageRepository villageRepository;
    private final VillageFeaturesRepository villageFeaturesRepository;
    private final BusinessCategoryRepository businessCategoryRepository;
    private final ObjectMapper objectMapper;

    public record FeasibilityResult(
            int opportunityScore,
            String label,
            int competitorCount,
            int population5kmRadius,
            double demandRatio
    ) {}

    public FeasibilityResult calculate(String villageName, String businessCategory) {
        Village village = villageRepository.findFirstByNameNormalized(VillageService.normalize(villageName))
                .orElseThrow(() -> new IllegalArgumentException("Village not found: " + villageName));
        BusinessCategory category = businessCategoryRepository.findByCategoryNameIgnoreCase(businessCategory)
                .orElseThrow(() -> new IllegalArgumentException("Invalid business category: " + businessCategory));
        VillageFeatures features = villageFeaturesRepository.findById(village.getId()).orElse(null);

        int population = (features != null && features.getPopulationProjected() != null)
                ? features.getPopulationProjected() : 0;
        // Floor of 1, not 0, to avoid a divide-by-zero - matches the prior demo-data code's
        // Math.max(businessData.getCompetitorCount(), 1).
        int competitorCount = Math.max(findCompetitorCount(features, category), 1);

        double demandRatio = (double) population / competitorCount;
        int opportunityScore = (int) Math.round(Math.min(100, Math.max(0, (demandRatio / REFERENCE_HIGH) * 100)));

        String label;
        if (opportunityScore >= 70) {
            label = "High opportunity";
        } else if (opportunityScore >= 40) {
            label = "Moderate opportunity";
        } else {
            label = "Low opportunity (saturated)";
        }

        return new FeasibilityResult(opportunityScore, label, competitorCount, population, demandRatio);
    }

    /** Null-safe: no features row, no JSON yet, or no entry for this category all mean "0 known competitors", not an error. */
    private int findCompetitorCount(VillageFeatures features, BusinessCategory category) {
        if (features == null || features.getCompetitorCountsJson() == null || features.getCompetitorCountsJson().isBlank()) {
            return 0;
        }
        try {
            JsonNode map = objectMapper.readTree(features.getCompetitorCountsJson());
            JsonNode count = map.get(category.getCategoryName());
            return (count == null || count.isNull()) ? 0 : count.asInt();
        } catch (Exception e) {
            log.warn("Could not parse competitor_counts_json for village {}: {}", features.getVillageId(), e.getMessage());
            return 0;
        }
    }
}
