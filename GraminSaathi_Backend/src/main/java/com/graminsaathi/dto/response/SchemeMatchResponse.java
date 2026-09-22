package com.graminsaathi.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SchemeMatchResponse {
    /**
     * Filtered + ranked (see {@link com.graminsaathi.service.SchemeMatchingService#findEligibleRanked}),
     * full set - deliberately not paginated server-side; the frontend does its own "best 5-7 + see more"
     * slicing on top (status doc section 6, Phase C).
     */
    private List<SchemeMatchItemResponse> matches;
}
