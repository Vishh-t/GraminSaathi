package com.graminsaathi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.model.Scheme;
import com.graminsaathi.service.BenefitCalculator.BenefitInput;
import com.graminsaathi.service.BenefitCalculator.BenefitResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Formulas from the handoff doc section 3.2, checked both in isolation and against real dataset schemes. */
class BenefitCalculatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final BenefitCalculator calculator = new BenefitCalculator();

    // ---- pct_of_cost_capped ----

    @Test
    void pctOfCostCappedComputesWhenCostIsKnown() {
        Scheme s = scheme(benefit("pct_of_cost_capped", 80.0, 500000.0, null));
        BenefitResult r = calculator.calculate(s, new BenefitInput(1000000.0, null));
        assertEquals("pct_of_cost_capped", r.calculationType());
        assertEquals(500000.0, r.amount()); // 80% of 10L = 8L, capped at 5L
    }

    @Test
    void pctOfCostCappedUncappedWhenNoMaxGiven() {
        Scheme s = scheme(benefit("pct_of_cost_capped", 40.0, null, null));
        BenefitResult r = calculator.calculate(s, new BenefitInput(1000000.0, null));
        assertEquals(400000.0, r.amount());
    }

    @Test
    void pctOfCostCappedFallsBackToTextWithoutACost() {
        Scheme s = scheme(benefit("pct_of_cost_capped", 80.0, 500000.0, null));
        BenefitResult r = calculator.calculate(s, BenefitInput.EMPTY);
        assertNull(r.amount());
        assertTrue(r.displayText().contains("80%"));
        assertTrue(r.displayText().contains("500,000") || r.displayText().contains("5,00,000") || r.displayText().contains("₹5,00,000") || r.displayText().contains("₹500,000"));
    }

    // ---- pct_of_loan_capped / uncapped ----

    @Test
    void pctOfLoanCappedComputesWhenLoanIsKnown() {
        Scheme s = scheme(benefit("pct_of_loan_capped", 10.0, 25000.0, null));
        BenefitResult r = calculator.calculate(s, new BenefitInput(null, 500000.0));
        assertEquals(25000.0, r.amount()); // 10% of 5L = 50k, capped at 25k
    }

    @Test
    void pctOfLoanUncappedHasNoCeiling() {
        Scheme s = benefitOnlyScheme("pct_of_loan_uncapped", 15.0, null);
        BenefitResult r = calculator.calculate(s, new BenefitInput(null, 200000.0));
        assertEquals(30000.0, r.amount());
        assertTrue(r.displayText().contains("no upper cap"));
    }

    // ---- interest_rate_reduction: the deviation from the handoff's literal formula ----

    @Test
    void interestRateReductionUsesTheStoredRateDirectlyWhenPresent() {
        // Annasaheb Patil shape: interest_rate=0, subsidy_pct=100 - a 0% loan, NOT "0 - 100"
        Scheme s = scheme(benefit("interest_rate_reduction", 100.0, null, null));
        s.setInterestRate(0.0);
        BenefitResult r = calculator.calculate(s, BenefitInput.EMPTY);
        assertEquals(0.0, r.effectiveInterestRatePct());
        assertTrue(r.displayText().contains("0%"));
    }

    @Test
    void interestRateReductionFallsBackToSubsidyPctWhenNoRateStored() {
        // Rajasthan MLUPY shape: no interest_rate on file, subsidy_pct is a genuine points-off figure
        Scheme s = scheme(benefit("interest_rate_reduction", 8.0, null, null));
        BenefitResult r = calculator.calculate(s, BenefitInput.EMPTY);
        assertNull(r.effectiveInterestRatePct());
        assertTrue(r.displayText().contains("8"));
        assertTrue(r.displayText().toLowerCase().contains("percentage-point"));
    }

    @Test
    void interestRateReductionPrefersTheNoteWhenNeitherRateNorPctAreNumeric() {
        Scheme s = scheme(benefit("interest_rate_reduction", null, null, null));
        s.setEffectiveInterestRateNote("A massive 3% per annum interest subvention is provided.");
        BenefitResult r = calculator.calculate(s, BenefitInput.EMPTY);
        assertNull(r.effectiveInterestRatePct());
        assertEquals("A massive 3% per annum interest subvention is provided.", r.displayText());
    }

    // ---- guarantee_cover_pct ----

    @Test
    void guaranteeCoverPctNeverProducesACashAmount() {
        Scheme s = scheme(benefit("guarantee_cover_pct", 85.0, 100000000.0, 100000000.0));
        BenefitResult r = calculator.calculate(s, new BenefitInput(1000000.0, 900000.0));
        assertNull(r.amount());
        assertEquals(85.0, r.guaranteeCoverPct());
        assertTrue(r.displayText().contains("85%"));
        assertTrue(r.displayText().contains("not counted as a cash benefit"));
    }

    // ---- per_unit_or_in_kind (documented as == pct_of_cost_capped for now) ----

    @Test
    void perUnitOrInKindMatchesPctOfCostCapped() {
        Scheme s = scheme(benefit("per_unit_or_in_kind", 100.0, 20000.0, null));
        BenefitResult r = calculator.calculate(s, new BenefitInput(50000.0, null));
        assertEquals("per_unit_or_in_kind", r.calculationType());
        assertEquals(20000.0, r.amount()); // 100% of 50k = 50k, capped at 20k
    }

    // ---- equity_or_uncapped ----

    @Test
    void equityOrUncappedShowsRangeAndNoteVerbatim() {
        Scheme s = benefitOnlyScheme("equity_or_uncapped", null, null);
        s.setEffectiveInterestRateNote("Financial assistance is provided as equity.");
        Scheme.LoanAmountRange range = new Scheme.LoanAmountRange();
        range.setMin(2000000L);
        range.setMax(150000000L);
        s.setLoanAmountRange(range);

        BenefitResult r = calculator.calculate(s, BenefitInput.EMPTY);
        assertNull(r.amount());
        assertTrue(r.displayText().contains("Financial assistance is provided as equity."));
    }

    // ---- text_only_see_note / unknown benefit ----

    @Test
    void nullBenefitFallsBackToEligibilityHuman() {
        Scheme s = new Scheme();
        s.setEligibilityHuman("Open to any rural applicant above 18.");
        BenefitResult r = calculator.calculate(s, BenefitInput.EMPTY);
        assertEquals("text_only_see_note", r.calculationType());
        assertEquals("Open to any rural applicant above 18.", r.displayText());
    }

    // ---- composite: shared fields across named component types, never summed ----

    @Test
    void compositeComputesEachSharedComponentSeparatelyAndNeverSums() {
        // PM Vishwakarma shape: one benefit block, two component type names
        Scheme s = scheme(benefitWithComponents(null, 15000.0, 300000.0, "pct_of_cost_capped", "pct_of_loan_uncapped"));
        BenefitResult r = calculator.calculate(s, new BenefitInput(15000.0, 300000.0));

        assertEquals("composite", r.calculationType());
        assertNull(r.amount(), "composite must never sum sub-results into one ₹ figure");
        assertEquals(2, r.components().size());

        BenefitResult grantPart = r.components().get(0);
        assertEquals("pct_of_cost_capped", grantPart.calculationType());
        // subsidy_pct is null on this shared block, so it falls back to the flat cap
        assertEquals(15000.0, grantPart.amount());

        BenefitResult loanPart = r.components().get(1);
        assertEquals("pct_of_loan_uncapped", loanPart.calculationType());
    }

    @Test
    void compositeWithASingleComponentStillWorks() {
        // YSR Cheyutha shape: composite with only one listed component
        Scheme s = scheme(benefitWithComponents(100.0, 75000.0, null, "pct_of_cost_capped"));
        BenefitResult r = calculator.calculate(s, new BenefitInput(75000.0, null));
        assertEquals(1, r.components().size());
        assertEquals(75000.0, r.components().get(0).amount());
    }

    // ---- helpers ----

    private static Scheme scheme(JsonNode benefit) {
        Scheme s = new Scheme();
        s.setBenefit(benefit);
        return s;
    }

    /** A scheme whose only relevant field is its benefit block - for cases the test doesn't need interestRate/notes on. */
    private static Scheme benefitOnlyScheme(String calculationType, Double subsidyPct, Double maxSubsidyAmount) {
        return scheme(benefit(calculationType, subsidyPct, maxSubsidyAmount, null));
    }

    private static JsonNode benefit(String calculationType, Double subsidyPct, Double maxSubsidyAmount, Double onLoanUpto) {
        // put(String, Double) writes a real JSON null (NullNode) for null values, matching the dataset;
        // putPOJO(..., null) would create a POJONode that asDouble() reads as 0.0.
        return MAPPER.createObjectNode()
                .put("calculation_type", calculationType)
                .put("subsidy_pct", subsidyPct)
                .put("max_subsidy_amount", maxSubsidyAmount)
                .put("on_loan_upto", onLoanUpto);
    }

    private static JsonNode benefitWithComponents(Double subsidyPct, Double maxSubsidyAmount, Double onLoanUpto, String... components) {
        var node = MAPPER.createObjectNode()
                .put("calculation_type", "composite")
                .put("subsidy_pct", subsidyPct)
                .put("max_subsidy_amount", maxSubsidyAmount)
                .put("on_loan_upto", onLoanUpto);
        var arr = node.putArray("calculation_components");
        for (String c : components) arr.add(c);
        return node;
    }
}
