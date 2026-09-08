package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PeerBenchmarkService {

    private final DemoDataLoader demoDataLoader;

    public record BenchmarkResult(
            Integer sampleSize,
            Double avgMonthlyRevenueAfter6Months,
            Integer pctStillOperatingAfter1Year,
            String disclaimer
    ) {}

    public BenchmarkResult getBenchmark(String businessCategory) {
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(businessCategory);
        if (category == null) {
            throw new IllegalArgumentException("Invalid business category: " + businessCategory);
        }

        DemoData.PeerBenchmark benchmark = category.getPeerBenchmark();

        return new BenchmarkResult(
                benchmark.getSampleSize(),
                benchmark.getAvgMonthlyRevenueAfter6Months(),
                benchmark.getPctStillOperatingAfter1Year(),
                "Illustrative sample data — in production this would populate from real anonymized user outcomes over time"
        );
    }
}