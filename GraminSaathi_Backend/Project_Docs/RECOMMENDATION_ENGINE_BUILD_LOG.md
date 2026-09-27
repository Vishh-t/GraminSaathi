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

(Superseded for the coordinates/2b/2c blocker specifically by the next section — the friend's 4 files
changed the plan below. The WorldPop/GHSL and business-category-list gaps described above are still real
and still unstarted.)

## Friend's 4 files investigated (2026-09-26) — coordinates/2b/2c blocker re-scoped

The friend supplied 4 files intended to unblock phase 1 (coordinates) + step 2b (competitor counts) +
step 2c-partial (OSM building counts), as an OSM-based alternative to the SHRUG plan above (SHRUG was
never obtained — see phase 1 section, "no SHRUG file was available"). Opened all 4 directly this session
to check actual schemas before writing any script against assumed columns. All at
`Population Data Csv/csv files/`:

| file | size | actual columns | row count |
|---|---|---|---|
| `villages.csv` | 14 MB | `osm_id, name, place_type, lat, lon` | 370,106 |
| `village_counts.csv` | 21 MB | `village_id, name, place_type, lat, lon, business_count, building_count, population_est` | 370,106 (same places as villages.csv, same order, 3 columns appended) |
| `businesses.csv` | 48 MB | `lat, lon, shop, amenity, craft, office, name` (raw OSM POI extract, all of India — includes plain urban POIs like restaurants/parking/fuel, not just the 4 target categories) | not yet counted exactly, ~1M+ |
| `buildings.csv` | 1.45 GB | `lat, lon, area_m2` (raw building footprint points; `area_m2` is 0 in every sampled row — not usable as a weight, treat as point-count only) | not yet counted — too large to load fully, will be streamed in chunks by the processing script |

