package com.graminsaathi.data;

import com.graminsaathi.service.VillageEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Backfills columns onto EXISTING village rows at startup - both off by default (one-off migration steps,
 * not something that should run on every boot). Neither is an HTTP endpoint, same reasoning as
 * {@link VillageDataInitializer}.
 *
 * <ul>
 *   <li>{@code graminsaathi.villages.pc11-backfill-path} (default empty): path to a CSV or directory of
 *       them, produced by the (updated) {@code village_processor.py} - i.e. the same {@code data/output/}
 *       files used for the original import, re-generated after the pc11 columns were added to that script.
 *       Fills {@code pc11_*_code} on existing rows.</li>
 *   <li>{@code graminsaathi.villages.coordinates-backfill-path} (default empty): path to a CSV or directory
 *       of them, produced by {@code match_shrug_coordinates.py} (phase 1). Fills {@code latitude}/
 *       {@code longitude}/{@code coordinates_source} on existing rows.</li>
 *   <li>{@code graminsaathi.villages.coordinates-as-of} (default: today): the "as of" date recorded for
 *       every row the coordinates backfill touches in that run - one value per run, like the population
 *       pass's target year, not a per-row CSV column.</li>
 * </ul>
 *
 * <p>{@code @Order(1)} - runs alongside {@link VillageDataInitializer}; both only touch rows that already
 * exist by the time these flags would realistically be turned on (a fresh import and a backfill are never
 * meant to run together), so exact ordering between the two backfills here and the import doesn't matter.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class VillageEnrichmentInitializer implements ApplicationRunner {

    private final VillageEnrichmentService villageEnrichmentService;

    @Value("${graminsaathi.villages.pc11-backfill-path:}")
    private String pc11BackfillPath;

    @Value("${graminsaathi.villages.coordinates-backfill-path:}")
    private String coordinatesBackfillPath;

    @Value("${graminsaathi.villages.coordinates-as-of:}")
    private String coordinatesAsOf;

    @Override
    public void run(ApplicationArguments args) {
        if (pc11BackfillPath != null && !pc11BackfillPath.isBlank()) {
            runOverFiles(pc11BackfillPath, this::backfillPc11File);
        }
        if (coordinatesBackfillPath != null && !coordinatesBackfillPath.isBlank()) {
            LocalDate asOf = (coordinatesAsOf == null || coordinatesAsOf.isBlank())
                    ? LocalDate.now() : LocalDate.parse(coordinatesAsOf.trim());
            runOverFiles(coordinatesBackfillPath, path -> backfillCoordinatesFile(path, asOf));
        }
    }

    private void runOverFiles(String pathProperty, java.util.function.Consumer<Path> perFile) {
        Path path = Path.of(pathProperty.trim());
        if (Files.isDirectory(path)) {
            for (Path csv : listCsvFiles(path)) {
                perFile.accept(csv);
            }
        } else {
            perFile.accept(path);
        }
    }

    private List<Path> listCsvFiles(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            log.error("Village enrichment: could not list backfill directory {}", dir, e);
            return List.of();
        }
    }

    private void backfillPc11File(Path path) {
        withReader(path, reader -> villageEnrichmentService.backfillPc11Codes(reader),
                (s) -> "pc11 " + path + " (matched=" + s.matched() + ", unmatched=" + s.unmatched()
                        + ", skippedInvalid=" + s.skippedInvalid() + ")");
    }

    private void backfillCoordinatesFile(Path path, LocalDate asOf) {
        withReader(path, reader -> villageEnrichmentService.backfillCoordinates(reader, asOf),
                (s) -> "coordinates " + path + " as of " + asOf + " (matched=" + s.matched()
                        + ", unmatched=" + s.unmatched() + ", skippedInvalid=" + s.skippedInvalid() + ")");
    }

    private void withReader(Path path,
                             ThrowingFunction<Reader, VillageEnrichmentService.BackfillSummary> action,
                             Function<VillageEnrichmentService.BackfillSummary, String> describe) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            VillageEnrichmentService.BackfillSummary s = action.apply(reader);
            log.info("Village enrichment: backfilled {}", describe.apply(s));
        } catch (Exception e) {
            log.error("Village enrichment: backfill of {} failed", path, e);
        }
    }

    @FunctionalInterface
    private interface ThrowingFunction<T, R> {
        R apply(T t) throws IOException;
    }
}
