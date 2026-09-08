package com.graminsaathi.dto.response;

import lombok.Data;

@Data
public class PeerBenchmarkResponse {
    private Integer sampleSize;
    private Double avgMonthlyRevenueAfter6Months;
    private Integer pctStillOperatingAfter1Year;
    private String disclaimer;
}