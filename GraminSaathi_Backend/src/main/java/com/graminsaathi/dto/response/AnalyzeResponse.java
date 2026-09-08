package com.graminsaathi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzeResponse {
    private FinancialResponse financial;
    private FeasibilityResponse feasibility;
    private DscrResponse dscr;
    private SurvivalResponse survival;
    private CombinationResponse combination;
    private EvidenceResponse evidence;
    private CommitteeResponse committee;
    private HealthScoreResponse healthScore;
    private SupplyRiskResponse supplyRisk;
    private PeerBenchmarkResponse peerBenchmark;
    private RoadmapResponse roadmap;
}