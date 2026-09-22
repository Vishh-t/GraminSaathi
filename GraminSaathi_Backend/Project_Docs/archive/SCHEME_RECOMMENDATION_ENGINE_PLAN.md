# SCHEME RECOMMENDATION ENGINE — Comprehensive Implementation Plan
**Created:** 2026-09-20 | **Status:** PLANNING PHASE | **Project:** GraminSaathi (SIH 2026)

---

## 1. EXECUTIVE SUMMARY

This document captures the complete plan to build a **comprehensive government scheme recommendation engine** for rural/urban micro-entrepreneurs in India, integrating with the existing GraminSaathi backend (Spring Boot 3.2, Java 17) and frontend (React + Vite).

**Goal:** Given a user's profile (location, category, business type, capital, etc.), surface ALL eligible central + state government loan/subsidy schemes, ranked by effective cost to user, with full eligibility transparency, documents required, and application guidance.

**Current State:** 
- Backend has 4 schemes in `schemes_reference.json` (2 are illustrative/placeholder)
- No eligibility filtering exists
- No state-level schemes researched
- No documents/application flow data

**Target State:** 40+ verified central schemes + state schemes for 7 priority states, all with computable eligibility rules (JSONLogic), documents checklist, pros/cons, application steps, and official source citations.

---

## 2. SCHEME DATA ARCHITECTURE

### 2.1 Canonical Scheme Schema (JSON)

```json
{
  "scheme_id": "central-msme-clcss-2024",
  "name": "Credit Linked Capital Subsidy Scheme (CLCSS)",
  "level": "central",
  "state": null,
  "ministry": "Ministry of MSME",
  "implementing_agency": "DCMSME / Nodal Banks (SIDBI, NABARD, SBI, BoB, etc.)",
  "type": "subsidy",
  "sub_type": "capital_subsidy_technology_upgradation",
  "benefit": {
    "subsidy_pct": 15,
    "max_subsidy_amount": 1500000,
    "on_loan_upto": 10000000,
    "special_category_enhancement": {
      "sc_st_women_ner_hill_aspirational": { "subsidy_pct": 15, "note": "Relaxed eligibility for any technology upgradation" }
    }
  },
  "interest_rate": null,
  "effective_interest_rate_note": "Market rate; subsidy reduces effective cost of capital",
  "loan_amount_range": { "min": 0, "max": 10000000 },
  "tenure_years": null,
  "moratorium_months": null,
  "eligibility_rules": {
    "and": [
      { "in": [{ "var": "applicant.enterprise_type" }, ["micro", "small"]] },
      { "in": [{ "var": "applicant.udyam_status" }, ["registered"]] },
      { "in": [{ "var": "applicant.sector" }, ["manufacturing", "services"]] },
      { "or": [
        { "in": [{ "var": "applicant.category" }, ["general", "obc", "sc", "st", "women"]] },
        { "==": [{ "var": "applicant.is_special_category" }, true] }
      ]}
    ]
  },
  "eligibility_human": "Micro & Small Enterprises with Udyam Registration in manufacturing/services. Special benefits for SC/ST/Women/NER/Hill States/Aspirational Districts — subsidy applies to any technology upgradation (not just approved list).",
  "target_applicant": ["individual", "partnership", "company", "llp", "cooperative"],
  "target_sector": ["manufacturing", "services"],
  "rural_urban": "both",
  "greenfield_only": false,
  "existing_unit_allowed": true,
  "min_age": 18,
  "max_age": null,
  "documents_required": [
    { "name": "Udyam Registration Certificate", "mandatory": true, "source": "udyamregistration.gov.in" },
    { "name": "PAN Card", "mandatory": true },
    { "name": "Aadhaar Card", "mandatory": true },
    { "name": "Caste Certificate", "mandatory_if": "category_in_sc_st", "source": "state revenue portal" },
    { "name": "Project Report / DPR", "mandatory": true, "template_link": "https://dcmsme.gov.in/project-profiles" },
    { "name": "Bank Sanction Letter (in-principle)", "mandatory": true },
    { "name": "Quotations for Plant & Machinery", "mandatory": true },
    { "name": "Land/Lease Documents", "mandatory": true }
  ],
  "application_channel": ["bank_branch", "online_portal", "nodal_agency"],
  "portal_url": "https://dcmsme.gov.in/CLCS_TUS_Scheme/CLCS/Scheme_Guidelines.aspx",
  "application_form_url": "https://dcmsme.gov.in/CLCS_TUS_Scheme/CLCS/Scheme_Guidelines.aspx",
  "nodal_agencies": ["SIDBI", "NABARD", "SBI", "Bank of Baroda", "PNB", "Bank of India", "TIIC", "Andhra Bank", "Corporation Bank", "Canara Bank", "Indian Bank"],
  "status": "open",
  "last_verified": "2026-09-18",
  "source_urls": [
    "https://dcmsme.gov.in/CLCS_TUS_Scheme/CLCS/Scheme_Guidelines.aspx",
    "https://dcmsme.gov.in/CLCS-TUS-Revised-Guidelines.pdf"
  ],
  "flags": [],
  "pros": [
    "Direct 15% capital subsidy reduces project cost",
    "Available through 11 nodal banks — wide access",
    "Special category entrepreneurs get relaxed technology list"
  ],
  "cons": [
    "Only for technology upgradation (not new unit setup)",
    "Requires bank sanction first — two-step process",
    "Second-hand/fabricated machinery not eligible"
  ],
  "application_steps": [
    "Prepare project report with machinery quotations",
    "Approach bank branch for in-principle sanction",
    "Bank applies for subsidy via online MIS portal",
    "Nodal agency verifies and releases subsidy to bank",
    "Bank adjusts subsidy against loan outstanding"
  ],
  "processing_time_days": 30,
  "contact_info": {
    "helpline": "1800-115-565 (MSME Champions Portal)",
    "email": "clcs-tus@dcmsme.gov.in"
  }
}
```

