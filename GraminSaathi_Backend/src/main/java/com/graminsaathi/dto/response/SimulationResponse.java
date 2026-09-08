package com.graminsaathi.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SimulationResponse {
    private List<CashPoint> cashCurve;
    private String verdict;
    private Integer deficitMonth;

    @Data
    public static class CashPoint {
        private int month;
        private double cumulativeCash;

        public CashPoint(int month, double cumulativeCash) {
            this.month = month;
            this.cumulativeCash = cumulativeCash;
        }
    }
}