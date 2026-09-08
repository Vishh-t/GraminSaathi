package com.graminsaathi.controller;

import com.graminsaathi.dto.request.AnalyzeRequest;
import com.graminsaathi.dto.request.SaveReportRequest;
import com.graminsaathi.model.SavedReport;
import com.graminsaathi.model.User;
import com.graminsaathi.repository.SavedReportRepository;
import com.graminsaathi.repository.UserRepository;
import com.graminsaathi.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final PdfGeneratorService pdfGeneratorService;
    private final SavedReportRepository savedReportRepository;
    private final UserRepository userRepository;

    @GetMapping("/analyze/pdf")
    public ResponseEntity<Resource> downloadPdf(@RequestParam String villageName,
                                                  @RequestParam String businessCategory,
                                                  @RequestParam Double availableMarginCapital) {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setVillageName(villageName);
        request.setBusinessCategory(businessCategory);
        request.setAvailableMarginCapital(availableMarginCapital);

        byte[] pdfBytes = pdfGeneratorService.generateReport(request);

        ByteArrayResource resource = new ByteArrayResource(pdfBytes);
        String filename = String.format("GraminSaathi_Report_%s_%s.pdf", villageName, businessCategory);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    @PostMapping("/reports")
    public ResponseEntity<SavedReport> saveReport(@RequestBody SaveReportRequest request,
                                                   @AuthenticationPrincipal User user) {
        User fullUser = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SavedReport savedReport = SavedReport.builder()
                .user(fullUser)
                .villageName(request.getVillageName())
                .businessCategory(request.getBusinessCategory())
                .availableMarginCapital(request.getAvailableMarginCapital())
                .resultJson(request.getResultJson())
                .build();

        SavedReport saved = savedReportRepository.save(savedReport);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<SavedReport>> getReports(@AuthenticationPrincipal User user) {
        User fullUser = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<SavedReport> reports = savedReportRepository.findByUserIdOrderByCreatedAtDesc(fullUser.getId());
        return ResponseEntity.ok(reports);
    }
}