# GraminSaathi Frontend Design — Master Prompt for Design Generation

> **Context:** This prompt contains everything a design model needs to generate a complete, production-ready frontend for the GraminSaathi backend. The backend is a Spring Boot (Java 17) REST API with 26 financial/business advisory features for rural Indian entrepreneurs applying for government concessional loans (NBCFDC/NSFDC-style schemes).

---

## 1. PROJECT OVERVIEW

**Product:** GraminSaathi — AI-style business advisory & loan-structuring tool for rural, first-time entrepreneurs in India.

**Core Problem:** Rural entrepreneurs pick businesses based on anecdote (not local demand data) and don't understand their real borrowing/repayment capacity → high post-funding failure rates.

**Target Users:** Semi-literate to literate rural entrepreneurs, SHG members, first-time borrowers. Primary language: Hindi + English.

**Key Differentiator:** "We don't just tell you how much you can borrow — we tell you if your business will survive, before you do."

---

## 2. TECH STACK (Mandatory)

| Layer | Technology |
|-------|------------|
| Framework | React 18 + Vite |
| Routing | react-router-dom v6 |
| State/Auth | React Context (AuthContext) |
| API | axios (base URL: `http://localhost:8080/api`) |
| Charts | recharts (Survival Simulator cumulative cash-flow line chart) |
| Maps | leaflet + react-leaflet (OpenStreetMap tiles) |
| Styling | Tailwind CSS (mandatory) |
| i18n | Simple JSON dictionaries (en.json, hi.json) + React Context |
| Voice | Browser native Web Speech API (SpeechRecognition + speechSynthesis) |
| PWA | vite-plugin-pwa (offline-first, cache static assets) |

**Environment Variable:** `VITE_API_BASE_URL=http://localhost:8080/api`

---

## 3. BACKEND API CONTRACT (Complete)

### Base URL: `http://localhost:8080/api`

### Public Endpoints (No Auth Required)

| Method | Endpoint | Request | Response |
|--------|----------|---------|----------|
| GET | `/villages` | — | `VillageResponse[]` |
| GET | `/business-categories` | — | `BusinessCategoryResponse[]` |
| GET | `/schemes` | — | `SchemeComparisonResponse[]` |
| POST | `/analyze` | `AnalyzeRequest` | `AnalyzeResponse` |
| POST | `/discover` | `DiscoverRequest` | `DiscoveryResponse` |
| POST | `/simulate` | `SimulateRequest` | `SimulationResponse` |
| POST | `/goal-seek` | `GoalSeekRequest` | `GoalSeekResponse` |
| GET | `/analyze/pdf` | Query params (same as AnalyzeRequest) | **PDF binary** |

### Auth Required Endpoints

| Method | Endpoint | Request | Response |
|--------|----------|---------|----------|
| POST | `/auth/signup` | `SignupRequest` | `AuthResponse` (JWT) |
| POST | `/auth/login` | `LoginRequest` | `AuthResponse` (JWT) |
| POST | `/reports` | `SaveReportRequest` | `SavedReport` |
| GET | `/reports` | — | `SavedReport[]` |

---

### Request/Response DTOs (Exact Shapes)

