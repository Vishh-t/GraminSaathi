# MASTER BUILD BRIEF v3 — "GraminSaathi" Full-Feature Demo Prototype
### (Paste this ENTIRE document into your coding agent. Assumes ZERO prior context. This is the complete, final, self-contained brief — supersedes all earlier versions. Team size: 6 people, so scope is intentionally generous.)

---

## 1. PROJECT CONTEXT

GraminSaathi is an AI-style business advisory and loan-structuring tool for rural, first-time entrepreneurs applying for Indian government concessional loans, modeled on NBCFDC/NSFDC-style schemes:
- Beneficiary contributes **10%** of total project cost as "margin money."
- Government/agency lends the remaining **90%**.
- **Micro Finance Scheme:** project cost ≤ ₹1,40,000 → 6.5% interest/annum → 3-year tenure, incl. 3-month moratorium.
- **Term Loan Scheme:** ₹1,40,000 < project cost ≤ ₹50,00,000 → 8% interest/annum → 7-year tenure, incl. 6-month moratorium.

**Root problem being solved:** rural entrepreneurs pick businesses based on anecdote (not local demand data) and don't understand their real borrowing/repayment capacity, leading to high post-funding failure rates.

**Honesty principle for this build:** every number shown must come from a real formula running on real (even if fixed/cached) data. Where a feature would normally need a live external system (voice AI, RAG, multi-agent LLMs, live geodata), we substitute a genuinely-working simpler mechanism that produces the same category of output — explained per feature below so the team can answer technical questions honestly and confidently.

---

## 2. FULL FEATURE LIST (final — this is the complete build)

| # | Feature | Real vs. substituted |
|---|---|---|
| 1 | Deterministic financial calculator (project cost, loan, scheme routing, EMI) | 100% real |
| 2 | Local Opportunity Score (feasibility scoring) | 100% real, over cached data |
| 3 | Optimal Loan vs Maximum Loan recommendation | 100% real |
| 4 | DSCR / "Can I afford this loan?" indicator | 100% real |
| 5 | Business Survival Simulator + stress-test sliders | 100% real, deterministic 24-month simulation |
| 6 | Business Discovery Engine (rank businesses for a location) | 100% real |
| 7 | Reverse Financial Engineering / Goal-Seek ("I want ₹X/month income") | 100% real, inverse of the calculator |
| 8 | Business Combination Suggestions (secondary revenue stream ideas) | Templated rule-based mapping (e.g., Dairy → Vermicompost) — a lookup table, not AI-generated |
| 9 | Evidence Trail / explainability panel | Real citation of which cached data field fed which output — not live RAG |
| 10 | Multi-Agent "AI Investment Committee" panel (Market/Finance/Risk/Verdict) | Templated text from real thresholds on real numbers — not separate LLM calls |
| 11 | Voice input/output | Real — browser's native Web Speech API (SpeechRecognition + speechSynthesis), no external voice service |
| 12 | WhatsApp-style chat UI shell | Real frontend component calling real backend endpoints |
| 13 | Bilingual UI toggle (English/Hindi labels) | Real — a static i18n JSON dictionary switched client-side (represents the vernacular-access vision without a live translation API) |
| 14 | Local Business Map view | Real map rendering (Leaflet + OpenStreetMap tiles) plotting the cached village + competitor-count data as markers around a center point — competitor pin positions are illustrative/scattered around the village center, not real GPS-surveyed locations; say so if asked |
| 15 | Auto-generated bank-ready DPR (PDF) | 100% real, generated from the real computed object |
| 16 | Login / Signup + saved report history | 100% real, PostgreSQL-backed |

---

## 3. TECH STACK — EXACT SPECIFICATION (use exactly this; do not substitute)

### Backend: Java + Spring Boot

- **Java version:** 17 (LTS)
- **Framework:** Spring Boot 3.2.x
- **Build tool:** Maven

**`pom.xml` — required dependencies (add all of these):**
```xml
<dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.11.5</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.11.5</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.11.5</version><scope>runtime</scope></dependency>
    <dependency><groupId>com.itextpdf</groupId><artifactId>itextpdf</artifactId><version>5.5.13.3</version></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
</dependencies>
```

