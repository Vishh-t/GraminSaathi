# Recommendation Engine — Build Log

Tracks actual build progress against `GraminSaathi_Recommendation_Engine_Plan.md`. This file is referenced
by `Village.java`'s Javadoc — it didn't exist yet when that comment was written; created now to close that
gap and give future sessions (human or AI) a real status to read instead of guessing from code.

## Step 1 — Village master table + autocomplete: COMPLETE (2026-09-24)

**Schema / service layer** (pre-existing before this session):
- `Village` entity, `VillageRepository`, `VillageService` (search/import/seed/dedup logic)
- `VillageCsvParser`, `VillageDataInitializer` (startup-only loader — deliberately no HTTP write route)

**Built this session:**
- `VillageController` — `GET /api/villages/search?q=&state=&limit=`, public, read-only autocomplete
- `VillageSearchResponse` DTO (separate from the old demo-data `VillageResponse`)
- Wired the new route into `SecurityConfig` as `permitAll`
- `VillageCsvParser.parse()` rewritten to stream row-by-row via a `Consumer<Row>` instead of building a
  full `List<Row>` in memory first — needed once real files got into the hundreds of MB
- `VillageService.importCsv` now flushes + clears the Hibernate persistence context every 1,000-row batch,
  so a 600k+-row import doesn't pin every entity in the session for the whole transaction
- `hibernate.jdbc.batch_size=1000` + `order_inserts=true` added so those batches are actually sent as
  batched SQL, not one round-trip per row
- `VillageDataInitializer` extended to accept a **directory** for `import-path`, not just one file — it
  imports every `.csv` inside in name order, each as its own transaction (one bad file can't break the rest)

**Real data loaded (2026-09-24, two runs — first interrupted, second completed the remainder):**
- Source: Census 2011 PCA (`2011-IndiaStateDistSbDistVill-0000.csv`, all-India, rural villages only) fuzzy-matched
  by name+district against per-state LGD directory exports (`lgdirectory.gov.in`, 34 state/UT `.xlsx` files),
  via a locally-run Python script (`Population Data Csv/village_processor.py`, pandas + thefuzz)
- Output: 32 state/UT CSVs in `Population Data Csv/data/output/`, already matching the importer's exact schema
- **~640,848 total villages imported** — inserted this run: 384,488; already present from the first
  (interrupted) run: 256,360; skipped as invalid: 21. This lines up almost exactly with the official
  Census 2011 total village count (~640,867), which is a strong sanity check on the pipeline.
- LGD-code match rate is partial (spot-checked at ~25% for Andhra Pradesh) — expected, since Census and LGD
  use different code systems and matching is by fuzzy name; `lgd_code` is nullable by design for this reason,
  and dedup falls back to name+district+state when it's absent.

**Known data-quality caveats, not yet cleaned up:**
- A handful of rows across states have garbage in `district` (e.g. the state name leaking in as district) —
  didn't block import since only `name`/`district`/`state` being *non-blank* is enforced, not that they're
  *correct*. Low row count, not fixed yet.
- Andhra Pradesh data reflects the **2011 undivided state** — districts now in Telangana (Adilabad,
  Karimnagar, Warangal, etc.) are still tagged `state=ANDHRA PRADESH` in this import. Worth knowing before a
  demo if a judge picks a Telangana village by name.
- No `latitude`/`longitude` in this import — the Census PCA source doesn't carry coordinates. Village rows
  exist without geocoding for now.

**Not started yet:**
- Nothing from `DiscoveryService`/`FeasibilityScoreService` has been wired to the real `Village` table yet —
  they still run entirely on `DemoData`/`DemoDataLoader`. That's step 2+ territory (`village_features` ETL,
  demand-supply scoring), not step 1.

## Steps 2–7: NOT STARTED

See `GraminSaathi_Recommendation_Engine_Plan.md` for what each covers. Step 2 (`village_features` ETL —
competitor counts via OpenStreetMap Overpass, coverage confidence via WorldPop/GHSL) is the next one in
line, and is the first step likely to need an external API key (Overpass itself is free but rate-limited
for bulk use; WorldPop/GHSL access varies by dataset — to be confirmed when we get there).