### 2.2 User Profile Schema (Input to Eligibility Engine)

```json
{
  "applicant": {
    "name": "Ramesh Kumar",
    "age": 32,
    "gender": "male",
    "category": "obc",
    "is_woman": false,
    "is_sc": false,
    "is_st": false,
    "is_minority": false,
    "is_transgender": false,
    "is_pwd": false,
    "state": "Uttar Pradesh",
    "district": "Gorakhpur",
    "block": "Campierganj",
    "village": "Bhiti",
    "pincode": "273158",
    "rural_urban": "rural",
    "education": "10th_pass",
    "occupation": "farmer",
    "annual_income": 120000,
    "enterprise_type": "micro",
    "udyam_status": "not_registered",
    "udyam_number": null,
    "has_business_pan": false,
    "sector": "manufacturing",
    "sub_sector": "food_processing",
    "business_stage": "greenfield",
    "project_cost_estimate": 800000,
    "margin_money_available": 80000,
    "is_shg_member": false,
    "shg_name": null,
    "is_fpo_member": false,
    "fpo_name": null,
    "is_artisan": false,
    "artisan_type": null,
    "is_street_vendor": false,
    "has_kcc": false,
    "land_holding_acres": 2.5
  }
}
```

---

## 3. CENTRAL SCHEMES — VERIFIED INVENTORY (36+ Schemes)

Based on research from official portals (NABARD, DCMSME, Startup India, NSIC, AIM, MoFPI, MNRE, DAHD, Fisheries, MoRD) with 2024-26 circulars:

