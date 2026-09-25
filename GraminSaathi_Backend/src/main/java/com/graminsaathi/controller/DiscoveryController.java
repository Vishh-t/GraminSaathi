package com.graminsaathi.controller;

import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.dto.request.DiscoverRequest;
import com.graminsaathi.dto.response.DiscoveredBusinessResponse;
import com.graminsaathi.dto.response.DiscoveryResponse;
import com.graminsaathi.model.User;
import com.graminsaathi.service.DiscoveryService;
import com.graminsaathi.service.FinancialRangeService;
import com.graminsaathi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;
    private final ProfileService profileService;

    /**
     * Same auto-pull pattern as {@link AnalysisController#analyze} - a logged-in user's saved "ask once"
     * profile (ProfileService) is used as the applicant for discovery, so nothing needs to change on the
     * frontend for this to at least run. Anonymous callers get an empty profile (see
     * {@link ProfileService#getApplicantProfile}), which just means every category clears the applicant
     * hard-filter and person-fit scores capital adequacy only.
     */
    @PostMapping("/discover")
    public ResponseEntity<DiscoveryResponse> discover(@RequestBody DiscoverRequest request,
                                                        @AuthenticationPrincipal User user) {
        ApplicantProfile applicant = profileService.getApplicantProfile(user);

        DiscoveryService.DiscoveryResult result = discoveryService.discover(request, applicant);

        DiscoveryResponse response = new DiscoveryResponse();
        response.setBusinesses(result.businesses().stream()
                .map(this::mapToResponse)
                .toList());

        return ResponseEntity.ok(response);
    }

    private DiscoveredBusinessResponse mapToResponse(DiscoveryService.DiscoveredBusiness b) {
        DiscoveredBusinessResponse r = new DiscoveredBusinessResponse();
        r.setCategoryName(b.categoryName());
        r.setMarketScore(b.marketScore());
        r.setMarketConfidence(b.marketConfidence());
        r.setMarketExplanation(b.marketExplanation());
        r.setPersonFitScore(b.personFitScore());
        r.setPersonFitLabel(b.personFitLabel());
        r.setAffordabilityFlag(b.affordabilityFlag());
        r.setReferenceProjectCost(b.referenceProjectCost());
        r.setReferenceMonthlyRevenue(b.referenceMonthlyRevenue());
        r.setReferenceMonthlyOperatingCost(b.referenceMonthlyOperatingCost());
        r.setFinancialRange(mapFinancialRange(b.financialRange()));
        return r;
    }

    private DiscoveredBusinessResponse.FinancialRange mapFinancialRange(FinancialRangeService.FinancialRangeResult f) {
        DiscoveredBusinessResponse.FinancialRange r = new DiscoveredBusinessResponse.FinancialRange();
        r.setRevenue(mapMonthlyRange(f.revenue()));
        r.setOperatingCost(mapMonthlyRange(f.operatingCost()));
        r.setProfit(mapMonthlyRange(f.profit()));
        r.setStateConsumptionMultiplierUsed(f.stateConsumptionMultiplierUsed());
        return r;
    }

    private DiscoveredBusinessResponse.MonthlyRange mapMonthlyRange(FinancialRangeService.MonthlyRange m) {
        return new DiscoveredBusinessResponse.MonthlyRange(m.pessimistic(), m.base(), m.optimistic());
    }
}
