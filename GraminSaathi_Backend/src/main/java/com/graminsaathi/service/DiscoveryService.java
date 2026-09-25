package com.graminsaathi.service;

import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.dto.request.DiscoverRequest;
import com.graminsaathi.model.BusinessCategory;
import com.graminsaathi.model.Village;
import com.graminsaathi.model.VillageFeatures;
import com.graminsaathi.repository.BusinessCategoryRepository;
import com.graminsaathi.repository.VillageFeaturesRepository;
import com.graminsaathi.repository.VillageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Real-data version of discovery (see Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, "data-independent
 * steps" pass). Runs entirely off DB entities now - Village/VillageFeatures/BusinessCategory - rather
 * than DemoDataLoader's hardcoded JSON. VillageFeatures (competitor counts, coverage) is mostly null for
 * real villages until steps 2b/2c land; every downstream service here is written to treat that as
 * "no data yet" rather than zero, so this wires cleanly today and starts returning richer results with
 * zero further code changes once that data exists.
 */
@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private static final double DEFAULT_MARGIN_FRACTION = 0.10;

    private final VillageRepository villageRepository;
    private final VillageFeaturesRepository villageFeaturesRepository;
    private final BusinessCategoryRepository businessCategoryRepository;
    private final HardFilterService hardFilterService;
    private final PersonFitScoreService personFitScoreService;
    private final DemandSupplyScoreService demandSupplyScoreService;
    private final FinancialRangeService financialRangeService;

    public record DiscoveredBusiness(
            String categoryName,
            Integer marketScore,
            String marketConfidence,
            String marketExplanation,
            int personFitScore,
            String personFitLabel,
            String affordabilityFlag,
            double referenceProjectCost,
            double referenceMonthlyRevenue,
            double referenceMonthlyOperatingCost,
            FinancialRangeService.FinancialRangeResult financialRange
    ) {}

    public record DiscoveryResult(List<DiscoveredBusiness> businesses) {}

    /**
     * @param applicant never null at the call site - DiscoveryController resolves it (request override
     *                  merged onto the logged-in user's saved profile, or an empty profile for anonymous
     *                  callers) the same way AnalysisController does for /analyze.
     */
    public DiscoveryResult discover(DiscoverRequest request, ApplicantProfile applicant) {
        Village village = villageRepository.findFirstByNameNormalized(VillageService.normalize(request.getVillageName()))
                .orElseThrow(() -> new IllegalArgumentException("Village not found: " + request.getVillageName()));

        VillageFeatures features = villageFeaturesRepository.findById(village.getId()).orElse(null);

        double availableMarginCapital = request.getAvailableMarginCapital();
        double projectCost = availableMarginCapital / DEFAULT_MARGIN_FRACTION;

        List<DiscoveredBusiness> businesses = new ArrayList<>();

        for (BusinessCategory category : businessCategoryRepository.findAll()) {
            HardFilterService.FilterResult filter = hardFilterService.evaluate(category, applicant, features);
            if (!filter.passed()) {
                continue; // e.g. "a flour mill needs electricity" - excluded outright, not just scored low
            }

            PersonFitScoreService.FitResult fit = personFitScoreService.calculate(category, applicant, availableMarginCapital);
            DemandSupplyScoreService.ScoreResult demandSupply = demandSupplyScoreService.calculate(village, features, category);
            FinancialRangeService.FinancialRangeResult financialRange = financialRangeService.calculate(category, Optional.empty());

            Double refCost = category.getReferenceProjectCost();
            String affordabilityFlag = (refCost != null && projectCost >= refCost)
                    ? "Within budget"
                    : "May require phasing";

            businesses.add(new DiscoveredBusiness(
                    category.getCategoryName(),
                    demandSupply.marketScore(),
                    demandSupply.confidence().name(),
                    demandSupply.explanation(),
                    fit.overallScore(),
                    fit.label(),
                    affordabilityFlag,
                    category.getReferenceProjectCost() != null ? category.getReferenceProjectCost() : 0.0,
                    category.getReferenceMonthlyRevenue() != null ? category.getReferenceMonthlyRevenue() : 0.0,
                    category.getReferenceMonthlyOperatingCost() != null ? category.getReferenceMonthlyOperatingCost() : 0.0,
                    financialRange
            ));
        }

        // Person-fit is always computable today; market score is usually still null (steps 2b/2c/HCES not
        // populated), so it can't be the primary sort key yet - flip this once that data lands.
        businesses.sort(Comparator.comparingInt(DiscoveredBusiness::personFitScore).reversed());

        return new DiscoveryResult(businesses);
    }
}