```typescript
// ===== REQUESTS =====

interface AnalyzeRequest {
  villageName: string;           // e.g., "Ghoti"
  businessCategory: string;      // e.g., "Dairy"
  availableMarginCapital: number; // e.g., 100000 (₹1 lakh)
}

interface DiscoverRequest {
  villageName: string;
  availableMarginCapital: number;
}

interface SimulateRequest {
  villageName: string;
  businessCategory: string;
  availableMarginCapital: number;
  revenueShockPct: number;   // default 0, e.g., -0.20 for -20%
  costShockPct: number;      // default 0, e.g., 0.15 for +15%
}

interface GoalSeekRequest {
  businessCategory: string;
  desiredMonthlyIncome: number;
}

interface SignupRequest {
  fullName: string;
  email: string;
  password: string;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface SaveReportRequest {
  villageName: string;
  businessCategory: string;
  availableMarginCapital: number;
  resultJson: AnalyzeResponse; // full analysis object
}

// ===== RESPONSES =====

interface VillageResponse {
  villageName: string;
  block: string;
  district: string;
  state: string;
  latitude: number;
  longitude: number;
  population5kmRadius: number;
  households5kmRadius: number;
  businessData: Record<string, BusinessDataResponse>;
}

interface BusinessDataResponse {
  competitorCount: number;
  avgLocalPrice: number | null;
}

interface BusinessCategoryResponse {
  categoryName: string;
  referenceProjectCost: number;
  referenceCostRange: [number, number];
  workingCapitalMonths: number;
  referenceMonthlyRevenue: number;
  referenceMonthlyOperatingCost: number;
  secondaryOpportunity: string;
  sourceNote: string;
  // Addendum fields:
  singleBuyerDependencyRisk: "High" | "Medium" | "Low";
  primaryInputDependencyNote: string;
  peerBenchmark: {
    sampleSize: number;
    avgMonthlyRevenueAfter6Months: number;
    pctStillOperatingAfter1Year: number;
  };
  roadmapMilestones: { month: number; milestone: string }[];
}

interface SchemeComparisonResponse {
  schemeName: string;
  interestRate: number;
  tenureYears: number;
  moratoriumMonths: number;
  emi: number;
  agency: string;
  subsidyNote: string | null;
  isPrimary: boolean;
}

// ===== FULL ANALYZE RESPONSE =====

interface AnalyzeResponse {
  financial: FinancialResponse;
  feasibility: FeasibilityResponse;
  dscr: DscrResponse;
  survival: SurvivalResponse;
  combination: CombinationResponse;
  evidence: EvidenceResponse;
  committee: CommitteeResponse;
  healthScore: HealthScoreResponse;
  supplyRisk: SupplyRiskResponse;
  peerBenchmark: PeerBenchmarkResponse;
  roadmap: RoadmapResponse;
}

interface FinancialResponse {
  projectCost: number;
  loanAmount: number;
  schemeName: string;
  interestRateAnnual: number;
  tenureYears: number;
  moratoriumMonths: number;
  repaymentMonths: number;
  monthlyRate: number;
  emi: number;
  workingCapitalEstimate: number;
  recommendedProjectCost: number;
  recommendedLoanAmount: number;
  bufferAmount: number;
  schemeComparison: SchemeComparisonResponse[];
  workingCapitalWarning: string | null;
  localAveragePrice: number;
  recommendedPriceLow: number | null;
  recommendedPriceHigh: number | null;
  recommendedLaunchPrice: number | null;
  breakevenPrice: number | null;
  breakevenNote: string | null;
}

interface FeasibilityResponse {
  opportunityScore: number;       // 0-100
  label: string;                  // "High opportunity" | "Moderate opportunity" | "Low opportunity (saturated)"
  competitorCount: number;
  population5kmRadius: number;
  demandRatio: number;
}

interface DscrResponse {
  dscr: number;
  label: "Healthy" | "Moderate" | "Risky";  // >=2.0, >=1.0, <1.0
  monthlyNetOperatingIncome: number;
  emi: number;
}

interface SurvivalResponse {
  cashCurve: CashPoint[];   // 24 points for recharts
  verdict: string;
  deficitMonth: number | null;
}

interface CashPoint {
  month: number;      // 1-24
  cumulativeCash: number;
}

interface CombinationResponse {
  secondaryOpportunity: string;
}

interface EvidenceResponse {
  evidence: EvidenceItem[];
}

interface EvidenceItem {
  claim: string;      // e.g., "Opportunity Score: 85"
  source: string;     // e.g., "Village population (8,420) + competitor count (2)"
  details: string;    // e.g., "demand_ratio = 8420/2 = 4210; score = min(100, (4210/4000)*100) = 100 → capped at 100"
}

interface CommitteeResponse {
  marketAgent: AgentOpinion;
  financeAgent: AgentOpinion;
  riskAgent: AgentOpinion;
  finalVerdict: "VIABLE" | "VIABLE WITH MODIFICATIONS" | "NOT RECOMMENDED AS STRUCTURED";
  verdictReason: string;
}

interface AgentOpinion {
  agent: "Market Agent" | "Finance Agent" | "Risk Agent";
  opinion: string;
  basis: string;
}

interface HealthScoreResponse {
  overallScore: number;       // 0-100
  recommendation: "🟢 Proceed" | "🟡 Proceed with modifications" | "🔴 Do not finance as structured";
  marketDemand: SubScore;
  capitalAdequacy: SubScore;
  profitability: SubScore;
  cashFlow: SubScore;
  supplyRisk: SubScore;
  seasonality: SubScore;
}

interface SubScore {
  name: string;
  score: number;    // 0-100
  description: string;
}

interface SupplyRiskResponse {
  riskLevel: "High" | "Medium" | "Low";
  note: string;
}

interface PeerBenchmarkResponse {
  sampleSize: number;
  avgMonthlyRevenueAfter6Months: number;
  pctStillOperatingAfter1Year: number;
  disclaimer: string;  // "Illustrative sample data — in production this would populate from real anonymized user outcomes over time"
}

interface RoadmapResponse {
  milestones: Milestone[];
}

interface Milestone {
  month: number;
  milestone: string;
}

interface DiscoveryResponse {
  businesses: DiscoveredBusinessResponse[];
}

interface DiscoveredBusinessResponse {
  categoryName: string;
  opportunityScore: number;
  opportunityLabel: string;
  affordabilityFlag: "Within budget" | "May require phasing";
  referenceProjectCost: number;
  referenceMonthlyRevenue: number;
  referenceMonthlyOperatingCost: number;
}

interface SimulationResponse = SurvivalResponse;  // Same shape

interface GoalSeekResponse {
  estimatedProjectCost: number;
  estimatedMarginRequired: number;
  estimatedLoanRequired: number;
  requiredMonthlyRevenue: number;
  note: string;  // "Estimated — scaled from reference model data."
}

interface AuthResponse {
  token: string;
  user: { id: number; fullName: string; email: string };
}

interface SavedReport {
  id: number;
  villageName: string;
  businessCategory: string;
  availableMarginCapital: number;
  resultJson: AnalyzeResponse;
  createdAt: string;  // ISO timestamp
}
```