**Package structure:**
```
com.graminsaathi
├── GraminSaathiApplication.java
├── config/          -> SecurityConfig.java, CorsConfig.java
├── security/        -> JwtUtil.java, JwtAuthFilter.java
├── controller/       -> AuthController, AnalysisController, DiscoveryController,
│                        SimulationController, ReportController, ReferenceDataController
├── service/          -> FinancialCalculatorService, FeasibilityScoreService,
│                        SurvivalSimulatorService, DiscoveryService,
│                        EvidenceService, AgentCommitteeService, PdfGeneratorService,
│                        AuthService
├── repository/       -> UserRepository, SavedReportRepository (Spring Data JPA)
├── model/            -> User.java, SavedReport.java (JPA @Entity classes)
├── dto/               -> request/response DTOs for every endpoint
└── data/              -> DemoDataLoader.java (loads demo_data.json at startup into memory)
```

**`application.properties`:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/graminsaathi
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
server.port=8080
jwt.secret=replace-with-a-long-random-secret-string-for-hackathon-use
jwt.expiration-ms=86400000
```
Create the `graminsaathi` PostgreSQL database manually before first run (`CREATE DATABASE graminsaathi;`), or via Docker (see Section 4).

**Run:** `mvn spring-boot:run` from the backend project root.

---

### Frontend: React

- **Scaffold with Vite:** `npm create vite@latest graminsaathi-frontend -- --template react`
- **Required npm packages:**
```
npm install react-router-dom axios recharts leaflet react-leaflet
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```
- **`recharts`** → for the Survival Simulator's cumulative cash-flow line chart
- **`leaflet` + `react-leaflet`** → for the Local Business Map view
- **`axios`** → for all API calls to the Spring Boot backend
- **`react-router-dom`** → for page navigation (Login, Dashboard, Discovery, Analysis, Simulator, Map, Reports)

**Folder structure:**
```
src/
├── main.jsx, App.jsx
├── pages/            -> LoginPage.jsx, SignupPage.jsx, DashboardPage.jsx,
│                        DiscoveryPage.jsx, AnalysisPage.jsx, SimulatorPage.jsx,
│                        MapPage.jsx, ReportsPage.jsx
├── components/        -> ChatShell.jsx, VoiceButton.jsx, LanguageToggle.jsx,
│                        OpportunityScoreCard.jsx, LoanComparisonCard.jsx,
│                        DSCRIndicator.jsx, AgentCommitteePanel.jsx, EvidencePanel.jsx,
│                        SurvivalChart.jsx, BusinessMap.jsx
├── services/           -> api.js (axios instance with base URL + JWT header injection)
├── context/             -> AuthContext.jsx (stores JWT + logged-in user)
├── i18n/                 -> en.json, hi.json (flat key-value label dictionaries), i18n.js (simple context/hook to swap dictionaries)
└── App.css / tailwind config
```

**`.env` (frontend):**
```
VITE_API_BASE_URL=http://localhost:8080/api
```

**Run:** `npm run dev` from the frontend project root.

---

### CORS
Configure Spring Boot's `CorsConfig` to allow requests from `http://localhost:5173` (Vite's default dev port) to all `/api/**` endpoints, all methods, with credentials allowed for the JWT-bearing requests.

---

### Optional: Docker Compose for PostgreSQL (recommended so all 6 team members run an identical DB locally)
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: graminsaathi
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
volumes:
  pgdata:
