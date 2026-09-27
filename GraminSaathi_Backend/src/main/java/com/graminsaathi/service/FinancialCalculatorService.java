package com.graminsaathi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.AnalyzeRequest;
import com.graminsaathi.dto.request.ApplicantProfile;
import com.graminsaathi.dto.response.SchemeComparisonResponse;
import com.graminsaathi.model.Scheme;
import com.graminsaathi.model.Village;
import com.graminsaathi.repository.VillageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FinancialCalculatorService {

    /** How many financing schemes to show in the comparison table (the best-ranked first). */
    static final int MAX_COMPARISON_ROWS = 5;

    private final DemoDataLoader demoDataLoader;
    private final FinancingSchemeService financingSchemeService;
    private final VillageRepository villageRepository;

    /** A real scheme with its repayment terms worked out for a specific loan amount. */
    private record FinancingOption(
            Scheme scheme,
            double annualRate,
            int tenureYears,
            int moratoriumMonths,
            int repaymentMonths,
            double emi,
            double totalRepayment,
            /** Capital / margin-money subsidy the scheme gives on this loan (0 when none we can count). */
            double subsidy,
            /** totalRepayment - subsidy: what options are ranked by. */
            double netCost,
            /** EMI fits within the business's monthly net operating income. */
            boolean affordable
    ) {}

    public record FinancialResult(
            double projectCost,
            double loanAmount,
            String schemeName,
            double interestRateAnnual,
            int tenureYears,
            int moratoriumMonths,
            int repaymentMonths,
            double monthlyRate,
            double emi,
            double workingCapitalEstimate,
            double recommendedProjectCost,
            double recommendedLoanAmount,
            double bufferAmount,
            List<SchemeComparisonResponse> schemeComparison,
            String workingCapitalWarning,
            double localAveragePrice,
            Double recommendedPriceLow,
            Double recommendedPriceHigh,
            Double recommendedLaunchPrice,
            Double breakevenPrice,
            String breakevenNote
    ) {}

    public FinancialResult calculate(AnalyzeRequest request) {
        DemoData.BusinessCategoryData category = demoDataLoader.getBusinessCategory(request.getBusinessCategory());
        if (category == null) {
            throw new IllegalArgumentException("Invalid business category: " + request.getBusinessCategory());
        }

        double availableMarginCapital = request.getAvailableMarginCapital();

        double projectCost = availableMarginCapital / 0.10;
        double loanAmount = projectCost * 0.90;

        // Real village's state (not the demo lookup, which returns null for any village outside the ~4-
        // village demo set and silently drops every state-specific financing scheme as a result - see
        // Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, "part 7").
        String villageState = villageRepository.findFirstByNameNormalized(VillageService.normalize(request.getVillageName()))
                .map(Village::getState)
                .orElse(null);
        ApplicantProfile applicant = financingSchemeService.resolveApplicant(villageState, category, request.getApplicant());

        // Same figure DscrService uses, so "affordable" here means the DSCR label will not be "Risky".
        double monthlyNetOperatingIncome = category.getReferenceMonthlyRevenue() - category.getReferenceMonthlyOperatingCost();
        List<FinancingOption> financingOptions = rankFinancingOptions(loanAmount, applicant, monthlyNetOperatingIncome);
        if (financingOptions.isEmpty()) {
            String location = villageState != null ? villageState : "this location";
            throw new IllegalArgumentException(String.format(
                    "No government financing scheme covers a loan of Rs. %.0f in %s.", loanAmount, location));
        }

        FinancingOption primary = financingOptions.get(0);
        String schemeName = primary.scheme().getName();
        double interestRateAnnual = primary.annualRate();
        int tenureYears = primary.tenureYears();
        int moratoriumMonths = primary.moratoriumMonths();

        int repaymentMonths = primary.repaymentMonths();
        double monthlyRate = interestRateAnnual / 12;

        double emi = primary.emi();

        double workingCapitalEstimate = category.getReferenceMonthlyOperatingCost() * category.getWorkingCapitalMonths();

        double recommendedProjectCost = Math.min(projectCost, category.getReferenceProjectCost());
        double recommendedLoanAmount = recommendedProjectCost * 0.90;
        double bufferAmount = loanAmount - recommendedLoanAmount;

        List<SchemeComparisonResponse> schemeComparison = buildSchemeComparison(financingOptions);

        String workingCapitalWarning = checkWorkingCapitalWarning(
                availableMarginCapital, recommendedProjectCost, workingCapitalEstimate, category.getWorkingCapitalMonths()
        );

        Double avgLocalPrice = null;
        DemoData.BusinessData businessData = demoDataLoader.getBusinessData(request.getVillageName(), request.getBusinessCategory());
        if (businessData != null) {
            avgLocalPrice = businessData.getAvgLocalPrice();
        }

        Double recommendedPriceLow = null;
        Double recommendedPriceHigh = null;
        Double recommendedLaunchPrice = null;
        Double breakevenPrice = null;
        String breakevenNote = null;

        if (avgLocalPrice != null && avgLocalPrice > 0) {
            recommendedPriceLow = round(avgLocalPrice * 0.95, 2);
            recommendedPriceHigh = round(avgLocalPrice * 1.05, 2);
            recommendedLaunchPrice = round(avgLocalPrice * 0.97, 2);

            double estimatedMonthlyUnitsSold = category.getReferenceMonthlyRevenue() / avgLocalPrice;
            if (estimatedMonthlyUnitsSold > 0) {
                breakevenPrice = round((category.getReferenceMonthlyOperatingCost() + emi) / estimatedMonthlyUnitsSold, 2);
                breakevenNote = String.format(
                        "At the current sales volume, your minimum viable price is Rs. %.2f — your planned price of Rs. %.2f gives you a Rs. %.2f per-unit safety margin.",
                        breakevenPrice, avgLocalPrice, avgLocalPrice - breakevenPrice
                );
            }
        } else {
            breakevenNote = "Not applicable — mixed product pricing.";
        }

        return new FinancialResult(
                projectCost, loanAmount, schemeName, interestRateAnnual, tenureYears,
                moratoriumMonths, repaymentMonths, monthlyRate, emi,
                workingCapitalEstimate, recommendedProjectCost, recommendedLoanAmount,
                bufferAmount, schemeComparison, workingCapitalWarning,
                avgLocalPrice != null ? avgLocalPrice : 0,
                recommendedPriceLow, recommendedPriceHigh, recommendedLaunchPrice,
                breakevenPrice, breakevenNote
        );
    }

    private double calculateEmi(double principal, double monthlyRate, int months) {
        if (months <= 0) return 0;
        if (monthlyRate == 0) return principal / months;

        double factor = Math.pow(1 + monthlyRate, months);
        return principal * monthlyRate * factor / (factor - 1);
    }

    /**
     * Real financing schemes the applicant is eligible for, worked out for this loan amount and ranked so that
     * the first entry - the scheme the rest of the analysis (EMI, DSCR, survival) is built on - is the cheapest
     * one the business can actually pay:
     * <ol>
     *   <li>options whose EMI fits within the business's monthly net operating income come first, so a loan
     *       that is cheap on paper but would leave the business unable to pay is never recommended over one it can;</li>
     *   <li>then the lowest net cost: total repayment minus the scheme's capital / margin-money subsidy
     *       (see {@link #estimateSubsidy});</li>
     *   <li>ties: lower EMI, then scheme id.</li>
     * </ol>
     * If nothing is affordable, the same order applies (cheapest first) and DSCR will show as Risky.
     */
    private List<FinancingOption> rankFinancingOptions(double loanAmount, ApplicantProfile applicant,
                                                       double monthlyNetOperatingIncome) {
        return financingSchemeService.findFinancingCandidates(loanAmount, applicant).stream()
                .map(scheme -> toFinancingOption(scheme, loanAmount, monthlyNetOperatingIncome))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing((FinancingOption option) -> !option.affordable()) // false (affordable) sorts first
                        .thenComparingDouble(FinancingOption::netCost)
                        .thenComparingDouble(FinancingOption::emi)
                        .thenComparing(option -> option.scheme().getSchemeId()))
                .toList();
    }

    /**
     * Empty when the moratorium swallows the whole tenure, i.e. there is nothing to repay in instalments.
     *
     * <p>Tenure is worked out in months, so fractional tenures (2.5 years) are exact; {@code tenureYears}
     * is only the rounded figure for display. Interest keeps accruing through the moratorium and is
     * capitalised, so the instalments repay the grown principal - a moratorium delays payments, it does
     * not make the loan cheaper.
     *
     * <p>The EMI is on the full loan: a subsidy is counted in the ranking (net cost) but never lowers the
     * EMI, so DSCR and survival stay conservative until the subsidy is actually credited.
     */
    private Optional<FinancingOption> toFinancingOption(Scheme scheme, double loanAmount, double monthlyNetOperatingIncome) {
        int tenureMonths = (int) Math.round(scheme.getTenureYears() * 12);
        int tenureYears = (int) Math.round(scheme.getTenureYears());
        int moratoriumMonths = scheme.getMoratoriumMonths() != null ? scheme.getMoratoriumMonths() : 0;
        int repaymentMonths = tenureMonths - moratoriumMonths;
        if (repaymentMonths <= 0) {
            return Optional.empty();
        }

        double annualRate = scheme.getInterestRate() / 100.0; // dataset stores percent (11 = 11%)
        double monthlyRate = annualRate / 12;
        double principalAtRepaymentStart = loanAmount * Math.pow(1 + monthlyRate, moratoriumMonths);
        double emi = calculateEmi(principalAtRepaymentStart, monthlyRate, repaymentMonths);
        double totalRepayment = emi * repaymentMonths;
        double subsidy = estimateSubsidy(scheme, loanAmount);
        return Optional.of(new FinancingOption(
                scheme, annualRate, tenureYears, moratoriumMonths, repaymentMonths, emi, totalRepayment,
                subsidy, totalRepayment - subsidy, emi <= monthlyNetOperatingIncome));
    }

    /**
     * Capital / margin-money subsidy on this loan, only where the dataset states it unambiguously:
     * {@code calculation_type = pct_of_loan_capped}, i.e. min(subsidy_pct of the loan, max_subsidy_amount).
     * Other types keep "subsidy_pct" for different things (interest subvention, share of project cost, ...),
     * so they are not netted. The base percentage is used, never the special-category enhancement, so this is
     * a lower bound. A positive percentage next to a cap of 0 is ambiguous and counts as no subsidy.
     */
    private double estimateSubsidy(Scheme scheme, double loanAmount) {
        JsonNode benefit = scheme.getBenefit();
        if (benefit == null || !"pct_of_loan_capped".equals(benefit.path("calculation_type").asText())) {
            return 0;
        }
        double pct = benefit.path("subsidy_pct").asDouble(0);
        if (pct <= 0) {
            return 0;
        }

        double subsidy = loanAmount * pct / 100.0;
        JsonNode cap = benefit.path("max_subsidy_amount");
        if (!cap.isMissingNode() && !cap.isNull()) {
            double maxAmount = cap.asDouble(0);
            if (maxAmount <= 0) {
                return 0;
            }
            subsidy = Math.min(subsidy, maxAmount);
        }
        return Math.min(subsidy, loanAmount);
    }

    private List<SchemeComparisonResponse> buildSchemeComparison(List<FinancingOption> rankedOptions) {
        List<SchemeComparisonResponse> comparisons = new ArrayList<>();

        for (int i = 0; i < rankedOptions.size() && i < MAX_COMPARISON_ROWS; i++) {
            FinancingOption option = rankedOptions.get(i);

            SchemeComparisonResponse resp = new SchemeComparisonResponse();
            resp.setSchemeId(option.scheme().getSchemeId());
            resp.setSchemeName(option.scheme().getName());
            resp.setInterestRate(round(option.annualRate() * 100, 2));
            resp.setTenureYears(option.tenureYears());
            resp.setMoratoriumMonths(option.moratoriumMonths());
            resp.setEmi(round(option.emi(), 2));
            resp.setAgency(option.scheme().getImplementingAgency());
            resp.setSubsidyNote(option.scheme().getEffectiveInterestRateNote());
            resp.setPrimary(i == 0);
            resp.setTotalRepayment(round(option.totalRepayment(), 2));
            resp.setEstimatedSubsidy(option.subsidy() > 0 ? round(option.subsidy(), 2) : null);
            resp.setNetCost(round(option.netCost(), 2));
            resp.setRateEstimated(Boolean.TRUE.equals(option.scheme().getRateEstimated()));
            resp.setAffordable(option.affordable());
            resp.setEligibilityHuman(option.scheme().getEligibilityHuman());
            resp.setDocumentsRequired(documentNames(option.scheme()));
            resp.setApplicationSteps(option.scheme().getApplicationSteps());
            resp.setPortalUrl(option.scheme().getPortalUrl());
            comparisons.add(resp);
        }

        return comparisons;
    }

    /** Just the document names, in dataset order - {@link Scheme#getDocumentsRequired()} carries mandatory/source detail this summary view doesn't need. */
    private List<String> documentNames(Scheme scheme) {
        List<JsonNode> docs = scheme.getDocumentsRequired();
        if (docs == null || docs.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (JsonNode doc : docs) {
            String name = doc.path("name").asText(null);
            if (name != null) {
                names.add(name);
            }
        }
        return names.isEmpty() ? null : names;
    }

    private String checkWorkingCapitalWarning(double availableMarginCapital, double recommendedProjectCost,
                                               double workingCapitalEstimate, int workingCapitalMonths) {
        double marginRequired = recommendedProjectCost * 0.10;
        double remainingAfterMargin = availableMarginCapital - marginRequired;

        if (remainingAfterMargin < workingCapitalEstimate) {
            return String.format(
                    "Your margin covers the 10%% contribution, but you have no separate buffer for the first %d months of operating costs (~Rs. %.0f). Consider starting with the recommended lower project size, or arranging a small additional buffer before applying.",
                    workingCapitalMonths, workingCapitalEstimate
            );
        }
        return null;
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}