| # | Scheme ID | Name | Type | Max Benefit | Target |
|---|-----------|------|------|-------------|--------|
| 1 | central-msme-clcss | CLCSS (Technology Upgradation) | subsidy | ₹15L (15%) | MSEs, mfg/services |
| 2 | central-msme-cgtmse | CGTMSE Credit Guarantee | guarantee | ₹10Cr cover | All MSEs, collateral-free |
| 3 | central-msme-innovative-incubation | MSME Innovative - Incubation | grant | Varies | Host institutions, MSMEs |
| 4 | central-msme-innovative-design | MSME Innovative - Design | grant | Varies | MSMEs, design houses |
| 5 | central-msme-innovative-ipr | MSME Innovative - IPR (SCIP) | grant | Varies | MSMEs, startups |
| 6 | central-msme-digital | Digital MSME | subsidy | Varies | MSEs adopting Industry 4.0 |
| 7 | central-msme-lean | MSME Competitive (LEAN) | grant | Consultant fees | MSEs |
| 8 | central-msme-zed | ZED Certification | subsidy | Varies | MSEs |
| 9 | central-msme-gift | MSE-GIFT (Tech Acquisition) | subsidy | Varies | MSEs |
| 10 | central-msme-spice | MSE-SPICE (Innovation) | grant | Varies | Innovation-driven MSMEs |
| 11 | central-msme-pms | Procurement & Marketing Support | reimbursement | Varies | MSEs with Udyam |
| 12 | central-msme-esdp | ESDP (Skill/EDP Training) | training | Free/low-cost | Aspiring/existing entrepreneurs |
| 13 | central-msme-cdp | Cluster Development (MSE-CDP) | grant | ₹25Cr (CFC) | Industrial/rural clusters |
| 14 | central-msme-ner | NER & Sikkim Promotion | subsidy | Enhanced | NE States + Sikkim |
| 15 | central-msme-sri-fund | SRI Fund (Fund of Funds) | equity/debt | Via AIFs | Scaling MSMEs |
| 16 | central-agri-acabc | ACABC (Agri Clinics/Business) | subsidy+training | 44% (SC/ST/W) | Agri graduates |
| 17 | central-agri-iss | Interest Subvention (KCC) | interest_sub | 7% effective | KCC holders |
| 18 | central-agri-ami | AMI (Agri Marketing Infra) | subsidy | 33.33% | Agri infra, FPOs |
| 19 | central-msme-sclcss | SCLCSS (SC/ST Capital Subsidy) | subsidy | Varies | SC/ST entrepreneurs |
| 20 | central-dpiit-sisfs | SISFS (Startup Seed Fund) | grant/debt | ₹50L | DPIIT startups <2 yrs |
| 21 | central-dpiit-ffs2 | Startup Fund of Funds 2.0 | fund_of_funds | Via AIFs | Startup scaling |
| 22 | central-dpiit-cgss | Credit Guarantee for Startups | guarantee | ₹10Cr | DPIIT startups |
| 23 | central-aim-atl | Atal Tinkering Labs | grant | ₹20L | Schools |
| 24 | central-aim-aic | Atal Incubation Centres | grant | ₹10Cr/5yrs | Incubators |
| 25 | central-aim-acic | Atal Community Innovation Centres | grant | ₹2.5Cr | Underserved regions |
| 26 | central-aim-anich | Atal New India Challenge | grant | ₹1Cr | Product development |
| 27 | central-nsic-raw-material | NSIC Raw Material Assistance | credit | Varies | MSEs |
| 28 | central-nsic-bill-discounting | NSIC Bill Discounting | credit | Varies | MSEs |
| 29 | central-nsic-sprs | NSIC Single Point Registration | market_access | Govt procurement | MSEs |
| 30 | central-nsic-nssh | National SC-ST Hub | capacity+market | Varies | SC/ST entrepreneurs |
| 31 | central-mofpi-pmfme | PM FME (Food Processing) | subsidy | ₹10L (35%) | Food micro units, SHGs |
| 32 | central-mord-daynrlm-svep | DAY-NRLM SVEP | grant+credit | Varies | SHG entrepreneurs |
| 33 | central-msme-pm-vishwakarma | PM Vishwakarma | credit+skill | ₹2L @5% | 18 traditional crafts |
| 34 | central-mnre-pmkusum | PM KUSUM (Solar) | subsidy | 60% total | Farmers, FPOs |
| 35 | central-agri-smam | SMAM (Farm Mechanization) | subsidy | 50% (CHC ₹60L) | Farmers, CHCs |
| 36 | central-fisheries-pmmsy | PMMSY (Fisheries) | subsidy | 60% | Fishers, FPOs |

