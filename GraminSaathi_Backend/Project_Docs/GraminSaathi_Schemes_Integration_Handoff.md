# GraminSaathi — Government Schemes Knowledge Base Integration
### Handoff document — read this first in any new chat about this feature
### Then read `SCHEMES_STATUS_AND_NEXT_STEPS.md` in this same folder — it has the current status (this doc is the original design, that one tracks what's actually built) and, as of 2026-09-22, section 6: the roadmap for the full schemes-browse feature (all types, filterable, 5-7 best + see more, click-through detail) that supersedes sections 5-8 below as the active plan.

---

## 1. Project context

- **Project**: GraminSaathi — a Smart India Hackathon 2026 (SIH) submission by Vishesh.
- **Purpose**: rural business discovery and analysis app — includes a voice assistant, business opportunity scoring, maps, simulation, reports, and an AI agent committee panel.
- **Stack**: Spring Boot backend (`GraminSaathi_Backend/`) + React/Vite frontend (`sih_frontend/`). Tailwind CSS, Lucide React icons. Project root: `E:\computer_science\SIH\`.
- **This feature**: adding a government schemes matching + benefit calculation engine — given a user's profile (age, state, category, business type, etc.), tell them which of ~230 real government schemes (central + state) they're eligible for, and compute the actual ₹ benefit / interest relief / grant amount for each.
- **Timeline pressure**: 3–4 days total, alongside other SIH work. Everything below is written to minimize manual effort — automation over hand-review wherever the automation is reliable enough to trust.

---

## 2. The data source

**Original file**: `All_Schemes.json` — a flat JSON array of **230 scheme objects** (mix of central-government and state-government schemes across all Indian states).

### Top-level fields on every scheme object
```
scheme_id, name, level (central|state), state (null for central),
ministry, implementing_agency, type, sub_type,
benefit {...}, interest_rate, effective_interest_rate_note,
loan_amount_range {min,max}, tenure_years, moratorium_months,
eligibility_rules {...JsonLogic...}, eligibility_human,
target_applicant[], target_sector[], rural_urban, greenfield_only,
existing_unit_allowed, min_age, max_age,
documents_required[], application_channel[], application_steps,
portal_url, application_form_url, nodal_agencies[],
status, last_verified, source_urls[], flags[], pros[], cons[],
contact_info, processing_time_days
```

### `eligibility_rules` — this is JsonLogic, not custom JSON
Example:
```json
{"and": [
  {">=": [{"var": "applicant.age"}, 18]},
  {"==": [{"var": "applicant.business_stage"}, "greenfield"]}
]}
```
This is evaluated against an `applicant` object at runtime. **You need a JsonLogic library** (e.g. `json-logic-java`) — do not hand-write a parser for this, the rule shapes vary too much (`and`, `or`, `==`, `>=`, `<=`, `in`).

### The `applicant.*` variable vocabulary (everything eligibility_rules can reference)
~45 variables, all under the `applicant.` namespace. Grouped by type:

**Booleans** (true/false):
`is_sc, is_st, is_woman, is_pwd, is_bpl, is_shg_member, is_fpo_member, is_jlg_member, is_artisan, is_street_vendor, is_landless, is_returned_emigrant, is_manual_scavenger, is_safai_karamchari, is_registered_unemployed, is_registered_labour, is_income_tax_payer, is_incubated, is_sc_majority_owned, dpiit_recognized, has_electricity_connection, has_kalia_bsky_card, holds_driving_license, owns_indigenous_cow, owns_loom, owns_vehicle, completed_dairy_training`

**Numbers**: `age, annual_family_income, annual_turnover, business_age_months, land_holding_acres, overseas_service_years`

**Enums (with the exact string values actually used in the dataset)**:
| Variable | Allowed values |
|---|---|
| `category` | general, obc, sc, st, bc, mbc, minority, pwd, dnc, maratha_ebc |
| `business_stage` | greenfield, early_stage, existing |
| `enterprise_type` | individual, proprietorship, partnership, llp, company, cooperative, shg, shg_federation, fpo, jlg, ngo, section_8_company, startup, micro, small, medium, msme, artisan_guild, state_dairy_federation |
| `education` | 4th_pass, 5th_pass, 7th_pass, 8th_pass, 10th_pass, 12th_pass, iti, diploma, graduate, vocational |
| `occupation` | farmer, tenant_farmer, agricultural_labourer, artisan, traditional_artisan, street_vendor, hawker, vegetable_seller, auto_driver, taxi_driver, cab_driver, crew_member, barber, tailor, washerman, cattle_rearer, fisherman, laborer, unemployed |
| `sector` | manufacturing, services, trading, agri_allied, technology, tourism, green_energy, logistics |
| `sub_sector` | food_processing, dairy, fisheries, poultry, livestock, goat, sheep, piggery, aquaculture, horticulture, forestry, handicrafts, furniture, warehousing, coir_processing, animal_feed, fodder, meat_processing, marine_logistics, post_harvest_management, community_farming_assets |
| `rural_urban` | rural, urban |
| `artisan_type` | handloom, powerloom, weaver |
| `sub_caste` | chambhar, dhor, holiya, mochi (very narrow, rarely used) |
| `udyam_status` | registered |
| `state` | any of the 28 Indian states (used for state-level scheme matching) |
| `parent_occupation` | farmer |

This vocabulary is the contract for your `ApplicantProfile` — every field the frontend form needs to eventually be able to supply, one way or another.

### `benefit` object — same 4 keys on every scheme, meaning differs by scheme
```json
{
  "subsidy_pct": <number|null>,
  "max_subsidy_amount": <number|null>,
  "on_loan_upto": <number|null>,
  "special_category_enhancement": <object|null>
}
```
**Critical realization from this project's analysis**: the shape is uniform, but the *meaning* of `subsidy_pct` differs completely by scheme — 85% on a loan-guarantee scheme is bank collateral cover, not cash to the user; 100% on an interest-subvention scheme means 0% interest, not a rupee figure. **You cannot write one generic formula without knowing which kind of benefit it is.** This is what section 4 solves.

`special_category_enhancement`, when present, looks like:
```json
"special_category_enhancement": {
  "sc_st_women_obc_minorities_rural": {
    "subsidy_pct": 35,
    "note": "General category gets 15% urban / 25% rural. SC/ST/OBC/Women/Minorities get 25% urban / 35% rural."
  }
}
```
The key name is a **human-readable label only** — there was originally no machine-checkable condition telling code when to apply it. This has now been fixed (see section 4).

---

## 3. Decisions made so far

### 3.1 No separate database / knowledge base needed
230 records is small. Decision: load the JSON as a `List<Scheme>` in a Spring `@Service` at startup (`@PostConstruct`), query it in-memory with Java streams. No Postgres/Mongo needed for the SIH demo. Revisit only if (a) schemes need to be edited without a redeploy, or (b) the dataset grows to thousands of rows — neither applies here.

### 3.2 `benefit.calculation_type` — added to solve the "same 4 fields, different meaning" problem
Every scheme in the dataset now has an added field `benefit.calculation_type`, one of:

| calculation_type | Meaning | Formula |
|---|---|---|
| `flat_grant_or_cap` | Fixed cash amount | `benefit = max_subsidy_amount` |
| `pct_of_cost_capped` | % of project/certification/input cost, capped (covers flat grants too — when `subsidy_pct` is 100 it collapses to a flat cap) | `benefit = min(subsidy_pct% × actual_cost, max_subsidy_amount)`; if `max_subsidy_amount` is null, uncapped |
| `pct_of_loan_capped` | Subsidy as % of a loan, capped | `benefit = min(subsidy_pct% × loan_amount, max_subsidy_amount)` |
| `pct_of_loan_uncapped` | % relates to a loan but no cap is given | same formula, no cap applied |
| `interest_rate_reduction` | Not cash — reduces effective interest rate | `effective_rate = interest_rate − subsidy_pct` (or shown as "0% interest up to ₹X") |
| `guarantee_cover_pct` | Not cash to the applicant — % of loan the government guarantees to the bank | display as "X% collateral-free cover," never sum into a "total ₹ benefit" figure |
| `per_unit_or_in_kind` | Per-acre / per-animal / in-kind toolkit, needs a quantity from the applicant | `benefit = max_subsidy_amount × applicant_quantity` (quantity var depends on scheme, e.g. `land_holding_acres`) |
| `equity_or_uncapped` | Equity/convertible-debt style funds with no simple % or cap | no computed ₹ figure — show `loan_amount_range` and `effective_interest_rate_note` as-is |
| `text_only_see_note` | No computable benefit fields at all | show `effective_interest_rate_note` / `eligibility_human` verbatim |
| `composite` | Genuinely bundles 2+ of the above (only triggered when `type` itself contains `+`, e.g. `grant+credit` — not guessed from `sub_type` naming) | see `benefit.calculation_components`, an array of the above types — compute and display each component separately, don't sum blindly |

Confidence tag also added: `benefit.calculation_type_confidence` = `high` / `medium`. Current distribution: **211 high, 19 medium, 0 low** — every scheme resolves to a concrete, usable type; nothing needs manual classification.

### 3.3 `special_category_enhancement.<key>.condition` — auto-generated JsonLogic
Each enhancement block now has a `condition` field in the same JsonLogic format as `eligibility_rules`, generated by keyword-matching the enhancement's key name + note text against the known `applicant.*` vocabulary (demographic terms, geography including NER/hill states, SHG/FPO/artisan flags, business stage, sector/sub-sector, land holding size, group-vs-individual enterprise type, age). Tagged with `condition_confidence` = `high` / `medium` / `default_true`.

Current distribution: **73 high, 24 medium, 60 `default_true`**. The `default_true` ones aren't gaps — most of them (`toolkit_incentive`, `digital_cashback`, `credit_guarantee`, generic `interest_subvention` tiers, etc.) genuinely aren't demographic bonuses at all, they're just additional components of the base scheme with no extra eligibility gate. Treating them as always-on once the base scheme is eligible is the correct behavior, not a placeholder.

**Usage**: when a scheme is eligible, also run each `special_category_enhancement.<key>.condition` through JsonLogic against the same applicant object (a bare `true` always passes). If true, use that block's `subsidy_pct` (and its note, for display) instead of the base `benefit.subsidy_pct`.

**No manual review step remains.** The earlier version of this doc mentioned a 104-item review report — that was a heuristic limitation, since fixed. `enrichment_review_report.json` still exists but is now just an informational list of the 60 `default_true` calls, not an action item.

---

## 4. Files produced so far (all in this conversation's outputs — re-attach if starting a new chat)

| File | What it is |
|---|---|
| `all_schemes_enriched.json` | The dataset to actually use — original 230 schemes + `calculation_type` + `calculation_components` (composites) + `condition` on every `special_category_enhancement`. **This replaces the raw upload as the backend's data source.** |
| `enrichment_review_report.json` | 104 flagged items (low/medium-confidence `calculation_type` or `special_category_enhancement.condition` guesses) — optional spot-check list, not a blocker. |

If starting a fresh chat, **re-upload `all_schemes_enriched.json`** (not the original file) so the new chat inherits the enrichment already done — don't redo the classification work.

---

## 5. Remaining implementation checklist (backend — Spring Boot)

1. **Dependency**: add `json-logic-java` (or equivalent) to `pom.xml`/`build.gradle`.
2. **`ApplicantProfile`** — a Java record/class with all ~45 `applicant.*` fields from section 2's vocabulary table (correct types: Boolean, Integer/Double, String enums).
3. **`SchemeDataService`** — `@PostConstruct` loads `all_schemes_enriched.json` from `src/main/resources/data/` into a `List<Scheme>` (deserialize `benefit`/`eligibility_rules` as `JsonNode` since their shape varies).
4. **`BenefitCalculator`** — one method, `switch` on `benefit.calculation_type` implementing the formulas from section 3.2 (9 types, 2 of which — `equity_or_uncapped`, `text_only_see_note` — just pass through text/range fields instead of computing a number). For `composite`, loop `calculation_components` and return a list of sub-results, not one number.
5. **`SchemeMatchingService.findEligible(ApplicantProfile)`**:
   - Pre-filter cheaply: `state == null (central) OR state == applicant.state`, `status == "open"`, optionally `rural_urban`/`target_sector`.
   - Run `eligibility_rules` via JsonLogic on the narrowed set.
   - For each match, check every `special_category_enhancement.<key>.condition`; if any true, apply that tier instead of the base rate.
   - Run `BenefitCalculator` on the resolved rate.
   - Return `{scheme, applies_enhancement, computed_benefit, calculation_type}` per match.
6. **API**: `POST /api/schemes/match` — body is a (partial) `ApplicantProfile`, response is the list above. Also useful: `GET /api/schemes/needs-review` returning the 104 flagged scheme IDs, for your own dev use, not end users.

## 6. Remaining implementation checklist (frontend — React)

1. **Progressive intake form** — don't ask for all 45 fields upfront. Start with baseline fields (age, state, category, sector, business stage). After a first `/match` call, use each unmatched-but-close scheme's missing fields to decide what to ask next (you may need the backend to return `missing_fields: []` per scheme for this to work well — not yet built, add if time allows; otherwise a single well-chosen form covering the most common ~15 fields is an acceptable fallback for the demo).
2. **Results display**:
   - `flat_grant_or_cap` / `pct_of_loan_capped` / `per_unit_or_in_kind` → show as a ₹ figure.
   - `interest_rate_reduction` → show as "effective interest rate: X%" not a ₹ figure.
   - `guarantee_cover_pct` → show as "X% collateral-free loan cover," explicitly not counted in any "total benefit" sum.
   - `composite` → show each component on its own line.
   - When `applies_enhancement` is true, show the enhancement's `note` text so the user understands why they got a better rate.

## 7. Open questions / things to decide if time allows (not blockers)

- Whether to spend any time manually reviewing the 104-item report, or ship as-is (recommendation: ship as-is, come back only if a demo scheme happens to be one of the flagged ones).
- Whether `pct_of_loan_uncapped_or_rate` schemes need per-scheme handling or can share one formula (currently ambiguous by design — check `effective_interest_rate_note` per scheme if one shows up in a demo).
- Whether the progressive form or a single static form is used for the demo, given the time budget.

## 8. Suggested order of work for the remaining 3–4 days

1. Backend: `ApplicantProfile` + `SchemeDataService` loading the enriched JSON (fast, mechanical).
2. Backend: `BenefitCalculator` for the 3 most common types first (`flat_grant_or_cap`, `pct_of_loan_capped`, `interest_rate_reduction` — these cover the majority of schemes) — ship the rest (`guarantee_cover_pct`, `per_unit_or_in_kind`, `composite`) once the core path works end-to-end.
3. Backend: `SchemeMatchingService` + `/api/schemes/match` endpoint, test with 2–3 known profiles (e.g. an SC applicant from a rural area vs a general-category urban applicant) to sanity-check eligibility filtering and enhancement application.
4. Frontend: static intake form (skip progressive form if time is tight) → call `/match` → results list with the calculation-type-aware display rules from section 6.
5. Only if time remains: progressive form, `needs-review` review pass, richer result cards (documents required, application steps, pros/cons already exist in the data and can be shown as-is).
