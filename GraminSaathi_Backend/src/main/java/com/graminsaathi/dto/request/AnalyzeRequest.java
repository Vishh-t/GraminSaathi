package com.graminsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRequest {
    @NotBlank
    private String villageName;

    @NotBlank
    private String businessCategory;

    @NotNull
    @Positive
    private Double availableMarginCapital;

    /**
     * Optional applicant facts (caste category, gender, income, age, ...). When present, the financing
     * schemes offered in the analysis are the ones this applicant is actually eligible for; when absent,
     * a baseline applicant built from the village and business category is used.
     * The village's state and the category's sector always win over anything sent here.
     */
    private ApplicantProfile applicant;

    /** Keeps the original three-field construction working for callers that don't send applicant details. */
    public AnalyzeRequest(String villageName, String businessCategory, Double availableMarginCapital) {
        this(villageName, businessCategory, availableMarginCapital, null);
    }
}