---

## 4. SCREENS & USER FLOWS (Build in This Order)

### 4.1 Authentication Flow
```
[Landing] → [Login Page] ↔ [Signup Page] → [Dashboard (Chat Shell)]
```
- JWT stored in memory (AuthContext), auto-attached to axios requests via interceptor
- Protected routes: `/reports`, `/post-loan-preview`
- Public routes: `/`, `/login`, `/signup`, `/dashboard`, `/discovery`, `/analysis`, `/simulator`, `/map`, `/goal-seek`

---

### 4.2 Screen Specifications

#### **SCREEN 1: Login / Signup Page** (`/login`, `/signup`)
**Layout:** Centered card, mobile-first, Tailwind
- Toggle between Login/Signup modes (tabs or link)
- Fields: Full Name (signup only), Email, Password
- Primary button: "Continue" / "Create Account"
- Error toast for invalid credentials / duplicate email
- Footer: "GraminSaathi — SIH 2026 Prototype"
- **Voice:** Mic button prefills email (speech-to-text)

---

#### **SCREEN 2: Dashboard / Chat Shell** (`/dashboard`) — **HOME AFTER LOGIN**
**Layout:** WhatsApp-styled conversation panel
- **Header:** App logo + "GraminSaathi" + Language Toggle (EN/HI) + User avatar dropdown (Logout)
- **Chat Area:** Message bubbles (bot/user), scrollable
- **Quick-Reply Buttons** (always visible above input):
  - 🔍 "Find best business" → navigates to `/discovery` with prefilled village+capital from context
  - 💰 "Check my loan" → navigates to `/analysis` 
  - 📊 "Analyze a business" → navigates to `/analysis`
  - 🎯 "Set an income goal" → navigates to `/goal-seek`
  - 🎙️ "Talk to AI" → activates voice input
