package com.graminsaathi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.model.Scheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Estimated interest rates for bank loans whose rate the dataset leaves open (see scheme_rate_overrides.json). */
class SchemeRateOverridesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static SchemeDataService service;

    @BeforeAll
    static void loadOnce() {
        service = new SchemeDataService(MAPPER);
        service.load();
    }

    // ---- the estimate formula ----

    @Test
    void bankRateMinusSubventionOverWholeTenure() {
        assertEquals(7.5, SchemeDataService.estimateRate(scheme(7.0), override(3.0, null, null, null)), 0.001);
    }

    @Test
    void noSubventionGivesTheAssumedBankRate() {
        assertEquals(SchemeDataService.ASSUMED_BANK_RATE_PERCENT,
                SchemeDataService.estimateRate(scheme(5.0), override(null, null, null, null)), 0.001);
    }

    @Test
    void schemeSpecificBankRateReplacesTheDefault() {
        assertEquals(11.5, SchemeDataService.estimateRate(scheme(7.0), override(null, null, null, 11.5)), 0.001);
        assertEquals(7.0, SchemeDataService.estimateRate(scheme(7.0), override(5.0, null, null, 12.0)), 0.001);
    }

    @Test
    void subventionLastingPartOfTheTenureIsAveraged() {
        // 6 points off for 2 of 4 years = 3 points off on average: 10.5 - 3 = 7.5
        assertEquals(7.5, SchemeDataService.estimateRate(scheme(4.0), override(6.0, 2.0, null, null)), 0.001);
    }

    @Test
    void subventionLongerThanTheTenureCountsInFull() {
        assertEquals(5.5, SchemeDataService.estimateRate(scheme(3.0), override(5.0, 7.0, null, null)), 0.001);
    }

    @Test
    void rateNeverGoesBelowZero() {
        assertEquals(0.0, SchemeDataService.estimateRate(scheme(5.0), override(20.0, null, null, null)), 0.001);
    }

    @Test
    void lendingRateCapApplies() {
        // 10.5 - 1 = 9.5, but the scheme promises never more than 9%
        assertEquals(9.0, SchemeDataService.estimateRate(scheme(5.0), override(1.0, null, 9.0, null)), 0.001);
        // cap above the result changes nothing
        assertEquals(7.5, SchemeDataService.estimateRate(scheme(5.0), override(3.0, null, 9.0, null)), 0.001);
    }

    // ---- what the loader did to the real dataset ----

    @Test
    void schemesWithTheirOwnRateAreUntouched() {
        Scheme pmegp = service.getById("central-msme-pmegp-2024").orElseThrow();
        Scheme kishor = service.getById("central-mof-mudra-kishor-2015").orElseThrow();

        assertEquals(11.0, pmegp.getInterestRate(), 0.001);
        assertEquals(9.5, kishor.getInterestRate(), 0.001);
        assertNull(pmegp.getRateEstimated());
        assertNull(kishor.getRateEstimated());
    }

    @Test
    void subventionLoansGetAnEstimatedRate() {
        assertEstimated("state-mp-birsa-munda-2022", 5.5);   // 10.5 - 5 (7y subvention over a 7y tenure)
        assertEstimated("state-gj-dtaisy-2014", 3.5);        // 10.5 - 7
        assertEstimated("state-mp-tantya-mama-2022", 3.5);   // 10.5 - 7
        assertEstimated("central-agri-aif-2020", 7.5);       // 10.5 - 3
        assertEstimated("state-hp-mmsy-2018", 7.5);          // 5 points for 3 of 5 years = 3 off
        assertEstimated("state-hr-heep-2020", 6.93);         // 5 points for 5 of 7 years = 3.57 off
        assertEstimated("central-fish-fidf-2018", 7.5);      // 10.5 - 3, under the 9% cap
    }

    @Test
    void plainBankLoansGetTheAssumedRate() {
        assertEstimated("central-mof-standup-2016", 11.5);   // MCLR + 3%, own base rate
        assertEstimated("state-up-gopalak-2019", 10.5);
        assertEstimated("central-ncdc-yuva-sahakar-2018", 10.5);
    }

    @Test
    void everyEstimatedSchemeIsARealLoanWithSaneTerms() {
        long estimated = service.getAllSchemes().stream().filter(s -> Boolean.TRUE.equals(s.getRateEstimated())).count();

        // one per entry in scheme_rate_overrides.json - a typo'd scheme_id would show up as a lower count
        assertEquals(23, estimated);
        for (Scheme s : service.getAllSchemes()) {
            if (Boolean.TRUE.equals(s.getRateEstimated())) {
                assertNotNull(s.getInterestRate(), s.getSchemeId());
                assertTrue(s.getInterestRate() >= 0 && s.getInterestRate() <= SchemeDataService.ASSUMED_BANK_RATE_PERCENT + 1.0,
                        s.getSchemeId() + " has an implausible estimated rate " + s.getInterestRate());
                assertNotNull(s.getTenureYears(), s.getSchemeId());
                assertTrue(s.getTenureYears() >= 1, s.getSchemeId());
            }
        }
    }

    @Test
    void grantsWithoutARateStayWithoutOne() {
        // A pure grant is not a loan; the loader must not invent a rate for it.
        Scheme grant = service.getById("central-tribal-pmvdy-2019").orElseThrow();

        assertNull(grant.getInterestRate());
        assertNull(grant.getRateEstimated());
    }

    // ---- helpers ----

    private static void assertEstimated(String schemeId, double expectedRate) {
        Scheme s = service.getById(schemeId).orElseThrow(() -> new AssertionError("missing " + schemeId));
        assertEquals(Boolean.TRUE, s.getRateEstimated(), schemeId);
        assertEquals(expectedRate, s.getInterestRate(), 0.001, schemeId);
    }

    private static Scheme scheme(double tenureYears) {
        Scheme s = new Scheme();
        s.setTenureYears(tenureYears);
        return s;
    }

    private static SchemeDataService.RateOverride override(Double subvention, Double years, Double cap, Double bankRate) {
        SchemeDataService.RateOverride o = new SchemeDataService.RateOverride();
        o.setInterestSubventionPct(subvention);
        o.setSubventionYears(years);
        o.setMaxLendingRatePct(cap);
        o.setAssumedBankRatePct(bankRate);
        return o;
    }
}
