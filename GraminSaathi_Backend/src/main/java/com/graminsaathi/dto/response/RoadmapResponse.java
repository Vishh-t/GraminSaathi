package com.graminsaathi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class RoadmapResponse {
    private List<Milestone> milestones;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Milestone {
        private int month;
        private String milestone;

    }
}