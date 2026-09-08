package com.graminsaathi.dto.response;

import lombok.Data;

@Data
public class DscrResponse {
    private double dscr;
    private String label;
    private double monthlyNetOperatingIncome;
    private double emi;
}