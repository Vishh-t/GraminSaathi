package com.graminsaathi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.graminsaathi.dto.request.SchemeMatchRequest;
import com.graminsaathi.dto.response.SchemeMatchItemResponse;
import com.graminsaathi.dto.response.SchemeMatchResponse;
import com.graminsaathi.model.Scheme;
import com.graminsaathi.service.BenefitCalculator;
import com.graminsaathi.service.SchemeMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * Full-schemes-browse feature, backend half (status doc section 6, Phase B): unlike
 * {@code /api/analyze}'s baseline-applicant top-5 loan comparison, this runs an applicant's full
 * profile against every one of the 201 schemes (loans, grants, subsidies, guarantees alike) via
 * {@link SchemeMatchingService}, not just the 47 loan-type ones.
 */
@RestController
@RequestMapping("/api/schemes")
@RequiredArgsConstructor
public class SchemesController {

    private final SchemeMatchingService schemeMatchingService;

    @PostMapping("/match")
    public ResponseEntity<SchemeMatchResponse> match(@RequestBody(required = false) SchemeMatchRequest request) {
        SchemeMatchRequest req = request != null ? request : new SchemeMatchRequest();

        SchemeMatchingService.MatchFilters filters = new SchemeMatchingService.MatchFilters(
                req.getLevel(), req.getSector(), req.getState(), req.getCalculationType());
        BenefitCalculator.BenefitInput input =
                new BenefitCalculator.BenefitInput(req.getActualCost(), req.getLoanAmount());

        List<SchemeMatchingService.SchemeMatch> matches =
                schemeMatchingService.findEligibleRanked(req.getApplicant(), filters, input);

        SchemeMatchResponse response = new SchemeMatchResponse();
        response.setMatches(matches.stream().map(this::toResponse).toList());
        return ResponseEntity.ok(response);
    }

    private SchemeMatchItemResponse toResponse(SchemeMatchingService.SchemeMatch match) {
        Scheme scheme = match.scheme();
        SchemeMatchItemResponse r = new SchemeMatchItemResponse();

        r.setSchemeId(scheme.getSchemeId());
        r.setName(scheme.getName());
        r.setLevel(scheme.getLevel());
        r.setState(scheme.getState());
        r.setMinistry(scheme.getMinistry());
        r.setImplementingAgency(scheme.getImplementingAgency());
        r.setType(scheme.getType());
        r.setSubType(scheme.getSubType());

        r.setBenefit(toBenefitResponse(match.benefit()));
        r.setEnhancementApplied(toEnhancementResponse(match.enhancementApplied()));

        r.setInterestRate(scheme.getInterestRate());
        r.setRateEstimated(scheme.getRateEstimated());
        r.setTenureYears(scheme.getTenureYears());
        r.setMoratoriumMonths(scheme.getMoratoriumMonths());
        if (scheme.getLoanAmountRange() != null) {
            r.setLoanAmountMin(scheme.getLoanAmountRange().getMin());
            r.setLoanAmountMax(scheme.getLoanAmountRange().getMax());
        }

        r.setEligibilityHuman(scheme.getEligibilityHuman());
        r.setDocumentsRequired(documentNames(scheme));
        r.setApplicationSteps(scheme.getApplicationSteps());
        r.setApplicationChannel(scheme.getApplicationChannel());
        r.setPortalUrl(scheme.getPortalUrl());
        r.setApplicationFormUrl(scheme.getApplicationFormUrl());
        r.setPros(scheme.getPros());
        r.setCons(scheme.getCons());
        r.setProcessingTimeDays(scheme.getProcessingTimeDays());

        return r;
    }

    /** Mirrors {@code SchemeComparisonResponse}'s approach - names only, mandatory/source detail dropped. */
    private List<String> documentNames(Scheme scheme) {
        List<JsonNode> docs = scheme.getDocumentsRequired();
        if (docs == null) return null;
        return docs.stream()
                .map(d -> d.path("name").asText(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private SchemeMatchItemResponse.BenefitResponse toBenefitResponse(BenefitCalculator.BenefitResult result) {
        if (result == null) return null;
        SchemeMatchItemResponse.BenefitResponse r = new SchemeMatchItemResponse.BenefitResponse();
        r.setCalculationType(result.calculationType());
        r.setAmount(result.amount());
        r.setEffectiveInterestRatePct(result.effectiveInterestRatePct());
        r.setGuaranteeCoverPct(result.guaranteeCoverPct());
        r.setDisplayText(result.displayText());
        r.setComponents(result.components() == null ? null
                : result.components().stream().map(this::toBenefitResponse).toList());
        return r;
    }

    private SchemeMatchItemResponse.EnhancementResponse toEnhancementResponse(SchemeMatchingService.EnhancementApplied enhancement) {
        if (enhancement == null) return null;
        SchemeMatchItemResponse.EnhancementResponse r = new SchemeMatchItemResponse.EnhancementResponse();
        r.setKey(enhancement.key());
        r.setSubsidyPct(enhancement.subsidyPct());
        r.setNote(enhancement.note());
        return r;
    }
}
