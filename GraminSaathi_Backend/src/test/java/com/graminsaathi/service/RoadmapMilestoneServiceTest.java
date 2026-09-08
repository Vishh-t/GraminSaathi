package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoadmapMilestoneServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;

    private RoadmapMilestoneService roadmapMilestoneService;

    @BeforeEach
    void setUp() {
        roadmapMilestoneService = new RoadmapMilestoneService(demoDataLoader);
    }

    @Test
    void testGetRoadmapDairy() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Dairy");
        category.setRoadmapMilestones(List.of(
                new DemoData.RoadmapMilestone(1, "Complete loan disbursal"),
                new DemoData.RoadmapMilestone(2, "Begin operations"),
                new DemoData.RoadmapMilestone(3, "Moratorium ends"),
                new DemoData.RoadmapMilestone(6, "Evaluate pricing"),
                new DemoData.RoadmapMilestone(12, "First full-year review")
        ));

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        RoadmapMilestoneService.RoadmapResult result = roadmapMilestoneService.getRoadmap("Dairy");

        assertEquals(5, result.milestones().size());
        assertEquals(1, result.milestones().get(0).month());
        assertEquals("Complete loan disbursal", result.milestones().get(0).milestone());
        assertEquals(12, result.milestones().get(4).month());
        assertEquals("First full-year review", result.milestones().get(4).milestone());
    }

    @Test
    void testInvalidCategory() {
        when(demoDataLoader.getBusinessCategory("Invalid")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> roadmapMilestoneService.getRoadmap("Invalid"));
    }
}