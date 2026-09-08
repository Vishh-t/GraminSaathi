package com.graminsaathi.controller;

import com.graminsaathi.dto.request.GoalSeekRequest;
import com.graminsaathi.dto.response.GoalSeekResponse;
import com.graminsaathi.service.GoalSeekService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GoalSeekControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoalSeekService goalSeekService;

    @Test
    void goalSeek() throws Exception {
        when(goalSeekService.goalSeek(any())).thenReturn(
                new GoalSeekService.GoalSeekResult(
                        1144444.44, 114444.44, 1030000.0, 51500.0, "Estimated — scaled from reference model data."
                )
        );

        GoalSeekRequest request = new GoalSeekRequest("Dairy", 20000.0);

        mockMvc.perform(post("/api/goal-seek")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedProjectCost").value(1144444.44))
                .andExpect(jsonPath("$.estimatedMarginRequired").value(114444.44))
                .andExpect(jsonPath("$.note").value("Estimated — scaled from reference model data."));
    }
}