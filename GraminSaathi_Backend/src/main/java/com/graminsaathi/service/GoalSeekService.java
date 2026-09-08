package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.GoalSeekRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoalSeekService {

    private final DemoDataLoader demoDataLoader;

    public record GoalSeekResult(
            double estimatedProjectCost,
            double estimatedMarginRequired,
            double estimatedLoanRequired,
            double requiredMonthlyRevenue,
            String note
    ) {}

    public GoalSeekResult goalSeek(GoalSeekRequest request) {
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(request.getBusinessCategory());
        if (category == null) {
            throw new IllegalArgumentException("Invalid business category: " + request.getBusinessCategory());
        }

        double desiredMonthlyIncome = request.getDesiredMonthlyIncome();
        double requiredMonthlyRevenue = desiredMonthlyIncome + category.getReferenceMonthlyOperatingCost();

        double scaleFactor = requiredMonthlyRevenue / category.getReferenceMonthlyRevenue();
        double estimatedProjectCost = category.getReferenceProjectCost() * scaleFactor;
        double estimatedMarginRequired = estimatedProjectCost * 0.10;
        double estimatedLoanRequired = estimatedProjectCost * 0.90;

        String note = "Estimated — scaled from reference model data.";

        return new GoalSeekResult(
                estimatedProjectCost,
                estimatedMarginRequired,
                estimatedLoanRequired,
                requiredMonthlyRevenue,
                note
        );
    }
}