**Corrected understanding of `village_counts.csv`'s `business_count`/`building_count` columns:** these are
the friend's own precomputed totals per named place (methodology/radius unknown, not documented, not
assumed reusable), and they're TOTALS, not broken down by our 4 business categories. They are NOT a
substitute for a real spatial join: (a) step 2b needs a per-category breakdown, which only `businesses.csv`
can supply; (b) `buildings.csv` turned out to be a real raw file (confirmed this session via screenshot —
it had not been placed in `csv files/` yet the first time this folder was checked), so
`village_features.osmBuildingCount` will be computed directly from it via our own 5km-radius join (same
radius convention as `demo_data.json`'s `population_5km_radius`), not taken from `village_counts.csv`'s
`building_count` column — keeps the methodology consistent and traceable to one source, which the friend's
precomputed column isn't.

**The real blocker, confirmed by opening the files (not assumed): the join key.** None of the 4 files
carry `district`, `state`, `lgd_code`, or any PC11 code — `osm_id` in `villages.csv` is present as a column
but is 100% empty (0/370,106 populated). `VillageEnrichmentService.backfillCoordinates()` matches only by
pc11 code → `lgd_code` → exact `name+district+state` (see `matchByLgdThenName` in that class) — none of
which this data has. Name-only matching against the 640k-row Census table is unsafe on its own: **164,848
of the 370,106 OSM place names (45%) are not even unique within `villages.csv` itself**, before comparing
against Census's much larger name-collision rate.

A true spatial nearest-neighbor match (what the original plan assumed as the fallback) doesn't actually
apply either: the Census `villages` table has no `latitude`/`longitude` to match against — that's the exact
gap phase 1 is trying to fill, so there is nothing to do point-to-point nearest-neighbor matching against
on the Census side.

**Resolution decided this session:** reverse-geocode each OSM place's `(lat, lon)` to a **state** via
point-in-polygon against a small free India state-boundary file (`geohacker/india`'s `india_state.geojson`
on GitHub, 35 features, ~23 MB, verified downloadable and valid geometry this session), then match
`name normalized + state` against `data/output/village_master_<state>.csv` (the same per-state files
already used for the original village import — no DB query needed, these ARE the source of the DB rows).
Two useful properties of this specific file, verified this session:
- It uses **old (pre-2014) state boundaries**, so Telangana falls inside the "Andhra Pradesh" polygon —
  matching this project's Census master data exactly, since there is no separate
  `village_master_telangana.csv` (Andhra Pradesh data reflects the 2011 undivided state, per the step 1
  section above). No special-case Telangana/AP remapping needed.
- A few state names differ from the master-CSV filenames and need a small manual rename table before
  matching (`Orissa` → `odisha`, `Uttaranchal` → `uttarakhand`, `Delhi` → `nct_of_delhi`, etc.) — checked
  against the actual 31 filenames in `data/output/`.

When `name+state` matches more than one Census candidate in that state (common — verified 10 separate
candidates for the name "Shrirampur" in Maharashtra alone), use `village_counts.csv`'s `population_est`
as a tiebreaker against each candidate's `population_2011`; if no candidate is a clear population match,
leave the row unmatched rather than guessing (same "honest can't-tell" convention this log already follows
for missing data elsewhere). Matched rows get `lgd_code` (when the candidate has one) or the candidate's
own exact `district`/`state` text written into the coordinates-backfill CSV, so `backfillCoordinates()`
matches them through its existing `lgd_code`-then-`name+district+state` path with **no Java changes needed
for phase 1 itself**.

**Business-category tag mapping (step 2b), sampled from `businesses.csv` this session:** confirms the
original plan's prediction — `shop=tailor`/`craft=tailor` and `shop=convenience`/`grocery`/`general`/
`supermarket` map cleanly to Tailoring and Retail/Kirana Store; no OSM tag reliably captures a village-level
dairy or flour-mill business, so those two categories should carry a lower-confidence flag on the resulting
counts rather than trusting a near-zero competitor count as a real market signal (same call the original
plan leaned toward — now confirmed against real sample data, not just anticipated).

**Not done yet (blocking, in order):**
1. `resolve_osm_places.py` (new script, `Population Data Csv/`) — implements the state-boundary +
   name+state+population-tiebreak match above; outputs a coordinates-backfill CSV in `VillageCsvParser`'s
   exact format plus an unmatched-rows CSV for review. Logic prototyped and spot-checked against
   `village_master_maharashtra.csv` this session (found the 10-way "Shrirampur" collision that motivated
   the population tiebreak) but not yet run end-to-end or written to the project.
2. A second script to compute, per matched village and using the standard 5km radius: (a) per-category
   competitor counts from `businesses.csv` (KD-tree, tag→category map above, category-level confidence
   flag for Dairy/Flour Mill) and (b) `osmBuildingCount` from `buildings.csv` (same KD-tree, streamed in
   chunks — the file is 1.45 GB, too large to load at once). Not started.
3. Two new `VillageEnrichmentService` methods (Java) to backfill `village_features.competitor_counts_json`/
   `competitor_counts_source`/`competitor_counts_as_of` and `osm_building_count`/
   `coverage_confidence_source`/`coverage_confidence_as_of`, same `JdbcTemplate` batch-upsert shape as
   `VillageFeaturesService.upsert` (`INSERT ... ON CONFLICT (village_id) DO UPDATE`, since a
   `village_features` row may not exist yet for every village) — plus the matching
   `VillageEnrichmentInitializer` property wiring (`graminsaathi.villages.competitor-building-backfill-path`,
   same off-by-default pattern as the other two backfills). Not started.
4. `expectedBuildingCount` (step 2c's other half, needed for `coverageConfidenceRatio`) is still the
   genuine gap the original plan flagged — none of these 4 files supply it. `osmBuildingCount` alone can be
   written now (nullable-column convention already supports a partial 2c fill); the ratio/label stay null
   until this is resolved separately.

## Session 2026-09-26 (part 2) — items 1–3 above actually DONE; 2c raster still blocks; HCES scoped; category-expansion decision pending

The previous section's "Not done yet (blocking, in order)" list undersold reality again (same pattern as
the "Correction to phase 1 status" note earlier in this log) — a separate chat session had already built
and run most of it without this file being updated. Reconciled by reading the actual code and file system
directly rather than trusting this doc, which is why this correction exists.

**Item 1 (`resolve_osm_places.py`) — DONE, RUN.** Script exists at `Population Data Csv/resolve_osm_places.py`,
implements exactly the state-boundary + name+state+population-tiebreak match this log already specified.
Outputs on disk, dated 2026-09-26: `data/output_coordinates/village_coordinates_osm.csv` (matched, ~9.6 MB)
and `village_coordinates_osm_unmatched.csv`. Match-rate/breakdown numbers from the actual run were not
captured in this log — re-run with output piped to a file next time so the stats survive past the terminal.

**Item 2 (competitor counts + osmBuildingCount script) — DONE, RUN.** `compute_village_market_signals.py`
exists, does the two-tree/streamed-chunks join this log specified (BallTree+haversine for the true 5km
radius, `businesses.csv` loaded whole, `buildings.csv` streamed in 500k-row chunks). Output on disk, dated
2026-09-26: `data/output_market_signals/village_market_signals.csv` (~22.9 MB) — columns confirmed:
`lgd_code, name, district, state, competitor_counts_json, competitor_counts_source, competitor_counts_as_of,
osm_building_count, coverage_confidence_source, coverage_confidence_as_of`. Dairy and Flour Mill confirmed
written as `null` (not `0`) in `competitor_counts_json`, per the no-reliable-tag call made earlier.

**Item 3 (Java backfill for market signals) — DONE, NOT YET RUN.** All of this already exists in the
codebase, verified by reading the actual files:
- `data/MarketSignalsCsvParser.java` — dependency-free CSV reader, column set matches
  `compute_village_market_signals.py`'s output (and also accepts `compute_coverage_confidence.py`'s columns
  for when that lands — see below). Only `lgd_code` mandatory; every other column independently optional.
- `VillageEnrichmentService.backfillMarketSignals(Reader)` — upserts `village_features` keyed by
  `village_id` resolved from `lgd_code`, via `INSERT ... ON CONFLICT (village_id) DO UPDATE`. Every SET
  clause is `COALESCE(EXCLUDED.x, village_features.x)`, not a bare overwrite — deliberately so, since 2b's
  script and 2c's script (below) write to the same table at different times with non-overlapping columns,
  and this makes running either one, in either order, any number of times, safe and idempotent.
- `VillageEnrichmentInitializer` — reads `graminsaathi.villages.market-signals-backfill-path` (directory-or-
  file, same pattern as the other two backfills).

