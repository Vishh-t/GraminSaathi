package com.graminsaathi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulateRequest {
    @NotBlank
    private String villageName;

    @NotBlank
    private String businessCategory;

    @NotNull
    private Double availableMarginCapital;

    private Double revenueShockPct = 0.0;

    private Double costShockPct = 0.0;
}