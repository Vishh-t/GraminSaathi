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
     * more onboarding screen later never wipes out earlier answers. Presence-aware: a key that's in
     * {@code incoming} always wins - including an explicit {@code null}, which is how a chip toggled
     * back OFF gets cleared - while a key simply absent from {@code incoming} leaves the saved value
     * untouched.
     */
    public ApplicantProfile saveApplicantProfile(User user, Map<String, Object> incoming) {
        ApplicantProfile merged = mergePresenceAware(getApplicantProfile(user), incoming);
        try {
            user.setApplicantProfileJson(objectMapper.writeValueAsString(merged));
            userRepository.save(user);
        } catch (Exception e) {
            throw new IllegalStateException("Could not save applicant profile", e);
        }
        return merged;
    }

    /** Used only by {@link #saveApplicantProfile}, where the client always sends the full profile
     *  object (see ApplicantIntakeModal) and a present-but-null field is a deliberate clear - unlike
     *  {@link #merge}, which stays skip-null for the request-time overlay in AnalysisController, where
     *  the incoming {@code applicant} really is partial and must never clear the saved profile. */
    @SuppressWarnings("unchecked")
    private ApplicantProfile mergePresenceAware(ApplicantProfile base, Map<String, Object> overrides) {
        if (overrides == null) return base != null ? base : ApplicantProfile.builder().build();
        Map<String, Object> baseMap = new LinkedHashMap<>(objectMapper.convertValue(base, Map.class));
        baseMap.putAll(overrides);
        return objectMapper.convertValue(baseMap, ApplicantProfile.class);
    }

    /**
     * Field-by-field merge: any non-null field on {@code overrides} wins; otherwise the value from
     * {@code base} is kept. Used only at analyze time to fold a request-supplied {@code applicant}
     * (session-specific, one-off facts that may genuinely be partial) onto the user's saved profile -
     * skip-null is correct there since that request was never meant to clear saved answers. The
     * ask-once intake form's own save path uses {@link #mergePresenceAware} instead, which does
     * distinguish an explicit null from an absent field.
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
