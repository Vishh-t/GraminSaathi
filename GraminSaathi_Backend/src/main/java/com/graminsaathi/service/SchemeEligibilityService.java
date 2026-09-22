package com.graminsaathi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.model.Scheme;
import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Evaluates the JsonLogic rules stored in the schemes dataset (eligibility rules and, later,
 * special-category enhancement conditions) against an {@link ApplicantProfile}.
 *
 * <p>Rules read {@code applicant.<field>} variables. Anything the profile leaves {@code null}
 * makes the comparison fail (json-logic-java treats null as "no match"), so a scheme only counts
 * as eligible when every fact it needs has actually been provided.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemeEligibilityService {

    private final ObjectMapper objectMapper;
    private final JsonLogic jsonLogic = new JsonLogic();

    public boolean isEligible(Scheme scheme, ApplicantProfile applicant) {
        return matches(scheme.getEligibilityRules(), applicant);
    }

    /** Evaluates any JsonLogic node (a bare {@code true} passes). A rule that cannot be evaluated counts as no match. */
    public boolean matches(JsonNode rule, ApplicantProfile applicant) {
        if (rule == null || rule.isNull()) {
            return true;
        }
        Map<String, Object> data = Map.of("applicant", toMap(applicant != null ? applicant : new ApplicantProfile()));
        try {
            return JsonLogic.truthy(jsonLogic.apply(rule.toString(), data));
        } catch (JsonLogicException e) {
            log.warn("Could not evaluate rule {}: {}", rule, e.getMessage());
            return false;
        }
    }

    private Map<String, Object> toMap(ApplicantProfile applicant) {
        return objectMapper.convertValue(applicant, new TypeReference<Map<String, Object>>() {});
    }
}
