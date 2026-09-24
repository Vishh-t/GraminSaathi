package com.graminsaathi.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Minimal, dependency-free CSV reader for the village master import.
 *
 * <p>Header row required. Columns are matched by name (case-insensitive), so order doesn't matter:
 * {@code lgd_code, name, block, district, state, latitude, longitude, population_2011, households_2011,
 * source} - only name, district and state are mandatory. Handles quoted fields, commas inside quotes and
 * doubled quotes; does NOT support newlines inside a quoted field (village names never need them).
 * Rows with a missing mandatory field, or a number that doesn't parse, are skipped and counted, never
 * "repaired".
 *
 * <p><b>Streams row-by-row</b> via {@code rowConsumer} instead of returning a {@code List<Row>} - a full
 * national village file is 300MB+ and easily half a million rows, and materializing that as a List before
 * the caller can process any of it doubles the memory the import needs for no benefit. This keeps peak
 * memory to roughly one row + one buffered line, however large the file is.
 */
public final class VillageCsvParser {

    private VillageCsvParser() {}

    public record Row(
            String lgdCode, String name, String block, String district, String state,
            Double latitude, Double longitude,
            Integer population2011, Integer households2011, String source
    ) {}

    /**
     * Parses {@code reader} line by line, calling {@code rowConsumer} for each valid row as soon as it's
     * read - nothing is buffered beyond the current line. The consumer typically hands each row straight
     * to a batched DB write (see {@code VillageService#importCsv}), so import memory stays flat regardless
     * of file size.
     *
     * @return count of rows skipped for missing mandatory fields or an unparseable number
     */
    public static int parse(Reader reader, Consumer<Row> rowConsumer) throws IOException {
        BufferedReader in = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
        String headerLine = in.readLine();
        if (headerLine == null) {
            return 0;
        }
        Map<String, Integer> col = new HashMap<>();
        List<String> headers = splitLine(stripBom(headerLine));
        for (int i = 0; i < headers.size(); i++) {
            col.put(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        for (String required : List.of("name", "district", "state")) {
            if (!col.containsKey(required)) {
                throw new IllegalArgumentException("Village CSV is missing required column: " + required);
            }
        }

        int skipped = 0;
        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) continue;
            List<String> f = splitLine(line);
            String name = get(f, col, "name");
            String district = get(f, col, "district");
            String state = get(f, col, "state");
            if (name == null || district == null || state == null) {
                skipped++;
                continue;
            }
            try {
                Row row = new Row(
                        get(f, col, "lgd_code"), name, get(f, col, "block"), district, state,
                        toDouble(get(f, col, "latitude")), toDouble(get(f, col, "longitude")),
                        toInt(get(f, col, "population_2011")), toInt(get(f, col, "households_2011")),
                        get(f, col, "source")
                );
                rowConsumer.accept(row);
            } catch (NumberFormatException e) {
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

    private static Double toDouble(String s) {
        return s == null ? null : Double.valueOf(s);
    }

    private static Integer toInt(String s) {
        return s == null ? null : Integer.valueOf(s.replace(",", ""));
    }

    static List<String> splitLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
