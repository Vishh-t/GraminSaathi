# ADDENDUM TO MASTER BUILD BRIEF v3 — 10 Additional Features
### Paste this RIGHT AFTER the v3 document into your coding agent — this is not standalone, it assumes v3's context (data schema, existing services, existing screens) is already loaded. Together, v3 + this addendum = the complete feature set discussed across the whole project.

---

## ADD TO SECTION 2 (FULL FEATURE LIST) — these 10 rows extend the table

| # | Feature | Real vs. substituted |
|---|---|---|
| 17 | Multi-Scheme Comparator (beyond the 2 PS schemes) | 100% real — extra rule-based rows in the scheme lookup table |
| 18 | Working Capital / Cash-Runway Warning | 100% real — a conditional message layered on the existing working_capital_estimate calculation |
| 19 | Local Price Intelligence (recommended selling price) | 100% real — a formula using the cached avg_local_price field |
| 20 | Business Health Score (multi-factor, distinct from Opportunity Score) | 100% real — a weighted combination of existing computed sub-scores |
| 21 | Counterfactual "Failure Boundary" Calculator | 100% real — an algebraic breakeven calculation |
| 22 | Supply Chain / Single-Buyer Risk Flag | Templated rule-based flag using cached category metadata (not live supply-chain data) |
| 23 | SHG Peer Benchmarking | Illustrative/mocked — seeded cohort statistics in the JSON dataset, clearly labeled as "sample cohort data" since real longitudinal user data doesn't exist yet; honest to disclose if asked |
| 24 | First 365 Days Roadmap | Templated — a fixed month-by-month milestone template per business category, not AI-generated |
| 25 | Post-Loan Monitoring / Nudges ("AI CFO") | Illustrative UI mockup screen — shows what a nudge would look like, using static example data, since there's no real post-disbursal usage history in a hackathon prototype; present as a "Roadmap / Coming Next" screen rather than a live feature |
| 26 | Offline-first / low-bandwidth design note | A real, small implementation choice (see below), not a large feature |

---

## 17. MULTI-SCHEME COMPARATOR (real — extends the calculator)

Add these additional scheme rows to a static lookup table (not from `demo_data.json` — put this in a separate `schemes_reference.json`):

```json
[
  { "scheme_name": "Micro Finance Scheme", "min_cost": 0, "max_cost": 140000, "interest_rate": 0.065, "tenure_years": 3, "moratorium_months": 3, "agency": "NBCFDC/NSFDC-style SCA" },
  { "scheme_name": "Term Loan Scheme", "min_cost": 140001, "max_cost": 5000000, "interest_rate": 0.08, "tenure_years": 7, "moratorium_months": 6, "agency": "NBCFDC/NSFDC-style SCA" },
  { "scheme_name": "PMEGP (illustrative)", "min_cost": 0, "max_cost": 2000000, "interest_rate": 0.11, "tenure_years": 7, "moratorium_months": 0, "agency": "KVIC / Banks", "subsidy_note": "15-35% margin money subsidy depending on category and area — illustrative figure, verify current rate before real use" },
  { "scheme_name": "Mudra Loan - Kishor (illustrative)", "min_cost": 50000, "max_cost": 500000, "interest_rate": 0.105, "tenure_years": 5, "moratorium_months": 0, "agency": "Any scheduled bank/NBFC", "subsidy_note": "Market rate, no margin-money subsidy — shown for comparison only" }
]
```

**Logic:** in `/api/analyze`, in addition to the primary scheme routing result (Section 5 of v3), also run the same `project_cost` through every row in `schemes_reference.json` where `min_cost <= project_cost <= max_cost`, compute an EMI for each, and return a `scheme_comparison` array so the frontend can show a small comparison table ("Your scheme" highlighted vs. 1-2 alternatives). Label the PMEGP/Mudra rows clearly as "illustrative rates for comparison — always verify current scheme terms" since these are approximate and not the PS's own scheme figures.

**Frontend:** add a small "Compare with other schemes" expandable table on the Analysis screen, with the PS's own matched scheme visually highlighted/starred.

---

## 18. WORKING CAPITAL / CASH-RUNWAY WARNING (real — layered on existing calculation)

```
# working_capital_estimate already exists in v3 Section 5. Add this check:
IF available_margin_capital < (recommended_project_cost * 0.10 + working_capital_estimate_share_from_margin):
    # i.e., after putting in their 10% margin, do they have anything left for
    # the first few months of operating costs, or does the loan itself need to cover it?
    warning = "⚠️ Your margin covers the 10% contribution, but you have no separate buffer for the first " 
              + working_capital_months + " months of operating costs (~₹" + working_capital_estimate + "). 
              Consider starting with the recommended lower project size, or arranging a small additional buffer before applying."
ELSE:
    warning = null  # no warning needed
```

**Frontend:** show this as a distinct, visually prominent (amber/orange) warning banner on the Analysis screen whenever it's non-null — this was one of the strongest "wow" lines from our earlier discussion ("you can afford the loan, but you'll run short of cash by month 2") and deserves its own visual treatment, not just a buried number.

