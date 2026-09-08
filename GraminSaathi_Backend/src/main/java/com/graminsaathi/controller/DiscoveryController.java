package com.graminsaathi.controller;

import com.graminsaathi.dto.request.DiscoverRequest;
import com.graminsaathi.dto.response.DiscoveryResponse;
import com.graminsaathi.dto.response.DiscoveredBusinessResponse;
import com.graminsaathi.service.DiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    @PostMapping("/discover")
    public ResponseEntity<DiscoveryResponse> discover(@RequestBody DiscoverRequest request) {
        DiscoveryService.DiscoveryResult result = discoveryService.discover(request);

        DiscoveryResponse response = new DiscoveryResponse();
        response.setBusinesses(result.businesses().stream()
                .map(this::mapToResponse)
                .toList());

        return ResponseEntity.ok(response);
    }

    private DiscoveredBusinessResponse mapToResponse(DiscoveryService.DiscoveredBusiness b) {
        DiscoveredBusinessResponse r = new DiscoveredBusinessResponse();
        r.setCategoryName(b.categoryName());
        r.setOpportunityScore(b.opportunityScore());
        r.setOpportunityLabel(b.opportunityLabel());
        r.setAffordabilityFlag(b.affordabilityFlag());
        r.setReferenceProjectCost(b.referenceProjectCost());
        r.setReferenceMonthlyRevenue(b.referenceMonthlyRevenue());
        r.setReferenceMonthlyOperatingCost(b.referenceMonthlyOperatingCost());
        return r;
    }
}