package com.graminsaathi.dto.response;

import lombok.Data;

/**
 * One autocomplete result from the real village master table ({@link com.graminsaathi.model.Village}).
 * Deliberately separate from {@link VillageResponse}, which shapes the old demo dataset (5km-radius
 * business figures) - this one only carries identity/location fields, plus Census figures when known.
 */
@Data
public class VillageSearchResponse {
    private Long id;
    private String lgdCode;
    private String name;
    private String block;
    private String district;
    private String state;
    private Double latitude;
    private Double longitude;
    private Integer population2011;
    private Integer households2011;
    /** Where this row's data came from (file name / dataset), so the frontend can show provenance if needed. */
    private String source;
}
