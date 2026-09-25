package com.graminsaathi.data;

import com.graminsaathi.service.VillageFeaturesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Year;

/**
 * Runs the population-projection pass of the {@code village_features} ETL at startup, same
 * off-by-default / property-gated pattern as {@link VillageDataInitializer}'s CSV import: this is a
 * batch job over 640k+ rows, not something that should re-run on every restart by accident, and (like
 * the village import) it is deliberately not an HTTP endpoint - no public route can trigger a bulk write.
 *
 * <ul>
 *   <li>{@code graminsaathi.features.population.run-on-startup} (default {@code false}): set to
 *       {@code true} for one run, then back to {@code false} - re-running is safe (upsert on villageId)
 *       but pointless until the source data or projection year changes.</li>
 *   <li>{@code graminsaathi.features.population.projection-year} (default: the current year). {@code 0}
 *       (the "unset" marker, since a real year is never 0) falls back to {@link Year#now()} in code -
 *       {@code ${...:#{spel}}} does NOT evaluate the SpEL default, it's read as a literal string, so that
 *       cannot be done as a one-line placeholder default.</li>
 * </ul>
 *
 * A failure here is logged, never fatal - the app must still boot if the run errors partway through.
 *
 * <p>{@code @Order(2)} - must run after {@link VillageDataInitializer}, which this depends on.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class VillageFeaturesInitializer implements ApplicationRunner {

    private final VillageFeaturesService villageFeaturesService;

    @Value("${graminsaathi.features.population.run-on-startup:false}")
    private boolean runOnStartup;

    @Value("${graminsaathi.features.population.projection-year:0}")
    private int configuredProjectionYear;

    @Override
    public void run(ApplicationArguments args) {
        if (!runOnStartup) {
            return;
        }
        int year = configuredProjectionYear > 0 ? configuredProjectionYear : Year.now().getValue();
        try {
            var summary = villageFeaturesService.runPopulationProjection(year);
            log.info("Village features: population pass complete for year {} - processed={}, projected={}, "
                            + "skippedNoPopulation={}",
                    year, summary.processed(), summary.projected(), summary.skippedNoPopulation());
        } catch (Exception e) {
            log.error("Village features: population pass failed", e);
        }
    }
}
