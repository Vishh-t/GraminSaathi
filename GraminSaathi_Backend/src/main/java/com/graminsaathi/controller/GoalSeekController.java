package com.graminsaathi.controller;

import com.graminsaathi.dto.request.GoalSeekRequest;
import com.graminsaathi.dto.response.GoalSeekResponse;
import com.graminsaathi.service.GoalSeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GoalSeekController {

    private final GoalSeekService goalSeekService;

    @PostMapping("/goal-seek")
    public ResponseEntity<GoalSeekResponse> goalSeek(@RequestBody GoalSeekRequest request) {
        GoalSeekService.GoalSeekResult result = goalSeekService.goalSeek(request);

        GoalSeekResponse response = new GoalSeekResponse();
        response.setEstimatedProjectCost(result.estimatedProjectCost());
        response.setEstimatedMarginRequired(result.estimatedMarginRequired());
        response.setEstimatedLoanRequired(result.estimatedLoanRequired());
        response.setRequiredMonthlyRevenue(result.requiredMonthlyRevenue());
        response.setNote(result.note());

        return ResponseEntity.ok(response);
    }
}