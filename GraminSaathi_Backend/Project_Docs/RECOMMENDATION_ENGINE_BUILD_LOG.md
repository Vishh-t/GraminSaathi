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

## Step 2a — village_features population/households group: COMPLETE, RUN SUCCESSFULLY (2026-09-25)

**Found already in place at the start of this session (not previously reflected in this log):**
- `VillageFeatures` entity (`village_features` table, `villageId` as a plain non-FK column so a lazy
  `Village` proxy can never leak into an API response) — only the population column-group filled in;
  competitor counts (2b) and OSM coverage confidence (2c) are separate nullable column groups, not added
  yet, by design (`ddl-auto=update` can add them later without touching existing rows)
- `PopulationProjector` — pure compound-growth calculator (Census 2011 → target year), national default
  12.18% decadal (Census 2011 rural 2001–11), with an optional per-state override CSV
  (`data/state_rural_growth.csv`, not present yet, so the national default applies everywhere right now)
- Neither had a repository, a service to actually run the projector over the village table, a way to
  trigger it, or a test — i.e. the calculation existed but nothing wrote a single `village_features` row.

**Built this session, to close that gap:**
- `VillageFeaturesRepository` — plain `JpaRepository<VillageFeatures, Long>`
- `VillageFeaturesService.runPopulationProjection(year)` — pages through the `villages` table
  (id-ordered, 1000/page), calls `PopulationProjector` per row, upserts the population group of that
  village's `VillageFeatures` row (villageId is the shared key, so this only ever touches population
  columns — future 2b/2c passes are additive). Villages with no Census 2011 population are skipped, not
  guessed at. (Originally one big transaction with flush+clear via Hibernate `saveAll` — superseded, see
  "Fourth fix" below.)
- `VillageFeaturesInitializer` — `ApplicationRunner`, off by default
  (`graminsaathi.features.population.run-on-startup=false`), same reasoning as `VillageDataInitializer`:
  no public HTTP route should be able to trigger a 640k-row write. Flip the flag to `true` for one run,
  then back to `false`; re-running is safe (idempotent upsert on `villageId`) but pointless until the
  source data or target year changes. Projection year defaults to the current year, overridable via
  `graminsaathi.features.population.projection-year`.
- `VillageFeaturesServiceTest` — unit test with mocked repositories/`EntityManager`, covers a village
  with population (gets projected + all fields populated correctly) and one without (skipped, no row).

