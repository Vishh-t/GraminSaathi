package com.graminsaathi.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row of the village master table - the unit everything else (features, scoring, discovery) will
 * key on. See Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, step 1.
 *
 * <p>{@code lgdCode} is the Local Government Directory village code. It is nullable ONLY because the
 * 3 demo-seed villages have no verified code yet; rows imported from a real Census/LGD extract must
 * carry one. Population/households are Census 2011 figures exactly as published (not projected -
 * projection is a later step) and stay null when unknown; we never fill them with guesses.
 */
@Entity
@Table(
        name = "villages",
        indexes = {
                @Index(name = "idx_villages_name_norm", columnList = "name_normalized"),
                @Index(name = "idx_villages_state", columnList = "state"),
                @Index(name = "idx_villages_district", columnList = "district"),
                @Index(name = "idx_villages_state_district", columnList = "state, district"),
                @Index(name = "idx_villages_lat_lon", columnList = "latitude, longitude")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Village {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lgd_code", unique = true, length = 32)
    private String lgdCode;

    @Column(nullable = false)
    private String name;

    /** Lower-cased, diacritics-stripped, letters/digits/spaces only. What autocomplete matches against. */
    @Column(name = "name_normalized", nullable = false)
    private String nameNormalized;

    private String block;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String state;

    private Double latitude;
    private Double longitude;

    /** Where {@link #latitude}/{@link #longitude} came from (e.g. "shrug_pc11", "osm_place"). Null until phase 1 loads coordinates. */
    @Column(name = "coordinates_source", length = 64)
    private String coordinatesSource;

    @Column(name = "coordinates_as_of")
    private LocalDate coordinatesAsOf;

    /**
     * Census 2011 PC11 codes (State/District/Subdistt/Town-Village, as published in the PCA source CSV) -
     * the join key used by SHRUG's village polygons for step 2 phase 1. Nullable: rows imported before this
     * column existed, and the 3 demo-seed villages, have none. NOT the same as {@link #lgdCode}.
     */
    @Column(name = "pc11_state_code", length = 8)
    private String pc11StateCode;

    @Column(name = "pc11_district_code", length = 8)
    private String pc11DistrictCode;

    @Column(name = "pc11_subdistrict_code", length = 8)
    private String pc11SubdistrictCode;

    @Column(name = "pc11_village_code", length = 8)
    private String pc11VillageCode;

    @Column(name = "population_2011")
    private Integer population2011;

    @Column(name = "households_2011")
    private Integer households2011;

    /** Where this row came from (file name / dataset), so every number stays traceable. */
    @Column(length = 255)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