**BUG found this session, FIXED this session: the property above was not actually wired in
`application.properties`.** The committed file had three *different*, unused property names left over from
an earlier plan — `graminsaathi.features.competitors.load-path`, `graminsaathi.features.coverage.load-path`,
`graminsaathi.features.coverage.as-of` — none of which `VillageEnrichmentInitializer` (or anything else)
read. `graminsaathi.villages.market-signals-backfill-path`, the property the code actually binds to, was
missing from the file entirely. Net effect had been: even with `village_market_signals.csv` sitting ready on
disk, turning the app on would have silently done nothing for this backfill. **Fixed:** the three dead
`graminsaathi.features.*` lines removed, `graminsaathi.villages.market-signals-backfill-path=` added (empty
by default, same convention as the other two backfill-path properties — set it for one run, clear it after).
Still needs to actually be set to `data/output_market_signals` (or the specific CSV) and run once to load
the data — the property existing doesn't load anything by itself.

**Item 4 (`expectedBuildingCount`/coverage-confidence ratio) — SCRIPT WRITTEN, NOT RUN, blocked on one file.**
`compute_coverage_confidence.py` exists and implements the WorldPop-vs-OSM-building-density method this log
already called for: windowed per-village read off the raster (bounded to a ~100x100px window at 100m
resolution, not a full-array load), true-circle haversine masking (not just the bounding box), `expected =
population / 4.9` (Census 2011 all-India rural average household size — a methodology assumption, stated as
such in the output's `coverage_confidence_source` column), `ratio = osmBuildingCount / expected` capped at
1.0, label thresholds `>=0.7 high / 0.3-0.7 medium / <0.3 low`. Writes a separate
`data/output_market_signals/village_coverage_confidence.csv`, designed to be run through the SAME
`backfillMarketSignals` Java path as item 3 (COALESCE upsert means running this second pass afterwards only
fills the ratio/label/expected-count columns, never clobbers the competitor counts item 3 already wrote).

**Genuinely missing (the one real data-file gap in the whole 2b/2c chain):** `data/ind_ppp_2020_constrained.tif`
(WorldPop India 2020 constrained population raster, ~506 MB, free, no login) is not present anywhere in the
project. Confirmed by listing `Population Data Csv/data/` directly — not just assumed. Download from
`https://data.worldpop.org/GIS/Population/Global_2000_2020_Constrained/2020/BSGM/IND/ind_ppp_2020_constrained.tif`,
save to that exact path, then run the script. Nothing else blocks it — `village_market_signals.csv` and
`village_coordinates_osm.csv` (its two other required inputs) already exist.

## HCES household-spend data — PDF is real and extractable, but doesn't map 1:1 onto our 4 categories

`Final_Report_HCES_2023-24L.pdf` (MoSPI Report No. 592, "Household Consumption Expenditure Survey 2023-24",
225 pages, downloaded 2026-09-26) opened and inspected directly this session (`pdfinfo`/`pdftotext`, not
assumed from the filename) — has a real embedded text layer, not scanned, so `pdftotext -layout` extracts
cleanly.

**What it actually contains:** "Statement 3R" (rural) — monthly *per-capita* consumption expenditure,
broken into 32 broad item categories, for every state/UT plus All-India. Confirmed real numbers pulled this
session, e.g. Maharashtra rural: `milk and milk products` = Rs 260.29/capita/month.

**Category-mapping problem, checked line-by-line against our 4 `BusinessCategory` rows:**
- **Dairy → "milk and milk products"**: clean, direct, unambiguous match.
- **Tailoring → NOT "clothing & bedding"**: that row's own footnote says it *excludes* tailoring charges.
  The actual tailoring spend is folded into "consumer services excluding conveyance" (footnote confirms
  *that* row *includes* tailoring charges) — but that row also bundles barbers, repairs, and other
  services, so it's not a clean tailoring-only figure. Needs a defined proxy/judgment call before it goes in
  the seed file, not a straight transcription.
- **Retail / Kirana Store → no line item at all.** A kirana store's basket spans several rows (cereal,
  pulses, sugar, salt, edible oil, spices, ...). Needs a defined sum-of-rows proxy, not a straight
  transcription.
- **Flour Mill → not tracked at all.** Milling is a service the survey doesn't capture as a consumption
  item. This lines up with the OSM finding above — Flour Mill (with Dairy) already had no reliable tag in
  `businesses.csv` either. Two independent sources agreeing this category is structurally hard to pin down
  is a real signal, not a coincidence.

**Unit mismatch, not yet resolved:** `DemandSupplyScoreService` computes
`demand = householdsProjected x monthlyHouseholdSpend` — a *per-household* figure. HCES gives *per-capita*
MPCE. The report has no state-wise household-size table to convert with. Don't need one, though — real
`population_2011`/`households_2011` per village already exist from the Census import, so a per-state (or
even per-village) household-size ratio can be derived from data already on hand, rather than sourcing
another file. Decision on exactly where that conversion happens (in the Python seed-prep step vs. in
`DemandSupplyScoreService` itself) not yet made.

**Current seed-file state:** `hce_category_spend_seed.json` has exactly 4 rows, all Maharashtra, all
`monthly_household_spend: null` — placeholders only, nothing transcribed yet. Until this is filled,
`DemandSupplyScoreService.calculate()` returns `LOW_PARTIAL_DATA` for every village x every category in the
entire app — demand/supply scoring is currently a total no-op, not a partial one.

**Proposed next step (not started, awaiting go-ahead):** transcribe the clean Dairy numbers (all
states/UTs, one unambiguous HCES row) first, since it needs no proxy decision. Tailoring/Retail/Flour Mill
proxies need an explicit methodology decision before any numbers go in.

## Business-category-list expansion — under discussion, no decision made yet

