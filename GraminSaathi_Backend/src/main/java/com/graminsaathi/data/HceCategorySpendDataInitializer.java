package com.graminsaathi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.model.HceCategorySpend;
import com.graminsaathi.repository.HceCategorySpendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Iterator;

/**
 * Fills {@code hce_category_spend} from {@code hce_category_spend_seed.json} at startup - same
 * insert-only, idempotent-by-key shape as {@link BusinessCategoryDataInitializer}, uniqueness here being
 * {@code (state, category_name)} rather than just {@code category_name}.
 *
 * <p>Unlike that seed file, {@code hce_category_spend_seed.json} is deliberately shipped with
 * {@code monthly_household_spend: null} placeholder rows (see {@link HceCategorySpend}'s javadoc for why
 * a fabricated number is worse than no row) - a row whose spend is still null is SKIPPED, not inserted,
 * since the column is {@code NOT NULL} in the DB and a placeholder has nothing real to write yet. This
 * lets you fill the JSON in progressively (state by state, category by category, as you transcribe MoSPI
 * HCES tables) and just restart - already-seeded rows are left alone, newly-filled ones get inserted, and
 * still-null ones are silently skipped again until filled.
 *
 * <p>Runs on by default ({@code graminsaathi.hce-category-spend.seed:true}) - same reasoning as
 * {@link BusinessCategoryDataInitializer}: a handful of rows (at most 31 states x however many
 * categories exist), no 640k-row cost to gate behind a flag.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class HceCategorySpendDataInitializer implements ApplicationRunner {

    private final HceCategorySpendRepository hceCategorySpendRepository;
    private final ObjectMapper objectMapper;

    @Value("${graminsaathi.hce-category-spend.seed:true}")
    private boolean seed;

    @Override
    public void run(ApplicationArguments args) {
        if (!seed) {
            return;
        }
        int inserted = 0, alreadyPresent = 0, skippedNull = 0, total = 0;
        try (InputStream is = new ClassPathResource("hce_category_spend_seed.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            for (Iterator<JsonNode> it = root.elements(); it.hasNext(); ) {
                JsonNode node = it.next();
                total++;
                String state = text(node, "state");
                String categoryName = text(node, "category_name");
                Double spend = number(node, "monthly_household_spend");

                if (spend == null) {
                    skippedNull++;
                    continue;
                }
                if (hceCategorySpendRepository.findByStateIgnoreCaseAndCategoryNameIgnoreCase(state, categoryName).isPresent()) {
                    alreadyPresent++;
                    continue;
                }
                hceCategorySpendRepository.save(HceCategorySpend.builder()
                        .state(state)
                        .categoryName(categoryName)
                        .monthlyHouseholdSpend(spend)
                        .source(text(node, "source"))
                        .surveyYear(node.path("survey_year").isMissingNode() || node.path("survey_year").isNull()
                                ? null : node.path("survey_year").asInt())
                        .build());
                inserted++;
            }
            log.info("HCE category spend: seed inserted {} new, {} already present, {} skipped (no spend value yet), {} total rows in file",
                    inserted, alreadyPresent, skippedNull, total);
        } catch (Exception e) {
            log.error("HCE category spend: seed failed", e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return (v.isMissingNode() || v.isNull()) ? null : v.asText();
    }

    private Double number(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return (v.isMissingNode() || v.isNull()) ? null : v.asDouble();
    }
}
