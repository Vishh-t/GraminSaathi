package com.graminsaathi.controller;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.model.Scheme;
import com.graminsaathi.service.FinancingSchemeService;
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
    private FinancingSchemeService financingSchemeService;

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
        Scheme scheme = new Scheme();
        scheme.setSchemeId("test-scheme");
        scheme.setName("Small Loan Scheme");
        scheme.setInterestRate(6.5); // percent, matching the real dataset
        scheme.setTenureYears(3.0);
        scheme.setMoratoriumMonths(3);
        scheme.setImplementingAgency("NBCFDC/NSFDC-style SCA");
        scheme.setEffectiveInterestRateNote("Test note");

        when(financingSchemeService.getFinancingSchemes()).thenReturn(List.of(scheme));

        mockMvc.perform(get("/api/schemes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].schemeName").value("Small Loan Scheme"))
                .andExpect(jsonPath("$[0].interestRate").value(6.5))
                .andExpect(jsonPath("$[0].tenureYears").value(3))
                .andExpect(jsonPath("$[0].moratoriumMonths").value(3))
                .andExpect(jsonPath("$[0].agency").value("NBCFDC/NSFDC-style SCA"))
                .andExpect(jsonPath("$[0].subsidyNote").value("Test note"))
                .andExpect(jsonPath("$[0].isPrimary").value(false)) // @JsonProperty("isPrimary") pins the key (see doc finding 3.7)
                .andExpect(jsonPath("$[0].rateEstimated").value(false))
                .andExpect(jsonPath("$[0].emi").doesNotExist())
                .andExpect(jsonPath("$[0].totalRepayment").doesNotExist());
    }

    @Test
    void getAllSchemesFillsInAMissingMoratoriumAndFlagsAnEstimatedRate() throws Exception {
        Scheme scheme = new Scheme();
        scheme.setSchemeId("test-estimated");
        scheme.setName("Estimated Rate Scheme");
        scheme.setInterestRate(7.5);
        scheme.setTenureYears(2.5); // fractional tenure, e.g. PM Vishwakarma - rounds for display
        scheme.setMoratoriumMonths(null);
        scheme.setRateEstimated(true);
        scheme.setImplementingAgency("Test Agency");

        when(financingSchemeService.getFinancingSchemes()).thenReturn(List.of(scheme));

        mockMvc.perform(get("/api/schemes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenureYears").value(3))
                .andExpect(jsonPath("$[0].moratoriumMonths").value(0))
                .andExpect(jsonPath("$[0].rateEstimated").value(true));
    }

    private DemoData.BusinessData createBusinessData(int competitors, Double price) {
        DemoData.BusinessData data = new DemoData.BusinessData();
        data.setCompetitorCount(competitors);
        data.setAvgLocalPrice(price);
        return data;
    }
}
