package com.graminsaathi.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.graminsaathi.model.Scheme;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the enriched government-schemes dataset into memory at startup.
 *
 * <p>Source: {@code src/main/resources/data/all_schemes_enriched.json} (a flat JSON array).
 * 230 records is small enough to query with plain streams - no database needed for the demo.
 *
 * <p>The raw file contains repeated {@code scheme_id}s (28 exact copies + 1 near-copy), so records
 * are de-duplicated by id at load time: the first occurrence wins and later ones are skipped
 * with a warning. The data file itself is left untouched.
 *
 * <p>About 20 bank loans in the dataset have no interest rate because the bank sets it and the state
 * pays back part of the interest (an "interest subvention"). Those would never be offered as loans,
 * so {@code data/scheme_rate_overrides.json} records the subvention for each and the loader turns it
 * into an <em>estimated</em> rate (see {@link #estimateRate}). Schemes that already carry a rate are
 * never touched.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemeDataService {

    static final String DATA_FILE = "data/all_schemes_enriched.json";
    static final String RATE_OVERRIDES_FILE = "data/scheme_rate_overrides.json";

    /**
     * Bank lending rate assumed for loans whose rate the dataset leaves to the bank (percent).
     * Chosen between the dataset's own figures: Mudra 9.5-10%, PMEGP 11%.
     */
    static final double ASSUMED_BANK_RATE_PERCENT = 10.5;

    private final ObjectMapper objectMapper;

    private List<Scheme> schemes = List.of();
    private Map<String, Scheme> schemesById = Collections.emptyMap();

    /** One entry of {@code scheme_rate_overrides.json}; every field is optional. */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class RateOverride {
        /** Percentage points the state pays back on the borrower's interest. Null = none counted. */
        private Double interestSubventionPct;
        /** How many years the subvention lasts. Null = the whole tenure. */
        private Double subventionYears;
        /** The scheme guarantees the borrower never pays more than this rate (percent). */
        private Double maxLendingRatePct;
        /** Replaces {@link #ASSUMED_BANK_RATE_PERCENT} for this scheme (percent). */
        private Double assumedBankRatePct;
        /** Where the numbers come from; documentation only. */
        private String basis;
    }

    @PostConstruct
    public void load() {
        List<Scheme> raw;
        try (InputStream is = new ClassPathResource(DATA_FILE).getInputStream()) {
            raw = objectMapper.readValue(is, new TypeReference<List<Scheme>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + DATA_FILE, e);
        }

        Map<String, Scheme> unique = new LinkedHashMap<>();
        int duplicates = 0;
        for (Scheme scheme : raw) {
            if (scheme.getSchemeId() == null || scheme.getSchemeId().isBlank()) {
                log.warn("Skipping scheme without scheme_id: {}", scheme.getName());
                continue;
            }
            if (unique.putIfAbsent(scheme.getSchemeId(), scheme) != null) {
                duplicates++;
                log.warn("Duplicate scheme_id '{}' ('{}') skipped - keeping first occurrence",
                        scheme.getSchemeId(), scheme.getName());
            }
        }

        int estimated = applyRateOverrides(unique);

        this.schemesById = Collections.unmodifiableMap(unique);
        this.schemes = List.copyOf(unique.values());
        log.info("Loaded {} schemes from {} ({} duplicate records skipped, {} estimated interest rates)",
                schemes.size(), DATA_FILE, duplicates, estimated);
    }

    /** All schemes, de-duplicated by scheme_id, in file order. Never null. */
    public List<Scheme> getAllSchemes() {
        return schemes;
    }

    public Optional<Scheme> getById(String schemeId) {
        return Optional.ofNullable(schemesById.get(schemeId));
    }

    /**
     * Estimated interest rate in percent: the assumed bank rate less the state's interest subvention,
     * never below 0% and never above the scheme's own lending-rate cap. A subvention that only lasts part
     * of the tenure is averaged over the whole tenure, so it counts for that share of the discount.
     */
    static double estimateRate(Scheme scheme, RateOverride override) {
        double bankRate = override.getAssumedBankRatePct() != null ? override.getAssumedBankRatePct() : ASSUMED_BANK_RATE_PERCENT;
        double subvention = override.getInterestSubventionPct() != null ? override.getInterestSubventionPct() : 0;

        double tenure = scheme.getTenureYears() != null ? scheme.getTenureYears() : 0;
        double share = 1.0;
        if (override.getSubventionYears() != null && tenure > 0) {
            share = Math.min(1.0, override.getSubventionYears() / tenure);
        }

        double rate = Math.max(0, bankRate - subvention * share);
        if (override.getMaxLendingRatePct() != null) {
            rate = Math.min(rate, override.getMaxLendingRatePct());
        }
        return Math.round(rate * 100) / 100.0;
    }

    /** Returns how many schemes got an estimated rate. */
    private int applyRateOverrides(Map<String, Scheme> byId) {
        Map<String, RateOverride> overrides;
        try (InputStream is = new ClassPathResource(RATE_OVERRIDES_FILE).getInputStream()) {
            overrides = objectMapper.readValue(is, new TypeReference<Map<String, RateOverride>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + RATE_OVERRIDES_FILE, e);
        }

        int applied = 0;
        for (Map.Entry<String, RateOverride> entry : overrides.entrySet()) {
            Scheme scheme = byId.get(entry.getKey());
            if (scheme == null) {
                log.warn("Rate override for unknown scheme_id '{}' ignored", entry.getKey());
            } else if (scheme.getInterestRate() != null) {
                log.warn("Scheme '{}' already has an interest rate ({}%) - override ignored",
                        entry.getKey(), scheme.getInterestRate());
            } else if (scheme.getTenureYears() == null || scheme.getTenureYears() < 1) {
                log.warn("Scheme '{}' has no repayment tenure - rate override ignored", entry.getKey());
            } else {
                scheme.setInterestRate(estimateRate(scheme, entry.getValue()));
                scheme.setRateEstimated(true);
                applied++;
            }
        }
        return applied;
    }
}
