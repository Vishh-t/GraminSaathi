package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoadmapMilestoneService {

    private final DemoDataLoader demoDataLoader;

    public record Milestone(int month, String milestone) {}

    public record RoadmapResult(List<Milestone> milestones) {}

    public RoadmapResult getRoadmap(String businessCategory) {
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(businessCategory);
        if (category == null) {
            throw new IllegalArgumentException("Invalid business category: " + businessCategory);
        }

        List<Milestone> milestones = category.getRoadmapMilestones().stream()
                .map(m -> new Milestone(m.getMonth(), m.getMilestone()))
                .toList();

        return new RoadmapResult(milestones);
    }
}