Raised this session: should the 4 `BusinessCategory` rows (Dairy, Tailoring, Retail/Kirana, Flour Mill) be
expanded before going further? Relevant facts already on record above, brought together here because they
bear directly on the decision:
- All 4 categories' `source_note` says "carried over from `demo_data.json`" — i.e. picked for the demo, not
  from real market analysis.
- 2 of the 4 (Dairy, Flour Mill) are the ones with no reliable OSM tag (2b) AND no HCES line (above) —
  the same two categories are the hardest to verify in both independent data sources.
- The architecture already treats category as data (`BusinessCategory` rows + `CATEGORY_TAG_RULES`-style
  mapping + JsonLogic fit-rules), so adding a category is mostly a content task (one seed JSON row + one
  OSM tag rule + one HCES line/proxy decision), not a new-code task.
- Counter-consideration: none of the 4 *existing* categories has a working end-to-end market score yet
  (HCES unseeded, coverage confidence blocked on the raster, nothing run through Maven this session).
  Expanding the list now would multiply an unproven pipeline across more categories before validating it
  works for even one.

**Working recommendation (not yet acted on):** finish one category (Dairy — cleanest match in both OSM and
HCES) all the way through to a real, verified `marketScore` first, then decide on expansion. If/when
expansion happens, prefer categories that map cleanly to both a real OSM tag and a real HCES line (e.g.
food-service via `amenity=restaurant|fast_food` ↔ "beverages, processed food, etc.") over categories that
would repeat the Dairy/Flour-Mill verification problem.

## CURRENT STATE, as of 2026-09-26 (end of this session) — summary

- **Step 1 (village master + autocomplete):** complete, real data loaded (~633k villages).
- **Step 2a (population/households):** complete, run successfully.
- **Phase 1 (coordinates):** data produced (`village_coordinates_osm.csv`) via the OSM-name-match path;
  Java-side `backfillCoordinates` already built; **not yet loaded into the DB** (backfill run not done).
- **Step 2b (competitor counts):** data produced (`village_market_signals.csv`); Java-side
  `backfillMarketSignals` already built; the `application.properties` property-name bug is **fixed**
  (`graminsaathi.villages.market-signals-backfill-path` now exists, empty by default); still **not yet
  loaded into the DB** — the property needs to be set to the CSV's path for one run.
- **Step 2c (coverage confidence):** script written, **blocked on the missing WorldPop raster file**
  (`ind_ppp_2020_constrained.tif`) — the one genuine missing data file in this whole chain.
- **Step 3 (demand-supply scoring):** code complete and wired into `DiscoveryService`; returns
  `LOW_PARTIAL_DATA` for everything right now because HCES is unseeded and competitor counts aren't loaded
  yet (see above) — will start returning real scores automatically once both land, no code change needed.
- **Step 4 (hard filters + person-fit):** code complete; amenity filter (electricity) wired but no data
  source feeds `village_features`'s amenity columns yet.
- **Step 5 (financial ranges):** code complete, unscaled (no HCES-derived state factor yet).
- **Business category list:** still the original 4 demo categories; expansion under discussion, no decision.
- **Nothing in this session's changes has been run through `mvnw.cmd test`.**

## NEXT STEPS, in order (updated — step 1 done, rest not started)

1. ~~Fix the `application.properties` bug~~ — **DONE (2026-09-26):** the three dead `graminsaathi.features.*`
   lines removed, `graminsaathi.villages.market-signals-backfill-path=` added.
2. Run the coordinates backfill (`graminsaathi.villages.coordinates-backfill-path` →
   `village_coordinates_osm.csv`) and the market-signals backfill
   (`graminsaathi.villages.market-signals-backfill-path` → `village_market_signals.csv`) — one startup run
   each, then clear both properties back to empty.
3. Download `ind_ppp_2020_constrained.tif` (~506 MB — note: NOT the same file as the unconstrained
   `Individual_countries/IND/India_100m_Population/` mirror; must come from
   `Global_2000_2020_Constrained/2020/BSGM/IND/`, in progress as of this session), run
   `compute_coverage_confidence.py`, then run its output through the same market-signals backfill path.
4. Decide the Tailoring/Retail/Flour-Mill HCES proxy methodology; transcribe Dairy numbers now (no proxy
   needed) into `hce_category_spend_seed.json`; decide where the per-capita→per-household conversion lives.
5. Run `mvnw.cmd test` — nothing from steps 2a onward has been verified to compile/pass this session.
6. Once Dairy has a verified real `marketScore` end-to-end, revisit the business-category-expansion
   decision above.

## Session 2026-09-26 (part 3) — real bug found in `resolve_osm_places.py`: name-popularity collisions
were silently mismatching villages, causing the merge freeze/crash above

While actually running the chain (raster downloaded, `rasterio` installed, ran `compute_coverage_confidence.py`),
the merge between `village_market_signals.csv` and `village_coordinates_osm.csv` first hung for 2+ minutes
(diagnosed as blank/NaN `lgd_code` rows in both files being treated as matching keys by pandas' merge, unlike
SQL NULL semantics — fixed by dropping blank-code rows before merging), then failed fast and correctly with
`validate="one_to_one"` on **real, non-blank duplicate `lgd_code`s** in both files.