```
Run with `docker compose up -d` — this alone solves "works on my machine" database problems across a 6-person team.

---

## 4. HARDCODED REFERENCE DATA (`demo_data.json`, loaded once at backend startup)

```json
{
  "villages": [
    {
      "village_name": "Ghoti", "block": "Igatpuri", "district": "Nashik", "state": "Maharashtra",
      "latitude": 19.7515, "longitude": 73.6197,
      "population_5km_radius": 8420, "households_5km_radius": 1640,
      "business_data": {
        "Dairy": { "competitor_count": 2, "avg_local_price": 42 },
        "Tailoring": { "competitor_count": 11, "avg_local_price": 250 },
        "Retail / Kirana Store": { "competitor_count": 14, "avg_local_price": null },
        "Flour Mill": { "competitor_count": 1, "avg_local_price": 4 }
      }
    },
    {
      "village_name": "Bilaspur", "block": "Kota", "district": "Rajgarh", "state": "Madhya Pradesh",
      "latitude": 23.3315, "longitude": 76.9349,
      "population_5km_radius": 5200, "households_5km_radius": 980,
      "business_data": {
        "Dairy": { "competitor_count": 4, "avg_local_price": 38 },
        "Tailoring": { "competitor_count": 3, "avg_local_price": 180 },
        "Retail / Kirana Store": { "competitor_count": 6, "avg_local_price": null },
        "Flour Mill": { "competitor_count": 2, "avg_local_price": 3.5 }
      }
    },
    {
      "village_name": "Peddapuram", "block": "Kakinada Rural", "district": "East Godavari", "state": "Andhra Pradesh",
      "latitude": 17.0788, "longitude": 82.1400,
      "population_5km_radius": 12300, "households_5km_radius": 2450,
      "business_data": {
        "Dairy": { "competitor_count": 6, "avg_local_price": 45 },
        "Tailoring": { "competitor_count": 9, "avg_local_price": 300 },
        "Retail / Kirana Store": { "competitor_count": 21, "avg_local_price": null },
        "Flour Mill": { "competitor_count": 3, "avg_local_price": 4.2 }
      }
    }
  ],
  "business_categories": [
    {
      "category_name": "Dairy", "reference_project_cost": 1000000, "reference_cost_range": [650000, 1400000],
      "working_capital_months": 3, "reference_monthly_revenue": 45000, "reference_monthly_operating_cost": 31500,
      "secondary_opportunity": "Vermicompost from dairy waste",
      "source_note": "Anchored to a real PMEGP dairy/goshala DPR (~Rs 13.46 lakh project cost)."
    },
    {
      "category_name": "Tailoring", "reference_project_cost": 250000, "reference_cost_range": [150000, 350000],
      "working_capital_months": 2, "reference_monthly_revenue": 18000, "reference_monthly_operating_cost": 12600,
      "secondary_opportunity": "School uniform bulk contracts",
      "source_note": "Anchored to real PMEGP/Mudra tailoring shop project data."
    },
    {
      "category_name": "Retail / Kirana Store", "reference_project_cost": 500000, "reference_cost_range": [150000, 2000000],
      "working_capital_months": 2, "reference_monthly_revenue": 35000, "reference_monthly_operating_cost": 24500,
      "secondary_opportunity": "Bill-payment / mobile-recharge counter",
      "source_note": "PMEGP business/service sector ceiling is Rs 20 lakh; mid-range figure used as default."
    },
    {
      "category_name": "Flour Mill", "reference_project_cost": 400000, "reference_cost_range": [200000, 600000],
      "working_capital_months": 2, "reference_monthly_revenue": 22000, "reference_monthly_operating_cost": 15400,
      "secondary_opportunity": "Spice grinding using the same machinery",
      "source_note": "Standard KVIC/PMEGP model project profile range for small flour-milling units."
    }
  ]
}
```

---

## 5. FINANCIAL CALCULATOR (real — implement exactly, never via an LLM)

```
project_cost = available_margin_capital / 0.10
loan_amount  = project_cost * 0.90

IF project_cost <= 140000:
    scheme_name = "Micro Finance Scheme"; interest_rate_annual = 0.065; tenure_years = 3; moratorium_months = 3
ELSE IF project_cost <= 5000000:
    scheme_name = "Term Loan Scheme"; interest_rate_annual = 0.08; tenure_years = 7; moratorium_months = 6
ELSE:
    return error: "Project cost exceeds the ₹50 lakh scheme ceiling."

repayment_months = (tenure_years * 12) - moratorium_months
monthly_rate = interest_rate_annual / 12
EMI = loan_amount * monthly_rate * (1+monthly_rate)^repayment_months / ((1+monthly_rate)^repayment_months - 1)

working_capital_estimate = business_category.reference_monthly_operating_cost * business_category.working_capital_months

recommended_project_cost = MIN(project_cost, business_category.reference_project_cost)
recommended_loan_amount  = recommended_project_cost * 0.90
buffer_amount = loan_amount - recommended_loan_amount
```

---

## 6. LOCAL OPPORTUNITY SCORE (real)

```
demand_ratio = village.population_5km_radius / MAX(competitor_count, 1)
reference_high = 4000   # tune after testing so scores spread ~20-90 across the sample data
opportunity_score = MIN(100, MAX(0, ROUND((demand_ratio / reference_high) * 100)))
label = "High opportunity" if >=70, "Moderate opportunity" if >=40, else "Low opportunity (saturated)"
```

---

## 7. DSCR (real)

```
monthly_net_operating_income = reference_monthly_revenue - reference_monthly_operating_cost
DSCR = monthly_net_operating_income / EMI
label: >=2.0 "Healthy", 1.0-2.0 "Moderate", <1.0 "Risky"
```

---

## 8. BUSINESS SURVIVAL SIMULATOR + STRESS TEST (real, deterministic)

```
INPUT: revenue_shock_pct (0, -0.10, -0.20, -0.30), cost_shock_pct (0, 0.10, 0.20, 0.30)