**Flags to verify:**
- NLM-EDEG (Livestock): Marked closed on NABARD — verify with DAHD
- DEDS (Dairy): Marked closed on NABARD — verify if subsumed
- Interest Subvention: Rate changed FY22-24 — verify current FY

---

## 4. STATE SCHEMES — PRIORITY STATES (7 by Rural Population)

| Rank | State | Rural Population (2011) | Key Portals to Scrape |
|------|-------|------------------------|----------------------|
| 1 | **Uttar Pradesh** | 155M | `up.gov.in`, `diupmsme.upsdc.gov.in`, `ruraldevelopment.up.gov.in`, `startinup.gov.in` |
| 2 | **Bihar** | 92M | `state.bihar.gov.in`, `industries.bihar.gov.in`, `rural.bihar.gov.in`, `jeevika.org.in` |
| 3 | **Maharashtra** | 61M | `mahaindustry.gov.in`, `msme.maharashtra.gov.in`, `rural.maharashtra.gov.in`, `startupmaharashtra.org` |
| 4 | **West Bengal** | 62M | `wb.gov.in`, `msmewb.org`, `pandrd.wb.gov.in`, `wbsdms.gov.in` |
| 5 | **Madhya Pradesh** | 52M | `mp.gov.in`, `msme.mp.gov.in`, `rural.mp.gov.in`, `mpstartup.gov.in` |
| 6 | **Rajasthan** | 51M | `rajasthan.gov.in`, `industries.rajasthan.gov.in`, `ruraldevelopment.rajasthan.gov.in`, `startup.rajasthan.gov.in` |
| 7 | **Odisha** | 34M | `odisha.gov.in`, `msme.odisha.gov.in`, `mission.shakti.odisha.gov.in`, `startup.odisha.gov.in` |

**Per-state target:** 8-12 schemes each (livelihood, subsidy, interest subvention, skill, market access) → ~60-80 state schemes total.

---

## 5. ELIGIBILITY ENGINE DESIGN

### 5.1 Rule Format: JSONLogic (Evaluatable in Java + JavaScript)

```json
{
  "and": [
    { ">=": [{ "var": "applicant.age" }, 18] },
    { "in": [{ "var": "applicant.state" }, ["UP", "Bihar", "MP", "WB", "Rajasthan", "Odisha", "Maharashtra"]] },
    { "or": [
      { "==": [{ "var": "applicant.rural_urban" }, "rural"] },
      { "==": [{ "var": "applicant.rural_urban" }, "both"] }
    ]},
    { "in": [{ "var": "applicant.category" }, ["sc", "st", "obc", "general", "women"]] }
  ]
}
```

### 5.2 Java Evaluation Library Options

| Library | Pros | Cons |
|---------|------|------|
| **jsonlogic-java** | Pure Java, no JS runtime needed | Limited community |
| **GraalVM Polyglot (JS)** | Full JS compatibility, fast | Adds GraalVM dependency |
| **Custom DSL** | Full control, no external dep | More dev effort |

**Recommendation:** Start with **jsonlogic-java** (lightweight, Maven Central). Fallback to GraalVM if complex expressions needed.

### 5.3 Engine API

```java
public interface EligibilityEngine {
    List<SchemeMatch> evaluate(UserProfile profile, List<Scheme> allSchemes);
}

public record SchemeMatch(
    Scheme scheme,
    boolean eligible,
    List<String> failedRules,
    double effectiveCostScore,  // for ranking
    Map<String, Object> computedValues
) {}
```

---

## 6. DATA PIPELINE & FRESHNESS STRATEGY