- **Input Bar:** Text input + Mic button (Voice) + Send button
- **Voice Behavior:** 
  - Mic activates `SpeechRecognition` (continuous=false, lang="hi-IN" or "en-IN")
  - Transcript appears in input field → user can edit → Send
  - Simple keyword extraction: village names, numbers (lakh/crore), business categories
  - On result: `speechSynthesis` reads one-line summary: *"Dairy in Ghoti with ₹1 lakh margin: Opportunity Score 85, recommended loan ₹6.5 lakh, EMI ₹21,400"*

---

#### **SCREEN 3: Business Discovery** (`/discovery`)
**Input:** Village (select) + Available Margin Capital (number input) — prefilled from chat/voice if available
**Action Button:** "Discover Businesses"
**Output:** Ranked cards (vertical list), sorted by `opportunityScore` descending

**Card Design per Business:**
```
┌─────────────────────────────────────────────────────┐
│  DAIRY                                    [85] 🟢   │  ← Score + color badge
│  Opportunity: High opportunity                      │
│  Competitors: 2  |  Population (5km): 8,420         │
│  Ref. Project Cost: ₹10,00,000                       │
│  Ref. Monthly Revenue: ₹45,000  |  Op. Cost: ₹31,500 │
│  [Within budget]  or  [May require phasing]         │  ← affordabilityFlag badge
│  [Analyze This Business]  (primary button)          │  → navigates to /analysis with params
└─────────────────────────────────────────────────────┘
```
- Score bar: visual horizontal bar (0-100) with color gradient (red→yellow→green)
- Click "Analyze" → `/analysis?village=Ghoti&category=Dairy&capital=100000`

---

#### **SCREEN 4: Analysis Results** (`/analysis`) — **MOST COMPLEX SCREEN**
**Input:** Village + Business Category + Margin Capital (from URL params or form)
**Load:** POST `/api/analyze` → shows loading skeleton → renders all sections

**Tabbed Layout (Recommended due to density):**
```
[Overview] [Financials] [Risks & Roadmap] [Evidence]
```

**Tab 1: Overview**
- **Opportunity Score Card:** Large circular progress (score), label, competitor count, population
- **Loan Comparison Card:** Side-by-side
  - **Max Eligible:** Project Cost, Loan, EMI, Scheme (highlighted)
  - **Recommended (Optimal):** Project Cost, Loan, EMI, **Buffer Amount** (visually emphasized — green)
  - Visual: Two cards, Recommended card has green border + "✓ Recommended" badge
- **DSCR Indicator:** Colored pill (Green ≥2.0, Yellow 1.0-2.0, Red <1.0) with label + value
- **Secondary Opportunity Callout:** Highlighted box: *"💡 Suggested secondary revenue stream: Vermicompost from dairy waste"*
- **Working Capital Warning Banner** (if present): Amber/orange banner with warning text
- **Local Price Intelligence Card:** (skip for Retail)
  - Local Average Price: ₹42
  - Recommended Launch Price: ₹40.74 (97% of local avg)
  - Price Band: ₹39.90 – ₹44.10 (95%-105%)
- **Failure Boundary Callout:** *"At current sales volume, your minimum viable price is ₹38.50 — your planned price of ₹42 gives you ₹3.50/unit safety margin."*
- **CTA Buttons Row:**
  - [Download PDF] → GET `/api/analyze/pdf?...`
  - [View on Map] → `/map?village=Ghoti&category=Dairy`
  - [Run Survival Simulation] → `/simulator?village=Ghoti&category=Dairy&capital=100000`
  - [Save Report] (if logged in) → POST `/api/reports`

**Tab 2: Financials**
- Scheme Comparison Table (expandable):
  | Scheme | Interest | Tenure | Moratorium | EMI | Agency | Notes |
  |--------|----------|--------|------------|-----|--------|-------|
  | ★ Micro Finance | 6.5% | 3yr | 3mo | ₹21,400 | NBCFDC | *Your scheme* |
  | PMEGP | 11% | 7yr | 0 | ₹18,200 | KVIC | 15-35% subsidy |
  | Mudra Kishor | 10.5% | 5yr | 0 | ₹21,800 | Banks | Market rate |
- Detailed financial breakdown (all FinancialResponse fields)