adjusted_monthly_revenue = reference_monthly_revenue * (1 + revenue_shock_pct)
adjusted_monthly_cost    = reference_monthly_operating_cost * (1 + cost_shock_pct)

cumulative_cash = working_capital_estimate
deficit_month = null
cash_curve = []

FOR month = 1 to 24:
    net_cash_flow = adjusted_monthly_revenue - adjusted_monthly_cost - (EMI if month > moratorium_months else 0)
    cumulative_cash += net_cash_flow
    cash_curve.append({month, cumulative_cash})
    IF cumulative_cash < 0 AND deficit_month IS null: deficit_month = month

verdict = "Business survives comfortably across the 24-month simulated period ✅" if deficit_month is null
          else "⚠️ Cash-flow deficit likely from Month " + deficit_month + " under this scenario"

Return cash_curve (for the recharts line chart) + verdict + deficit_month.
```

---

## 9. BUSINESS DISCOVERY ENGINE (real)

```
INPUT: village_name, available_margin_capital  (no business_category required)
project_cost = available_margin_capital / 0.10

FOR EACH business_category:
    opportunity_score = <Section 6 formula>
    affordability_flag = "Within budget" if project_cost >= reference_project_cost else "May require phasing"

Return all categories sorted by opportunity_score descending.
```

---

## 10. REVERSE FINANCIAL ENGINEERING / GOAL-SEEK (real, new feature)

```
INPUT: desired_monthly_income, business_category

# Required monthly revenue = desired income + the category's typical operating cost
required_monthly_revenue = desired_monthly_income + business_category.reference_monthly_operating_cost

# Scale the reference project cost proportionally to the revenue gap
#   (simple linear scaling assumption for this prototype — clearly label it as an estimate)
scale_factor = required_monthly_revenue / business_category.reference_monthly_revenue
estimated_project_cost = business_category.reference_project_cost * scale_factor
estimated_margin_required = estimated_project_cost * 0.10
estimated_loan_required = estimated_project_cost * 0.90

Return all four values, clearly labeled "Estimated — scaled from reference model data."
```

---

## 11. BUSINESS COMBINATION SUGGESTIONS (templated lookup — not AI-generated)

```
Simply return business_category.secondary_opportunity from the reference data
(e.g., "Dairy" -> "Vermicompost from dairy waste") alongside the main analysis result.
This is a static lookup table, not a generative feature — present it as "Suggested secondary revenue stream" in the UI.
```

---

## 12. EVIDENCE TRAIL (real citation of cached data — not live RAG)

Return an `evidence` array with each claim mapped to which cached field produced it (Opportunity Score → population+competitor data for that village; Project Cost/Loan → scheme calculator; Recommended Loan → reference project cost for that category; Survival Simulation → 24-month deterministic cash-flow model). Build this as a plain function reading the same variables used above — no vector database needed.

---

## 13. MULTI-AGENT "AI INVESTMENT COMMITTEE" PANEL (templated, real thresholds)

```
Market Agent:   based on opportunity_score thresholds (>=70 / >=40 / else) — see wording bank below
Finance Agent:  based on DSCR thresholds (>=2.0 / >=1.0 / else)
Risk Agent:     based on survival simulator's deficit_month (null vs a value)
Final Verdict:  "VIABLE" if opportunity_score>=60 AND DSCR>=1.5 AND deficit_month is null;
                "VIABLE WITH MODIFICATIONS" if opportunity_score>=40 AND DSCR>=1.0;
                else "NOT RECOMMENDED AS STRUCTURED"
