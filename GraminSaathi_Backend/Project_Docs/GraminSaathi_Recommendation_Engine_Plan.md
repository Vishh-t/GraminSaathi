# GraminSaathi: Business Recommendation Engine (Production Plan)

This is the plan for building the business recommendation feature properly for a live deployment, not a demo. It replaces the shortcuts in `DISCOVERY_REAL_DATA_PLAN.md` (three hardcoded villages, dropped population, guessed saturation thresholds).

Costs and data-source details below are from general knowledge. Verify current prices and access methods before committing to any of them.

---

## 1. What the user finally gets (the output)

A user picks their village, answers a short set of questions (capital, land, assets, skills, time, category, etc.) and gets a **ranked list of businesses** for that village. For each business:

| Output | What it means |
|---|---|
| **Market score** | Is there unmet demand in this village for this business? Based on the demand-supply gap in rupees. |
| **Person fit score** | Can this person actually run it? Based on their capital, assets, skills, time, gender, land. |
| **Confidence level** | How much to trust the numbers for this village (low / medium / high), driven by data coverage. |
| **Revenue and profit range** | Pessimistic, base and optimistic monthly figures, not a single number. |
| **Project cost and margin money** | What it costs to start and how much the person must put in. |
| **Matched schemes** | Government schemes the person is eligible for, with computed benefit, interest rate, EMI and total repayment. |
| **DSCR and survival curve** | Whether the business can service the loan, and how cash flow behaves over 24 months, including stress cases. |
| **Working-capital warning** | If the person can afford the loan but will run short of operating cash. |
| **Why it ranked here** | A plain-language explanation in the user's language, plus the data sources and dates behind each number. |
| **Bank-ready DPR (PDF)** | The full analysis as a document the person can take to a bank or SCA. |

Illustrative example for one business (numbers are made up to show the shape, not real output):

```
Dairy, village X
Market score: 72 / 100    Person fit: 81 / 100    Confidence: Medium
Monthly profit: Rs 9,000 (pessimistic) / Rs 14,000 (base) / Rs 19,000 (optimistic)
Best matched scheme: <scheme name>, EMI Rs X, DSCR 1.6 (Healthy)
Why: 1,200 households in range, ~Rs X/month spent on milk, 2 mapped dairies serve about 40% of it.
Sources: Census 2011 (projected), HCES, OSM (as of <date>)
```

What it does not give: a guarantee. It gives an evidence-backed estimate with a stated uncertainty range.

---

## 2. How it works

### 2.1 Village as the unit
A village master table in Postgres/PostGIS, keyed by LGD code (Local Government Directory). Fields: name, block, district, state, lat/lng, Census 2011 population and households, projected current population, amenities (power, water, roads). Users pick from autocomplete instead of typing free text, which removes geocoding ambiguity and gives one key to join every dataset on.

### 2.2 Demand in rupees
- Village demand for a category = households x monthly household spend on that category (from the MoSPI Household Consumption Expenditure Survey, rural, by state).
- Existing supply = competitors x typical revenue per shop (the PMEGP-anchored reference figures already in the project).
- Market gap = demand - supply.

This replaces population / competitors and the guessed per-category thresholds with something defensible.

### 2.3 Coverage confidence
OpenStreetMap coverage in rural India is uneven, so a count of zero often means "not mapped". Compare OSM building density with a gridded population layer (WorldPop or GHSL) to estimate how complete mapping is in that area. Low coverage means wider uncertainty and lower confidence, never a confident score.

### 2.4 Hard filters first
Pass/fail before any scoring: a flour mill needs electricity, dairy needs fodder and water, and so on. Sources are Census amenity data plus the user's own answers (land, cattle, labour, skills, capital). Only businesses that survive are scored.

### 2.5 Two separate scores
- **Market score:** demand-supply gap, distance to nearest town and mandi.
- **Person fit score:** capital, experience, time, gender, assets.

### 2.6 Financials as ranges
Reference revenue is a category average, so scale it by state consumption level and show pessimistic, base and optimistic cases. Then run the existing pipeline: scheme matching, EMI, DSCR, survival simulation, working-capital check.

