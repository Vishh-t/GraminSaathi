package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessCombinationServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;

    private BusinessCombinationService businessCombinationService;

    @BeforeEach
    void setUp() {
        businessCombinationService = new BusinessCombinationService(demoDataLoader);
    }

    @Test
    void testGetCombinationDairy() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Dairy");
        category.setSecondaryOpportunity("Vermicompost from dairy waste");

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        BusinessCombinationService.CombinationResult result = businessCombinationService.getCombination("Dairy");

        assertEquals("Vermicompost from dairy waste", result.secondaryOpportunity());
    }

    @Test
    void testGetCombinationTailoring() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Tailoring");
        category.setSecondaryOpportunity("School uniform bulk contracts");

        when(demoDataLoader.getBusinessCategory("Tailoring")).thenReturn(category);

        BusinessCombinationService.CombinationResult result = businessCombinationService.getCombination("Tailoring");

        assertEquals("School uniform bulk contracts", result.secondaryOpportunity());
    }

    @Test
    void testInvalidCategory() {
        when(demoDataLoader.getBusinessCategory("Invalid")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> businessCombinationService.getCombination("Invalid"));
    }
}