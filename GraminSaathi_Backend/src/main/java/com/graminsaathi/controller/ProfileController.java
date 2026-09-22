package com.graminsaathi.controller;

import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.model.User;
import com.graminsaathi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * The "ask once" applicant profile, saved against the logged-in user so the frontend intake form never
 * has to be shown twice. {@code /api/analyze} auto-merges this in server-side (see AnalysisController) -
 * these endpoints exist for the frontend to read back what's saved (e.g. to skip/show the intake banner)
 * and to write new answers to it.
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/applicant")
    public ResponseEntity<ApplicantProfile> getApplicantProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getApplicantProfile(user));
    }

    /** Merges onto whatever is already saved - never a destructive overwrite. */
    @PutMapping("/applicant")
    public ResponseEntity<ApplicantProfile> saveApplicantProfile(@AuthenticationPrincipal User user,
                                                                   @RequestBody ApplicantProfile profile) {
        return ResponseEntity.ok(profileService.saveApplicantProfile(user, profile));
    }
}