**Root cause, confirmed by pulling the actual rows:** `pick_best()` in `resolve_osm_places.py` auto-accepts
any name that has exactly one Census village of that name anywhere in the state (`"single_candidate"`),
with no check on whether the OSM point being matched is anywhere near that village. Confirmed example: 5
OSM points named "Venkatagiri" scattered from 13.19N to 17.20N across Andhra Pradesh (>400km apart) were
all silently matched to the one Census village named "Venkatagiri", in Chittoor — because "Venkatagiri" is
a name-popularity collision (a common name reused by multiple real, distinct places that Census only
happens to have one entry for), not a real 1:1 match. Same pattern confirmed for "Kodumuru" and
"Telagavaram". This is very likely a meaningful chunk of why OSM coverage "feels low" — some of what
looked matched was actually wrong, not just the raw match-rate percentage being low.

**Fix applied:** before trusting a `single_candidate` match, `resolve_osm_places.py` now checks whether
every raw OSM point sharing that exact name *within the same state* actually clusters together
(`DISPERSION_KM_THRESHOLD = 25.0` km, max pairwise haversine distance). If they're scattered further apart
than that, the whole group is routed to unmatched (`reason=single_candidate_but_dispersed_osm_points`) for
honest review instead of guessed at — same convention the script already used for population-tiebreak
ambiguity. Also added, separately, to `compute_coverage_confidence.py`: per-file blank/duplicate `lgd_code`
diagnostics printed before merging, and `validate="one_to_one"` on the merge itself, so any *future*
matching bug fails fast with a clear diagnosis instead of a multi-minute silent hang.

**Run order matters, correction to earlier guidance in this log:** loading `market-signals-backfill-path`
pointed at the whole `output_market_signals/` directory was suggested above as safe regardless of order,
reasoning that `COALESCE` protects every column. That's true for columns unique to one file, but
`coverage_confidence_source` is a **non-null value in both** `village_market_signals.csv`
(`osm_buildings_csv_5km_radius`, no expected count yet) and `village_coverage_confidence.csv` (the fuller
`worldpop_2020_constrained_5km_radius_household_size_4.9`, plus the actual ratio/label) -- 
`compute_coverage_confidence.py`'s own printed output says as much. Directory mode loads files
alphabetically, which puts `village_coverage_confidence.csv` (c) BEFORE `village_market_signals.csv` (m) --
backwards, so the weaker string would win. Correct sequence: two explicit runs on the single
`market-signals-backfill-path` property -- `village_market_signals.csv` first, `village_coverage_confidence.csv`
second, changing the property's value between runs (clearing it in between isn't required, changing the
value is).

Also added this session, unrelated to the bug: `compute_coverage_confidence.py` now prints fine-grained
timed progress (per-file load time, merge time, per-1000-village rate/ETA during the raster lookup loop,
all with `flush=True`) since the original version's only feedback was a single unflushed print every 5,000
villages, which looked indistinguishable from a hang on Windows.

## Session 2026-09-26 (part 4) — the single_candidate fix above was incomplete; added a reason-agnostic
final dedup pass, now structural rather than a per-case patch

Re-running the chain after part 3's fix still hit the exact same `MergeError` in `compute_coverage_confidence.py`.
Pulled the actual duplicate rows again to check: `Telagavaram` (579592.0) still had 3 rows, but genuinely
clustered ~15km apart (probably multiple real OSM point-nodes for the one village -- not a bug). `Garhi`
(72524.0, Rajasthan) had **16** rows spread ~370km apart, and `Mottur` (643983.0, Tamil Nadu) had 16 rows
spread ~150km apart -- both via `osm_place_population_tiebreak`, not `single_candidate`. Root cause: the
part-3 fix only guarded the `single_candidate` path; `population_tiebreak` can suffer the exact same
name-popularity collision (many real, distinct villages sharing a common name AND similar population sizes
across a state, all tiebreaking onto the same one Census candidate) and nothing was checking for it there.

**Fix: stopped patching per-match-reason and added one final, reason-agnostic pass** in
`resolve_osm_places.py`, after all per-state matching finishes and before writing the output CSV: group
the accumulated matches by `lgd_code`; any code with more than one row is checked with the same
`max_pairwise_distance_km` helper -- if the rows cluster within `DISPERSION_KM_THRESHOLD` (25km), collapse
them to one row at the centroid (multiple OSM nodes, same real place); if they're more scattered than
that, reject the whole group to unmatched (`reason=duplicate_lgd_code_dispersed`) rather than arbitrarily
keeping one. This makes "at most one row per lgd_code in the output" a structural guarantee independent of
which matching path produced the rows, rather than a property that happens to hold for the cases inspected
so far. Script now also prints a `Post-match dedup:` summary line (groups collapsed vs. rejected, with raw
row counts) so this is visible on every run, not just when something crashes downstream.

**Still needs:** the full three-script chain re-run again with this fix (not yet done as of this note):
```
python resolve_osm_places.py
python compute_village_market_signals.py
python compute_coverage_confidence.py
```

## Session 2026-09-26 (part 5) — `resolve_osm_places.py` was taking ~1 hour; switched `.iterrows()` ->
`.itertuples()`, added per-phase/per-state timing

Reported runtime was ~1 hour for ~370k OSM rows, which is far too slow for this kind of matching and
pointed at a classic pandas anti-pattern rather than genuinely heavy work. Found it: all three row-loops in
the per-state matching block used `.iterrows()`, which reconstructs a full pandas Series object per row --
notoriously one of the slowest ways to iterate a DataFrame, often 10-50x slower than the alternative for
loops this size. Switched all three to `.itertuples(index=False)` (attribute access instead of dict-style
indexing, e.g. `row.name` instead of `row["name"]`). Also added `flush=True` timing prints at every phase
(load, sjoin reverse-geocode, and per-state) plus a running and final total, so if it's still slow after
this, the per-state breakdown will show exactly which state(s) dominate (most likely UP/Bihar/MP, simply by
village-count) instead of the whole hour being one opaque black box again.