```
(Use the exact wording bank already defined in the earlier draft of this brief, or write natural equivalents — the logic/thresholds must stay as specified.)

---

## 14. LOCAL BUSINESS MAP (real Leaflet map, illustrative marker positions)

- Center the map on the selected village's `latitude`/`longitude`.
- Plot: 1 marker for the village center (blue), and `competitor_count` markers scattered randomly within a ~5km radius around the center (red) to visually represent competitor density — generate these scattered coordinates client-side or server-side using a small random-offset function (e.g., ± 0.03 degrees lat/lon, seeded by village+category so it's consistent on repeat views, not re-randomized every render).
- Add a legend: 🔵 Village center, 🔴 Existing competitors.
- Requires internet access to load OpenStreetMap tiles — this is fine for a recorded demo video; note this dependency to the team.

---

## 15. AUTO-GENERATED DPR (PDF) — real, includes everything

Include: applicant location, business category, margin capital, Opportunity Score + label, Project Cost/Max Loan/Scheme/EMI, Recommended (Optimal) Loan + buffer, Working Capital estimate, DSCR + label, Survival Simulator base-case verdict, Multi-Agent Committee verdict, Secondary revenue suggestion, footer: "Generated by GraminSaathi — SIH 2026 Prototype."

---

## 16. LOGIN / SIGNUP + SAVED REPORTS (real, PostgreSQL)

```sql
CREATE TABLE users (
  id SERIAL PRIMARY KEY, full_name VARCHAR(255) NOT NULL, email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL, created_at TIMESTAMP DEFAULT NOW()
);
CREATE TABLE saved_reports (
  id SERIAL PRIMARY KEY, user_id INTEGER REFERENCES users(id),
  village_name VARCHAR(255), business_category VARCHAR(255), available_margin_capital NUMERIC,
  result_json JSONB, created_at TIMESTAMP DEFAULT NOW()
);
```
Endpoints: `POST /api/auth/signup`, `POST /api/auth/login` (returns JWT), `POST /api/reports` (auth), `GET /api/reports` (auth). Use BCrypt for password hashing and a Spring Security JWT filter. Keep `/api/analyze`, `/api/discover`, `/api/simulate`, `/api/goal-seek` public for smooth demoing.

---

## 17. FULL API ENDPOINT LIST

```
GET  /api/villages
GET  /api/business-categories
POST /api/analyze        -> full analysis: financial + feasibility + DSCR + evidence + agent panel + secondary opportunity
POST /api/discover        -> ranked business list for a location
POST /api/simulate         -> survival simulation with shock params
POST /api/goal-seek         -> reverse financial engineering
GET  /api/analyze/pdf?...  -> downloadable PDF
POST /api/auth/signup
POST /api/auth/login
POST /api/reports           (auth)
GET  /api/reports           (auth)
```

---

## 18. FRONTEND SCREENS — BUILD IN THIS ORDER

1. **Login / Signup** — email + password, toggle between modes, on success store JWT in AuthContext.
2. **Dashboard (chat shell)** — WhatsApp-styled panel, quick-reply buttons ("🔍 Find best business," "💰 Check my loan," "📊 Analyze a business," "🎯 Set an income goal," "🎙️ Talk to AI"), a language toggle (EN/HI) in the header, free-text input, mic button.
3. **Business Discovery results** — ranked cards with opportunity score bars + affordability badges.
4. **Analysis results screen** — Opportunity Score card, Loan Comparison card (Max vs Recommended, recommended visually emphasized), DSCR indicator (colored), secondary-opportunity callout, Multi-Agent Committee panel (4 cards), expandable Evidence panel, "Download PDF" button, "View on Map" button, "Run Survival Simulation" button.
5. **Survival Simulator screen** — shock sliders/buttons, recharts line chart of cumulative cash over 24 months, verdict banner.
6. **Map screen** — Leaflet map centered on the village with village + competitor markers and legend.
7. **Goal-Seek screen** — input for desired monthly income + business category dropdown, output showing estimated project cost/margin/loan required.
8. **Report History** (requires login) — list of saved analyses, "View" and "Download PDF" per row.
9. **Voice layer** — mic button triggers `SpeechRecognition`; do simple keyword/number extraction (regex for currency figures, and keyword matching against the known village/business lists) to prefill form fields; `speechSynthesis` reads back a one-line summary of the result.
10. **Bilingual toggle** — a simple React context that swaps all UI label strings between `en.json` and `hi.json` dictionaries app-wide (data values themselves stay numeric/unaffected).

---

## 19. BUILD ORDER (for a 6-person team — suggested parallelization)

- **Person 1:** PostgreSQL + Spring Security/JWT auth (Section 16), User/SavedReport entities and repositories.
- **Person 2:** Financial Calculator + Opportunity Score + DSCR services (Sections 5-7), unit-tested against manual calculations.
- **Person 3:** Survival Simulator + Discovery Engine + Goal-Seek services (Sections 8-10).
- **Person 4:** Evidence Trail + Multi-Agent Committee + Business Combination + PDF generation (Sections 11-13, 15).
- **Person 5:** React frontend shell — routing, AuthContext, Dashboard/chat UI, Login/Signup pages, i18n toggle.
- **Person 6:** React frontend — Discovery/Analysis/Simulator/Map/Reports pages, recharts + Leaflet integration, voice layer.

Integrate incrementally: get `/api/analyze` fully working end-to-end (backend + one frontend screen) before parallelizing further, so everyone is building against a known-working pattern.

---

## 20. ACCEPTANCE CRITERIA

- [ ] Signup/login/logout work against PostgreSQL; JWT correctly protects `/api/reports`.
- [ ] All financial formulas match Section 5 exactly for 3 manually-verified test cases.
- [ ] Opportunity Score and Discovery ranking differ sensibly across all 3 villages.
- [ ] DSCR indicator changes color/label correctly across at least 2 scenarios.
- [ ] Survival Simulator's deficit month updates correctly when shock sliders change, verified against 1 manual recalculation.
- [ ] Goal-Seek returns sensible, proportionally scaled numbers for at least 2 test inputs.
- [ ] Multi-Agent panel text changes based on underlying numbers, never static.
- [ ] Evidence panel lists sources accurately reflecting the real data used.
- [ ] Map correctly centers on the selected village and shows the right competitor count as markers.
- [ ] Bilingual toggle swaps all visible UI labels without breaking layout.
- [ ] Voice input successfully transcribes and prefills at least village + capital in one test run (Chrome).
- [ ] PDF includes every section with no placeholder text.
- [ ] A logged-in user can save and later view/download a report.
- [ ] Full flow runs with no internet dependency except the Map screen's tile loading.

---

## 21. DEMO VIDEO SCRIPT (~3 minutes — use this verbatim as your recording outline)

**0:00–0:15 — Hook**
Open on the Login screen. Voiceover: *"Rural entrepreneurs get government loans, but most don't know if their business idea will actually survive — GraminSaathi fixes that before they borrow a rupee."* Log in.

**0:15–0:40 — Voice-first entry**
On the Dashboard (chat shell), tap the mic and say something like *"I have one lakh rupees, I want to start a dairy business in Ghoti."* Show the transcription appear, then the app extracting village/capital/business automatically.

**0:40–1:00 — Business Discovery**
Instead of jumping straight to dairy, show the Discovery screen for the same village + capital with no business type — the ranked list appears, showing Dairy scoring highest with a clear opportunity score bar, next to lower-ranked, more saturated categories.

**1:00–1:30 — Full Analysis**
Select Dairy. Show the Analysis screen: Opportunity Score card, then the Loan Comparison card — call out on camera: *"We're eligible for ₹9 lakh, but we recommend only ₹6.5 lakh, with a safety buffer."* Show the DSCR indicator turning green. Show the secondary-opportunity callout ("Vermicompost from dairy waste").

**1:30–1:50 — Multi-Agent Committee + Evidence**
Open the Multi-Agent Committee panel — narrate: *"Instead of one AI opinion, we get a Market view, a Finance view, and a Risk view, before a final verdict."* Expand the Evidence panel to show exactly which data point backs each number.

**1:50–2:15 — Survival Simulator (the "wow" moment)**
Go to the Simulator screen. Move the cost-shock slider to +20%. Show the cash-flow chart dip and the verdict banner update live to flag a deficit month. Narrate: *"This is the difference between funding a business and funding one that survives."*

**2:15–2:35 — Map + bilingual toggle**
Show the Map screen with the village and competitor pins. Toggle the language switch from English to Hindi live on screen to show vernacular readiness.

**2:35–2:50 — Output**
Click "Download PDF Report" — show the generated document briefly. Mention: *"This is bank-ready — the same document a beneficiary can hand to their SCA or bank."*

**2:50–3:00 — Close**
Cut back to Report History showing the saved report, then end on your one-line USP: *"We don't just tell you how much you can borrow — we tell you if your business will survive, before you do."*

Feel free to trim any single section if the full script runs long, but keep the Survival Simulator and Loan Comparison beats — those are your two strongest, most memorable visual moments.
