package com.graminsaathi.controller;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.response.VillageResponse;
import com.graminsaathi.dto.response.BusinessCategoryResponse;
import com.graminsaathi.dto.response.BusinessDataResponse;
import com.graminsaathi.dto.response.SchemeComparisonResponse;
import com.graminsaathi.model.Scheme;
import com.graminsaathi.service.FinancingSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final DemoDataLoader demoDataLoader;
    private final FinancingSchemeService financingSchemeService;

    @GetMapping("/villages")
    public ResponseEntity<List<VillageResponse>> getAllVillages() {
        List<VillageResponse> villages = demoDataLoader.getAllVillages().stream()
                .map(this::mapToVillageResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(villages);
    }

    @GetMapping("/business-categories")
    public ResponseEntity<List<BusinessCategoryResponse>> getAllBusinessCategories() {
        List<BusinessCategoryResponse> categories = demoDataLoader.getAllBusinessCategories().stream()
                .map(this::mapToBusinessCategoryResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    /**
     * Every real financing scheme (own or estimated interest rate, tenure >= 1 year - see
     * {@link FinancingSchemeService#getFinancingSchemes()}), with no loan amount or applicant to rank
     * them against. So unlike the per-analysis comparison table, no row here is a "primary" pick and
     * none carries total repayment, subsidy, net cost or an affordability verdict - those only make
     * sense for a specific loan.
     */
    @GetMapping("/schemes")
    public ResponseEntity<List<SchemeComparisonResponse>> getAllSchemes() {
        List<SchemeComparisonResponse> schemes = financingSchemeService.getFinancingSchemes().stream()
                .map(this::mapToSchemeResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(schemes);
    }

    private VillageResponse mapToVillageResponse(DemoData.VillageData village) {
        VillageResponse response = new VillageResponse();
        response.setVillageName(village.getVillageName());
        response.setBlock(village.getBlock());
        response.setDistrict(village.getDistrict());
        response.setState(village.getState());
        response.setLatitude(village.getLatitude());
        response.setLongitude(village.getLongitude());
        response.setPopulation5kmRadius(village.getPopulation5kmRadius());
        response.setHouseholds5kmRadius(village.getHouseholds5kmRadius());

        if (village.getBusinessData() != null) {
            Map<String, BusinessDataResponse> businessDataMap = village.getBusinessData().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> {
                                BusinessDataResponse bdr = new BusinessDataResponse();
                                bdr.setCompetitorCount(e.getValue().getCompetitorCount());
                                bdr.setAvgLocalPrice(e.getValue().getAvgLocalPrice());
                                return bdr;
                            }
                    ));
            response.setBusinessData(businessDataMap);
        }

        return response;
    }

    private BusinessCategoryResponse mapToBusinessCategoryResponse(DemoData.BusinessCategoryData category) {
        BusinessCategoryResponse response = new BusinessCategoryResponse();
        response.setCategoryName(category.getCategoryName());
        response.setReferenceProjectCost(category.getReferenceProjectCost());
        response.setReferenceCostRange(category.getReferenceCostRange());
        response.setWorkingCapitalMonths(category.getWorkingCapitalMonths());
        response.setReferenceMonthlyRevenue(category.getReferenceMonthlyRevenue());
        response.setReferenceMonthlyOperatingCost(category.getReferenceMonthlyOperatingCost());
        response.setSecondaryOpportunity(category.getSecondaryOpportunity());
        response.setSourceNote(category.getSourceNote());
        return response;
    }

    private SchemeComparisonResponse mapToSchemeResponse(Scheme scheme) {
        SchemeComparisonResponse response = new SchemeComparisonResponse();
        response.setSchemeName(scheme.getName());
        response.setInterestRate(scheme.getInterestRate()); // dataset/estimate already in percent
        response.setTenureYears((int) Math.round(scheme.getTenureYears()));
        response.setMoratoriumMonths(scheme.getMoratoriumMonths() != null ? scheme.getMoratoriumMonths() : 0);
        response.setAgency(scheme.getImplementingAgency());
        response.setSubsidyNote(scheme.getEffectiveInterestRateNote());
        response.setRateEstimated(Boolean.TRUE.equals(scheme.getRateEstimated()));
        response.setPrimary(false);
        return response;
    }
}