**Tab 3: Risks & Roadmap**
- **Multi-Agent Committee Panel** (4 cards in grid):
  - Market Agent: opinion + basis
  - Finance Agent: opinion + basis  
  - Risk Agent: opinion + basis
  - **Final Verdict:** Large colored badge (Green/Yellow/Red) + reason
- **Supply Chain Risk:** Risk level badge + advisory note
- **Peer Benchmark Card:** *"Sample cohort: 24 similar businesses averaged ₹41,000/month after 6 months, 78% still operating after 1 year"* + disclaimer tag
- **Roadmap Milestones Timeline:** Vertical timeline with 5 nodes (Month 1, 2, 3, 6, 12)

**Tab 4: Evidence**
- Expandable list of EvidenceItems:
  - Claim (bold) → Source → Details (monospace, smaller)

---

#### **SCREEN 5: Survival Simulator** (`/simulator`)
**Input:** Village + Category + Capital (from URL) + **Shock Controls**
- Revenue Shock: Slider/buttons: 0%, -10%, -20%, -30%
- Cost Shock: Slider/buttons: 0%, +10%, +20%, +30%
- **Live Recalculation:** POST `/api/simulate` on every change (debounced 300ms)

**Output:**
- **Verdict Banner:** Green "✅ Business survives comfortably across 24 months" OR Amber "⚠️ Cash-flow deficit likely from Month X"
- **Recharts Line Chart:** X-axis: Month (1-24), Y-axis: Cumulative Cash (₹)
  - Line color: Green if positive, Red if negative
  - Tooltip: Month, Cumulative Cash
  - Horizontal line at y=0
- **Deficit Month Highlight:** If deficit, mark that month on chart with red dot

---

#### **SCREEN 6: Local Business Map** (`/map`)
**Center:** Village latitude/longitude from VillageResponse
**Markers:**
- 🔵 **Village Center** (blue, larger, popup: village name + block + district)
- 🔴 **Competitors** (red, smaller, count = `competitorCount` from businessData)
  - Positions: Scattered randomly within ~5km radius (consistent seed: hash(village+category))
  - Popup: "Existing competitor"
- **Legend:** Bottom-left: 🔵 Village Center, 🔴 Existing Competitors
- **Map Tiles:** OpenStreetMap (requires internet)
- **Note:** Display disclaimer: *"Competitor positions are illustrative — not real GPS locations"*

---

#### **SCREEN 7: Goal Seek** (`/goal-seek`)
**Input Form:**
- Business Category (dropdown from `/business-categories`)
- Desired Monthly Income (number input, ₹)
- [Calculate] button

**Output Card:**
- Required Monthly Revenue: ₹X
- Estimated Project Cost: ₹X
- Estimated Margin Required (10%): ₹X
- Estimated Loan Required (90%): ₹X
- Note: *"Estimated — scaled from reference model data."*

---

#### **SCREEN 8: Report History** (`/reports`) — **AUTH REQUIRED**
**Layout:** Table/List of saved reports
| Date | Village | Business | Margin Capital | Actions |
|------|---------|----------|----------------|---------|
| 2026-09-01 | Ghoti | Dairy | ₹1,00,000 | [View] [Download PDF] |

- [View] → navigates to `/analysis` with prefilled data (read-only mode)
- [Download PDF] → GET `/api/analyze/pdf?...` with report's params

---

#### **SCREEN 9: Post-Loan Preview** (`/post-loan-preview`) — **ROADMAP SCREEN**
**Label prominently:** "🔮 Preview: Post-Loan Advisory (Future Roadmap)"
**Static Example Nudge Cards:**
```
⚠️ Your expenses increased 18% this month vs. your plan — review your input costs.
📉 Your projected profit margin has narrowed from 22% to 16% — consider the pricing suggestion above.
💰 Your next EMI of ₹21,400 is due in 12 days.
```
- Not wired to backend — purely illustrative

---

## 5. GLOBAL UI COMPONENTS