**Not yet re-run with this fix as of this note** -- expected to bring runtime down to low single-digit
minutes based on typical iterrows-vs-itertuples speedups at this row count, but unconfirmed until actually
timed.

## Session 2026-09-27 (part 1) — `graminsaathi.villages.import-path` was left permanently set, re-importing
all 31 state CSVs (all skippedExisting) on every single boot for no reason; cleared

`VillageDataInitializer`'s own Javadoc says this property should be "set to load a Census/LGD extract;
leave it empty afterwards" -- same one-off-switch convention as every other backfill-path property -- but
it had been left pointed at `data/output` since the original import, costing ~1.5-2 min of pure waste on
every boot (31 files, hundreds of thousands of rows, every single one `skippedExisting`). Cleared in
`application.properties`. No code change needed; this was a config oversight, not a bug.

## Session 2026-09-27 (part 2) — real bug: `expected_building_count` written as "1016.0" instead of
"1016", silently destroying 97.8% of the coverage-confidence backfill

Ran the two ordered backfills (Run 1: coordinates + `village_market_signals.csv`, matched=68114/66153 --
both healthy). **Run 2** (`village_coverage_confidence.csv`) came back `matched=1529, unmatched=0,
skippedInvalid=66609` -- i.e. only the rows with a BLANK `expected_building_count` parsed; every row that
actually had a value failed.

**Root cause, confirmed by inspecting the actual CSV:** every non-blank `expected_building_count` value was
written as e.g. `1016.0`, not `1016`. `compute_coverage_confidence.py` built `out_df` straight from a list
of dicts where this column mixes real ints with `None` (villages with zero population in their 5km radius);
pandas can't represent `NaN` in an int64 column, so it silently upcasts the WHOLE column to float64, and
every integer then round-trips through `to_csv` with a trailing `.0`. On the Java side,
`MarketSignalsCsvParser.toInt()` calls `Integer.valueOf(s)`, which throws on `"1016.0"` -- and because the
entire `Row` record is constructed inside one `try` block, that one column's exception discarded the whole
row (`lgd_code`, ratio, label, source -- everything), not just the malformed field.

**Fix applied, two parts:**
1. `compute_coverage_confidence.py`: cast `expected_building_count` to pandas' nullable `Int64` (capital I)
   dtype before `to_csv` -- this keeps `None` as `<NA>` without upcasting the column, so it round-trips as
   a plain integer string or an empty cell, never `.0`. Fixes all FUTURE runs of this script.
2. The already-generated `village_coverage_confidence.csv` was patched in place rather than re-running the
   ~53-minute raster pass again: a small one-off script,
   `fix_coverage_confidence_dot_zero.py`, strips the trailing `.0` from every affected cell. Run once,
   confirmed 66,609 of 68,138 rows fixed (matches the exact skippedInvalid count from the failed backfill
   log line, which is strong confirmation this was the one and only cause).

**Still needs:** re-run the Run 2 backfill (property is already pointed at the right file, nothing to
change) now that the CSV is patched -- expect `matched` to land near 66,153, not 1,529.

**CONFIRMED FIXED, 2026-09-27:** re-ran after the patch script -- `matched=66153, unmatched=1985,
skippedInvalid=0`, matching `village_market_signals.csv`'s healthy numbers exactly. Step 2b/2c's data is
now fully loaded into `village_features` (coordinates, competitor counts, osmBuildingCount,
expectedBuildingCount, coverageConfidenceRatio/Label all populated). Both backfill-path properties cleared
back to empty in `application.properties`. Remaining items from the earlier NEXT STEPS list: spot-check the
database directly, run `mvnw.cmd test`, then move to the HCES spend-data task
(Tailoring/Retail/Flour-Mill proxy decision + Dairy transcription) and, after that, revisit business-category
expansion.

## Session 2026-09-27 (part 3) — not a bug: IntelliJ showed 22 "Cannot resolve column" errors on
`BusinessCategory.java`

