package com.graminsaathi.controller;

import com.graminsaathi.dto.request.DiscoverRequest;
import com.graminsaathi.dto.response.DiscoveryResponse;
import com.graminsaathi.service.DiscoveryService;
import com.graminsaathi.service.FeasibilityScoreService;
import com.graminsaathi.service.FinancialCalculatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiscoveryService discoveryService;
    @MockBean
    private FeasibilityScoreService feasibilityScoreService;
    @MockBean
    private FinancialCalculatorService financialCalculatorService;

    @Test
    void discover() throws Exception {
        when(discoveryService.discover(any())).thenReturn(
                new DiscoveryService.DiscoveryResult(List.of(
                        new DiscoveryService.DiscoveredBusiness("Dairy", 85, "High opportunity", "Within budget", 1000000.0, 45000.0, 31500.0),
                        new DiscoveryService.DiscoveredBusiness("Tailoring", 45, "Moderate opportunity", "May require phasing", 250000.0, 18000.0, 12600.0)
                ))
        );

        DiscoverRequest request = new DiscoverRequest("Ghoti", 100000.0);

        mockMvc.perform(post("/api/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businesses[0].categoryName").value("Dairy"))
                .andExpect(jsonPath("$.businesses[0].opportunityScore").value(85))
                .andExpect(jsonPath("$.businesses[1].categoryName").value("Tailoring"));
    }
}