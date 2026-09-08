package com.graminsaathi.controller;

import com.graminsaathi.dto.request.SimulateRequest;
import com.graminsaathi.dto.response.SimulationResponse;
import com.graminsaathi.service.SurvivalSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SimulationController {

    private final SurvivalSimulatorService survivalSimulatorService;

    @PostMapping("/simulate")
    public ResponseEntity<SimulationResponse> simulate(@RequestBody SimulateRequest request) {
        SurvivalSimulatorService.SimulationResult result = survivalSimulatorService.simulate(request);

        SimulationResponse response = new SimulationResponse();
        response.setCashCurve(result.cashCurve().stream()
                .map(p -> new SimulationResponse.CashPoint(p.month(), p.cumulativeCash()))
                .toList());
        response.setVerdict(result.verdict());
        response.setDeficitMonth(result.deficitMonth());

        return ResponseEntity.ok(response);
    }
}