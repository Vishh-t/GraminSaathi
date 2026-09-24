package com.graminsaathi.controller;

import com.graminsaathi.dto.response.VillageSearchResponse;
import com.graminsaathi.model.Village;
import com.graminsaathi.service.VillageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only autocomplete over the real village master table ({@link Village}), keyed by LGD code.
 * See Project_Docs/GraminSaathi_Recommendation_Engine_Plan.md, section 2.1 / build order step 1.
 *
 * <p>Deliberately GET-only and read-only: bulk-writing the table (CSV import, demo seeding) stays
 * off the HTTP surface and runs only at startup via {@code VillageDataInitializer}, per the note on
 * that class - no public route can bulk-write this table.
 */
@RestController
@RequestMapping("/api/villages")
@RequiredArgsConstructor
public class VillageController {

    private final VillageService villageService;

    /**
     * @param q     search text, matched against normalized village name (min 2 chars after normalizing,
     *              shorter queries return an empty list rather than scanning the table)
     * @param state optional state filter; omit or leave blank for any state
     * @param limit optional result cap (1-20, default 10)
     */
    @GetMapping("/search")
    public ResponseEntity<List<VillageSearchResponse>> search(
            @RequestParam("q") String q,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        List<Village> results = villageService.search(q, state, limit);
        return ResponseEntity.ok(results.stream().map(this::mapToResponse).toList());
    }

    private VillageSearchResponse mapToResponse(Village v) {
        VillageSearchResponse r = new VillageSearchResponse();
        r.setId(v.getId());
        r.setLgdCode(v.getLgdCode());
        r.setName(v.getName());
        r.setBlock(v.getBlock());
        r.setDistrict(v.getDistrict());
        r.setState(v.getState());
        r.setLatitude(v.getLatitude());
        r.setLongitude(v.getLongitude());
        r.setPopulation2011(v.getPopulation2011());
        r.setHouseholds2011(v.getHouseholds2011());
        r.setSource(v.getSource());
        return r;
    }
}
