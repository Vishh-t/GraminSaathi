package com.graminsaathi.controller;

import com.graminsaathi.dto.request.DiscoverRequest;
import com.graminsaathi.service.DiscoveryService;
import com.graminsaathi.service.FinancialRangeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rewritten against the real discover(request, applicant) signature and the marketScore/personFitScore
 * DiscoveredBusiness shape - see Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, "Discovery wiring"
 * section. ProfileService is intentionally left un-mocked, same pattern as AnalysisControllerTest: with
 * no auth principal on this anonymous MockMvc call, DiscoveryController resolves an empty ApplicantProfile
 * without touching the DB (see ProfileService#getApplicantProfile).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiscoveryService discoveryService;

    @Test
    void discover() throws Exception {
        FinancialRangeService.MonthlyRange zero = new FinancialRangeService.MonthlyRange(0, 0, 0);
        FinancialRangeService.FinancialRangeResult emptyRange =
                new FinancialRangeService.FinancialRangeResult(zero, zero, zero, 1.0);

        when(discoveryService.discover(any(), any())).thenReturn(
                new DiscoveryService.DiscoveryResult(List.of(
                        new DiscoveryService.DiscoveredBusiness("Dairy", 85, "MEDIUM_FULL_DATA",
                                "Estimated monthly demand exceeds supply", 85, "Strong fit", "Within budget",
                                1000000.0, 45000.0, 31500.0, emptyRange),
                        new DiscoveryService.DiscoveredBusiness("Tailoring", 45, "MEDIUM_FULL_DATA",
                                "Estimated monthly demand roughly matches supply", 45, "Moderate fit",
                                "May require phasing", 250000.0, 18000.0, 12600.0, emptyRange)
                ))
        );

        DiscoverRequest request = new DiscoverRequest("Ghoti", 100000.0);

        mockMvc.perform(post("/api/discover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businesses[0].categoryName").value("Dairy"))
                .andExpect(jsonPath("$.businesses[0].personFitScore").value(85))
                .andExpect(jsonPath("$.businesses[1].categoryName").value("Tailoring"));
    }
}
