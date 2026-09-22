package com.graminsaathi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.graminsaathi.model.Scheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the actual benefit for one scheme, keyed off {@code benefit.calculation_type} - see the
 * handoff doc (GraminSaathi_Schemes_Integration_Handoff.md) section 3.2 for the design and
 * SCHEMES_STATUS_AND_NEXT_STEPS.md section 6 (Phase B) for where this fits in the roadmap.
 *
 * <p>The same 4 raw {@code benefit} fields ({@code subsidy_pct}, {@code max_subsidy_amount},
 * {@code on_loan_upto}, {@code special_category_enhancement}) mean something different depending on
 * {@code calculation_type} - callers should never read them directly, only go through {@link #calculate}.
 *
 * <p>Two deliberate deviations from the handoff's literal spec, found by checking real dataset values
 * before implementing (not blockers, just judgement calls - see each method's own note):
 * <ul>
 *   <li>{@code interest_rate_reduction}: the handoff says {@code effective_rate = interest_rate -
 *       subsidy_pct}. In the real data, most of these 50 schemes already store the final NET rate in
 *       {@code interest_rate} (e.g. Annasaheb Patil: interest_rate=0, subsidy_pct=100 - it's a 0%
 *       loan, not a rate needing "100" subtracted from it). Subtracting would double-count or go
 *       negative. See {@link #interestRateReduction}.</li>
 *   <li>{@code per_unit_or_in_kind}: the handoff says {@code max_subsidy_amount × applicant_quantity}
 *       (per acre/animal). None of the current dataset's 11 schemes of this type actually carry a
 *       distinct per-unit rate separate from {@code max_subsidy_amount} - every sampled example is
 *       really a percent-of-cost grant with a cap. Treated identically to {@code pct_of_cost_capped}
 *       until a scheme is found that genuinely needs a multiplier - see {@link #perUnitOrInKind}.</li>
 * </ul>
 */
@Slf4j
@Service
public class BenefitCalculator {

    /**
     * What's known at calculation time, beyond the scheme itself - both optional. A scheme can still be
     * shown (with a text description instead of a ₹ figure) when neither is supplied, e.g. on a
     * schemes-browse page before the person has entered a project cost or picked a loan amount.
     */
    public record BenefitInput(Double actualCost, Double loanAmount) {
        public static final BenefitInput EMPTY = new BenefitInput(null, null);
    }

    /**
     * One scheme's computed benefit. For a leaf (non-composite) result, at most one of {@code amount},
     * {@code effectiveInterestRatePct}, {@code guaranteeCoverPct} is populated - which one depends on
     * {@code calculationType} (handoff section 6: a ₹ figure for grant/capped types, an effective rate
     * for rate-reduction, a cover % for guarantees - never summed together). {@code components} is only
     * populated for {@code composite}. {@code displayText} is always populated, so a caller never has
     * to branch on which numeric field to read before it has real input to compute one.
     */
    public record BenefitResult(
            String calculationType,
            Double amount,
            Double effectiveInterestRatePct,
            Double guaranteeCoverPct,
            String displayText,
            List<BenefitResult> components
    ) {
        static BenefitResult amount(String type, Double amount, String displayText) {
            return new BenefitResult(type, amount, null, null, displayText, List.of());
        }

        static BenefitResult rate(String type, Double effectiveRatePct, String displayText) {
            return new BenefitResult(type, null, effectiveRatePct, null, displayText, List.of());
        }

        static BenefitResult cover(String type, Double coverPct, String displayText) {
            return new BenefitResult(type, null, null, coverPct, displayText, List.of());
        }

        static BenefitResult textOnly(String type, String displayText) {
            return new BenefitResult(type, null, null, null, displayText, List.of());
        }
    }

    public BenefitResult calculate(Scheme scheme, BenefitInput input) {
        JsonNode benefit = scheme.getBenefit();
        String type = benefit != null ? benefit.path("calculation_type").asText(null) : null;
        if (type == null) {
            return BenefitResult.textOnly("text_only_see_note", fallbackNote(scheme));
        }
        return calculateType(scheme, benefit, type, input != null ? input : BenefitInput.EMPTY);
    }

    private BenefitResult calculateType(Scheme scheme, JsonNode benefit, String type, BenefitInput input) {
        return switch (type) {
            case "flat_grant_or_cap" -> flatGrantOrCap(benefit);
            case "pct_of_cost_capped" -> pctOfCostCapped(benefit, input);
            case "pct_of_loan_capped" -> pctOfLoanCapped(benefit, input);
            case "pct_of_loan_uncapped" -> pctOfLoanUncapped(benefit, input);
            case "interest_rate_reduction" -> interestRateReduction(scheme, benefit);
            case "guarantee_cover_pct" -> guaranteeCoverPct(benefit);
            case "per_unit_or_in_kind" -> perUnitOrInKind(benefit, input);
            case "equity_or_uncapped" -> equityOrUncapped(scheme);
            case "text_only_see_note" -> BenefitResult.textOnly(type, fallbackNote(scheme));
            case "composite" -> composite(scheme, benefit, input);
            default -> {
                log.warn("Unknown calculation_type '{}' on scheme '{}' - falling back to text", type, scheme.getSchemeId());
                yield BenefitResult.textOnly(type, fallbackNote(scheme));
            }
        };
    }

    // ---- Individual formulas (handoff section 3.2) -------------------------------------------------

    /** Not present at the top level of the current dataset, but a cheap, fully-defined fallback regardless. */
    private BenefitResult flatGrantOrCap(JsonNode benefit) {
        Double amount = num(benefit, "max_subsidy_amount");
        String text = amount != null ? "Grant of up to " + rupees(amount) : "Fixed grant amount (see scheme notes)";
        return BenefitResult.amount("flat_grant_or_cap", amount, text);
    }

    private BenefitResult pctOfCostCapped(JsonNode benefit, BenefitInput input) {
        Double pct = num(benefit, "subsidy_pct");
        Double cap = num(benefit, "max_subsidy_amount");
        Double cost = input.actualCost();

        if (pct == null) {
            String text = cap != null ? "Up to " + rupees(cap) + " of project cost" : "Percentage of project cost (see scheme notes)";
            return BenefitResult.amount("pct_of_cost_capped", cap, text);
        }
        if (cost == null) {
            String text = trimZero(pct) + "% of project cost" + (cap != null ? ", capped at " + rupees(cap) : "");
            return BenefitResult.amount("pct_of_cost_capped", null, text);
        }
        double raw = cost * pct / 100.0;
        double amount = cap != null ? Math.min(raw, cap) : raw;
        String text = trimZero(pct) + "% of your " + rupees(cost) + " project cost" + (cap != null ? " (capped at " + rupees(cap) + ")" : "");
        return BenefitResult.amount("pct_of_cost_capped", round2(amount), text);
    }

    private BenefitResult pctOfLoanCapped(JsonNode benefit, BenefitInput input) {
        Double pct = num(benefit, "subsidy_pct");
        Double cap = num(benefit, "max_subsidy_amount");
        Double loan = input.loanAmount();

        if (pct == null || loan == null) {
            String pctText = pct != null ? trimZero(pct) + "% of your loan amount" : "Percentage of your loan amount";
            String text = pctText + (cap != null ? ", capped at " + rupees(cap) : "");
            return BenefitResult.amount("pct_of_loan_capped", null, text);
        }
        double raw = loan * pct / 100.0;
        double amount = cap != null ? Math.min(raw, cap) : raw;
        String text = trimZero(pct) + "% of your " + rupees(loan) + " loan" + (cap != null ? " (capped at " + rupees(cap) + ")" : "");
        return BenefitResult.amount("pct_of_loan_capped", round2(amount), text);
    }

    /** Never present at the top level in the current dataset - only as a composite component (PM Vishwakarma). */
    private BenefitResult pctOfLoanUncapped(JsonNode benefit, BenefitInput input) {
        Double pct = num(benefit, "subsidy_pct");
        Double loan = input.loanAmount();
        if (pct == null || loan == null) {
            String pctText = pct != null ? trimZero(pct) + "% of your loan amount" : "Percentage of your loan amount";
            return BenefitResult.amount("pct_of_loan_uncapped", null, pctText + ", no upper cap");
        }
        double amount = loan * pct / 100.0;
        return BenefitResult.amount("pct_of_loan_uncapped", round2(amount), trimZero(pct) + "% of your " + rupees(loan) + " loan, no upper cap");
    }

    /**
     * See the class-level note: {@code scheme.interestRate}, when present, is already the NET rate the
     * borrower ends up paying for most of the 50 schemes of this type (0% for a "100% interest
     * subvention" scheme, 1% for "50% grant + effectively interest-free loan", etc.) - so it is shown
     * directly, never reduced further by {@code subsidy_pct}. Only when no rate is stored at all
     * (interest set by the bank / not fixed - e.g. Rajasthan MLUPY) does {@code subsidy_pct} get used,
     * and even then only as a rough "X percentage points off" description, since we don't reliably know
     * what base rate it comes off of.
     */
    private BenefitResult interestRateReduction(Scheme scheme, JsonNode benefit) {
        Double rate = scheme.getInterestRate();
        Double pct = num(benefit, "subsidy_pct");
        String note = scheme.getEffectiveInterestRateNote();

        if (rate != null) {
            String text = "Effective interest rate: " + trimZero(rate) + "% p.a." + (note != null ? " — " + note : "");
            return BenefitResult.rate("interest_rate_reduction", rate, text);
        }
        if (note != null) {
            return BenefitResult.rate("interest_rate_reduction", null, note);
        }
        if (pct != null) {
            return BenefitResult.rate("interest_rate_reduction", null,
                    trimZero(pct) + " percentage-point interest-rate reduction (exact effective rate not specified — check scheme notes)");
        }
        return BenefitResult.rate("interest_rate_reduction", null, "Reduces your effective interest rate (see scheme notes)");
    }

    private BenefitResult guaranteeCoverPct(JsonNode benefit) {
        Double pct = num(benefit, "subsidy_pct");
        Double onLoanUpto = num(benefit, "on_loan_upto");
        String suffix = onLoanUpto != null ? " on loans up to " + rupees(onLoanUpto) : "";
        String text = pct != null
                ? trimZero(pct) + "% collateral-free loan cover" + suffix + " (not counted as a cash benefit)"
                : "Collateral-free loan cover (see scheme notes)";
        return BenefitResult.cover("guarantee_cover_pct", pct, text);
    }

    /** See the class-level note on why this is currently identical to {@link #pctOfCostCapped}. */
    private BenefitResult perUnitOrInKind(JsonNode benefit, BenefitInput input) {
        BenefitResult asPctOfCost = pctOfCostCapped(benefit, input);
        return new BenefitResult("per_unit_or_in_kind", asPctOfCost.amount(), null, null, asPctOfCost.displayText(), List.of());
    }

    /** Handoff spec: no computed ₹ figure - show the loan range and note as-is. */
    private BenefitResult equityOrUncapped(Scheme scheme) {
        StringBuilder text = new StringBuilder();
        Scheme.LoanAmountRange range = scheme.getLoanAmountRange();
        if (range != null && (range.getMin() != null || range.getMax() != null)) {
            text.append(rupeesRange(range));
        }
        if (scheme.getEffectiveInterestRateNote() != null) {
            if (!text.isEmpty()) text.append(" — ");
            text.append(scheme.getEffectiveInterestRateNote());
        }
        if (text.isEmpty()) {
            text.append("Equity or convertible-debt style funding (see scheme notes)");
        }
        return BenefitResult.textOnly("equity_or_uncapped", text.toString());
    }

    /**
     * {@code calculation_components} is a flat array of type-NAME strings (e.g.
     * {@code ["pct_of_cost_capped", "interest_rate_reduction"]}) that all share this same {@code benefit}
     * node's fields - it is NOT an array of separate sub-benefit objects with their own subsidy_pct/cap.
     * So each component is computed against the identical shared {@code subsidy_pct}/{@code
     * max_subsidy_amount}/{@code on_loan_upto}, just interpreted through a different formula - verified
     * against every composite scheme in the dataset (PM Vishwakarma, SVEP, SISFS, YSR Cheyutha, ...)
     * before writing this. Never summed into one ₹ total (handoff section 6): {@code amount} stays null,
     * each component keeps its own.
     */
    private BenefitResult composite(Scheme scheme, JsonNode benefit, BenefitInput input) {
        List<BenefitResult> parts = new ArrayList<>();
        for (JsonNode componentType : benefit.path("calculation_components")) {
            String type = componentType.asText(null);
            if (type == null) continue;
            parts.add(calculateType(scheme, benefit, type, input));
        }
        String text = parts.isEmpty()
                ? "Multiple benefits (see scheme notes)"
                : String.join("; ", parts.stream().map(BenefitResult::displayText).toList());
        return new BenefitResult("composite", null, null, null, text, List.copyOf(parts));
    }

    // ---- Helpers -------------------------------------------------------------------------------------

    private String fallbackNote(Scheme scheme) {
        if (scheme.getEffectiveInterestRateNote() != null) return scheme.getEffectiveInterestRateNote();
        if (scheme.getEligibilityHuman() != null) return scheme.getEligibilityHuman();
        return "See scheme details for benefit terms.";
    }

    private static Double num(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asDouble();
    }

    private static Double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private static String trimZero(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static String rupees(double value) {
        return "\u20B9" + String.format("%,.0f", value);
    }

    private String rupeesRange(Scheme.LoanAmountRange range) {
        if (range.getMin() != null && range.getMax() != null && range.getMax() > 0) {
            return rupees(range.getMin()) + "–" + rupees(range.getMax());
        }
        if (range.getMin() != null && range.getMin() > 0) {
            return rupees(range.getMin()) + "+";
        }
        return "Amount varies";
    }
}
