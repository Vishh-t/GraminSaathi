package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.DiscoverRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private final DemoDataLoader demoDataLoader;
    private final FeasibilityScoreService feasibilityScoreService;
    private final FinancialCalculatorService financialCalculatorService;

    public record DiscoveredBusiness(
            String categoryName,
            int opportunityScore,
            String opportunityLabel,
            String affordabilityFlag,
            double referenceProjectCost,
            double referenceMonthlyRevenue,
            double referenceMonthlyOperatingCost
    ) {}

    public record DiscoveryResult(List<DiscoveredBusiness> businesses) {}

    public DiscoveryResult discover(DiscoverRequest request) {
        DemoData.VillageData village = demoDataLoader.getVillage(request.getVillageName());
        if (village == null) {
            throw new IllegalArgumentException("Village not found: " + request.getVillageName());
        }

        double projectCost = request.getAvailableMarginCapital() / 0.10;

        List<DiscoveredBusiness> businesses = new ArrayList<>();

        for (DemoData.BusinessCategoryData category : demoDataLoader.getAllBusinessCategories()) {
            DemoData.BusinessData businessData = demoDataLoader.getBusinessData(request.getVillageName(), category.getCategoryName());
            if (businessData == null) continue;

            FeasibilityScoreService.FeasibilityResult feasibility = feasibilityScoreService.calculate(
                    request.getVillageName(), category.getCategoryName()
            );

            String affordabilityFlag = projectCost >= category.getReferenceProjectCost()
                    ? "Within budget"
                    : "May require phasing";

            businesses.add(new DiscoveredBusiness(
                    category.getCategoryName(),
                    feasibility.opportunityScore(),
                    feasibility.label(),
                    affordabilityFlag,
                    category.getReferenceProjectCost(),
                    category.getReferenceMonthlyRevenue(),
                    category.getReferenceMonthlyOperatingCost()
            ));
        }

        businesses.sort(Comparator.comparingInt(DiscoveredBusiness::opportunityScore).reversed());

        return new DiscoveryResult(businesses);
    }
}