package com.graminsaathi.controller;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.data.SchemesDataLoader;
import com.graminsaathi.dto.response.VillageResponse;
import com.graminsaathi.dto.response.BusinessCategoryResponse;
import com.graminsaathi.dto.response.SchemeComparisonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReferenceDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DemoDataLoader demoDataLoader;

    @MockBean
    private SchemesDataLoader schemesDataLoader;

    @Test
    void getAllVillages() throws Exception {
        DemoData.VillageData village = new DemoData.VillageData();
        village.setVillageName("Ghoti");
        village.setBlock("Igatpuri");
        village.setDistrict("Nashik");
        village.setState("Maharashtra");
        village.setLatitude(19.7515);
        village.setLongitude(73.6197);
        village.setPopulation5kmRadius(8420);
        village.setHouseholds5kmRadius(1640);
        village.setBusinessData(Map.of("Dairy", createBusinessData(2, 42.0)));

        when(demoDataLoader.getAllVillages()).thenReturn(List.of(village));

        mockMvc.perform(get("/api/villages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].villageName").value("Ghoti"))
                .andExpect(jsonPath("$[0].population5kmRadius").value(8420));
    }

    @Test
    void getAllBusinessCategories() throws Exception {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Dairy");
        category.setReferenceProjectCost(1000000.0);
        category.setReferenceCostRange(List.of(650000.0, 1400000.0));
        category.setWorkingCapitalMonths(3);
        category.setReferenceMonthlyRevenue(45000.0);
        category.setReferenceMonthlyOperatingCost(31500.0);
        category.setSecondaryOpportunity("Vermicompost");
        category.setSourceNote("Test");

        when(demoDataLoader.getAllBusinessCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/business-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryName").value("Dairy"))
                .andExpect(jsonPath("$[0].referenceProjectCost").value(1000000.0));
    }

    @Test
    void getAllSchemes() throws Exception {
        com.graminsaathi.data.SchemesReference.Scheme scheme = new com.graminsaathi.data.SchemesReference.Scheme();
        scheme.setSchemeName("Micro Finance Scheme");
        scheme.setMinCost(0.0);
        scheme.setMaxCost(140000.0);
        scheme.setInterestRate(0.065);
        scheme.setTenureYears(3);
        scheme.setMoratoriumMonths(3);
        scheme.setAgency("NBCFDC/NSFDC-style SCA");

        when(schemesDataLoader.getAllSchemes()).thenReturn(List.of(scheme));

        mockMvc.perform(get("/api/schemes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].schemeName").value("Micro Finance Scheme"))
                .andExpect(jsonPath("$[0].interestRate").value(6.5));
    }

    private DemoData.BusinessData createBusinessData(int competitors, Double price) {
        DemoData.BusinessData data = new DemoData.BusinessData();
        data.setCompetitorCount(competitors);
        data.setAvgLocalPrice(price);
        return data;
    }
}