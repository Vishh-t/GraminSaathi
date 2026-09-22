package com.graminsaathi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.model.Scheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SchemeDataServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static SchemeDataService service;

    @BeforeAll
    static void loadOnce() {
        service = new SchemeDataService(MAPPER);
        service.load();
    }

    @Test
    void loadsSchemesDeduplicatedById() {
        List<Scheme> all = service.getAllSchemes();

        // Raw file has 230 records but only 201 distinct scheme_ids
        assertEquals(201, all.size());
        assertEquals(all.size(), all.stream().map(Scheme::getSchemeId).distinct().count());
    }

    @Test
    void splitsCentralAndStateSchemes() {
        List<Scheme> all = service.getAllSchemes();

        List<Scheme> central = all.stream().filter(s -> "central".equals(s.getLevel())).toList();
        List<Scheme> state = all.stream().filter(s -> "state".equals(s.getLevel())).toList();

        assertEquals(51, central.size());
        assertEquals(150, state.size());
        assertTrue(central.stream().allMatch(s -> s.getState() == null));
        assertTrue(state.stream().allMatch(s -> s.getState() != null && !s.getState().isBlank()));
    }

    @Test
    void duplicateIdKeepsFirstOccurrence() {
        Scheme godhan = service.getById("state-cg-godhan-2020").orElseThrow();
        assertEquals("Godhan Nyay Yojana (Suraji Gaon Yojana)", godhan.getName());
    }

    @Test
    void getByIdReturnsEmptyForUnknownId() {
        assertTrue(service.getById("does-not-exist").isEmpty());
        assertTrue(service.getById(null).isEmpty());
    }

    @Test
    void pmegpFieldsDeserialiseCorrectly() {
        Scheme pmegp = service.getById("central-msme-pmegp-2024").orElseThrow();

        assertEquals("central", pmegp.getLevel());
        assertNull(pmegp.getState());
        assertEquals("pct_of_loan_capped", pmegp.getBenefit().path("calculation_type").asText());
        assertEquals(15, pmegp.getBenefit().path("subsidy_pct").asInt());
        assertTrue(pmegp.getEligibilityRules().has("and"));
        assertEquals(11.0, pmegp.getInterestRate().doubleValue(), 0.001); // percent, not fraction
        assertEquals(5_000_000L, pmegp.getLoanAmountRange().getMax().longValue());
        assertEquals(18, pmegp.getMinAge().intValue());
        assertFalse(pmegp.getDocumentsRequired().isEmpty());
        assertFalse(pmegp.getApplicationSteps().isEmpty());
    }

    @Test
    void everySchemeHasBenefitTypeAndRules() {
        for (Scheme s : service.getAllSchemes()) {
            assertNotNull(s.getBenefit(), s.getSchemeId() + " has no benefit");
            assertTrue(s.getBenefit().path("calculation_type").isTextual(),
                    s.getSchemeId() + " has no benefit.calculation_type");
            assertNotNull(s.getEligibilityRules(), s.getSchemeId() + " has no eligibility_rules");
            assertTrue(s.getEligibilityRules().isObject(), s.getSchemeId() + " eligibility_rules is not an object");
        }
    }

    @Test
    void compositeSchemesListTheirComponents() {
        long composites = 0;
        for (Scheme s : service.getAllSchemes()) {
            if ("composite".equals(s.getBenefit().path("calculation_type").asText())) {
                composites++;
                JsonNode components = s.getBenefit().path("calculation_components");
                // Note: 2 composites in the data (state-ap-ysr-cheyutha-2020, state-ml-prime-2020) list only 1 component
                assertTrue(components.isArray() && components.size() >= 1,
                        s.getSchemeId() + " is composite but has no calculation_components");
            }
        }
        assertTrue(composites > 0);
    }

    @Test
    void everyEnhancementHasACondition() {
        for (Scheme s : service.getAllSchemes()) {
            JsonNode enhancements = s.getBenefit().path("special_category_enhancement");
            if (!enhancements.isObject()) continue;
            enhancements.fields().forEachRemaining(e ->
                    assertTrue(e.getValue().has("condition"),
                            s.getSchemeId() + " enhancement '" + e.getKey() + "' has no condition"));
        }
    }

    /**
     * Contract check: every applicant.* variable referenced by any eligibility rule or enhancement
     * condition must exist on ApplicantProfile, otherwise that rule silently evaluates against null.
     */
    @Test
    void ruleVariablesAreAllCoveredByApplicantProfile() {
        Set<String> profileKeys = profileAsMap(new ApplicantProfile()).keySet();
        assertEquals(46, profileKeys.size());

        Set<String> used = new HashSet<>();
        for (Scheme s : service.getAllSchemes()) {
            collectVars(s.getEligibilityRules(), used);
            JsonNode enhancements = s.getBenefit().path("special_category_enhancement");
            if (enhancements.isObject()) {
                enhancements.elements().forEachRemaining(e -> collectVars(e.path("condition"), used));
            }
        }

        assertFalse(used.isEmpty());
        for (String var : used) {
            assertTrue(var.startsWith("applicant."), "Unexpected variable namespace: " + var);
            String key = var.substring("applicant.".length());
            assertTrue(profileKeys.contains(key), "ApplicantProfile has no field for rule variable: " + var);
        }
    }

    @Test
    void applicantProfileSerialisesToRuleVocabulary() {
        ApplicantProfile profile = ApplicantProfile.builder()
                .isSc(true)
                .annualFamilyIncome(120000.0)
                .ruralUrban("rural")
                .hasKaliaBskyCard(false)
                .build();

        Map<String, Object> map = profileAsMap(profile);

        assertEquals(true, map.get("is_sc"));
        assertEquals(120000.0, ((Number) map.get("annual_family_income")).doubleValue(), 0.001);
        assertEquals("rural", map.get("rural_urban"));
        assertEquals(false, map.get("has_kalia_bsky_card"));
        assertTrue(map.containsKey("is_woman"));
        assertNull(map.get("is_woman"));
    }

    @Test
    void applicantProfileDeserialisesFromSnakeCaseAndIgnoresUnknownFields() throws Exception {
        String json = "{\"age\":32,\"is_woman\":true,\"rural_urban\":\"rural\",\"state\":\"Bihar\",\"not_a_field\":1}";

        ApplicantProfile profile = MAPPER.readValue(json, ApplicantProfile.class);

        assertEquals(32, profile.getAge().intValue());
        assertEquals(Boolean.TRUE, profile.getIsWoman());
        assertEquals("rural", profile.getRuralUrban());
        assertEquals("Bihar", profile.getState());
        assertNull(profile.getIsSc());
    }

    private static Map<String, Object> profileAsMap(ApplicantProfile profile) {
        return MAPPER.convertValue(profile, new TypeReference<Map<String, Object>>() {});
    }

    private static void collectVars(JsonNode node, Set<String> out) {
        if (node == null) return;
        if (node.isObject() && node.has("var")) {
            JsonNode v = node.get("var");
            out.add(v.isArray() ? v.get(0).asText() : v.asText());
        }
        node.elements().forEachRemaining(child -> collectVars(child, out));
    }
}
