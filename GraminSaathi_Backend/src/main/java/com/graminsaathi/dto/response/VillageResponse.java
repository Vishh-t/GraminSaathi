package com.graminsaathi.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class VillageResponse {
    private String villageName;
    private String block;
    private String district;
    private String state;
    private Double latitude;
    private Double longitude;
    private Integer population5kmRadius;
    private Integer households5kmRadius;
    private Map<String, BusinessDataResponse> businessData;
}