---

## 19. LOCAL PRICE INTELLIGENCE (real — recommended selling price)

```
INPUT: village.business_data[category].avg_local_price   (already in demo_data.json)

# Simple recommended pricing band for this prototype:
recommended_price_low  = avg_local_price * 0.95
recommended_price_high = avg_local_price * 1.05
recommended_launch_price = avg_local_price * 0.97   # slightly under local average to encourage early adoption

OUTPUT: { local_average_price, recommended_price_low, recommended_price_high, recommended_launch_price }
```
Note: for "Retail / Kirana Store," `avg_local_price` is `null` in the dataset since retail covers many SKUs — skip this feature for that category and show "Pricing varies by product line — not applicable for general retail" instead of a broken calculation.

**Frontend:** add a small "Suggested Pricing" card on the Analysis screen showing the local average alongside the recommended launch price.

---

## 20. BUSINESS HEALTH SCORE (real — multi-factor, distinct from the single Opportunity Score)

This is a broader score than Section 6's Opportunity Score — it combines multiple already-computed sub-scores into one dashboard-style breakdown:

```
market_demand_score      = <the Opportunity Score from Section 6, 0-100, reused directly>
capital_adequacy_score   = 100 if available_margin_capital >= (recommended_project_cost * 0.10) else 
                            ROUND((available_margin_capital / (recommended_project_cost * 0.10)) * 100)
profitability_score      = ROUND(MIN(100, (reference_monthly_revenue - reference_monthly_operating_cost) 
                            / reference_monthly_revenue * 200))   # net margin %, scaled up to a 0-100 range
cash_flow_score          = 100 if DSCR >= 2.0 else (60 if DSCR >= 1.0 else 25)
supply_risk_score        = 100 - (competitor_count * 3)   # simple inverse penalty, floor at 20
                            supply_risk_score = MAX(20, supply_risk_score)
seasonality_score        = 70   # flat placeholder for this prototype (all categories treated as
                                  # moderately seasonal) — note this is a simplification; a future version
                                  # would derive this from Agmarknet price-volatility history

business_health_score = ROUND(
    market_demand_score * 0.25 +
    capital_adequacy_score * 0.15 +
    profitability_score * 0.20 +
    cash_flow_score * 0.20 +
    supply_risk_score * 0.10 +
    seasonality_score * 0.10
)

recommendation:
    >= 75: "🟢 Proceed"
    50-74: "🟡 Proceed with modifications"
    < 50:  "🔴 Do not finance as structured"
```

**Frontend:** show this as a radar/bar breakdown (7 factors, each 0-100) with the combined score prominently displayed — this is a strong, visually rich addition to the Analysis screen, sitting alongside (not replacing) the simpler Opportunity Score used in Discovery.

---

## 21. COUNTERFACTUAL "FAILURE BOUNDARY" CALCULATOR (real — algebraic breakeven)

```
# "What's the minimum price/volume before this business stops being profitable?"

INPUT: reference_monthly_operating_cost, EMI, current avg_local_price, estimated_monthly_units_sold
  (derive estimated_monthly_units_sold = reference_monthly_revenue / avg_local_price, where avg_local_price is available)

breakeven_price = (reference_monthly_operating_cost + EMI) / estimated_monthly_units_sold

output: "At the current sales volume, your minimum viable price is ₹" + breakeven_price + 
        " — your planned price of ₹" + avg_local_price + " gives you a ₹" + (avg_local_price - breakeven_price) + 
        " per-unit safety margin."

# If avg_local_price is null (e.g., Retail), skip this and show "Not applicable — mixed product pricing."
```

**Frontend:** a small callout under the Local Price Intelligence card: "Your price can drop to ₹X before this business becomes unprofitable."

---

## 22. SUPPLY CHAIN / SINGLE-BUYER RISK FLAG (templated rule, using cached category metadata)

Add a static field per business category to `demo_data.json`'s `business_categories` array:
```json
"single_buyer_dependency_risk": "High" | "Medium" | "Low",
"primary_input_dependency_note": "e.g., Relies on 1-2 fodder suppliers within typical rural supply radius"
```
Just add this note as a flat string per category (e.g., Dairy: "Fodder supply can be seasonal — identify at least 2 suppliers before starting"; Retail: "Diversify stock across multiple wholesalers to avoid single-supplier risk"). Surface it as a plain "⚠️ Supply Chain Note" line on the Analysis screen. This is intentionally simple — a lookup string, not computed logic — and that's fine; be upfront that it's a static advisory note per category, not a live supply-chain analysis.

---

## 23. SHG PEER BENCHMARKING (illustrative/mocked — disclose this honestly)

