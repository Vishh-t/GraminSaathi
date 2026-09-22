package com.graminsaathi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.model.User;
import com.graminsaathi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persists the "ask once" applicant profile against the user's account (see {@link User#getApplicantProfileJson()})
 * so the frontend intake form only ever needs to run a single time per person, not once per analysis.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /** Never null: an empty (all-fields-null) profile when nothing has been saved yet, or the user isn't logged in. */
    public ApplicantProfile getApplicantProfile(User user) {
        if (user == null || user.getApplicantProfileJson() == null) {
            return ApplicantProfile.builder().build();
        }
        try {
            return objectMapper.readValue(user.getApplicantProfileJson(), ApplicantProfile.class);
        } catch (Exception e) {
            // Corrupt/old-shape JSON should never break the analysis flow - fall back to empty.
            return ApplicantProfile.builder().build();
        }
    }

    /**
     * Merges {@code incoming} onto whatever is already saved and persists the result, so answering one
     * more onboarding screen later never wipes out earlier answers.
     */
    public ApplicantProfile saveApplicantProfile(User user, ApplicantProfile incoming) {
        ApplicantProfile merged = merge(getApplicantProfile(user), incoming);
        try {
            user.setApplicantProfileJson(objectMapper.writeValueAsString(merged));
            userRepository.save(user);
        } catch (Exception e) {
            throw new IllegalStateException("Could not save applicant profile", e);
        }
        return merged;
    }

    /**
     * Field-by-field merge: any non-null field on {@code overrides} wins; otherwise the value from
     * {@code base} is kept. Used both to fold new intake-form answers onto a saved profile, and to fold
     * a request-supplied {@code applicant} (session-specific, one-off facts) onto the user's saved one
     * at analysis time.
     */
    @SuppressWarnings("unchecked")
    public ApplicantProfile merge(ApplicantProfile base, ApplicantProfile overrides) {
        if (overrides == null) return base != null ? base : ApplicantProfile.builder().build();
        if (base == null) return overrides;

        Map<String, Object> baseMap = new LinkedHashMap<>(objectMapper.convertValue(base, Map.class));
        Map<String, Object> overrideMap = objectMapper.convertValue(overrides, Map.class);
        overrideMap.forEach((key, value) -> {
            if (value != null) baseMap.put(key, value);
        });
        return objectMapper.convertValue(baseMap, ApplicantProfile.class);
    }
}
