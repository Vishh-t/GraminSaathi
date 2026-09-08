package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeasibilityScoreService {

    private final DemoDataLoader demoDataLoader;
    private static final double REFERENCE_HIGH = 4000.0;

    public record FeasibilityResult(
            int opportunityScore,
            String label,
            int competitorCount,
            int population5kmRadius,
            double demandRatio
    ) {}

    public FeasibilityResult calculate(String villageName, String businessCategory) {
        DemoData.BusinessData businessData = demoDataLoader.getBusinessData(villageName, businessCategory);
        if (businessData == null) {
            throw new IllegalArgumentException("No business data for village: " + villageName + ", category: " + businessCategory);
        }

        DemoData.VillageData village = demoDataLoader.getVillage(villageName);
        if (village == null) {
            throw new IllegalArgumentException("Village not found: " + villageName);
        }

        int competitorCount = Math.max(businessData.getCompetitorCount(), 1);
        int population = village.getPopulation5kmRadius();

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
}