### 2.7 Precompute, don't compute per request
An offline ETL job builds a `village_features` table (population, households, competitors per category, amenities, coverage confidence). Every field stores its source and as-of date. The API reads and scores. This also removes dependence on public API rate limits at request time.

### 2.8 LLM only for language
All numbers stay deterministic. The LLM handles conversational intake, the plain-language explanation of the ranking, and translation.

---

## 3. Data sources

| Data | Source | Notes |
|---|---|---|
| Village identity and codes | LGD | Join key for everything else |
| Population, households, amenities | Census 2011 Village Directory | Static, so project forward with district growth rates and label as estimated |
| Household spend by category | MoSPI HCES | State-level rural spend |
| Competitors, buildings, roads | OpenStreetMap (Overpass) | Uneven rural coverage, needs attribution (ODbL) |
| Coverage check | WorldPop or GHSL | Verify the access method; may need raster preprocessing |
| Schemes | Existing enriched schemes dataset | Already in the project |
| Reference project economics | KVIC / PMEGP project profiles | Already in the project |
| Input commodity prices (optional) | Agmarknet via data.gov.in | Needs an API key |

---

## 4. Build order

1. Village master table plus autocomplete.
2. `village_features` ETL: population first, then competitors, then coverage confidence.
3. Demand-supply gap scoring.
4. Hard filters plus person fit score.
5. Ranges in the financials.
6. Deployment hardening (map services, privacy, monitoring).
7. Feedback loop.

---

## 5. Does it need money before deployment? Yes, some.

Most of the data is free. The money goes on running it reliably.

| Item | Cost | Notes |
|---|---|---|
| Census, HCES, LGD, OSM, WorldPop / GHSL | Free | Your time to process them is the cost |
| App hosting (backend + frontend) | Paid, small | A modest VPS or cloud instance |
| Managed Postgres/PostGIS | Paid, small to moderate | Or run it yourself on the same server, with backups |
| Geocoding / map services | Free only at hackathon scale | Public Nominatim and Overpass are not meant for production. Either self-host (needs a server with real disk and RAM, so check sizing for the India extract) or pay a provider (Google Maps Platform, Mapbox, Mappls) |
| LLM API | Pay per use | Scales with users; cache explanations to cut cost |
| Domain | Paid, small per year | SSL is free via Let's Encrypt |
| Monitoring, backups | Free to small | Free tiers exist |
| SMS / OTP if you add phone login | Pay per message | Skip until needed |
| Legal review of privacy policy and consent | Optional but wise | You handle caste, income, disability data |

Ways to cut the bill: as a CS student, check GitHub Student Developer Pack and cloud startup or student credits. Start with one small server running app + database + self-hosted map data if it fits, and split later. Cache aggressively so third-party calls stay low.

I have not priced any of this for you; get current quotes for the providers you pick.

---

## 6. Deployment hardening

- **Terms of use:** don't send production traffic to public Nominatim or Overpass. Keep OSM attribution.
- **Privacy (DPDP Act):** caste, income and disability are sensitive. Get explicit consent, collect only what matching needs, allow deletion, and don't log the raw profile.
- **Secrets:** API keys in environment variables, used only by the backend, never in `VITE_` variables. Remove real-looking fallback defaults from `application.properties`.
- **Security config:** turn off DEBUG SQL and Spring Security logging in production.
- **Honest labelling:** show "estimated", the data source and the as-of date next to every number.

---

## 7. Feedback loop

Ask users at 6 and 12 months whether they started the business and how it is doing. Use the answers to recalibrate revenue and demand assumptions. The Connect NGO feature could supply field validation. This is what turns an estimate into a system that improves.

---

## 8. Known limitations

- Census 2011 is old; projections carry error.
- OSM undercounts rural shops; confidence levels exist for this reason.
- Category-average revenue is not village-specific; ranges reduce, but don't remove, that error.
- Nothing has yet been compiled or run through Maven for the schemes work; run `mvnw.cmd test` before building on it.

---

## 9. Decisions still open

1. Self-host map services or use a paid provider.
2. Gridded population source (WorldPop vs GHSL) and how to query it.
3. Which states or districts to load first, since a national village table is a big ETL job.
4. Whether to launch with the feedback loop or add it after the first users.
