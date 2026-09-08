package com.graminsaathi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessDataResponse {
    private Integer competitorCount;
    private Double avgLocalPrice;
}