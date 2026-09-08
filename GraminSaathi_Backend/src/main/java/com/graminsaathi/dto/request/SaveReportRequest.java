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
public class SaveReportRequest {
    @NotBlank
    private String villageName;

    @NotBlank
    private String businessCategory;

    @NotNull
    @Positive
    private Double availableMarginCapital;

    @NotNull
    private String resultJson;
}