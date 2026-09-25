package com.graminsaathi.service;

import com.graminsaathi.model.Village;
import com.graminsaathi.model.VillageFeatures;
import com.graminsaathi.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VillageFeaturesServiceTest {

    @Mock
    private VillageRepository villageRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private VillageFeaturesService service;

    @BeforeEach
    void setUp() {
        PopulationProjector projector = new PopulationProjector(12.18, Map.of());
        service = new VillageFeaturesService(villageRepository, projector, jdbcTemplate);
    }

    private static Village village(long id, Integer pop2011, Integer households2011, String state, String source) {
        return Village.builder()
                .id(id)
                .name("V" + id)
                .nameNormalized("v" + id)
                .district("D")
                .state(state)
                .population2011(pop2011)
                .households2011(households2011)
                .source(source)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void projectsVillagesWithPopulationAndSkipsThoseWithout() {
        Village withPop = village(1L, 1000, 200, "Goa", "census.csv");
        Village withoutPop = village(2L, null, null, "Goa", "census.csv");

        List<Village> onlyBatch = List.of(withPop, withoutPop);
        when(villageRepository.findByIdGreaterThanOrderByIdAsc(anyLong(), any(Pageable.class))).thenReturn(onlyBatch);

        VillageFeaturesService.PopulationRunSummary summary = service.runPopulationProjection(2026);

        assertEquals(2, summary.processed());
        assertEquals(1, summary.projected());
        assertEquals(1, summary.skippedNoPopulation());

        ArgumentCaptor<Collection<VillageFeatures>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture(), anyInt(),
                any(ParameterizedPreparedStatementSetter.class));
        List<VillageFeatures> saved = List.copyOf(captor.getValue());
        assertEquals(1, saved.size());

        VillageFeatures f = saved.get(0);
        assertEquals(1L, f.getVillageId());
        assertEquals(2026, f.getProjectionYear());
        assertNotNull(f.getHouseholdsProjected());
        assertTrue(f.getHouseholdsProjected() > 200, "2026 projection should exceed the 2011 base households");
        assertTrue(f.getPopulationProjected() > 1000, "2026 projection should exceed the 2011 base population");
        assertEquals(5.0, f.getAvgHouseholdSize());
        assertEquals("census.csv", f.getPopulationSource());
        assertEquals(PopulationProjector.CENSUS_REFERENCE_DATE, f.getPopulationAsOf());
        assertNotNull(f.getFeaturesUpdatedAt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyVillageTableProducesNoWrites() {
        when(villageRepository.findByIdGreaterThanOrderByIdAsc(anyLong(), any(Pageable.class))).thenReturn(List.of());

        VillageFeaturesService.PopulationRunSummary summary = service.runPopulationProjection(2026);

        assertEquals(0, summary.processed());
        assertEquals(0, summary.projected());
        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(Collection.class), anyInt(),
                any(ParameterizedPreparedStatementSetter.class));
    }
}