After the boot sequence above, IntelliJ's Problems panel showed 22 "Cannot resolve table
'business_categories'" / "Cannot resolve column ..." errors on `BusinessCategory.java`, despite the app
having just logged `"Business categories: seed inserted 0 new, 4 already present"` on that same boot --
proof the table and every column genuinely exist in Postgres. Cause: IntelliJ's Database tool window had a
stale cached schema snapshot for the `GraminSaathi` connection (only showing 4 tables --
`saved_reports`/`users`/`village_features`/`villages` -- missing `business_categories`,
`hce_category_spend`, and whatever else Hibernate's `ddl-auto=update` has since created), and its
DB-aware code inspection cross-checks `@Column` annotations against that stale cache, not against
Hibernate/Spring at runtime. Purely cosmetic -- never affected compilation, `mvnw.cmd test`, or the running
app. Fixed by refreshing the Database connection in IntelliJ; no code or config change.

## NEXT STEPS, current (2026-09-27)

1. Spot-check `village_features` directly in the DB (e.g. via the now-working IntelliJ Database console) for
   a village known to have matched -- confirm `latitude`/`longitude`, `competitor_counts_json`,
   `osm_building_count`, `expected_building_count`, `coverage_confidence_ratio`, `coverage_confidence_label`
   all actually landed, not just the log counts.
2. Run `mvnw.cmd test` -- nothing from this entire multi-day session has been run through the test suite yet.
3. HCES spend-data task: decide the Tailoring/Retail/Flour-Mill proxy methodology; transcribe the clean
   Dairy numbers (all states/UTs) into `hce_category_spend_seed.json`; decide where the
   per-capita→per-household conversion lives.
4. Once Dairy has a verified real `marketScore` end-to-end, revisit the business-category-expansion decision.

## Session 2026-09-27 (part 4) — step 1 DB spot-check CONFIRMED; real bug found in `lgd_code` itself
(not just `expected_building_count`), fixed in code, DB cleanup still outstanding

**Step 1 spot-check done, per NEXT STEPS item 1 above:** queried `villages` JOIN `village_features` for
Campbell Bay (Nicobars, Andaman & Nicobar Islands) directly against the running DB. Every value matched
the source CSVs exactly: `latitude=7.008816`, `longitude=93.9327648`, `competitor_counts_json` (Dairy/Flour
Mill null, Tailoring/Retail 0), `osm_building_count=346`, `expected_building_count=1016` (confirmed clean
int, the part-2 float-formatting fix held), `coverage_confidence_ratio=0.3404`, `label=medium`. Step 1 is
now genuinely DB-verified, not just log-claimed.

**Bug found while constructing the spot-check query:** `villages.lgd_code` carries a spurious trailing
`.0` (e.g. `645200.0`, not `645200`) for every row that has one. Confirmed at the source: the very first
`village_processor.py` output (`data/output/village_master_andaman_&_nicobar_islands.csv`, from Step 1,
pre-dates every later session in this log) already writes `lgd_code` as `645012.0` etc. Classic pandas
float64-upcast (same root cause as the `expected_building_count` "1016.0" bug from part 2, but in a
different column, introduced much earlier and never caught until now). `VillageCsvParser` and
`MarketSignalsCsvParser` both read `lgd_code` as a raw string with no numeric normalization, so the `.0`
flowed straight into the `villages` table untouched at the original import, and into every enrichment CSV
since (coordinates, market signals, coverage confidence all carry it too — confirmed in their raw output
files).

**Why it never surfaced as a bug before now:** every backfill in this log matches `lgd_code` dirty-to-dirty
(both the DB value and the incoming CSV value carry the same `.0`), so `HashMap` string-equality lookups in
`VillageEnrichmentService` (`loadIdByLgdCode`, `backfillMarketSignals`) have been working by accident. It
only broke the moment a clean value (`'645200'`, as a human would naturally write it) was compared against
it.

**Fixed this session:** `VillageCsvParser.normalizeLgdCode(String)` (package-private, strips a trailing
`.0`) added and wired into `VillageCsvParser`'s row-construction (`lgd_code` field) and
`MarketSignalsCsvParser`'s row-construction (via `VillageCsvParser.normalizeLgdCode(...)`, same package).
Not yet run through `mvnw.cmd test` or recompiled.

**IMPORTANT — this fix is NOT safe to leave half-done, and nothing here is a DB migration:** the code fix
above only normalizes `lgd_code` on future CSV reads. The ~633k rows already sitting in `villages.lgd_code`
are still dirty (`'645200.0'`). Since matching only worked because both sides used to be equally dirty,
this fix as it stands will BREAK the next real run of `backfillPc11Codes`/`backfillCoordinates`/
`backfillMarketSignals` against the *existing* DB: `loadIdByLgdCode()` reads the still-dirty DB values into
its lookup map, while newly-parsed CSV rows now come out clean — a clean key will never match a dirty map
entry. **Before running any backfill again, or trusting a fresh `mvnw.cmd test`/manual query against
`lgd_code`, run this one-time cleanup on the DB itself:**
```sql
UPDATE villages SET lgd_code = regexp_replace(lgd_code, '\.0$', '') WHERE lgd_code LIKE '%.0';
```
Not yet run. Until it is, the DB and the newly-fixed code are in a mismatched state — matching will silently
regress (more `unmatched` on the next backfill run) rather than error loudly, so this is easy to miss if
skipped.

**NEXT STEPS, updated (2026-09-27, supersedes the list above):**
1. Run the `UPDATE villages SET lgd_code = regexp_replace(...)` cleanup above — one-time, safe (idempotent,
   only touches rows that still have the suffix).
2. Recompile and run `mvnw.cmd test` — nothing from this entire multi-day session (steps 2a onward) has
   been run through the test suite; this is now overdue, not just a nice-to-have.
3. HCES spend-data task (unchanged from above): decide the Tailoring/Retail/Flour-Mill proxy methodology;
   transcribe the clean Dairy numbers; decide where the per-capita→per-household conversion lives.
4. Once Dairy has a verified real `marketScore` end-to-end, revisit the business-category-expansion decision.

## Session 2026-09-27 (part 5) — DB cleanup run, full recompile + `mvnw.cmd test` CONFIRMED GREEN

**`lgd_code` cleanup migration — RUN.** The `UPDATE villages SET lgd_code = regexp_replace(...)` one-time
cleanup from part 4 has been run against the live DB (Vishesh, this session). Combined with the
`normalizeLgdCode` fix landing in both CSV parsers, `villages.lgd_code` and every future backfill read are
now consistently clean (no more `.0` suffix on either side of a match).

**Full recompile + app boot — CONFIRMED.** App compiled and started successfully after the `lgd_code` fix
landed — no regressions from the `VillageCsvParser`/`MarketSignalsCsvParser` changes.

**`mvnw.cmd test` — RUN, ALL GREEN (both service and controller suites).** This is the first real test run
of this entire multi-day recommendation-engine session (steps 2a through 2c, Discovery wiring, amenities,
the `lgd_code` fix) — everything flagged as "not yet run through Maven" throughout this log, across every
prior session dated 2026-09-24 through 2026-09-27, is now confirmed to actually compile and pass, not just
look correct on disk. This closes out the log's single most repeated caveat.

**CURRENT STATE, updated (2026-09-27, end of session):**
- Steps 1, 2a, 2b, 2c: complete, DB-verified (part 4's spot-check), backfills matching on clean `lgd_code`.
- Steps 3/4/5 (demand-supply, hard filters/person-fit, financial ranges): code complete, compiles, tests
  green; demand-supply still returns `LOW_PARTIAL_DATA` everywhere until HCES is seeded (unchanged — a data
  gap, not a code gap).
- Full test suite (service + controller): green.
- Only remaining blocker before a real end-to-end `marketScore`: the HCES seed data.

**NEXT STEPS, updated (2026-09-27, supersedes the list above):**
1. HCES spend-data task: decide the Tailoring/Retail/Flour-Mill proxy methodology (no clean single HCES
   line for any of the three); transcribe the clean Dairy numbers (all states/UTs, unambiguous match) into
   `hce_category_spend_seed.json`; decide where the per-capita→per-household conversion lives (Census
   `population_2011`/`households_2011` already on hand per village — no new file needed for the ratio).
2. Once Dairy has a verified real `marketScore` end-to-end (data seeded + a live `/discover` or equivalent
   call showing a non-`LOW_PARTIAL_DATA` result), revisit the business-category-expansion decision.

## Step 2b-extended — real competitor locations for the map — SCOPED THIS SESSION, NOT STARTED

**Problem found this session:** `MapPage.jsx`/`BusinessMap.jsx` (the village map with village + competitor
pins) is disconnected from this entire session's work, in two independent ways:

1. **Wrong data source.** `MapPage` calls `referenceAPI.getVillages()` → `GET /api/villages`, the legacy
   `ReferenceDataController` endpoint serving the original ~4 demo villages from `demo_data.json` — not
   `VillageController`'s `/api/villages/search` (the real ~633k-village Census/LGD table steps 1/2a/2b/2c
   built out). The map has never seen a real village.
2. **Fake competitor pins, by original design.** `BusinessMap.jsx`'s `generateCompetitorPositions(center,
   count, seed)` takes only a competitor COUNT and scatters that many pins pseudo-randomly in a ring
   (seed = `village.villageName.length * 1000`) — there is no real business location involved at all, even
   conceptually. This was never wired to real coordinates, not a regression.

**What real data already exists vs. what's missing:** `businesses.csv` (48MB raw OSM POI extract used by
`compute_village_market_signals.py`, all-India `lat, lon, shop, amenity, craft, office, name`) already gets
loaded and BallTree-queried per village for step 2b — but the script only ever emits the aggregate COUNT
per category into `competitor_counts_json`; each matched business's own lat/lon is computed against, then
thrown away. Nothing persists individual competitor locations anywhere (no CSV output, no DB column, no API
field). This is the one real gap; the join logic and radius convention to reuse already exist and are
proven (step 2b is live in production data, confirmed via the part-4 spot-check above).

**Plan (not started), in order:**
1. **Python:** extend `compute_village_market_signals.py` to also emit a second output — one row per
   matched business within radius, keyed by the village's `lgd_code`: `lgd_code, business_lat, business_lon,
   category (or raw osm tag), name`. One-to-many (a village can have dozens of competitors), so this is a
   new file, not an added column on the existing per-village CSV.
2. **Schema + Java backfill:** new `competitor_businesses` table (`village_id FK or lgd_code, lat, lon,
   category, name, source, as_of`) — doesn't fit as a `village_features` column group (those are 1:1 with a
   village; this is 1:many). New `CompetitorBusinessCsvParser` + a `VillageEnrichmentService` method
   following the exact same JDBC-batch-insert, lgd_code-matched, off-by-default-property pattern as every
   other backfill in this log (`graminsaathi.villages.competitor-locations-backfill-path` or similar).
3. **API:** a new read endpoint (e.g. `GET /api/villages/{id}/competitors` or a field added to the village
   detail response) returning the real lat/lon/category/name rows for a village, for the frontend to
   consume directly — no new scoring logic needed, this is pure display data.
4. **Frontend:** repoint `MapPage` from `referenceAPI.getVillages()`/`/api/villages` to the real village
   search endpoint (`/api/villages/search`) so it's browsing real villages in the first place, then replace
   `BusinessMap.jsx`'s `generateCompetitorPositions()` entirely with real pins from step 3's endpoint
   (dropping the seeded-random scatter function). `businessData.competitorCount`/`avgLocalPrice` (currently
   read from the demo `businessData` shape) also need a real source — likely `village_features`'s
   `competitor_counts_json` (already populated, step 2b) for the count; `avgLocalPrice` has no real-data
   equivalent yet anywhere in this session's work and would need its own scoping if kept.

**Not started — no code written for this yet.** Priority call (not made this session): this is demo-facing
polish, but the map is already fully non-functional against real data today (wrong villages, fake pins), so
it isn't a regression to leave as-is — weigh against the still-open HCES seeding task above when deciding
what to pick up next.