### 5.1 Language Toggle (EN/HI)
- **Location:** Top-right header (all screens)
- **Implementation:** React Context + two JSON files (`en.json`, `hi.json`)
- **Scope:** ALL UI labels, button text, placeholders, toast messages
- **Data values** (numbers, names) remain unchanged
- **Hindi translations needed for:** All labels in Section 4 screen specs

### 5.2 Voice Layer
- **Mic Button:** Present in Dashboard chat input, Login/Signup email field, Discovery/Analysis/GoalSeek forms
- **SpeechRecognition:** `lang` = 'hi-IN' if Hindi selected else 'en-IN'
- **Number Extraction:** Regex for "lakh" (×1,00,000), "crore" (×1,00,00,000), "thousand" (×1,000), plain numbers
- **Entity Extraction:** Match transcript against known villages (3) + business categories (4)
- **SpeechSynthesis:** Read result summary on analysis/simulation/goal-seek complete

### 5.3 PWA / Offline-First
- `vite-plugin-pwa` configured
- Cache: `demo_data.json`, `schemes_reference.json`, all static assets
- Service worker: `registerType: 'autoUpdate'`
- Manifest: name="GraminSaathi", short_name="GraminSaathi", icons, theme_color="#166534" (green)
- Offline shell loads; Map screen shows "Map requires internet connection" toast

### 5.4 Color System (Tailwind)
```css
/* Primary: Government scheme green */
--primary: #166534;      /* green-800 */
--primary-light: #16a34a; /* green-600 */
--primary-bg: #f0fdf4;   /* green-50 */

/* Semantic */
--success: #16a34a;      /* green-600 */
--warning: #f59e0b;      /* amber-500 */
--danger: #dc2626;       /* red-600 */
--info: #2563eb;         /* blue-600 */

/* Neutral */
--bg: #f9fafb;           /* gray-50 */
--card: #ffffff;         /* white */
--text: #111827;         /* gray-900 */
--text-muted: #6b7280;   /* gray-500 */
--border: #e5e7eb;       /* gray-200 */
```

### 5.5 Typography
- **Font:** Inter (Latin) + Noto Sans Devanagari (Hindi)
- **Scale:** text-xs (12px) → text-sm (14px) → text-base (16px) → text-lg (18px) → text-xl (20px) → text-2xl (24px) → text-3xl (30px)
- **Weights:** 400 (normal), 500 (medium), 600 (semibold), 700 (bold)

### 5.6 Spacing & Radius
- **Spacing:** 4px base (Tailwind default)
- **Radius:** rounded-lg (8px) for cards, rounded-full for pills/badges, rounded-xl (12px) for modals

### 5.7 Icons
- Use **lucide-react** or **heroicons** (outline style)
- Key icons: Mic, MapPin, TrendingUp, AlertTriangle, CheckCircle, XCircle, Download, Eye, FileText, Calendar, Target, Brain, Shield, Users, Home, LogOut, Globe, Volume2

---

## 6. RESPONSIVE BREAKPOINTS
- **Mobile (<640px):** Single column, stacked cards, bottom sheet for map
- **Tablet (640-1024px):** Two-column grids where appropriate
- **Desktop (>1024px):** Full tabbed layout, side-by-side panels

---

## 7. ACCESSIBILITY (Mandatory)
- Semantic HTML5 (header, main, section, article, aside, footer)
- ARIA labels on all icon-only buttons
- Focus visible outlines (Tailwind `focus-visible:ring-2 focus-visible:ring-primary`)
- Color contrast AA minimum
- Keyboard navigable (Tab order logical)
- Screen reader friendly: `sr-only` for chart data tables
- Hindi text: `lang="hi"` on Hindi segments

---

## 8. ERROR HANDLING UX
- **Network error:** Toast "Unable to connect to server. Please check your connection."
- **Validation error:** Inline field errors (red text below input)
- **401/403:** Redirect to `/login` with "Session expired. Please log in again."
- **500:** Toast "Something went wrong. Please try again."
- **Loading states:** Skeleton loaders for cards, spinner for buttons

---

## 9. KEY USER JOURNEYS (For Design Validation)

