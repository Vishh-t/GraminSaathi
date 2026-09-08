package com.graminsaathi.dto.response;

import lombok.Data;

@Data
public class SchemeComparisonResponse {
    private String schemeName;
    private Double interestRate;
    private Integer tenureYears;
    private Integer moratoriumMonths;
    private Double emi;
    private String agency;
    private String subsidyNote;
    private boolean isPrimary;
}