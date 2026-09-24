package com.graminsaathi.data;

import com.graminsaathi.service.VillageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Fills the village master table at startup. Both actions are insert-only and idempotent, so restarting
 * the app never duplicates or modifies rows.
 *
 * <ul>
 *   <li>{@code graminsaathi.villages.seed-demo} (default {@code true}): make sure the demo villages from
 *       demo_data.json exist, so autocomplete works before any real data is loaded.</li>
 *   <li>{@code graminsaathi.villages.import-path} (default empty): path to either a single village CSV, or
 *       a directory containing several (e.g. one file per state) - columns are documented on
 *       {@link VillageCsvParser}. Files in a directory are imported in name order, each logged separately;
 *       one bad file is logged and skipped rather than aborting the rest. Set this to load a Census/LGD
 *       extract; leave it empty afterwards. Importing is deliberately not an HTTP endpoint, so no public
 *       route can bulk-write.</li>
 * </ul>
 *
 * A failure here is logged, never fatal: the app must still boot if the CSV path is wrong.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VillageDataInitializer implements ApplicationRunner {

    private final VillageService villageService;
    private final DemoDataLoader demoDataLoader;

    @Value("${graminsaathi.villages.seed-demo:true}")
    private boolean seedDemo;

    @Value("${graminsaathi.villages.import-path:}")
    private String importPath;

    @Override
    public void run(ApplicationArguments args) {
        if (seedDemo) {
            try {
                int inserted = villageService.seedDemoVillages(demoDataLoader.getAllVillages());
                log.info("Village master: demo seed inserted {} new village(s)", inserted);
            } catch (Exception e) {
                log.error("Village master: demo seed failed", e);
            }
        }

        if (importPath == null || importPath.isBlank()) {
            return;
        }
        Path path = Path.of(importPath.trim());
        if (Files.isDirectory(path)) {
            for (Path csv : listCsvFiles(path)) {
                importOneFile(csv);
            }
        } else {
            importOneFile(path);
        }
    }

    private List<Path> listCsvFiles(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            log.error("Village master: could not list import directory {}", dir, e);
            return List.of();
        }
    }

    private void importOneFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            VillageService.ImportSummary s = villageService.importCsv(reader, path.getFileName().toString());
            log.info("Village master: imported {} (inserted={}, skippedExisting={}, skippedInvalid={})",
                    path, s.inserted(), s.skippedExisting(), s.skippedInvalid());
        } catch (Exception e) {
            log.error("Village master: import of {} failed", path, e);
        }
    }
}
