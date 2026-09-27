package com.graminsaathi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.AnalyzeRequest;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.dto.response.SchemeComparisonResponse;
import com.graminsaathi.model.Scheme;
import com.graminsaathi.model.Village;
import com.graminsaathi.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinancialCalculatorServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private DemoDataLoader demoDataLoader;

    @Mock
    private FinancingSchemeService financingSchemeService;
    @Mock
    private VillageRepository villageRepository;

    private FinancialCalculatorService financialCalculatorService;

    @BeforeEach
    void setUp() {
        // Rewritten 2026-09-27 (see Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, "part 7") - the real
        // service now resolves the village's state via VillageRepository, not demoDataLoader.getVillage().
        // villageRepository.findFirstByNameNormalized(...) defaults to Optional.empty() when unstubbed
        // (Mockito's built-in default answer for Optional-returning methods), matching the old
        // demoDataLoader.getVillage(...) == null default - so most tests below need no village stub at all.
        financialCalculatorService = new FinancialCalculatorService(demoDataLoader, financingSchemeService, villageRepository);
    }

    // ---- scheme terms -> analysis result ----

    @Test
    void testSmallLoanCalculation() {
        // margin = 10,000 -> project_cost = 100,000 -> loan = 90,000
        stubDairy();
        Scheme scheme = createScheme("test-small", "Small Loan Scheme", 6.5, 3, 3);
        scheme.setEffectiveInterestRateNote("Test note");
        stubCandidates(scheme);

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertEquals(100000.0, result.projectCost(), 0.01);
        assertEquals(90000.0, result.loanAmount(), 0.01);
        assertEquals("Small Loan Scheme", result.schemeName());
        assertEquals(0.065, result.interestRateAnnual(), 0.0001); // dataset percent -> fraction
        assertEquals(0.065 / 12, result.monthlyRate(), 1e-9);
        assertEquals(3, result.tenureYears());
        assertEquals(3, result.moratoriumMonths());
        assertEquals(33, result.repaymentMonths());

        // Interest accrues through the 3-month moratorium and is capitalised before the 33 instalments start.
        double monthlyRate = 0.065 / 12;
        double principal = 90000 * Math.pow(1 + monthlyRate, 3);
        double expectedEmi = principal * monthlyRate * Math.pow(1 + monthlyRate, 33) / (Math.pow(1 + monthlyRate, 33) - 1);
        assertEquals(expectedEmi, result.emi(), 0.01);

        assertEquals(94500.0, result.workingCapitalEstimate(), 0.01); // 31500 * 3
        assertEquals(100000.0, result.recommendedProjectCost(), 0.01); // min(100000, 1000000)
        assertEquals(90000.0, result.recommendedLoanAmount(), 0.01);
        assertEquals(0.0, result.bufferAmount(), 0.01);

        // The comparison row mirrors the primary result (rate reported back in percent).
        SchemeComparisonResponse row = result.schemeComparison().get(0);
        assertEquals(1, result.schemeComparison().size());
        assertEquals("Small Loan Scheme", row.getSchemeName());
        assertEquals(6.5, row.getInterestRate(), 0.0001);
        assertEquals(3, row.getTenureYears());
        assertEquals(3, row.getMoratoriumMonths());
        assertEquals(Math.round(result.emi() * 100) / 100.0, row.getEmi(), 0.001);
        assertEquals("Test Agency", row.getAgency());
        assertEquals("Test note", row.getSubsidyNote());

        // Money columns: instalments only, no subsidy, rate not estimated, EMI ~3k vs 13,500 net income -> affordable.
        assertEquals(result.emi() * 33, row.getTotalRepayment(), 0.5);
        assertEquals(row.getTotalRepayment(), row.getNetCost(), 0.001);
        assertNull(row.getEstimatedSubsidy());
        assertEquals(Boolean.FALSE, row.getRateEstimated());
        assertEquals(Boolean.TRUE, row.getAffordable());
    }

    @Test
    void testLargeLoanCalculation() {
        // margin = 500,000 -> project_cost = 5,000,000 -> loan = 4,500,000
        stubDairy();
        stubCandidates(createScheme("test-large", "Large Loan Scheme", 8, 7, 6));

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 500000.0));

        assertEquals(5000000.0, result.projectCost(), 0.01);
        assertEquals(4500000.0, result.loanAmount(), 0.01);
        assertEquals("Large Loan Scheme", result.schemeName());
        assertEquals(0.08, result.interestRateAnnual(), 0.0001);
        assertEquals(7, result.tenureYears());
        assertEquals(6, result.moratoriumMonths());
        assertEquals(78, result.repaymentMonths()); // 7*12 - 6

        assertEquals(1000000.0, result.recommendedProjectCost(), 0.01); // min(5000000, 1000000)
        assertEquals(900000.0, result.recommendedLoanAmount(), 0.01);
        assertEquals(3600000.0, result.bufferAmount(), 0.01); // 4500000 - 900000
    }

    // ---- wiring to FinancingSchemeService ----

    @Test
    void testResolvedApplicantAndLoanAmountPassedToSchemeLookup() {
        stubDairy();
        stubVillage("Maharashtra");
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory("Dairy");

        // The client-supplied applicant details go to resolveApplicant, and only the applicant it returns
        // is used for the lookup. Candidates are only stubbed for that exact object, so any mix-up fails.
        ApplicantProfile provided = ApplicantProfile.builder().isSc(true).build();
        ApplicantProfile resolved = ApplicantProfile.builder().state("Maharashtra").isSc(true).build();
        when(financingSchemeService.resolveApplicant("Maharashtra", category, provided)).thenReturn(resolved);
        when(financingSchemeService.findFinancingCandidates(anyDouble(), same(resolved)))
                .thenReturn(List.of(createScheme("s-1", "Wired Scheme", 6, 3, 0)));

        AnalyzeRequest request = request("Dairy", 10000.0);
        request.setApplicant(provided);
        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request);

        assertEquals("Wired Scheme", result.schemeName());
        ArgumentCaptor<Double> loan = ArgumentCaptor.forClass(Double.class);
        verify(financingSchemeService).findFinancingCandidates(loan.capture(), same(resolved));
        assertEquals(90000.0, loan.getValue(), 0.01); // 90% of the project cost, not the margin
    }

    @Test
    void testNoApplicantDetailsResolvesFromVillageAndCategoryOnly() {
        stubDairy();
        stubVillage("Maharashtra");
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory("Dairy");

        ApplicantProfile baseline = ApplicantProfile.builder().state("Maharashtra").build();
        when(financingSchemeService.resolveApplicant("Maharashtra", category, null)).thenReturn(baseline);
        when(financingSchemeService.findFinancingCandidates(anyDouble(), same(baseline)))
                .thenReturn(List.of(createScheme("s-1", "Baseline Scheme", 6, 3, 0)));

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertEquals("Baseline Scheme", result.schemeName());
    }

    @Test
    void testNoSchemeCoversLoanNamesTheState() {
        stubDairy();
        stubVillage("Maharashtra");
        when(financingSchemeService.findFinancingCandidates(anyDouble(), any())).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> financialCalculatorService.calculate(request("Dairy", 600000.0)));

        assertTrue(ex.getMessage().contains("No government financing scheme"));
        assertTrue(ex.getMessage().contains("Maharashtra"));
    }

    @Test
    void testNoSchemeCoversLoanUnknownVillage() {
        stubDairy();
        // Unstubbed villageRepository already defaults to Optional.empty() (see setUp() comment) - this
        // explicit stub is kept only to make the "unknown village" intent obvious at the call site.
        when(villageRepository.findFirstByNameNormalized(VillageService.normalize("Ghoti"))).thenReturn(Optional.empty());
        when(financingSchemeService.findFinancingCandidates(anyDouble(), any())).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> financialCalculatorService.calculate(request("Dairy", 600000.0)));

        assertTrue(ex.getMessage().contains("this location"));
    }

    @Test
    void testInvalidCategoryThrows() {
        when(demoDataLoader.getBusinessCategory("Nope")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> financialCalculatorService.calculate(request("Nope", 10000.0)));
    }

    // ---- ranking ----

    @Test
    void testRankedByTotalRepaymentNotByRate() {
        stubDairy();
        // Given in a scrambled order. By total repayment on 90,000: free (90,000) < 10% for 1y (~95k) < 8% for 7y (~118k).
        // A rate-only ranking would wrongly put the 7-year 8% loan ahead of the 1-year 10% one.
        Scheme longCheapRate = createScheme("s-long", "Long Tenure 8%", 8, 7, 0);
        Scheme shortHigherRate = createScheme("s-short", "Short Tenure 10%", 10, 1, 0);
        Scheme free = createScheme("s-free", "Interest Free", 0, 3, 0);
        stubCandidates(longCheapRate, shortHigherRate, free);

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertEquals("Interest Free", result.schemeName());
        assertEquals(90000.0 / 36, result.emi(), 0.01); // zero-rate loan: principal / months

        List<SchemeComparisonResponse> rows = result.schemeComparison();
        assertEquals(List.of("Interest Free", "Short Tenure 10%", "Long Tenure 8%"),
                rows.stream().map(SchemeComparisonResponse::getSchemeName).toList());

        // Only the winner is flagged primary.
        assertTrue(rows.get(0).isPrimary());
        assertFalse(rows.get(1).isPrimary());
        assertFalse(rows.get(2).isPrimary());
    }

    @Test
    void testTieBrokenByEmiThenSchemeId() {
        stubDairy();
        // Same total repayment (0% -> 90,000 either way) but different EMI: lower EMI (longer tenure) wins.
        Scheme highEmi = createScheme("a-high-emi", "High EMI", 0, 2, 0);
        Scheme lowEmi = createScheme("z-low-emi", "Low EMI", 0, 3, 0);
        // Identical terms: lower scheme_id wins.
        Scheme twinB = createScheme("b-twin", "Twin B", 7, 3, 0);
        Scheme twinA = createScheme("a-twin", "Twin A", 7, 3, 0);
        stubCandidates(highEmi, lowEmi, twinB, twinA);

        List<SchemeComparisonResponse> rows =
                financialCalculatorService.calculate(request("Dairy", 10000.0)).schemeComparison();

        assertEquals("Low EMI", rows.get(0).getSchemeName());
        assertEquals("High EMI", rows.get(1).getSchemeName());
        assertEquals("Twin A", rows.get(2).getSchemeName());
        assertEquals("Twin B", rows.get(3).getSchemeName());
    }

    @Test
    void testComparisonCappedAtMaxRows() {
        stubDairy();
        List<Scheme> many = new ArrayList<>();
        for (int i = 0; i < FinancialCalculatorService.MAX_COMPARISON_ROWS + 2; i++) {
            many.add(createScheme("s-" + i, "Scheme " + i, 5 + i, 3, 0));
        }
        stubCandidates(many.toArray(new Scheme[0]));

        List<SchemeComparisonResponse> rows =
                financialCalculatorService.calculate(request("Dairy", 10000.0)).schemeComparison();

        assertEquals(FinancialCalculatorService.MAX_COMPARISON_ROWS, rows.size());
        assertEquals("Scheme 0", rows.get(0).getSchemeName()); // lowest rate -> cheapest
        assertEquals("Scheme " + (FinancialCalculatorService.MAX_COMPARISON_ROWS - 1),
                rows.get(FinancialCalculatorService.MAX_COMPARISON_ROWS - 1).getSchemeName());
    }

    // ---- affordability ----

    @Test
    void testAffordableOptionBeatsCheaperButUnaffordableOne() {
        stubDairy(); // net operating income 13,500 a month
        // Cheapest on paper (0%, 90,000 in total) but six instalments of 15,000 exceed what the business earns.
        Scheme tight = createScheme("s-tight", "Cheap But Tight", 0, 0.5, 0);
        Scheme steady = createScheme("s-steady", "Steady", 6, 3, 0);
        stubCandidates(tight, steady);

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertEquals("Steady", result.schemeName());
        List<SchemeComparisonResponse> rows = result.schemeComparison();
        assertEquals("Cheap But Tight", rows.get(1).getSchemeName());
        assertEquals(Boolean.TRUE, rows.get(0).getAffordable());
        assertEquals(Boolean.FALSE, rows.get(1).getAffordable());
        assertTrue(rows.get(1).getTotalRepayment() < rows.get(0).getTotalRepayment()); // it really is the cheaper one
    }

    @Test
    void testWhenNothingIsAffordableTheCheapestStillComesFirst() {
        stubDairy();
        // margin 500,000 -> loan 4,500,000: every EMI is far above 13,500 a month.
        stubCandidates(
                createScheme("s-dear", "Dearer", 10, 7, 0),
                createScheme("s-cheap", "Cheaper", 8, 7, 0));

        List<SchemeComparisonResponse> rows =
                financialCalculatorService.calculate(request("Dairy", 500000.0)).schemeComparison();

        assertEquals("Cheaper", rows.get(0).getSchemeName());
        assertEquals(Boolean.FALSE, rows.get(0).getAffordable());
        assertEquals(Boolean.FALSE, rows.get(1).getAffordable());
    }

    // ---- subsidy in the net cost ----

    private static final String LOAN_PCT_SUBSIDY = "{\"calculation_type\":\"pct_of_loan_capped\",\"subsidy_pct\":15,\"max_subsidy_amount\":%s}";

    @Test
    void testSubsidyCanMakeAHigherRateLoanTheCheapest() {
        stubDairy();
        // On 90,000 over 5 years: 9% repays ~112k, 11% repays ~117k. The 15% subsidy (13,500) puts 11% at ~104k net.
        Scheme plain = createScheme("s-plain", "Plain 9%", 9, 5, 0);
        Scheme subsidised = createScheme("s-sub", "Subsidised 11%", 11, 5, 0);
        subsidised.setBenefit(json(String.format(LOAN_PCT_SUBSIDY, "1750000")));
        stubCandidates(plain, subsidised);

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertEquals("Subsidised 11%", result.schemeName());
        SchemeComparisonResponse winner = result.schemeComparison().get(0);
        assertEquals(13500.0, winner.getEstimatedSubsidy(), 0.01);
        assertEquals(winner.getTotalRepayment() - 13500.0, winner.getNetCost(), 0.01);
        assertNull(result.schemeComparison().get(1).getEstimatedSubsidy());
        // The subsidy never lowers the EMI: it is worked out on the full loan.
        assertTrue(winner.getEmi() > result.schemeComparison().get(1).getEmi());
    }

    @Test
    void testSubsidyIsCappedAndAZeroCapCountsNothing() {
        stubDairy();
        Scheme plain = createScheme("s-plain", "Plain 9%", 9, 5, 0);
        Scheme smallCap = createScheme("s-cap", "Capped At 1,000", 11, 5, 0);
        smallCap.setBenefit(json(String.format(LOAN_PCT_SUBSIDY, "1000")));
        Scheme zeroCap = createScheme("s-zero", "Zero Cap", 11, 5, 0);
        zeroCap.setBenefit(json(String.format(LOAN_PCT_SUBSIDY, "0")));
        stubCandidates(smallCap, zeroCap, plain);

        List<SchemeComparisonResponse> rows =
                financialCalculatorService.calculate(request("Dairy", 10000.0)).schemeComparison();

        assertEquals(List.of("Plain 9%", "Capped At 1,000", "Zero Cap"),
                rows.stream().map(SchemeComparisonResponse::getSchemeName).toList());
        assertEquals(1000.0, rows.get(1).getEstimatedSubsidy(), 0.01);
        assertNull(rows.get(2).getEstimatedSubsidy());
    }

    @Test
    void testUncappedSubsidyUsesThePercentage() {
        stubDairy();
        Scheme uncapped = createScheme("s-uncapped", "Uncapped", 9, 5, 0);
        uncapped.setBenefit(json(String.format(LOAN_PCT_SUBSIDY, "null")));
        stubCandidates(uncapped);

        SchemeComparisonResponse row =
                financialCalculatorService.calculate(request("Dairy", 10000.0)).schemeComparison().get(0);

        assertEquals(13500.0, row.getEstimatedSubsidy(), 0.01);
    }

    @Test
    void testOnlyPercentOfLoanSubsidiesAreCounted() {
        stubDairy();
        // For other benefit types "subsidy_pct" means something else (e.g. interest subvention) - never netted.
        Scheme other = createScheme("s-other", "Interest Subvention Type", 9, 5, 0);
        other.setBenefit(json("{\"calculation_type\":\"interest_rate_reduction\",\"subsidy_pct\":50,\"max_subsidy_amount\":500000}"));
        stubCandidates(other);

        SchemeComparisonResponse row =
                financialCalculatorService.calculate(request("Dairy", 10000.0)).schemeComparison().get(0);

        assertNull(row.getEstimatedSubsidy());
        assertEquals(row.getTotalRepayment(), row.getNetCost(), 0.001);
    }

    @Test
    void testEstimatedRateFlagIsPassedThrough() {
        stubDairy();
        Scheme estimated = createScheme("s-est", "Estimated Rate", 5.5, 5, 0);
        estimated.setRateEstimated(true);
        Scheme stated = createScheme("s-stated", "Stated Rate", 9, 5, 0);
        stubCandidates(estimated, stated);

        List<SchemeComparisonResponse> rows =
                financialCalculatorService.calculate(request("Dairy", 10000.0)).schemeComparison();

        assertEquals(Boolean.TRUE, rows.get(0).getRateEstimated()); // 5.5% is cheaper, so it ranks first
        assertEquals(Boolean.FALSE, rows.get(1).getRateEstimated());
    }

    // ---- unusable scheme terms ----

    @Test
    void testMoratoriumSwallowingTenureIsDropped() {
        stubDairy();
        // 1-year tenure with a 12-month moratorium leaves nothing to repay in instalments.
        // It would otherwise win: 0% interest.
        stubCandidates(
                createScheme("s-bad", "Grace Only Scheme", 0, 1, 12),
                createScheme("s-good", "Normal Scheme", 9, 3, 0));

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertEquals("Normal Scheme", result.schemeName());
        assertEquals(1, result.schemeComparison().size());
    }

    @Test
    void testOnlyUnrepayableSchemesThrows() {
        stubDairy();
        stubCandidates(createScheme("s-bad", "Grace Only Scheme", 0, 1, 12));

        assertThrows(IllegalArgumentException.class,
                () -> financialCalculatorService.calculate(request("Dairy", 10000.0)));
    }

    @Test
    void testMoratoriumInterestIsCapitalised() {
        stubDairy();
        stubCandidates(createScheme("s-mor", "With Moratorium", 12, 3, 6));

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        // 90,000 at 12% for 3 years with a 6-month moratorium: 30 instalments on the grown principal.
        double r = 0.12 / 12;
        double grown = 90000 * Math.pow(1 + r, 6);
        double expectedEmi = grown * r * Math.pow(1 + r, 30) / (Math.pow(1 + r, 30) - 1);
        assertEquals(30, result.repaymentMonths());
        assertEquals(expectedEmi, result.emi(), 0.01);

        // ...which is more than the same loan would cost without the moratorium's accrued interest.
        double plainEmi = 90000 * r * Math.pow(1 + r, 30) / (Math.pow(1 + r, 30) - 1);
        assertTrue(result.emi() > plainEmi);
    }

    @Test
    void testMoratoriumAddsNothingOnInterestFreeLoan() {
        stubDairy();
        stubCandidates(createScheme("s-free", "Free With Moratorium", 0, 3, 6));

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertEquals(90000.0 / 30, result.emi(), 0.01); // nothing accrues at 0%
    }

    @Test
    void testFractionalTenureUsesExactMonths() {
        stubDairy();
        // 2.5 years = 30 months, e.g. PM Vishwakarma's tenure in the dataset.
        stubCandidates(createScheme("s-frac", "Two And A Half Years", 5, 2.5, 0));

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertEquals(30, result.repaymentMonths());
        assertEquals(3, result.tenureYears()); // display figure only, rounded
    }

    @Test
    void testMissingMoratoriumTreatedAsZero() {
        stubDairy();
        stubCandidates(createScheme("s-none", "No Moratorium Scheme", 6, 2, null));

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertEquals(0, result.moratoriumMonths());
        assertEquals(24, result.repaymentMonths());
        assertEquals(0, result.schemeComparison().get(0).getMoratoriumMonths());
    }

    // ---- working capital warning ----

    @Test
    void testWorkingCapitalWarning() {
        stubDairy();
        stubCandidates(createScheme("test-small", "Small Loan Scheme", 6.5, 3, 3));

        // Margin = 10,000 -> project_cost = 100,000, margin_required = 10,000, remaining = 0 < working_capital 94,500
        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertNotNull(result.workingCapitalWarning());
        assertTrue(result.workingCapitalWarning().contains("no separate buffer"));
        assertTrue(result.workingCapitalWarning().contains("94500"));
    }

    @Test
    void testNoWorkingCapitalWarningWhenSufficientMargin() {
        stubDairy();
        stubCandidates(createScheme("test-small", "Small Loan Scheme", 6.5, 3, 3));

        // margin 194,500 -> project_cost 1,945,000, recommended capped at 1,000,000,
        // margin_required = 100,000, remaining = 94,500 = working_capital -> no warning
        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 194500.0));

        assertNull(result.workingCapitalWarning());
    }

    @Test
    void testWorkingCapitalWarningJustBelowThreshold() {
        stubDairy();
        stubCandidates(createScheme("test-small", "Small Loan Scheme", 6.5, 3, 3));

        // One rupee short of the 194,500 boundary above.
        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 194499.0));

        assertNotNull(result.workingCapitalWarning());
    }

    // ---- local price intelligence ----

    @Test
    void testLocalPriceIntelligence() {
        stubDairy();
        stubCandidates(createScheme("test-small", "Small Loan Scheme", 6.5, 3, 3));

        FinancialCalculatorService.FinancialResult result = financialCalculatorService.calculate(request("Dairy", 10000.0));

        assertEquals(42.0, result.localAveragePrice(), 0.01);
        assertEquals(39.9, result.recommendedPriceLow(), 0.01); // 42 * 0.95
        assertEquals(44.1, result.recommendedPriceHigh(), 0.01); // 42 * 1.05
        assertEquals(40.74, result.recommendedLaunchPrice(), 0.01); // 42 * 0.97

        // Breakeven: (31500 + emi) / (45000/42)
        double expectedBreakeven = (31500 + result.emi()) / (45000.0 / 42.0);
        assertEquals(expectedBreakeven, result.breakevenPrice(), 0.01);
    }

    @Test
    void testRetailCategoryNoLocalPrice() {
        DemoData.BusinessCategoryData category = createMockCategory("Retail / Kirana Store", 500000, 24500, 35000, 2);
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(14);
        businessData.setAvgLocalPrice(null);

        when(demoDataLoader.getBusinessCategory("Retail / Kirana Store")).thenReturn(category);
        when(demoDataLoader.getBusinessData("Ghoti", "Retail / Kirana Store")).thenReturn(businessData);
        stubCandidates(createScheme("test-small", "Small Loan Scheme", 6.5, 3, 3));

        FinancialCalculatorService.FinancialResult result =
                financialCalculatorService.calculate(request("Retail / Kirana Store", 10000.0));

        assertEquals(0.0, result.localAveragePrice());
        assertNull(result.recommendedPriceLow());
        assertNull(result.recommendedPriceHigh());
        assertNull(result.recommendedLaunchPrice());
        assertNull(result.breakevenPrice());
        assertEquals("Not applicable — mixed product pricing.", result.breakevenNote());
    }

    // ---- helpers ----

    /** Dairy category + Ghoti price data. resolveApplicant(...) stays unstubbed (null on the mock) unless a test needs it. */
    private void stubDairy() {
        DemoData.BusinessCategoryData category = createMockCategory("Dairy", 1000000, 31500, 45000, 3);
        DemoData.BusinessData businessData = new DemoData.BusinessData();
        businessData.setCompetitorCount(2);
        businessData.setAvgLocalPrice(42.0);

        when(demoDataLoader.getBusinessCategory("Dairy")).thenReturn(category);
        when(demoDataLoader.getBusinessData("Ghoti", "Dairy")).thenReturn(businessData);
    }

    /** Stubs villageRepository so the real service resolves "Ghoti" to the given state. */
    private Village stubVillage(String state) {
        Village village = Village.builder().name("Ghoti").state(state).build();
        when(villageRepository.findFirstByNameNormalized(VillageService.normalize("Ghoti"))).thenReturn(Optional.of(village));
        return village;
    }

    private void stubCandidates(Scheme... schemes) {
        when(financingSchemeService.findFinancingCandidates(anyDouble(), any())).thenReturn(List.of(schemes));
    }

    private AnalyzeRequest request(String category, double margin) {
        AnalyzeRequest request = new AnalyzeRequest();
        request.setVillageName("Ghoti");
        request.setBusinessCategory(category);
        request.setAvailableMarginCapital(margin);
        return request;
    }

    private DemoData.BusinessCategoryData createMockCategory(String name, double refProjectCost, double refMonthlyOpCost, double refMonthlyRevenue, int workingCapitalMonths) {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName(name);
        category.setReferenceProjectCost(refProjectCost);
        category.setReferenceMonthlyOperatingCost(refMonthlyOpCost);
        category.setReferenceMonthlyRevenue(refMonthlyRevenue);
        category.setWorkingCapitalMonths(workingCapitalMonths);
        category.setSecondaryOpportunity("Test opportunity");
        category.setSourceNote("Test source");
        return category;
    }

    private static JsonNode json(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** interestRate is in PERCENT (6.5 = 6.5%), matching the real dataset. */
    private Scheme createScheme(String id, String name, double interestRatePercent, double tenureYears, Integer moratoriumMonths) {
        Scheme scheme = new Scheme();
        scheme.setSchemeId(id);
        scheme.setName(name);
        scheme.setInterestRate(interestRatePercent);
        scheme.setTenureYears(tenureYears);
        scheme.setMoratoriumMonths(moratoriumMonths);
        scheme.setImplementingAgency("Test Agency");
        return scheme;
    }
}
