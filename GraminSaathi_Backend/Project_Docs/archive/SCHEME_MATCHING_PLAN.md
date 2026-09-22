# Scheme Matching — Core Feature Plan

**Goal:** Given a user's business profile (project cost, category, area, sector, applicant type), surface the real government loan/subsidy schemes they're actually eligible for, and rank them by genuine best deal — not just a cost-bracket lookup.

## Current gap (verified from code, 20 Sept 2026)
- `schemes_reference.json` has only 4 schemes. Two are marked in the file itself as `"illustrative figure, verify current rate before real use"` — i.e. not real numbers.
- `SchemesDataLoader.getPrimaryScheme()` just returns the first scheme whose cost range contains the project cost. No eligibility filtering (category, area, sector, applicant type) and no actual "best deal" ranking.
- This is the single most important thing to fix before nationals — it's the product's core promise.

## What "eligibility" needs to capture
Real schemes differ on more than loan size:
- **Category:** general / SC-ST-OBC / women / any
- **Area:** rural / urban / both
- **Sector:** manufacturing / service / trading / agri-allied / any
- **Applicant type:** new entrepreneur / existing unit / street vendor / SHG member
- **Collateral requirement:** collateral-free or not
- Each scheme entry should also carry `source_url` and `last_verified` date, since these terms change and a judge may ask "how current is this."

## Ranking approach (conceptual — not implemented yet)
1. Filter all schemes down to the ones the user is actually eligible for (category + area + sector + applicant type all match).
2. Rank the eligible set by **effective cost to the user**, not raw interest rate — something like `(loan amount × interest rate × tenure) − subsidy value`. A high-subsidy scheme (e.g. PMEGP at 35%) can beat a lower headline rate once the subsidy is netted out.
3. Present the top pick + a comparison table of the rest (the existing `SchemeComparisonTable` component can already render this once the data model supports it).

## Schemes verified so far (central government, sourced 20 Sept 2026)

| Scheme | Amount | Key terms | Eligibility | Source |
|---|---|---|---|---|
| PMEGP | ₹20L (service) / ₹50L (mfg) | 15% subsidy (urban/general) up to 35% (rural/special-category); 10%/5% beneficiary contribution | New units, rural+urban, any sector | kviconline.gov.in/pmegpeportal |
| Mudra – Shishu | up to ₹50,000 | Collateral-free, ~7.3–12% | Very early stage, any sector | mudra.org.in |
| Mudra – Kishor | ₹50,001–5L | Collateral-free | Growing micro-business | mudra.org.in |
| Mudra – Tarun / Tarun Plus | ₹5L–20L | Collateral-free; Tarun Plus needs a prior repaid Mudra loan | Established micro-business | mudra.org.in |
| Stand-Up India | ₹10L–1Cr | New unit financing | SC/ST and/or women entrepreneurs only | standupmitra.in |
| PM SVANidhi | ₹10k → ₹20k → ₹50k (cycles) | 7% interest subsidy on timely repayment, digital-payment cashback | Urban street vendors specifically | pmsvanidhi.mohua.gov.in |
| DAY-NRLM (women SHG) | up to ₹20L, collateral-free | 7% interest on loans up to ₹3L in Category-I districts, +3% subvention on timely repayment (effective ~4%) | Rural women in an SHG only | nabard.org |
| CGTMSE | Guarantee cover up to ₹10Cr (not a standalone loan) | Removes/reduces collateral requirement on an underlying loan (PMEGP, Mudra, term loan) | Any eligible MSE loan | Ministry of MSME / SIDBI |

Best single source for further scheme detail and verification: **https://www.myscheme.gov.in** (official MeitY/NeGD scheme-discovery platform, no public API but consistently structured per-scheme pages).

## Still needed
- **State-specific schemes** — your rural users will often be more eligible for a state livelihood/subsidy scheme than a central one, and none are researched yet.
- Real current interest rates/subsidy percentages double-checked closer to demo day (these change).
- Deciding the exact JSON/DB schema for the eligibility fields above (deferred — code discussion for later).