**Two real bugs found and fixed after the first live run attempt (2026-09-25, same session):**
- `@Value("${graminsaathi.features.population.projection-year:#{T(java.time.Year).now().getValue()}}")`
  was broken — mixing a `${...}` property placeholder with a `#{...}` SpEL expression *inside* its default
  does not evaluate the SpEL; Spring reads it as a literal string. Since the property was never set
  explicitly, this crashed bean creation (couldn't convert that literal string to `int`), which failed the
  **entire** Spring context, not just this feature — the app never started at all. Fixed: the property now
  defaults to `0` (a real int), and the code falls back to `Year.now()` in Java when it sees `0`.
- No explicit `@Order` on either `ApplicationRunner` — `VillageFeaturesInitializer` depends on villages
  already being in the table, but nothing guaranteed `VillageDataInitializer` ran first. Fixed:
  `VillageDataInitializer` is now `@Order(1)`, `VillageFeaturesInitializer` is `@Order(2)`.
- Also added: a startup log line (`starting population pass for year ... over N villages ...`) and a
  progress log every 10 batches (10,000 villages) with elapsed seconds — the pass can run for several
  minutes (longer than the village import) since it uses offset pagination, which gets slower on later
  pages; before this there was no visible sign of life between the first and last log line.

**Third bug — offset pagination measured as effectively quadratic, fixed with keyset pagination (2026-09-25, live run data):**
- Real progress log from the live run: page 10 → 8s, page 20 → 31s, page 30 → 71s, page 40 → 135s,
  page 50 → 214s elapsed. That's not linear — each successive 10-page block took longer than the last,
  consistent with `OFFSET n LIMIT 1000` making Postgres scan past `n` rows before every page. Extrapolating
  that curve across all ~633 pages put total runtime in the range of hours, not minutes — confirmed the
  "fine for a one-off job" assumption in the original code comment was wrong at this row count.
- Fixed: `VillageRepository.findByIdGreaterThanOrderByIdAsc(lastId, pageable)` (keyset/seek pagination —
  `WHERE id > :lastId ORDER BY id LIMIT :batchSize`, seeks straight to the right spot via the primary key
  index every time) replaces `findAll(Pageable)` in `VillageFeaturesService.runPopulationProjection`. Stays
  fast at any point in the table regardless of how many rows came before. `VillageFeaturesServiceTest`
  updated to mock the new method.

**Fourth fix — the real slowdown cause, found after the keyset change did NOT fix it (2026-09-25):**
- With keyset pagination already in place the run was still slowing down linearly (10 batches: 6s, then
  20s, then 34s per block; extrapolated to hours). So OFFSET paging was not the (main) cause.
- Most likely cause: `village_features` has an assigned id (`villageId`, no `@GeneratedValue`), so Spring
  Data's `saveAll` treats every entity as "not new" and calls `merge()`, i.e. one SELECT per row before each
  insert (~1000 extra queries per batch), all inside a single 640k-row transaction. Not isolated by
  experiment — several changes went in together (below), so exact per-change contribution is unknown.
- Changes made together:
  - `VillageFeaturesService` now writes with a JDBC batch upsert (`JdbcTemplate.batchUpdate`,
    `INSERT ... ON CONFLICT (village_id) DO UPDATE` on the population columns only) instead of
    `villageFeaturesRepository.saveAll`. Still idempotent; still never touches 2b/2c columns.
  - No more `@Transactional` on the run: each batch commits on its own, so an interrupted run keeps its
    progress and re-running is safe. `EntityManager` and `VillageFeaturesRepository` are no longer used by
    the service.
  - `ANALYZE villages` once at the start (freshly bulk-imported table may lack planner stats).
  - `?reWriteBatchedInserts=true` added to the JDBC URL in `application.properties`.
  - Progress log now also prints `(read Xs, write Ys)` so any future slowdown shows which side it's on.
  - `VillageFeaturesServiceTest` rewritten against a mocked `JdbcTemplate` (not yet run).
- Indexes added: `villages (state, district)`, `villages (latitude, longitude)` (for step 2b proximity
  lookups — unused until villages have coordinates), `village_features (population_projected)`. Created by
  `ddl-auto=update` on startup.

**Run result (2026-09-25, live):** the whole pass finished in **~35s** (read ~8s, write ~25s) instead of
an extrapolated several hours, flat batch times throughout.
- 632,893 villages processed -> **590,687 rows written to `village_features`**, 42,206 skipped (no Census
  2011 population). Projection year 2026.
- Note the villages table holds 632,893 rows now vs ~640,848 recorded at import time above; not yet
  reconciled.

**Not done yet (2a leftovers):**
- `VillageFeaturesServiceTest` (rewritten for the JDBC path) has not been run through `mvnw.cmd test`.
- `graminsaathi.features.population.run-on-startup` is still `true` in `application.properties` — set it
  back to `false` so the pass doesn't re-run on every startup.
- No state-level growth-rate overrides (`data/state_rural_growth.csv` doesn't exist) — every village uses
  the national 12.18% default.

## What's left in step 2 (2b, 2c) — scope is now ALL of India, not a demo subset

- **Phase 1 — coordinates for every village** (blocker for 2b and 2c): `villages` has no `latitude`/
  `longitude` populated (Census PCA has none). Plan: SHRUG village polygons (Development Data Lab,
  Census-2011-keyed, near-total coverage) → representative point per village, joined on Census **PC11**
  code, not name — see phase 1 prep below. Licence check needed: the copy found (AIKosh) is CC BY-NC-SA
  (non-commercial); OSM place nodes are the fallback if that blocks production use.
- **2b — competitor counts per category**: nullable column group on `village_features`. Extraction plan:
  offline Geofabrik India `.osm.pbf` (NOT public Overpass — not viable at 600k-village scale), filtered
  with `osmium tags-filter` to `shop=*`/`amenity=*`/`craft=*`/`office=*`, exported to a flat CSV of
  (lat, lon, tag, name). **No business-category list exists anywhere in the codebase yet** (checked this
  session — `RoadmapMilestoneService` etc. are unrelated; no `Category`/`BusinessType` model, no PMEGP
  reference-figures file found) — the OSM-tag → category mapping that 2b needs has nothing to map *to*
  until that list exists. Needs coordinates (phase 1) before per-village counting can happen.
- **2c — OSM coverage confidence**: nullable column group. Extraction plan: OSM buildings (same `.pbf`,
  `building=*`) vs WorldPop India 2020 constrained raster (or GHSL GHS-BUILT-S) summed in the same radius.
  Needs coordinates (phase 1).
- **Amenities** (power, water, roads — plan 2.1/2.7) are not in `village_features` yet; needed by the
  step 4 hard filters. Not scheduled in the 2a/2b/2c split — decide where they go.

## Phase 1 prep (coordinates) — schema + backfill infra built, NOT YET RUN (2026-09-25)

**Finding this session:** the raw Census source (`2011-IndiaStateDistSbDistVill-0000.csv`) carries
`State`/`District`/`Subdistt`/`Town-Village` numeric codes — the official Census **PC11** identifiers,
which are the join key SHRUG's village polygons use. But `village_processor.py`'s output CSVs were
dropping them; only the fuzzy-matched `lgd_code` (partial, ~25% for AP) made it into `villages`. Without
PC11 codes, coordinate-loading would have to fuzzy-match by name again — same risk as the original
Census↔LGD match. Fixed the leak, and added the plumbing to backfill the codes into the **already-loaded**
632,893 rows without re-importing (the importer is insert-only/skip-existing, so simply re-running it
would just skip everything and backfill nothing).

**Built, not yet run (all depend on someone re-running the Python script locally, which wasn't done this
session):**
- `village_processor.py` — `villages_df` rename step and each state's `final_columns` now keep
  `pc11_state_code`, `pc11_district_code`, `pc11_subdistrict_code`, `pc11_village_code`. Existing files in
  `data/output/` are stale until this is re-run.
- `Village` entity — added nullable `pc11StateCode`/`pc11DistrictCode`/`pc11SubdistrictCode`/
  `pc11VillageCode` (NOT the same thing as `lgdCode`), plus `coordinatesSource`/`coordinatesAsOf` for when
  phase 1 actually loads lat/lon (source-and-date rule, same as every other feature group).
- `VillageCsvParser.Row` — four new optional columns, backward compatible (old CSVs without them just get
  nulls there).
- `VillageRepository` — `findIdsByLgdCode()` / `findIdNameDistrictState()`, id-carrying versions of the
  existing dedup-key queries, for a backfill to match without a query per row.
- `VillageEnrichmentService.backfillPc11Codes(Reader)` — new service, NOT `VillageService.importCsv` (which
  can only insert, never update an existing row). Same write pattern as the population pass: plain JDBC
  batch `UPDATE ... WHERE id = ?`, no Hibernate in the write path, committed per batch. Matches each CSV
  row to an existing village by `lgd_code` first, else by the same name+district+state key the importer
  uses; unmatched rows are counted, never guessed at.
- `VillageEnrichmentInitializer` — `ApplicationRunner`, `@Order(1)`, off by default
  (`graminsaathi.villages.pc11-backfill-path`, empty = skip), same reasoning/shape as
  `VillageDataInitializer` (no HTTP route, directory-or-single-file, one bad file doesn't abort the rest).

**To actually get PC11 codes into the DB (not done):** re-run `village_processor.py` (regenerates
`data/output/*.csv` with the new columns) → set `graminsaathi.villages.pc11-backfill-path` to that
directory → one startup run → flag back to empty. Same shape as the population pass's run flag.

**Also not done:** `mvnw.cmd test` hasn't been run since these changes (no new test added for
`VillageEnrichmentService` yet either — flag if that's wanted before this runs for real).

## Correction to phase 1 status above (found this session, 2026-09-25)

The "Phase 1 prep" section above undersold where things actually are: `VillageEnrichmentService.backfillCoordinates()`
and its wiring in `VillageEnrichmentInitializer` (`graminsaathi.villages.coordinates-backfill-path` /
`graminsaathi.villages.coordinates-as-of`, both already present in `application.properties`) are **fully
built and were NOT reflected here before now** - not a gap found this session, just a logging gap. Matches
by PC11 code first, then `lgd_code`, then name+district+state, same insert-safe JDBC-batch pattern as
everything else. So the actual remaining blocker for coordinates is only the data file itself.

**CSV format required from the friend's file** (read by `VillageCsvParser`, header-matched case-insensitive,
column order doesn't matter): `lgd_code, name, block, district, state, latitude, longitude, population_2011,
households_2011, source, pc11_state_code, pc11_district_code, pc11_subdistrict_code, pc11_village_code`.
Only `name`, `district`, `state` are mandatory; everything else is optional per-row. `source` should say
where that row's coordinate came from (e.g. `shrug_pc11` vs `osm_place`) so it's traceable later.

## Data-independent steps pass (2026-09-25) - steps 3-5 built ahead of coordinates/competitor data

Scope: build everything in steps 3, 4, 5 that doesn't actually need coordinates, competitor counts, or
coverage data to exist - wired to read real data where it exists (step 2a's households) and to fail
gracefully (return "no data yet", not a wrong number or zero) where it doesn't, so nothing here needs a
second editing pass once the blocked data lands.

**New: `BusinessCategory` (real DB entity, replaces the old `DemoData.BusinessCategoryData` for this
purpose)** - `model/BusinessCategory.java`, `repository/BusinessCategoryRepository.java`,
`data/business_categories_seed.json` (same 4 categories as `demo_data.json`, carried over),
`data/BusinessCategoryDataInitializer.java` (on by default, insert-only/idempotent by name - a handful of
rows, no 640k-row cost to gate behind a flag). Adding a category from here on is a JSON edit, not a code
change. Two new field groups beyond the old economics fields:
- `hardFilterRulesJson` / `fitRequirementRulesJson` - JsonLogic against `applicant.*`, reusing
  `SchemeEligibilityService`'s evaluator (no new rule engine needed). Left null/unset in the seed for now
  (didn't want to invent blocking criteria without real usage data) except `fitRequirementRulesJson`,
  which is populated per category with weighted, non-blocking requirement checks.
- `requiresElectricity` (Boolean) - declared but NOT read by anything yet; there's no amenities column
  group on `village_features` to check it against. Documented as a one-line wiring job once that exists.

**`VillageFeatures` extended** with two more nullable column groups (pure schema addition, `ddl-auto=update`
handles it): `competitorCountsJson`/`competitorCountsSource`/`competitorCountsAsOf` (step 2b) and
`osmBuildingCount`/`expectedBuildingCount`/`coverageConfidenceRatio`/`coverageConfidenceLabel`/
`coverageConfidenceSource`/`coverageConfidenceAsOf` (step 2c). Both stay null until their respective ETL
jobs run (still blocked on coordinates); nothing writes to them yet.

**`service/PersonFitScoreService.java`** (step 4, person-fit half) - "can this person actually run it?"
Three weighted pillars, all readable today: capital adequacy (35%, available margin capital vs. the
category's required margin), land adequacy (15% when the category declares `minLandHoldingAcres`, unknown
answers score neutral not zero), and the category's weighted fit-requirement rules (remainder). Returns an
overall 0-100 score, a label, and the per-rule breakdown (matched/not) for the eventual "why it ranked
here" explanation.

**`service/HardFilterService.java`** (step 4, hard-filter half) - pass/fail before scoring. Applicant-answer
filter works today (JsonLogic against `hardFilterRulesJson`). Amenity filter (electricity/water/roads)
is a stub that always passes - written to already take `VillageFeatures` as a parameter so wiring in the
real check later doesn't touch any call site, just the one method body.

**`model/HceCategorySpend.java` + `repository/HceCategorySpendRepository.java`** (step 3 demand-side
reference data) - schema only, deliberately NOT seeded with any figures. State-level MoSPI HCES spend
numbers are a data-entry task (transcribing published survey tables), not a coding task; a fabricated
placeholder rupee figure would be worse than an empty table, since a wrong number looks authoritative and
a missing row visibly reads as "no data yet."

**`service/DemandSupplyScoreService.java`** (step 3) - demand = real `householdsProjected` (step 2a) x
household spend (schema above, unseeded); supply = competitor count (from `VillageFeatures.competitorCountsJson`,
step 2b, unpopulated) x category reference revenue. Returns an explicit confidence tier
(`INSUFFICIENT_NO_POPULATION` / `LOW_PARTIAL_DATA` / `MEDIUM_FULL_DATA`) and a plain-language note on what's
still missing, rather than a market score computed from placeholder inputs. Starts returning real scores
the moment both the HCES table is seeded and 2b runs - no code change needed then.

**`service/FinancialRangeService.java`** (step 5) - pessimistic/base/optimistic revenue, operating cost and
profit ranges off `BusinessCategory`'s reference figures. The plan's "scale by state consumption level"
factor is taken as an `Optional<Double>` parameter (empty today -> falls back to 1.0/unscaled) rather than
looked up internally, so it can be swapped for a real HCES-derived ratio later with zero change to this
class. Spread constants (0.70x / 1.30x pessimistic/optimistic) are named constants, easy to recalibrate
once real peer-benchmark variance data exists.

**Not done in this pass, by design:** none of the four new services were wired into `DiscoveryService`,
`FeasibilityScoreService`, or any controller yet - they were built additively so the working demo-based
flow kept working untouched. See "Discovery wiring" below for that follow-up pass.

## Discovery wiring — DiscoveryService/DiscoveryController on real data (2026-09-25/26, two sessions)

Swapped `DiscoveryService`'s `DemoData`/`DemoDataLoader` reads for the real `Village`/`VillageFeatures`/
`BusinessCategory` entities and wired in the four data-independent services from the pass above
(`HardFilterService`, `PersonFitScoreService`, `DemandSupplyScoreService`, `FinancialRangeService`). This
is the "natural next step" the previous section flagged.

**`VillageRepository`** - added `findFirstByNameNormalized(String)`, an exact single-village lookup for a
caller (like `DiscoveryService`) that already has one specific village name, as opposed to `search()` which
is fuzzy/multi-result for autocomplete.

**`DiscoveryService`** - rebuilt around entities instead of `DemoData.*`: looks up the `Village` by
`VillageRepository.findFirstByNameNormalized(VillageService.normalize(name))`, loads its `VillageFeatures`
(nullable - most real villages have none of the 2b/2c groups yet), iterates `BusinessCategoryRepository.findAll()`,
runs `HardFilterService.evaluate()` first (categories that fail are excluded outright, not scored low), then
`PersonFitScoreService`, `DemandSupplyScoreService`, and `FinancialRangeService` per surviving category.
`FeasibilityScoreService` (demo-only) was dropped from this service entirely. Sorted by person-fit score,
not market score - market score is usually still null until 2b/2c/HCES land, so it can't be the primary sort
key yet.

**`DiscoveredBusiness` record** - replaced the old `opportunityScore`/`opportunityLabel` fields with
`marketScore`/`marketConfidence`/`marketExplanation` (from `DemandSupplyScoreService`) and
`personFitScore`/`personFitLabel` (from `PersonFitScoreService`), plus a `financialRange`
(`FinancialRangeService.FinancialRangeResult`) field.

**`DiscoveryController`** - now takes `@AuthenticationPrincipal User` and resolves the applicant via
`ProfileService.getApplicantProfile(user)`, same auto-pull pattern `AnalysisController` already uses for
`/analyze` - a logged-in user's saved intake-form profile is used with zero frontend changes needed to at
least run; an anonymous caller gets an empty profile (every hard filter that reads `applicant.*` passes,
person-fit scores capital adequacy only). `discover()` now takes `(request, applicant)`, not just `(request)`.

**`DiscoveredBusinessResponse` DTO** - matching fields added (`marketScore`/`marketConfidence`/
`marketExplanation`, `personFitScore`/`personFitLabel`), plus a nested `FinancialRange`/`MonthlyRange` pair
carrying `FinancialRangeService`'s pessimistic/base/optimistic output. Controller's `mapToResponse` updated
to match.

**Tests** - `DiscoveryServiceTest` and `DiscoveryControllerTest` were left stale by the refactor above (still
mocking `DemoDataLoader`/`FeasibilityScoreService`, or asserting the old `opportunityScore` field) and were
rewritten to match: the service test now mocks the seven real constructor dependencies and covers the same
four cases as before (sorted results, both affordability-flag branches, village-not-found); the controller
test mocks `DiscoveryService` directly and asserts `personFitScore` instead of the removed `opportunityScore`.
ProfileService is deliberately left un-mocked in the controller test, matching `AnalysisControllerTest`'s
existing pattern, since an anonymous MockMvc call never reaches its DB-backed branch. **Not run through
Maven yet in this session** - `mvnw.cmd test` should be run to confirm both compile and pass before relying
on them.

## Amenities column group + electricity hard filter (2026-09-25/26)

Closed the "amenities not scheduled" gap flagged in both `VillageFeatures` and `BusinessCategory`'s
javadocs. Decided (this session) to leave the real business-category-list expansion for when the friend's
data files land, so this pass only does the part that's independent of that: the column group + wiring the
one flag that already exists on `BusinessCategory`.

**`VillageFeatures`** - new nullable amenities group: `hasElectricity`/`hasPipedWaterSupply`/
`hasAllWeatherRoad` (all `Boolean`) + `amenitiesSource`/`amenitiesAsOf`, same source-and-date convention as
every other group. Schema-only addition (`ddl-auto=update`); nothing writes to these columns yet - no data
source picked.

**`HardFilterService.passesAmenityFilter`** - no longer a stub. Checks `BusinessCategory.requiresElectricity`
against `VillageFeatures.hasElectricity`, null-safe both directions: unset category flag or missing/unknown
village row always passes; only fails when the category positively requires electricity and the village is
positively known not to have it. Water and road columns exist but aren't checked - `BusinessCategory` has no
`requiresWaterSupply`/`requiresAllWeatherRoad` flags yet, so there's nothing on the category side to check
them against.

**`BusinessCategory`** javadoc updated to match - `requiresElectricity` is no longer "declared but not read".

**Not done:** no data source feeds the new columns (same position 2b/2c are in - a future ETL/backfill job,
same JDBC-batch-upsert shape as `VillageEnrichmentService`'s other backfills, would populate it). No test
added for the new `passesAmenityFilter` branches yet (per your rule, tests all land together at the end).
`mvnw.cmd test` not run against this change.

## Steps 2b–7: mostly NOT STARTED

See `GraminSaathi_Recommendation_Engine_Plan.md` for what each covers, and the phase 1/2b/2c breakdown
above for step 2's remainder. Step 2b's competitor extraction needs a business-category list to exist
first (see above — genuinely missing, not just unverified). Step 2c's WorldPop/GHSL access is free, no key
needed, just a large one-time download.
