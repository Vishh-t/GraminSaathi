package com.graminsaathi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graminsaathi.model.BusinessCategory;
import com.graminsaathi.repository.BusinessCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Fills {@code business_categories} from {@code business_categories_seed.json} at startup. Insert-only
 * and idempotent by {@code category_name} (case-insensitive), same shape as {@link VillageDataInitializer}
 * - a rerun never duplicates or overwrites a row someone has since edited by hand in the DB.
 *
 * <p>Unlike the village import this is a handful of rows, so it runs on by default
 * ({@code graminsaathi.business-categories.seed:true}) - there's no 640k-row cost to gate behind a flag.
 * Adding a category later is a JSON edit in {@code business_categories_seed.json} + one restart, not a
 * code change - see {@link BusinessCategory} javadoc.
 *
 * <p>{@code @Order(1)} - no dependency on villages, but kept alongside {@link VillageDataInitializer} for
 * consistency; scoring services that need both tables should not assume startup order between the two.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class BusinessCategoryDataInitializer implements ApplicationRunner {

    private final BusinessCategoryRepository businessCategoryRepository;
    private final ObjectMapper objectMapper;

    @Value("${graminsaathi.business-categories.seed:true}")
    private boolean seed;

    @Override
    public void run(ApplicationArguments args) {
        if (!seed) {
            return;
        }
        try {
            List<BusinessCategory> parsed = parseSeedFile();
            Set<String> existing = Set.copyOf(businessCategoryRepository.findAllCategoryNamesLower());

            List<BusinessCategory> toInsert = new ArrayList<>();
            for (BusinessCategory category : parsed) {
                if (!existing.contains(category.getCategoryName().toLowerCase())) {
                    toInsert.add(category);
                }
            }
            businessCategoryRepository.saveAll(toInsert);
            log.info("Business categories: seed inserted {} new, {} already present",
                    toInsert.size(), parsed.size() - toInsert.size());
        } catch (Exception e) {
            log.error("Business categories: seed failed", e);
        }
    }

    private List<BusinessCategory> parseSeedFile() throws Exception {
        List<BusinessCategory> result = new ArrayList<>();
        try (InputStream is = new ClassPathResource("business_categories_seed.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            for (Iterator<JsonNode> it = root.elements(); it.hasNext(); ) {
                JsonNode node = it.next();
                result.add(BusinessCategory.builder()
                        .categoryName(text(node, "category_name"))
                        .sector(text(node, "sector"))
                        .subSector(text(node, "sub_sector"))
                        .referenceProjectCost(number(node, "reference_project_cost"))
                        .referenceCostRangeLow(number(node, "reference_cost_range_low"))
                        .referenceCostRangeHigh(number(node, "reference_cost_range_high"))
                        .workingCapitalMonths(node.path("working_capital_months").isMissingNode() || node.path("working_capital_months").isNull()
                                ? null : node.path("working_capital_months").asInt())
                        .referenceMonthlyRevenue(number(node, "reference_monthly_revenue"))
                        .referenceMonthlyOperatingCost(number(node, "reference_monthly_operating_cost"))
                        .secondaryOpportunity(text(node, "secondary_opportunity"))
                        .sourceNote(text(node, "source_note"))
                        .singleBuyerDependencyRisk(text(node, "single_buyer_dependency_risk"))
                        .primaryInputDependencyNote(text(node, "primary_input_dependency_note"))
                        .minLandHoldingAcres(number(node, "min_land_holding_acres"))
                        .minMarginCapitalFraction(number(node, "min_margin_capital_fraction"))
                        .requiresElectricity(node.path("requires_electricity").isMissingNode() || node.path("requires_electricity").isNull()
                                ? null : node.path("requires_electricity").asBoolean())
                        .hardFilterRulesJson(text(node, "hard_filter_rules_json"))
                        .fitRequirementRulesJson(text(node, "fit_requirement_rules_json"))
                        .build());
            }
        }
        return result;
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