### 6.1 One-Time Extraction (Current Phase)
- **Subagent 1:** Central schemes completer — fills gaps in 36 schemes (documents, steps, pros/cons, bank nuances)
- **Subagent 2:** State scheme harvester — parallel per state (7 agents), extracts from official portals
- **Subagent 3:** API/Feed mapper — documents RSS, scrape targets, partner APIs for freshness

### 6.2 Ongoing Freshness (Lightweight)
| Source | Mechanism | Frequency |
|--------|-----------|-----------|
| PIB Press Releases | RSS feed (`pib.gov.in/rss`) | Real-time |
| Ministry websites | Quarterly manual audit | Quarterly |
| `myscheme.gov.in` | Scrape on change detection | Monthly |
| State portals | Quarterly manual audit | Quarterly |
| User feedback | In-app "Report outdated" button | Continuous |

### 6.3 Storage
- **PostgreSQL** with JSONB for `eligibility_rules`, `documents_required`, `benefit`
- **Versioning:** `scheme_versions` table for audit trail
- **Cache:** Redis for evaluated eligibility results (TTL: 1 hour)

---

## 7. INTEGRATION WITH EXISTING GRAMINSAATHI

### 7.1 New Backend Components

```
com.graminsaathi
├── scheme/
│   ├── model/Scheme.java              // JPA @Entity with JSONB fields
│   ├── repository/SchemeRepository.java
│   ├── service/
│   │   ├── SchemeDataLoader.java       // Loads JSONL at startup
│   │   ├── EligibilityEngine.java      // JSONLogic evaluation
│   │   ├── SchemeRankingService.java   // Effective cost ranking
│   │   └── SchemeRefreshService.java   // Scheduled freshness checks
│   ├── controller/SchemeController.java
│   └── dto/SchemeRecommendationResponse.java
```

### 7.2 New API Endpoints

```
GET  /api/schemes                    -> All schemes (with filters: level, state, type)
GET  /api/schemes/{schemeId}         -> Full scheme detail
POST /api/schemes/recommend          -> Eligibility eval + ranked recommendations
GET  /api/schemes/categories         -> Distinct sectors, applicant types, etc.
POST /api/schemes/refresh            -> Admin: trigger re-scrape (secured)
```

### 7.3 Frontend Integration Points

| Screen | New Component | Data Source |
|--------|---------------|-------------|
| Dashboard/Chat | "🔍 Find Government Schemes" quick-reply | `/api/schemes/recommend` |
| Discovery | Scheme badges on business cards | `/api/schemes/recommend` (lightweight) |
| Analysis | **New "Government Schemes" tab** | Full scheme detail + comparison |
| Reports | Scheme info included in PDF | Embedded in report JSON |

---

## 8. IMPLEMENTATION PHASES

### Phase 1: Data Foundation (Week 1-2) — **PARALLEL SUBAGENTS**
- [ ] Finalize JSON schema (this doc)
- [ ] Subagent 1: Complete central schemes (36) → `central_schemes.jsonl`
- [ ] Subagent 2: Harvest 7 state schemes → `state_schemes_{UP,Bihar,...}.jsonl`
- [ ] Subagent 3: Map freshness sources → `freshness_map.json`
- [ ] Merge → `all_schemes.jsonl` (single source of truth)

### Phase 2: Backend Engine (Week 2-3)
- [ ] JPA Entity `Scheme` with JSONB columns
- [ ] `SchemeDataLoader` — loads JSONL at startup into DB
- [ ] `EligibilityEngine` with jsonlogic-java
- [ ] `SchemeRankingService` — effective cost computation
- [ ] REST endpoints + DTOs
- [ ] Unit tests for eligibility rules (5+ schemes x 10 profiles)

### Phase 3: Frontend Integration (Week 3-4)
- [ ] SchemeRecommendationService (React)
- [ ] "Find Schemes" chat flow
- [ ] Scheme Detail Modal/Tab on Analysis screen
- [ ] Comparison Table component (reuse existing)
- [ ] Bilingual labels (EN/HI) for scheme fields