### Journey A: Voice-First Discovery (Demo Script)
1. Login → Dashboard
2. Tap Mic → *"I have one lakh rupees, want to start dairy in Ghoti"*
3. Transcript appears → Send
4. Auto-navigate to Discovery → shows ranked list with Dairy at top (score 85)
5. Tap "Analyze" on Dairy card

### Journey B: Full Analysis → Simulation → PDF
1. Analysis screen loads → Overview tab
2. See Loan Comparison: Max ₹9L vs Recommended ₹6.5L (buffer highlighted)
3. DSCR shows "Healthy (2.7)"
4. Click "Run Survival Simulation"
5. Move Cost Shock to +20% → verdict updates to deficit month 14
6. Click "Download PDF" → bank-ready document

### Journey C: Bilingual Toggle
1. Any screen → click Language Toggle (EN → HI)
2. All labels instantly switch to Hindi
3. Data values unchanged
4. Toggle back → English restored

---

## 10. DELIVERABLES FOR DESIGN MODEL

Generate the following for **each screen**:

1. **Wireframe/Layout** (mobile + desktop)
2. **Component Breakdown** (reusable components with props)
3. **Color/Token Application** (which semantic color where)
4. **Interaction States** (hover, focus, loading, error, empty, success)
5. **Responsive Behavior** (stacking, hiding, reformatting)
6. **Hindi Label Mapping** (for i18n JSON)

**Priority Order for Design Generation:**
1. Dashboard/Chat Shell (most unique)
2. Analysis Screen (most complex — all 4 tabs)
3. Discovery Screen (card ranking pattern)
4. Survival Simulator (chart + controls)
5. Map Screen (Leaflet integration)
6. Login/Signup (auth flow)
7. Goal Seek (simple form → result)
8. Report History (table + actions)
9. Post-Loan Preview (static roadmap)

---

## 11. BACKEND INTEGRATION NOTES FOR FRONTEND DEV

- **CORS:** Backend allows `http://localhost:5173` (Vite default)
- **JWT:** Send as `Authorization: Bearer <token>` header (axios interceptor)
- **PDF Download:** `responseType: 'blob'` → createObjectURL → anchor.click()
- **Map Tiles:** Require internet — show offline notice if tiles fail
- **Voice:** Only works on HTTPS or localhost (Chrome requirement)
- **Number Formatting:** Indian numbering (1,00,000 not 100,000) — use `Intl.NumberFormat('en-IN')`

---

## 12. SAMPLE DATA FOR DESIGN MOCKUPS

Use **Ghoti + Dairy + ₹1,00,000 margin** as primary demo case:

```
Expected Analysis Output:
- Project Cost: ₹10,00,000
- Loan: ₹9,00,000 (Term Loan Scheme, 8%, 7yr, 6mo moratorium)
- EMI: ₹14,100/month
- Recommended: ₹6,50,000 project / ₹5,85,000 loan / EMI ₹9,160
- Buffer: ₹3,15,000
- Opportunity Score: 85 (High) — 2 competitors, 8,420 population
- DSCR: 2.7 (Healthy) — Net income ₹13,500 / EMI ₹9,160
- Survival: Base case survives 24mo; +20% cost shock → deficit month 14
- Secondary: Vermicompost from dairy waste
- Health Score: ~78 (🟢 Proceed)
- Supply Risk: Medium (fodder seasonal)
- Peer Benchmark: 24 firms, ₹41k avg, 78% survival
- Roadmap: 5 milestones (Months 1,2,3,6,12)
```

---

## 13. FINAL NOTES FOR DESIGN MODEL

- **Do NOT** design for features not in this spec (no chatbot, no live RAG, no external APIs)
- **Every number** in UI must map to a field in the API response DTOs above
- **Hindi toggle** is a core requirement — design for text expansion (Hindi ~1.3x English length)
- **Voice** is a first-class input method — mic button prominence matters
- **Map** is the only feature needing live internet — design graceful degradation
- **PDF** is generated by backend — frontend only triggers download
- **PWA** offline caching applies to reference data + shell — not user data

---

**This prompt is complete and self-contained.** A design model with this document + the backend API contract can generate all screens, components, interactions, and responsive behaviors for the GraminSaathi frontend.