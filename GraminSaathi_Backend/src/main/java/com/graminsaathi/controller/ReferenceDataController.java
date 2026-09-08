package com.graminsaathi.controller;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.data.SchemesDataLoader;
import com.graminsaathi.data.SchemesReference;
import com.graminsaathi.dto.response.VillageResponse;
import com.graminsaathi.dto.response.BusinessCategoryResponse;
import com.graminsaathi.dto.response.BusinessDataResponse;
import com.graminsaathi.dto.response.SchemeComparisonResponse;
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
    private final SchemesDataLoader schemesDataLoader;

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

    @GetMapping("/schemes")
    public ResponseEntity<List<SchemeComparisonResponse>> getAllSchemes() {
        List<SchemeComparisonResponse> schemes = schemesDataLoader.getAllSchemes().stream()
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

    private SchemeComparisonResponse mapToSchemeResponse(SchemesReference.Scheme scheme) {
        SchemeComparisonResponse response = new SchemeComparisonResponse();
        response.setSchemeName(scheme.getSchemeName());
        response.setInterestRate(scheme.getInterestRate() * 100);
        response.setTenureYears(scheme.getTenureYears());
        response.setMoratoriumMonths(scheme.getMoratoriumMonths());
        response.setAgency(scheme.getAgency());
        response.setSubsidyNote(scheme.getSubsidyNote());
        response.setPrimary(false);
        return response;
    }
}