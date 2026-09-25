package com.graminsaathi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Projects Census 2011 village population/households forward to a later year using compound growth:
 * {@code projected = census2011 * (1 + decadalPct/100) ^ ((year - 2011) / 10)}.
 *
 * <p><b>Growth rate.</b> Default is India's RURAL decadal growth for 2001-2011 (12.18%, Census 2011).
 * It is a national figure applied to every village, so it is a rough estimate, not a village forecast -
 * which is why the growth rate and its source are stored next to every projected number. To do better
 * for a state, add {@code src/main/resources/data/state_rural_growth.csv} (header
 * {@code state,decadal_growth_pct,source}); a row there overrides the default for that state
 * (case-insensitive match, e.g. {@code GOA}). The file is optional - nothing is assumed if it is absent.
 *
 * <p>Households are scaled by the same factor as population (constant household size assumed). Villages
 * with no 2011 population get no projection: unknowns are never filled with guesses.
 */
@Slf4j
@Component
public class PopulationProjector {

    public static final int CENSUS_YEAR = 2011;
    public static final LocalDate CENSUS_REFERENCE_DATE = LocalDate.of(2011, 3, 1);
    static final String DEFAULT_SOURCE = "Census 2011 - India rural decadal growth 2001-11 (national default)";
    private static final String OVERRIDES_RESOURCE = "data/state_rural_growth.csv";

    /** One village's projected figures, plus exactly which growth rate produced them. */
    public record Projection(
            int population,
            Integer households,
            Double avgHouseholdSize,
            int projectionYear,
            double decadalGrowthPct,
            String growthSource
    ) {}

    private record Rate(double decadalPct, String source) {}

    private final Rate defaultRate;
    private final Map<String, Rate> stateOverrides;

    @Autowired
    public PopulationProjector(
            @Value("${graminsaathi.features.projection.default-decadal-growth-pct:12.18}") double defaultDecadalPct) {
        this.defaultRate = new Rate(defaultDecadalPct, DEFAULT_SOURCE);
        this.stateOverrides = loadOverridesFromClasspath();
    }

    /** For tests: supply overrides directly as {@code state -> decadal growth pct}. */
    PopulationProjector(double defaultDecadalPct, Map<String, Double> overridesPct) {
        this.defaultRate = new Rate(defaultDecadalPct, DEFAULT_SOURCE);
        Map<String, Rate> m = new HashMap<>();
        overridesPct.forEach((state, pct) -> m.put(key(state), new Rate(pct, "state override for " + state)));
        this.stateOverrides = m;
    }

    /**
     * @return the projection, or {@code null} when {@code population2011} is unknown or non-positive
     * @throws IllegalArgumentException if {@code year} is before the Census year
     */
    public Projection project(Integer population2011, Integer households2011, String state, int year) {
        if (year < CENSUS_YEAR) {
            throw new IllegalArgumentException("Projection year " + year + " is before the Census year " + CENSUS_YEAR);
        }
        if (population2011 == null || population2011 <= 0) {
            return null;
        }
        Rate rate = stateOverrides.getOrDefault(key(state), defaultRate);
        double factor = Math.pow(1.0 + rate.decadalPct() / 100.0, (year - CENSUS_YEAR) / 10.0);

        int projectedPop = (int) Math.round(population2011 * factor);
        boolean hasHouseholds = households2011 != null && households2011 > 0;
        Integer projectedHouseholds = hasHouseholds ? (int) Math.round(households2011 * factor) : null;
        Double avgHouseholdSize = hasHouseholds ? Math.round(population2011 * 100.0 / households2011) / 100.0 : null;

        return new Projection(projectedPop, projectedHouseholds, avgHouseholdSize, year, rate.decadalPct(), rate.source());
    }

    private static String key(String state) {
        return state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Rate> loadOverridesFromClasspath() {
        Map<String, Rate> out = new HashMap<>();
        ClassPathResource res = new ClassPathResource(OVERRIDES_RESOURCE);
        if (!res.exists()) {
            return out;
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            in.readLine(); // header
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] p = line.split(",", 3);
                if (p.length < 2) continue;
                try {
                    String source = p.length == 3 && !p[2].isBlank() ? p[2].trim() : "state override for " + p[0].trim();
                    out.put(key(p[0]), new Rate(Double.parseDouble(p[1].trim()), source));
                } catch (NumberFormatException e) {
                    log.warn("Ignoring bad growth-rate row in {}: {}", OVERRIDES_RESOURCE, line);
                }
            }
            log.info("Population projector: loaded {} state growth-rate override(s)", out.size());
        } catch (IOException e) {
            log.warn("Could not read {} - using the national default only", OVERRIDES_RESOURCE, e);
        }
        return out;
    }
}
