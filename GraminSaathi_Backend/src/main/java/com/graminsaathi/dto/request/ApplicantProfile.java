package com.graminsaathi.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The applicant vocabulary that scheme eligibility rules (JsonLogic) can reference.
 *
 * <p>Every field maps 1:1 to an {@code applicant.<name>} variable used in
 * {@code all_schemes_enriched.json}. JSON names are snake_case (via {@link JsonNaming}) so that
 * {@code objectMapper.convertValue(profile, Map.class)} yields exactly the keys the rules expect
 * ({@code is_sc}, {@code annual_family_income}, ...).
 *
 * <p>All fields are wrapper types on purpose: {@code null} means "not provided", which lets the
 * client send a partial profile. Enum-like fields are plain Strings holding the exact values used
 * in the dataset (see the handoff doc, section 2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ApplicantProfile {

    // ---- Booleans -------------------------------------------------------------------------
    private Boolean isSc;
    private Boolean isSt;
    private Boolean isWoman;
    private Boolean isPwd;
    private Boolean isBpl;
    private Boolean isShgMember;
    private Boolean isFpoMember;
    private Boolean isJlgMember;
    private Boolean isArtisan;
    private Boolean isStreetVendor;
    private Boolean isLandless;
    private Boolean isReturnedEmigrant;
    private Boolean isManualScavenger;
    private Boolean isSafaiKaramchari;
    private Boolean isRegisteredUnemployed;
    private Boolean isRegisteredLabour;
    private Boolean isIncomeTaxPayer;
    private Boolean isIncubated;
    private Boolean isScMajorityOwned;
    private Boolean dpiitRecognized;
    private Boolean hasElectricityConnection;
    private Boolean hasKaliaBskyCard;
    private Boolean holdsDrivingLicense;
    private Boolean ownsIndigenousCow;
    private Boolean ownsLoom;
    private Boolean ownsVehicle;
    private Boolean completedDairyTraining;

    // ---- Numbers --------------------------------------------------------------------------
    private Integer age;
    private Double annualFamilyIncome;
    private Double annualTurnover;
    private Integer businessAgeMonths;
    private Double landHoldingAcres;
    private Double overseasServiceYears;

    // ---- Enums (exact dataset strings) ----------------------------------------------------
    /** general, obc, sc, st, bc, mbc, minority, pwd, dnc, maratha_ebc */
    private String category;
    /** greenfield, early_stage, existing */
    private String businessStage;
    /** individual, proprietorship, partnership, llp, company, cooperative, shg, shg_federation, fpo, jlg, ngo, section_8_company, startup, micro, small, medium, msme, artisan_guild, state_dairy_federation */
    private String enterpriseType;
    /** 4th_pass, 5th_pass, 7th_pass, 8th_pass, 10th_pass, 12th_pass, iti, diploma, graduate, vocational */
    private String education;
    /** farmer, tenant_farmer, agricultural_labourer, artisan, traditional_artisan, street_vendor, hawker, ... */
    private String occupation;
    /** manufacturing, services, trading, agri_allied, technology, tourism, green_energy, logistics */
    private String sector;
    /** food_processing, dairy, fisheries, poultry, livestock, goat, sheep, piggery, ... */
    private String subSector;
    /** rural, urban */
    private String ruralUrban;
    /** handloom, powerloom, weaver */
    private String artisanType;
    /** chambhar, dhor, holiya, mochi (very narrow, rarely used) */
    private String subCaste;
    /** registered */
    private String udyamStatus;
    /** Full state name as used in the dataset, e.g. "Uttar Pradesh", "Jammu and Kashmir". */
    private String state;
    /** farmer */
    private String parentOccupation;
}
