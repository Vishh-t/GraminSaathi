package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DscrService {

    private final DemoDataLoader demoDataLoader;

    public record DscrResult(
            double dscr,
            String label,
            double monthlyNetOperatingIncome,
            double emi
    ) {}

    public DscrResult calculate(String businessCategory, double emi) {
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(businessCategory);
        if (category == null) {
            throw new IllegalArgumentException("Invalid business category: " + businessCategory);
        }

        double monthlyNetOperatingIncome = category.getReferenceMonthlyRevenue() - category.getReferenceMonthlyOperatingCost();
        double dscr = emi > 0 ? monthlyNetOperatingIncome / emi : Double.MAX_VALUE;

        String label;
        if (dscr >= 2.0) {
            label = "Healthy";
        } else if (dscr >= 1.0) {
            label = "Moderate";
        } else {
            label = "Risky";
        }

        return new DscrResult(dscr, label, monthlyNetOperatingIncome, emi);
    }
}