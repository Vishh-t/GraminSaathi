package com.graminsaathi.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * State-level rural household monthly spend on one business category, from MoSPI's Household Consumption
 * Expenditure Survey - the demand-side reference figure step 3 needs (Project_Docs/
 * GraminSaathi_Recommendation_Engine_Plan.md, section 2.2: "Village demand for a category = households x
 * monthly household spend on that category").
 *
 * <p>Schema and repository only - deliberately NOT seeded with any figures by this pass. Household spend
 * numbers are a data-entry task (transcribing published MoSPI HCES tables), not a coding task, and
 * fabricating placeholder rupee figures here would be worse than leaving the table empty: a wrong number
 * looks authoritative, a missing row visibly reads as "no data yet" (see
 * {@link com.graminsaathi.service.DemandSupplyScoreService}, which treats an absent row as unknown
 * demand, not zero demand). Populate for real via whatever loader is built when the actual survey tables
 * are transcribed - same insert-only, source-and-date-stamped pattern as every other reference table here.
 */
@Entity
@Table(
        name = "hce_category_spend",
        uniqueConstraints = @UniqueConstraint(name = "uq_hce_state_category", columnNames = {"state", "category_name"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HceCategorySpend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full state name, same vocabulary as {@code Village.state} / {@code ApplicantProfile.state}. */
    @Column(nullable = false)
    private String state;

    /** Matches {@link BusinessCategory#getCategoryName()} - not a foreign key, same reasoning as {@link VillageFeatures#getVillageId()}: avoids a lazy-proxy leak into API responses. */
    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "monthly_household_spend", nullable = false)
    private Double monthlyHouseholdSpend;

    /** e.g. "MoSPI HCES 2022-23, rural, <state>". */
    @Column(length = 255)
    private String source;

    @Column(name = "survey_year")
    private Integer surveyYear;
}
