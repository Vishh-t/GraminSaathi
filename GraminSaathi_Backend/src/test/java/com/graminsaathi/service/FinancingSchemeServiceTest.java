package com.graminsaathi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.data.DemoData;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.model.Scheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against the real schemes dataset, so these also guard the data file and the eligibility rules. */
class FinancingSchemeServiceTest {

    private static final String MUDRA_KISHOR = "central-mof-mudra-kishor-2015";
    private static final String PMEGP = "central-msme-pmegp-2024";
    /** Maharashtra only; needs category maratha_ebc; lends 10-50 lakh at 0%. */
    private static final String ANNASAHEB_PATIL = "state-mh-annasaheb-patil-2019";
    /** Central; needs is_sc and family income up to 3 lakh. */
    private static final String NSFDC_TERM = "central-sje-nsfdc-term-1989";
    /** Central; needs is_artisan; tenure is 2.5 years. */
    private static final String PM_VISHWAKARMA = "central-msde-pmvishwakarma-2023";
    /** State scheme with a 3-month tenure - not a term loan. */
    private static final String KA_BADAVARA_BANDHU = "state-ka-badavara-bandhu-2018";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static FinancingSchemeService service;

    @BeforeAll
    static void setUp() {
        SchemeDataService data = new SchemeDataService(MAPPER);
        data.load();
        service = new FinancingSchemeService(data, new SchemeEligibilityService(MAPPER), MAPPER);
    }

    // ---- resolveApplicant ----

    @Test
    void noProvidedApplicantGivesTheBaseline() {
        ApplicantProfile resolved = service.resolveApplicant(village("Maharashtra"), category("manufacturing", null), null);

        assertEquals(FinancingSchemeService.BASELINE_AGE, resolved.getAge());
        assertEquals("rural", resolved.getRuralUrban());
        assertEquals("greenfield", resolved.getBusinessStage());
        assertEquals("Maharashtra", resolved.getState());
        assertEquals("manufacturing", resolved.getSector());
        assertNull(resolved.getSubSector());
        assertNull(resolved.getIsSc());
    }

    @Test
    void providedFactsOverrideBaselineDefaultsAndKeepTheRest() {
        ApplicantProfile provided = ApplicantProfile.builder()
                .age(45)
                .businessStage("existing")
                .isSc(true)
                .category("sc")
                .annualFamilyIncome(250000.0)
                .build();

        ApplicantProfile resolved = service.resolveApplicant(village("Maharashtra"), category("manufacturing", null), provided);

        assertEquals(45, resolved.getAge());
        assertEquals("existing", resolved.getBusinessStage());
        assertEquals(Boolean.TRUE, resolved.getIsSc());
        assertEquals("sc", resolved.getCategory());
        assertEquals(250000.0, resolved.getAnnualFamilyIncome());
        assertEquals("rural", resolved.getRuralUrban()); // not provided -> baseline default stays
    }

    @Test
    void businessFactsComeFromVillageAndCategoryNotFromTheApplicant() {
        ApplicantProfile provided = ApplicantProfile.builder()
                .state("Kerala")
                .sector("services")
                .subSector("handicrafts")
                .build();

        // Village and category know the state and sector, so those win; the category has no sub-sector, so the client's is used.
        ApplicantProfile resolved = service.resolveApplicant(village("Maharashtra"), category("manufacturing", null), provided);

        assertEquals("Maharashtra", resolved.getState());
        assertEquals("manufacturing", resolved.getSector());
        assertEquals("handicrafts", resolved.getSubSector());
    }

    // ---- which schemes come back ----

    @Test
    void baselineApplicantGetsOnlyTheCentralLoanSchemes() {
        ApplicantProfile baseline = service.resolveApplicant(village("Maharashtra"), category("manufacturing", null), null);

        List<Scheme> found = service.findFinancingCandidates(450000, baseline);

        assertTrue(ids(found).contains(MUDRA_KISHOR));
        assertTrue(ids(found).contains(PMEGP));
        assertTrue(found.stream().allMatch(s -> s.getState() == null), "nothing state-specific without applicant details");
    }

