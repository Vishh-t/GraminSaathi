package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessCombinationService {

    private final DemoDataLoader demoDataLoader;

    public record CombinationResult(String secondaryOpportunity) {}

    public CombinationResult getCombination(String businessCategory) {
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(businessCategory);
        if (category == null) {
            throw new IllegalArgumentException("Invalid business category: " + businessCategory);
        }

        return new CombinationResult(category.getSecondaryOpportunity());
    }
}