### Phase 4: Polish & Demo Prep (Week 4-5)
- [ ] Freshness audit of top 20 schemes
- [ ] Demo profiles for 5 personas (SC woman farmer, OBC youth, General artisan, ST SHG member, Minority street vendor)
- [ ] PDF report includes scheme recommendations
- [ ] Offline caching for scheme data (PWA)

---

## 9. DELIVERABLES FROM THIS PLAN

| File | Location | Description |
|------|----------|-------------|
| `SCHEME_RECOMMENDATION_ENGINE_PLAN.md` | `Project_Docs/` | This document |
| `scheme_schema.json` | `src/main/resources/schema/` | JSON Schema for validation |
| `central_schemes.jsonl` | `src/main/resources/data/schemes/` | 36+ central schemes |
| `state_schemes_up.jsonl` | `src/main/resources/data/schemes/` | UP state schemes |
| `state_schemes_bihar.jsonl` | `src/main/resources/data/schemes/` | Bihar state schemes |
| `state_schemes_maharashtra.jsonl` | `src/main/resources/data/schemes/` | Maharashtra state schemes |
| `state_schemes_wb.jsonl` | `src/main/resources/data/schemes/` | West Bengal state schemes |
| `state_schemes_mp.jsonl` | `src/main/resources/data/schemes/` | Madhya Pradesh state schemes |
| `state_schemes_rajasthan.jsonl` | `src/main/resources/data/schemes/` | Rajasthan state schemes |
| `state_schemes_odisha.jsonl` | `src/main/resources/data/schemes/` | Odisha state schemes |
| `freshness_map.json` | `src/main/resources/data/schemes/` | RSS/scrape/partner API map |
| `user_profile_schema.json` | `src/main/resources/schema/` | User input schema |

---

## 10. OPEN DECISIONS NEEDING YOUR INPUT

| # | Decision | Options | Recommendation |
|---|----------|---------|----------------|
| 1 | **JSONLogic library** | jsonlogic-java / GraalVM / Custom DSL | jsonlogic-java (start simple) |
| 2 | **Scheme versioning** | Full history table / Single current + last_verified | Full history (audit trail for judges) |
| 3 | **State scheme scope** | All 7 states now / 3 states now + 4 later | All 7 in parallel (subagents) |
| 4 | **Ranking algorithm** | Effective cost only / Multi-factor (cost + speed + success rate) | Effective cost first, add factors later |
| 5 | **Frontend scheme UI** | New tab on Analysis / Separate "Schemes" page | New tab (keeps flow in one screen) |
| 6 | **Hindi labels** | Machine translate / Manual curate | Manual curate for top 20, MT for rest |
| 7 | **Bank-specific nuances** | Model per-bank / Flag in notes only | Flag in `application_notes` field only |

---

## 11. NEXT ACTIONS (Immediate)

1. **You confirm:** Priority states (7 listed above OK?), JSONLogic library choice, ranking approach
2. **I execute:** Spin up 3 subagents for data extraction (central + 7 states + freshness map)
3. **You review:** Delivered JSONL files for accuracy
4. **We integrate:** Backend engine + frontend in parallel

---

## 12. REFERENCE: EXISTING PROJECT FILES TO UPDATE

| File | Change |
|------|--------|
| `pom.xml` | Add `jsonlogic-java` dependency |
| `application.properties` | Add Redis config for eligibility cache |
| `src/main/resources/data/demo_data.json` | No change (village/business data) |
| `src/main/resources/data/schemes_reference.json` | **Replace** with `all_schemes.jsonl` loaded via `SchemeDataLoader` |
| `AnalysisController.java` | Add `/api/schemes/recommend` call |
| `AnalysisPage.jsx` | Add "Government Schemes" tab |
| `dashboard` quick-replies | Add "Find Government Schemes" |

---

**End of Plan Document** — Ready for your review and go-ahead.