package com.graminsaathi.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Minimal, dependency-free CSV reader for {@code compute_village_market_signals.py}'s output
 * (village_market_signals.csv) - step 2b/2c backfill onto {@code village_features}. Same streaming,
 * skip-and-count-don't-repair conventions as {@link VillageCsvParser}; kept as a separate class rather
 * than extending that one since the column set and destination table are entirely different (this feeds
 * {@code village_features}, keyed by {@code lgd_code} resolved to {@code village_id}; that one feeds
 * {@code villages} directly).
 *
 * <p>Expected columns (see the Python scripts' headers - either compute_village_market_signals.py's
 * output, compute_coverage_confidence.py's, or a merged file with both): {@code lgd_code, name, district,
 * state, competitor_counts_json, competitor_counts_source, competitor_counts_as_of, osm_building_count,
 * expected_building_count, coverage_confidence_ratio, coverage_confidence_label, coverage_confidence_source,
 * coverage_confidence_as_of} - only {@code lgd_code} is mandatory (it's the join key; a row with no
 * lgd_code can never be matched to a village_id, so it's skipped rather than imported unmatched). Every
 * other column is independently optional, since these two scripts run as separate passes over
 * time and neither one's CSV carries the other's columns - see {@link com.graminsaathi.service
 * .VillageEnrichmentService#backfillMarketSignals}'s COALESCE-based upsert for how a column absent from
 * one pass's CSV avoids clobbering a value an earlier pass already wrote.
 *
 * <p>{@code competitor_counts_json} is taken as-is (already valid JSON text produced by the Python side,
 * e.g. {@code {"Dairy": null, "Tailoring": 3, ...}}) - this parser does not interpret it, just extracts
 * the CSV field, quoted-comma-safe like {@link VillageCsvParser}.
 *
 * <p>Unlike {@link VillageCsvParser}'s coordinates backfill, {@code as_of} here is read PER ROW from the
 * CSV rather than taken as one value for the whole run - the Python script already stamps each row with
 * its own generation date, which is more self-describing than assuming every row in a file was produced
 * at the same moment (usually true, but the CSV should not need a human to also remember to pass the
 * right date on the Java side).
 */
public final class MarketSignalsCsvParser {

    private MarketSignalsCsvParser() {}

    public record Row(
            String lgdCode,
            String competitorCountsJson, String competitorCountsSource, LocalDate competitorCountsAsOf,
            Integer osmBuildingCount, Integer expectedBuildingCount, Double coverageConfidenceRatio,
            String coverageConfidenceLabel, String coverageConfidenceSource, LocalDate coverageConfidenceAsOf
    ) {}

    /**
     * @return count of rows skipped for a missing lgd_code or an unparseable number/date
     */
    public static int parse(Reader reader, Consumer<Row> rowConsumer) throws IOException {
        BufferedReader in = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        String headerLine = in.readLine();
        if (headerLine == null) {
            return 0;
        }
        Map<String, Integer> col = new HashMap<>();
        List<String> headers = VillageCsvParser.splitLine(stripBom(headerLine));
        for (int i = 0; i < headers.size(); i++) {
            col.put(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        if (!col.containsKey("lgd_code")) {
            throw new IllegalArgumentException("Market signals CSV is missing required column: lgd_code");
        }

        int skipped = 0;
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) continue;
            List<String> f = VillageCsvParser.splitLine(line);
            String lgdCode = VillageCsvParser.normalizeLgdCode(get(f, col, "lgd_code"));
            if (lgdCode == null) {
                skipped++;
                continue;
            }
            try {
                Row row = new Row(
                        lgdCode,
                        get(f, col, "competitor_counts_json"),
                        get(f, col, "competitor_counts_source"),
                        toDate(get(f, col, "competitor_counts_as_of")),
                        toInt(get(f, col, "osm_building_count")),
                        toInt(get(f, col, "expected_building_count")),
                        toDouble(get(f, col, "coverage_confidence_ratio")),
                        get(f, col, "coverage_confidence_label"),
                        get(f, col, "coverage_confidence_source"),
                        toDate(get(f, col, "coverage_confidence_as_of"))
                );
                rowConsumer.accept(row);
            } catch (RuntimeException e) {
                skipped++;
            }
        }
        return skipped;
    }

    private static String stripBom(String s) {
        return s.startsWith("\uFEFF") ? s.substring(1) : s;
    }

    private static String get(List<String> fields, Map<String, Integer> col, String name) {
        Integer idx = col.get(name);
        if (idx == null || idx >= fields.size()) return null;
        String v = fields.get(idx).trim();
        return v.isEmpty() ? null : v;
    }

    private static Integer toInt(String s) {
        return s == null ? null : Integer.valueOf(s.replace(",", ""));
    }

    private static Double toDouble(String s) {
        return s == null ? null : Double.valueOf(s);
    }

    private static LocalDate toDate(String s) {
        return s == null ? null : LocalDate.parse(s);
    }
}