Add a small `peer_benchmark` object per category to `demo_data.json`:
```json
"peer_benchmark": { "sample_size": 24, "avg_monthly_revenue_after_6_months": 41000, "pct_still_operating_after_1_year": 78 }
```
Present on the Analysis screen as: *"Sample cohort data: N similar businesses averaged ₹X/month after 6 months, with Y% still operating after 1 year."* **Label this clearly as illustrative/sample data in code comments and, ideally, with a small "sample data" tag in the UI** — this is the one feature in the whole build that isn't derived from a real formula, since real peer outcome data doesn't exist without actual users on the platform over time. It's fine to include for narrative richness in the demo, but your team should be ready to say plainly "this uses illustrative sample figures; in production this would populate from real anonymized user outcomes over time" if a judge asks.

---

## 24. FIRST 365 DAYS ROADMAP (templated — fixed milestone template per category)

Add a `roadmap_milestones` array per category to `demo_data.json`, e.g. for Dairy:
```json
"roadmap_milestones": [
  { "month": 1, "milestone": "Complete loan disbursal, purchase cattle/equipment" },
  { "month": 2, "milestone": "Begin operations, establish first buyer relationships" },
  { "month": 3, "milestone": "Moratorium ends — first EMI due, review actual vs. projected costs" },
  { "month": 6, "milestone": "Evaluate pricing against local market, consider secondary revenue stream" },
  { "month": 12, "milestone": "First full-year review — compare to peer benchmark cohort" }
]
```
Write similar (category-appropriate) 5-entry milestone lists for the other 3 categories. Return this as-is from `/api/analyze` and render as a simple vertical timeline component on the Analysis screen.

---

## 25. POST-LOAN MONITORING / NUDGES — "AI CFO" (illustrative mockup screen, not a live feature)

Build this as a **separate "Roadmap / Coming Next" screen**, not wired to any real backend state (since there's no real post-disbursal usage data in a hackathon prototype). Use 2-3 static example nudge cards:
```
"⚠️ Your expenses increased 18% this month vs. your plan — review your input costs."
"📉 Your projected profit margin has narrowed from 22% to 16% — consider the pricing suggestion above."
"💰 Your next EMI of ₹[EMI] is due in 12 days."
```
Frame this screen explicitly in the UI as "Preview: Post-Loan Advisory (Future Roadmap)" — this is honest framing that still shows the judges you've thought past the funding moment, without claiming a live monitoring system exists.

---

## 26. OFFLINE-FIRST / LOW-BANDWIDTH DESIGN NOTE (a real, small implementation choice)

This isn't a screen — it's a design decision to actually implement:
- Configure the React app as a basic PWA (Vite has a `vite-plugin-pwa` package — add it) so it's installable and caches static assets for offline shell-loading.
- Since all reference data (`demo_data.json`, `schemes_reference.json`) is static and small, have the frontend fetch it once on load and cache it in memory/localStorage-free React state (remember: no localStorage per artifact rules if this were inside an Artifact — but this is a real deployed app, not a Claude artifact, so localStorage/IndexedDB caching is fine here) so subsequent interactions don't need repeated network calls.
- Mention this as a deliberate design choice in your pitch: "the reference dataset is small enough to cache client-side, so the core advisory flow keeps working even on an intermittent rural connection — only the Map screen's tile loading needs a live connection."

---

## UPDATE TO SECTION 17 (API ENDPOINTS) — add these

```
GET  /api/schemes                 -> returns schemes_reference.json (for Section 17 comparator)
```
(The other new features in this addendum — Sections 18-24 — should be additional fields merged into the existing `POST /api/analyze` response object, not new endpoints, since they're all computed from the same input. Section 25 needs no endpoint at all, it's static frontend content.)

---

## UPDATE TO SECTION 18 (FRONTEND SCREENS) — add these

- Add a "Compare Schemes" expandable section to the Analysis screen (Section 17 above).
- Add the Working Capital Warning banner, Local Price Intelligence card, Business Health Score radar/bar breakdown, Failure Boundary callout, Supply Chain Note, Peer Benchmark card, and Roadmap Milestones timeline — all onto the existing Analysis screen (it will now be a longer, richer, scrollable screen — consider breaking it into tabbed sections: "Overview," "Financials," "Risks & Roadmap" if it gets visually crowded).
- Add one new standalone screen: "Post-Loan Preview" (Section 25), reachable via a nav link, clearly labeled as a roadmap preview.

---

## UPDATED ACCEPTANCE CRITERIA (add these to v3's Section 20 checklist)

- [ ] Scheme comparison table shows at least 2 alternative schemes alongside the correctly-highlighted primary match.
- [ ] Working Capital Warning banner appears/disappears correctly based on the input numbers.
- [ ] Local Price Intelligence and Failure Boundary calculations correctly skip/degrade gracefully for "Retail / Kirana Store" (null avg_local_price).
- [ ] Business Health Score's 7 sub-factors are all visibly different from each other for at least one test case (not all defaulting to the same number).
- [ ] Supply Chain Note and Peer Benchmark data render correctly per category.
- [ ] Roadmap Milestones timeline shows the correct 5 category-specific entries.
- [ ] Post-Loan Preview screen is clearly labeled as a roadmap/preview, not presented as a live monitoring feature.
