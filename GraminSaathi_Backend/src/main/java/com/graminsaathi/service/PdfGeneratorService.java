package com.graminsaathi.service;

import com.graminsaathi.data.DemoData;
import com.graminsaathi.data.DemoDataLoader;
import com.graminsaathi.dto.request.AnalyzeRequest;
import com.graminsaathi.service.FinancialCalculatorService;
import com.graminsaathi.service.FeasibilityScoreService;
import com.graminsaathi.service.DscrService;
import com.graminsaathi.service.SurvivalSimulatorService;
import com.graminsaathi.service.BusinessCombinationService;
import com.graminsaathi.service.EvidenceService;
import com.graminsaathi.service.AgentCommitteeService;
import com.graminsaathi.service.BusinessHealthScoreService;
import com.graminsaathi.service.SupplyChainRiskService;
import com.graminsaathi.service.PeerBenchmarkService;
import com.graminsaathi.service.RoadmapMilestoneService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private final DemoDataLoader demoDataLoader;
    private final FinancialCalculatorService financialCalculatorService;
    private final FeasibilityScoreService feasibilityScoreService;
    private final DscrService dscrService;
    private final SurvivalSimulatorService survivalSimulatorService;
    private final BusinessCombinationService businessCombinationService;
    private final EvidenceService evidenceService;
    private final AgentCommitteeService agentCommitteeService;
    private final BusinessHealthScoreService businessHealthScoreService;
    private final SupplyChainRiskService supplyChainRiskService;
    private final PeerBenchmarkService peerBenchmarkService;
    private final RoadmapMilestoneService roadmapMilestoneService;

    private static final PDFont TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont HEADING_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont SUB_HEADING_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont SMALL_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final float PAGE_MARGIN = 50;
    private static final float LINE_HEIGHT = 14;
    private static final float HEADING_SPACING = 20;

    private float yPosition;
    private PDPageContentStream contentStream;
    private PDPage page;
    private PDDocument document;

    public byte[] generateReport(AnalyzeRequest request) {
        try {
            FinancialCalculatorService.FinancialResult financial = financialCalculatorService.calculate(request);
            FeasibilityScoreService.FeasibilityResult feasibility = feasibilityScoreService.calculate(request.getVillageName(), request.getBusinessCategory());
            DscrService.DscrResult dscr = dscrService.calculate(request.getBusinessCategory(), financial.emi());
            SurvivalSimulatorService.SimulationResult survival = survivalSimulatorService.simulate(
                    new com.graminsaathi.dto.request.SimulateRequest(
                            request.getVillageName(), request.getBusinessCategory(),
                            request.getAvailableMarginCapital(), 0.0, 0.0
                    )
            );
            BusinessCombinationService.CombinationResult combination = businessCombinationService.getCombination(request.getBusinessCategory());
            EvidenceService.EvidenceResult evidence = evidenceService.generateEvidence(request);
            AgentCommitteeService.CommitteeResult committee = agentCommitteeService.evaluate(request);
            BusinessHealthScoreService.HealthScoreResult health = businessHealthScoreService.calculate(request);
            SupplyChainRiskService.RiskResult supplyRisk = supplyChainRiskService.getRisk(request.getBusinessCategory());
            PeerBenchmarkService.BenchmarkResult benchmark = peerBenchmarkService.getBenchmark(request.getBusinessCategory());
            RoadmapMilestoneService.RoadmapResult roadmap = roadmapMilestoneService.getRoadmap(request.getBusinessCategory());

            document = new PDDocument();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            yPosition = page.getMediaBox().getHeight() - PAGE_MARGIN;

            addTitlePage(request);
            addExecutiveSummary(request, financial, feasibility, dscr, survival, committee);
            addFinancialAnalysis(financial, feasibility);
            addSchemeComparison(financial);
            addRiskAnalysis(dscr, survival, supplyRisk);
            addBusinessHealthScore(health);
            addLocalPriceIntelligence(financial);
            addFailureBoundary(financial);
            addPeerBenchmark(benchmark);
            addRoadmap(roadmap);
            addMultiAgentCommittee(committee);
            addEvidenceTrail(evidence);
            addSecondaryOpportunity(combination);
            addFooter();

            contentStream.close();
            document.save(baos);
            document.close();

            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void newPageIfNeeded(float requiredSpace) throws IOException {
        if (yPosition - requiredSpace < PAGE_MARGIN) {
            contentStream.close();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            yPosition = page.getMediaBox().getHeight() - PAGE_MARGIN;
        }
    }

    private void writeText(String text, PDFont font, float fontSize, float x, float y, PDColor color) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(color);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private void writeText(String text, PDFont font, float fontSize, float x, float y) throws IOException {
        writeText(text, font, fontSize, x, y, new PDColor(new float[]{0, 0, 0}, PDDeviceRGB.INSTANCE));
    }

    private void addTitlePage(AnalyzeRequest request) throws IOException {
        float centerX = page.getMediaBox().getWidth() / 2;

        String title = "GraminSaathi — Business Analysis Report";
        float titleWidth = TITLE_FONT.getStringWidth(title) / 1000 * 18;
        writeText(title, TITLE_FONT, 18, centerX - titleWidth / 2, yPosition);
        yPosition -= 30;

        String subtitle = "AI-Style Business Advisory & Loan Structuring Tool";
        float subWidth = SUB_HEADING_FONT.getStringWidth(subtitle) / 1000 * 12;
        writeText(subtitle, SUB_HEADING_FONT, 12, centerX - subWidth / 2, yPosition);
        yPosition -= 40;

        addDetailRow("Applicant Location", request.getVillageName());
        addDetailRow("Business Category", request.getBusinessCategory());
        addDetailRow("Available Margin Capital", CURRENCY.format(request.getAvailableMarginCapital()));
        addDetailRow("Report Generated", java.time.LocalDateTime.now().toString());
        addDetailRow("Generated By", "GraminSaathi — SIH 2026 Prototype");

        newPage();
    }

    private void newPage() throws IOException {
        contentStream.close();
        page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
        yPosition = page.getMediaBox().getHeight() - PAGE_MARGIN;
    }

    private void addDetailRow(String label, String value) throws IOException {
        newPageIfNeeded(LINE_HEIGHT * 2);
        writeText(label + ": ", BOLD_FONT, 10, PAGE_MARGIN, yPosition);
        float labelWidth = BOLD_FONT.getStringWidth(label + ": ") / 1000 * 10;
        writeText(value, BODY_FONT, 10, PAGE_MARGIN + labelWidth, yPosition);
        yPosition -= LINE_HEIGHT;
    }

    private void addHeading(String text) throws IOException {
        newPageIfNeeded(HEADING_SPACING);
        yPosition -= 5;
        writeText(text, HEADING_FONT, 14, PAGE_MARGIN, yPosition);
        yPosition -= HEADING_SPACING;
    }

    private void addSubHeading(String text) throws IOException {
        newPageIfNeeded(HEADING_SPACING);
        writeText(text, SUB_HEADING_FONT, 12, PAGE_MARGIN, yPosition);
        yPosition -= 18;
    }

    private void addBodyText(String text) throws IOException {
        newPageIfNeeded(LINE_HEIGHT);
        writeText(text, BODY_FONT, 10, PAGE_MARGIN, yPosition);
        yPosition -= LINE_HEIGHT;
    }

    private void addBodyText(String text, PDFont font, float fontSize) throws IOException {
        newPageIfNeeded(LINE_HEIGHT);
        writeText(text, font, fontSize, PAGE_MARGIN, yPosition);
        yPosition -= LINE_HEIGHT;
    }

    private void addExecutiveSummary(AnalyzeRequest request,
                                      FinancialCalculatorService.FinancialResult financial,
                                      FeasibilityScoreService.FeasibilityResult feasibility,
                                      DscrService.DscrResult dscr,
                                      SurvivalSimulatorService.SimulationResult survival,
                                      AgentCommitteeService.CommitteeResult committee) throws IOException {
        addHeading("Executive Summary");

        String summary = String.format(
                "Based on your margin capital of %s, you are eligible for a project cost of %s under the %s. " +
                "The recommended (optimal) project size is %s with a loan of %s, leaving a safety buffer of %s. " +
                "The local opportunity score for %s in %s is %d (%s). " +
                "DSCR stands at %.2f (%s). " +
                "The 24-month survival simulation shows: %s. " +
                "The Investment Committee's final verdict: %s.",
                CURRENCY.format(request.getAvailableMarginCapital()),
                CURRENCY.format(financial.projectCost()),
                financial.schemeName(),
                CURRENCY.format(financial.recommendedProjectCost()),
                CURRENCY.format(financial.recommendedLoanAmount()),
                CURRENCY.format(financial.bufferAmount()),
                request.getBusinessCategory(), request.getVillageName(),
                feasibility.opportunityScore(), feasibility.label(),
                dscr.dscr(), dscr.label(),
                survival.verdict(),
                committee.finalVerdict()
        );

        addWrappedText(summary, BODY_FONT, 10, PAGE_MARGIN, yPosition, page.getMediaBox().getWidth() - 2 * PAGE_MARGIN);
        yPosition -= 20;
    }

    private void addWrappedText(String text, PDFont font, float fontSize, float x, float y, float maxWidth) throws IOException {
        float charWidth = font.getStringWidth("a") / 1000 * fontSize;
        int charsPerLine = (int) (maxWidth / charWidth);

        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float currentY = y;

        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            if (font.getStringWidth(testLine) / 1000 * fontSize > maxWidth) {
                if (line.length() > 0) {
                    writeText(line.toString(), font, fontSize, x, currentY);
                    currentY -= LINE_HEIGHT;
                    newPageIfNeeded(LINE_HEIGHT);
                    line = new StringBuilder(word);
                } else {
                    writeText(word, font, fontSize, x, currentY);
                    currentY -= LINE_HEIGHT;
                    newPageIfNeeded(LINE_HEIGHT);
                }
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (line.length() > 0) {
            writeText(line.toString(), font, fontSize, x, currentY);
            currentY -= LINE_HEIGHT;
        }
        yPosition = currentY;
    }

    private void addFinancialAnalysis(FinancialCalculatorService.FinancialResult financial,
                                       FeasibilityScoreService.FeasibilityResult feasibility) throws IOException {
        addHeading("1. Financial Analysis");

        addSubHeading("1.1 Project Cost & Loan Structure");
        addDetailRow("Available Margin Capital (10%)", CURRENCY.format(financial.projectCost() * 0.10));
        addDetailRow("Maximum Project Cost", CURRENCY.format(financial.projectCost()));
        addDetailRow("Maximum Loan Amount (90%)", CURRENCY.format(financial.loanAmount()));

        addSubHeading("1.2 Scheme Assignment");
        addDetailRow("Scheme", financial.schemeName());
        addDetailRow("Interest Rate", String.format("%.1f%% per annum", financial.interestRateAnnual() * 100));
        addDetailRow("Tenure", financial.tenureYears() + " years");
        addDetailRow("Moratorium Period", financial.moratoriumMonths() + " months");
        addDetailRow("Repayment Months", String.valueOf(financial.repaymentMonths()));

        addSubHeading("1.3 EMI Calculation");
        addDetailRow("Monthly Interest Rate", String.format("%.4f%%", financial.monthlyRate() * 100));
        addDetailRow("EMI", CURRENCY.format(financial.emi()));

        addSubHeading("1.4 Recommended (Optimal) Loan");
        addDetailRow("Recommended Project Cost", CURRENCY.format(financial.recommendedProjectCost()));
        addDetailRow("Recommended Loan Amount", CURRENCY.format(financial.recommendedLoanAmount()));
        addDetailRow("Safety Buffer", CURRENCY.format(financial.bufferAmount()));

        addSubHeading("1.5 Working Capital Estimate");
        addDetailRow("Estimated Working Capital", CURRENCY.format(financial.workingCapitalEstimate()));

        if (financial.workingCapitalWarning() != null) {
            newPageIfNeeded(LINE_HEIGHT * 2);
            yPosition -= 5;
            writeText("⚠️ " + financial.workingCapitalWarning(), BOLD_FONT, 10, PAGE_MARGIN, yPosition, new PDColor(new float[]{1, 0.55f, 0}, PDDeviceRGB.INSTANCE));
            yPosition -= LINE_HEIGHT * 2;
        }

        addSubHeading("1.6 Local Opportunity Score");
        addDetailRow("Opportunity Score", String.valueOf(feasibility.opportunityScore()));
        addDetailRow("Label", feasibility.label());
        addDetailRow("Population (5km radius)", String.valueOf(feasibility.population5kmRadius()));
        addDetailRow("Competitor Count", String.valueOf(feasibility.competitorCount()));
    }

    private void addSchemeComparison(FinancialCalculatorService.FinancialResult financial) throws IOException {
        if (financial.schemeComparison() == null || financial.schemeComparison().isEmpty()) return;

        addHeading("2. Scheme Comparison");
        addSubHeading("Alternative schemes for your project cost:");

        String[] headers = new String[]{"Scheme", "Rate (%/yr)", "Tenure (yr)", "Moratorium (mo)", "EMI", "Agency"};

        List<List<String>> rows = financial.schemeComparison().stream()
                .map(s -> List.of(
                        s.getSchemeName() + (s.isPrimary() ? " ★" : ""),
                        String.format("%.1f", s.getInterestRate()),
                        String.valueOf(s.getTenureYears()),
                        String.valueOf(s.getMoratoriumMonths()),
                        CURRENCY.format(s.getEmi()),
                        s.getAgency()
                ))
                .toList();

        drawTable(headers, rows);
        yPosition -= 10;

        addBodyText("★ = Your matched scheme. PMEGP and Mudra rates are illustrative — verify current terms before applying.", SMALL_FONT, 8);
    }

    private void drawTable(String[] headers, List<List<String>> rows) throws IOException {
        float tableWidth = page.getMediaBox().getWidth() - 2 * PAGE_MARGIN;
        int colCount = headers.length;
        float colWidth = tableWidth / colCount;
        float rowHeight = 20;

        newPageIfNeeded((rows.size() + 1) * rowHeight + 10);

        // Header row
        float x = PAGE_MARGIN;
        for (int i = 0; i < colCount; i++) {
            drawCell(x, yPosition, colWidth, rowHeight, headers[i], true);
            x += colWidth;
        }
        yPosition -= rowHeight;

        // Data rows
        for (List<String> row : rows) {
            x = PAGE_MARGIN;
            for (int i = 0; i < colCount; i++) {
                drawCell(x, yPosition, colWidth, rowHeight, row.get(i), row.get(0).contains("★"));
                x += colWidth;
            }
            yPosition -= rowHeight;
        }
    }

    private void drawCell(float x, float y, float width, float height, String text, boolean highlight) throws IOException {
        // Draw border
        contentStream.setStrokingColor(0, 0, 0);
        contentStream.addRect(x, y - height, width, height);
        contentStream.stroke();

        // Draw background if highlighted
        if (highlight) {
            contentStream.setNonStrokingColor(1, 1, 0.78f);
            contentStream.addRect(x, y - height, width, height);
            contentStream.fill();
        }

        // Draw text
        float textX = x + 2;
        float textY = y - height + 4;
        writeText(text, highlight ? BOLD_FONT : BODY_FONT, 8, textX, textY);
    }

    private void addRiskAnalysis(DscrService.DscrResult dscr,
                                  SurvivalSimulatorService.SimulationResult survival,
                                  SupplyChainRiskService.RiskResult supplyRisk) throws IOException {
        addHeading("3. Risk Analysis");

        addSubHeading("3.1 Debt Service Coverage Ratio (DSCR)");
        addDetailRow("Monthly Net Operating Income", CURRENCY.format(dscr.monthlyNetOperatingIncome()));
        addDetailRow("Monthly EMI", CURRENCY.format(dscr.emi()));
        addDetailRow("DSCR", String.format("%.2f", dscr.dscr()));
        addDetailRow("Assessment", dscr.label());

        addSubHeading("3.2 Business Survival Simulation (24 Months)");
        addDetailRow("Base Case Verdict", survival.verdict());
        if (survival.deficitMonth() != null) {
            addDetailRow("Deficit Month", String.valueOf(survival.deficitMonth()));
        }

        addSubHeading("3.3 Supply Chain Risk");
        addDetailRow("Risk Level", supplyRisk.riskLevel());
        addDetailRow("Advisory Note", supplyRisk.note());
    }

    private void addBusinessHealthScore(BusinessHealthScoreService.HealthScoreResult health) throws IOException {
        addHeading("4. Business Health Score (Multi-Factor)");

        addDetailRow("Overall Score", health.overallScore() + "/100");
        addDetailRow("Recommendation", health.recommendation());

        String[] headers = {"Factor", "Score", "Details"};
        List<List<String>> rows = List.of(
                toRow(health.marketDemand()),
                toRow(health.capitalAdequacy()),
                toRow(health.profitability()),
                toRow(health.cashFlow()),
                toRow(health.supplyRisk()),
                toRow(health.seasonality())
        );

        drawTable(headers, rows);
    }

    private List<String> toRow(BusinessHealthScoreService.SubScore score) {
        return List.of(score.name(), String.valueOf(score.score()), score.description());
    }

    private void addLocalPriceIntelligence(FinancialCalculatorService.FinancialResult financial) throws IOException {
        if (financial.localAveragePrice() == 0) return;

        addHeading("5. Local Price Intelligence");
        addDetailRow("Local Average Price", CURRENCY.format(financial.localAveragePrice()));
        if (financial.recommendedPriceLow() != null) {
            addDetailRow("Recommended Price Range", CURRENCY.format(financial.recommendedPriceLow()) + " — " + CURRENCY.format(financial.recommendedPriceHigh()));
            addDetailRow("Recommended Launch Price", CURRENCY.format(financial.recommendedLaunchPrice()));
        }
    }

    private void addFailureBoundary(FinancialCalculatorService.FinancialResult financial) throws IOException {
        if (financial.breakevenPrice() == null) return;

        addHeading("6. Failure Boundary Analysis");
        addWrappedText(financial.breakevenNote(), BODY_FONT, 10, PAGE_MARGIN, yPosition, page.getMediaBox().getWidth() - 2 * PAGE_MARGIN);
        yPosition -= 10;
    }

    private void addPeerBenchmark(PeerBenchmarkService.BenchmarkResult benchmark) throws IOException {
        addHeading("7. Peer Benchmark (Illustrative)");
        addDetailRow("Sample Size", String.valueOf(benchmark.sampleSize()));
        addDetailRow("Avg Monthly Revenue (6 months)", CURRENCY.format(benchmark.avgMonthlyRevenueAfter6Months()));
        addDetailRow("% Still Operating (1 year)", benchmark.pctStillOperatingAfter1Year() + "%");

        addBodyText(benchmark.disclaimer(), SMALL_FONT, 8);
    }

    private void addRoadmap(RoadmapMilestoneService.RoadmapResult roadmap) throws IOException {
        addHeading("8. First 365 Days Roadmap");

        for (RoadmapMilestoneService.Milestone m : roadmap.milestones()) {
            newPageIfNeeded(LINE_HEIGHT);
            writeText("Month " + m.month() + ": ", BOLD_FONT, 10, PAGE_MARGIN, yPosition);
            float labelWidth = BOLD_FONT.getStringWidth("Month " + m.month() + ": ") / 1000 * 10;
            addWrappedText(m.milestone(), BODY_FONT, 10, PAGE_MARGIN + labelWidth, yPosition, page.getMediaBox().getWidth() - 2 * PAGE_MARGIN - labelWidth);
            yPosition -= 5;
        }
    }

    private void addMultiAgentCommittee(AgentCommitteeService.CommitteeResult committee) throws IOException {
        addHeading("9. Multi-Agent Investment Committee");

        addAgentOpinion(committee.marketAgent());
        addAgentOpinion(committee.financeAgent());
        addAgentOpinion(committee.riskAgent());

        newPageIfNeeded(LINE_HEIGHT * 2);
        yPosition -= 5;
        writeText("Final Verdict: ", BOLD_FONT, 12, PAGE_MARGIN, yPosition);
        float labelWidth = BOLD_FONT.getStringWidth("Final Verdict: ") / 1000 * 12;
        writeText(committee.finalVerdict(), BOLD_FONT, 12, PAGE_MARGIN + labelWidth, yPosition);
        yPosition -= 20;

        addWrappedText(committee.verdictReason(), BODY_FONT, 10, PAGE_MARGIN, yPosition, page.getMediaBox().getWidth() - 2 * PAGE_MARGIN);
        yPosition -= 10;
    }

    private void addAgentOpinion(AgentCommitteeService.AgentOpinion agent) throws IOException {
        newPageIfNeeded(LINE_HEIGHT * 3);
        writeText(agent.agent() + ": ", SUB_HEADING_FONT, 12, PAGE_MARGIN, yPosition);
        yPosition -= 18;

        addWrappedText(agent.opinion(), BODY_FONT, 10, PAGE_MARGIN, yPosition, page.getMediaBox().getWidth() - 2 * PAGE_MARGIN);
        yPosition -= 5;

        addBodyText("   Basis: " + agent.basis(), SMALL_FONT, 8);
    }

    private void addEvidenceTrail(EvidenceService.EvidenceResult evidence) throws IOException {
        addHeading("10. Evidence Trail");

        for (EvidenceService.EvidenceItem item : evidence.evidence()) {
            newPageIfNeeded(LINE_HEIGHT * 3);
            writeText(item.claim() + " — ", BOLD_FONT, 10, PAGE_MARGIN, yPosition);
            float claimWidth = BOLD_FONT.getStringWidth(item.claim() + " — ") / 1000 * 10;
            writeText(item.source(), BODY_FONT, 10, PAGE_MARGIN + claimWidth, yPosition);
            yPosition -= LINE_HEIGHT;

            addBodyText("   " + item.details(), SMALL_FONT, 8);
        }
    }

    private void addSecondaryOpportunity(BusinessCombinationService.CombinationResult combination) throws IOException {
        addHeading("11. Suggested Secondary Revenue Stream");
        addWrappedText(combination.secondaryOpportunity(), BODY_FONT, 10, PAGE_MARGIN, yPosition, page.getMediaBox().getWidth() - 2 * PAGE_MARGIN);
        yPosition -= 10;
    }

    private void addFooter() throws IOException {
        newPageIfNeeded(LINE_HEIGHT * 2);
        yPosition -= 20;
        String footer = "Generated by GraminSaathi — SIH 2026 Prototype";
        float footerWidth = SMALL_FONT.getStringWidth(footer) / 1000 * 8;
        float centerX = page.getMediaBox().getWidth() / 2;
        writeText(footer, SMALL_FONT, 8, centerX - footerWidth / 2, yPosition);
    }
}