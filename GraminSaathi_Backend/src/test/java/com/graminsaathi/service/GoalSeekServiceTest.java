package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.GoalSeekRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalSeekServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;

    private GoalSeekService goalSeekService;

    @BeforeEach
    void setUp() {
        goalSeekService = new GoalSeekService(demoDataLoader);
    }

    @Test
    void testGoalSeekDairy() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Dairy");
        category.setReferenceProjectCost(1000000.0);
        category.setReferenceMonthlyRevenue(45000.0);
        category.setReferenceMonthlyOperatingCost(31500.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        GoalSeekService.GoalSeekResult result = goalSeekService.goalSeek(
                new GoalSeekRequest("Dairy", 20000.0)
        );

        // required_revenue = 20000 + 31500 = 51500
        // scale = 51500 / 45000 = 1.144
        // project_cost = 1000000 * 1.144 = 1144444
        // margin = 114444
        // loan = 1030000
        assertEquals(51500.0, result.requiredMonthlyRevenue(), 0.01);
        assertEquals(1144444.44, result.estimatedProjectCost(), 1.0);
        assertEquals(114444.44, result.estimatedMarginRequired(), 1.0);
        assertEquals(1030000.0, result.estimatedLoanRequired(), 1.0);
        assertEquals("Estimated — scaled from reference model data.", result.note());
    }

    @Test
    void testGoalSeekTailoring() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Tailoring");
        category.setReferenceProjectCost(250000.0);
        category.setReferenceMonthlyRevenue(18000.0);
        category.setReferenceMonthlyOperatingCost(12600.0);

        when(demoDataLoader.getBusinessCategory("Tailoring")).thenReturn(category);

        GoalSeekService.GoalSeekResult result = goalSeekService.goalSeek(
                new GoalSeekRequest("Tailoring", 10000.0)
        );

        // required_revenue = 10000 + 12600 = 22600
        // scale = 22600 / 18000 = 1.256
        // project_cost = 250000 * 1.256 = 313889
        assertEquals(22600.0, result.requiredMonthlyRevenue(), 0.01);
        assertEquals(313888.89, result.estimatedProjectCost(), 1.0);
    }

    @Test
    void testInvalidCategory() {
        when(demoDataLoader.getBusinessCategory("Invalid")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> goalSeekService.goalSeek(
                new GoalSeekRequest("Invalid", 10000.0)
        ));
    }

    @Test
    void testZeroIncome() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Dairy");
        category.setReferenceProjectCost(1000000.0);
        category.setReferenceMonthlyRevenue(45000.0);
        category.setReferenceMonthlyOperatingCost(31500.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        GoalSeekService.GoalSeekResult result = goalSeekService.goalSeek(
                new GoalSeekRequest("Dairy", 0.0)
        );

        // required = 0 + 31500 = 31500
        // scale = 31500 / 45000 = 0.7
        // project_cost = 1000000 * 0.7 = 700000
        assertEquals(31500.0, result.requiredMonthlyRevenue(), 0.01);
        assertEquals(700000.0, result.estimatedProjectCost(), 1.0);
    }
}