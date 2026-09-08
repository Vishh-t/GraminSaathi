package com.graminsaathi.controller;

import com.graminsaathi.dto.request.SimulateRequest;
import com.graminsaathi.dto.response.SimulationResponse;
import com.graminsaathi.service.SurvivalSimulatorService;
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
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SurvivalSimulatorService survivalSimulatorService;

    @Test
    void simulate() throws Exception {
        when(survivalSimulatorService.simulate(any())).thenReturn(
                new SurvivalSimulatorService.SimulationResult(List.of(
                        new SurvivalSimulatorService.SimulationPoint(1, 100000.0),
                        new SurvivalSimulatorService.SimulationPoint(2, 95000.0)
                ), "survives", null)
        );

        SimulateRequest request = new SimulateRequest("Ghoti", "Dairy", 10000.0, 0.0, 0.0);

        mockMvc.perform(post("/api/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("survives"))
                .andExpect(jsonPath("$.cashCurve[0].month").value(1))
                .andExpect(jsonPath("$.deficitMonth").doesNotExist());
    }

    @Test
    void simulateWithShocks() throws Exception {
        when(survivalSimulatorService.simulate(any())).thenReturn(
                new SurvivalSimulatorService.SimulationResult(List.of(
                        new SurvivalSimulatorService.SimulationPoint(1, 90000.0),
                        new SurvivalSimulatorService.SimulationPoint(2, 80000.0)
                ), "deficit", 5)
        );

        SimulateRequest request = new SimulateRequest("Ghoti", "Dairy", 10000.0, -0.3, 0.2);

        mockMvc.perform(post("/api/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deficitMonth").value(5))
                .andExpect(jsonPath("$.verdict").value("deficit"));
    }
}