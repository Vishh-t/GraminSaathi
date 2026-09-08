package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupplyChainRiskService {

    private final DemoDataLoader demoDataLoader;

    public record RiskResult(
            String riskLevel,
            String note
    ) {}

    public RiskResult getRisk(String businessCategory) {
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(businessCategory);
        if (category == null) {
            throw new IllegalArgumentException("Invalid business category: " + businessCategory);
        }

        return new RiskResult(
                category.getSingleBuyerDependencyRisk(),
                category.getPrimaryInputDependencyNote()
        );
    }
}