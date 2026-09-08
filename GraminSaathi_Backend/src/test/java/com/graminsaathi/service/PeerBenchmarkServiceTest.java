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
class PeerBenchmarkServiceTest {

    @Mock
    private DemoDataLoader demoDataLoader;

    private PeerBenchmarkService peerBenchmarkService;

    @BeforeEach
    void setUp() {
        peerBenchmarkService = new PeerBenchmarkService(demoDataLoader);
    }

    @Test
    void testGetBenchmarkDairy() {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Dairy");
        DemoData.PeerBenchmark benchmark = new DemoData.PeerBenchmark();
        benchmark.setSampleSize(24);
        benchmark.setAvgMonthlyRevenueAfter6Months(41000.0);
        benchmark.setPctStillOperatingAfter1Year(78);
        category.setPeerBenchmark(benchmark);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);

        PeerBenchmarkService.BenchmarkResult result = peerBenchmarkService.getBenchmark("Dairy");

        assertEquals(24, result.sampleSize());
        assertEquals(41000.0, result.avgMonthlyRevenueAfter6Months());
        assertEquals(78, result.pctStillOperatingAfter1Year());
        assertTrue(result.disclaimer().contains("Illustrative sample data"));
    }

    @Test
    void testInvalidCategory() {
        when(demoDataLoader.getBusinessCategory("Invalid")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> peerBenchmarkService.getBenchmark("Invalid"));
    }
}