    @Test
    void stateSchemeAppearsOnceTheApplicantMeetsItsCriteria() {
        ApplicantProfile maratha = ApplicantProfile.builder().category("maratha_ebc").build();
        DemoData.BusinessCategoryData manufacturing = category("manufacturing", null);

        ApplicantProfile baseline = service.resolveApplicant(village("Maharashtra"), manufacturing, null);
        ApplicantProfile withDetails = service.resolveApplicant(village("Maharashtra"), manufacturing, maratha);

        assertFalse(ids(service.findFinancingCandidates(1_350_000, baseline)).contains(ANNASAHEB_PATIL));
        assertTrue(ids(service.findFinancingCandidates(1_350_000, withDetails)).contains(ANNASAHEB_PATIL));
    }

    @Test
    void stateSchemeNeverAppearsInAnotherState() {
        // Even an applicant who claims to be in Maharashtra: the village (Madhya Pradesh) decides the state.
        ApplicantProfile provided = ApplicantProfile.builder().category("maratha_ebc").state("Maharashtra").build();

        ApplicantProfile resolved = service.resolveApplicant(village("Madhya Pradesh"), category("manufacturing", null), provided);

        assertEquals("Madhya Pradesh", resolved.getState());
        assertFalse(ids(service.findFinancingCandidates(1_350_000, resolved)).contains(ANNASAHEB_PATIL));
    }

    @Test
    void scApplicantWithLowIncomeSeesTheScLoanScheme() {
        ApplicantProfile provided = ApplicantProfile.builder().isSc(true).annualFamilyIncome(250000.0).build();
        DemoData.BusinessCategoryData manufacturing = category("manufacturing", null);

        ApplicantProfile baseline = service.resolveApplicant(village("Maharashtra"), manufacturing, null);
        ApplicantProfile resolved = service.resolveApplicant(village("Maharashtra"), manufacturing, provided);

        assertFalse(ids(service.findFinancingCandidates(450000, baseline)).contains(NSFDC_TERM));
        assertTrue(ids(service.findFinancingCandidates(450000, resolved)).contains(NSFDC_TERM));
    }

    @Test
    void scApplicantAboveTheIncomeLimitDoesNotSeeIt() {
        ApplicantProfile provided = ApplicantProfile.builder().isSc(true).annualFamilyIncome(900000.0).build();

        ApplicantProfile resolved = service.resolveApplicant(village("Maharashtra"), category("manufacturing", null), provided);

        assertFalse(ids(service.findFinancingCandidates(450000, resolved)).contains(NSFDC_TERM));
    }

    @Test
    void artisanSeesTheFractionalTenureScheme() {
        ApplicantProfile provided = ApplicantProfile.builder().isArtisan(true).build();

        ApplicantProfile resolved = service.resolveApplicant(village("Maharashtra"), category("manufacturing", null), provided);

        assertTrue(ids(service.findFinancingCandidates(90000, resolved)).contains(PM_VISHWAKARMA));
    }

    @Test
    void financingSchemesKeepFractionalTenuresButDropSubYearOnes() {
        List<String> financing = ids(service.getFinancingSchemes());

        assertTrue(financing.contains(PM_VISHWAKARMA)); // 2.5 years
        assertFalse(financing.contains(KA_BADAVARA_BANDHU)); // 3 months
        assertTrue(service.getFinancingSchemes().stream().allMatch(s -> s.getInterestRate() != null && s.getTenureYears() >= 1));
    }

    @Test
    void loanOutsideASchemesRangeExcludesIt() {
        ApplicantProfile baseline = service.resolveApplicant(village("Maharashtra"), category("manufacturing", null), null);

        // Mudra Kishor lends 50,001 - 5,00,000
        assertTrue(ids(service.findFinancingCandidates(450000, baseline)).contains(MUDRA_KISHOR));
        assertFalse(ids(service.findFinancingCandidates(40000, baseline)).contains(MUDRA_KISHOR));
        assertFalse(ids(service.findFinancingCandidates(700000, baseline)).contains(MUDRA_KISHOR));
    }

    // ---- helpers ----

    private static List<String> ids(List<Scheme> schemes) {
        return schemes.stream().map(Scheme::getSchemeId).toList();
    }

    private static DemoData.VillageData village(String state) {
        DemoData.VillageData village = new DemoData.VillageData();
        village.setVillageName("Test Village");
        village.setState(state);
        return village;
    }

    private static DemoData.BusinessCategoryData category(String sector, String subSector) {
        DemoData.BusinessCategoryData category = new DemoData.BusinessCategoryData();
        category.setCategoryName("Test Category");
        category.setSector(sector);
        category.setSubSector(subSector);
        return category;
    }
}
