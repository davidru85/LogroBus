# API specification (client contract)

| Field | Value |
| --- | --- |
| Status | Living document |
| Last updated | 2026-10-22 |
| Last verified against the live source | 2026-10-22 (capture in spec v0.1) |
| Derived from | Spec §3.2, §4, §5, §8.7, §8.9 |

This is the engineering contract for every remote call the app makes: the source BFF at `transporteurbano.logrono.es`, the CartoCiudad geocoder and remote config. The functional spec captured the source contracts once, on 22 Oct 2026. **This document is where they are kept current.** When you observe a change, update this file, re-record the affected fixture, and add a row to §10 and an entry to `project_log.md`.

The source API is undocumented, unversioned and owned by a third party. It sends no `Cache-Control` or rate-limit headers, and it can change without notice (risk R1).

## 1. Ground rules

| Topic | Rule |
| --- | --- |
| Base URL | Remote config `api_base_url`, default `https://transporteurbano.logrono.es`. Paths below are relative to it |
| Transport | HTTPS only (NFR-SEC-01). No cookies, tokens or API keys in v1 |
| Headers | `User-Agent: LogronoBus/<version> (<Android\|iOS> <os-version>)`, `Accept: application/json`, `Accept-Encoding: gzip`, and `Content-Type: application/json` on POSTs |
| Timeouts | 5 s to connect. 10 s per request for live endpoints (API-03 to 08 and the geocoder). 30 s for static ones (API-01, 02, 10 to 12) |
| Success | Any `2xx`. POSTs answer `201` (DD-15) |
| Failure | A non-2xx status, a non-JSON body or a missing envelope key is a recoverable `AppError` for that resource only (NFR-REL-02) |
| Server clock | Read the `Date` header on every BFF response and feed it to `AppClock` (spec §8.6) |
| Load | Respect the polling intervals (10 s floor), and only poll for visible screens. Never load-test the source |
| Forbidden | Parsing `/env.js`, leaked non-production hosts (spec §3.4), API-13, the legacy recharge-points file, and the authenticated endpoints of spec §4.14 |

## 2. Endpoint catalogue

| Id | Method and path | Used by | Refresh | Storage | Milestone |
| --- | --- | --- | --- | --- | --- |
| API-01 | `GET /api/linesDiscovery/lines` | All sections (lines, patterns, shapes) | Daily static sync (TTL 24 h, content hash) | DB | MVP |
| API-02 | `GET /api/linesDiscovery/stops` | All sections (stops, served lines) | With API-01, same transaction | DB | MVP |
| API-03 | `GET /api/vehicleMonitoring/all` | Near Me "Buses" layer | 10 s while visible | Memory | MVP (P1) |
| API-04 | `GET /api/vehicleMonitoring/byLine/{lineId}` | Line detail map | 10 s while visible | Memory | MVP |
| API-05 | `GET /api/estimatedTimetable/byLine/{lineId}?previewMinutes=30` | Line detail stop list | 20 s while visible | Memory | MVP |
| API-06 | `GET /api/estimatedTimetable/byStop/{stopId}?lines={csv}&previewMinutes=60` | Stop sheet, saved stops, planner overlay | 20 s while visible | Memory (saved-stop snapshots are v1.0) | MVP |
| API-07 | `POST /api/transit/route` | How to Get There | On demand | Memory | MVP |
| API-08 | `POST /api/transit/walk-distances` | Near Me nearby list | When the location moves more than 50 m | Memory | MVP |
| API-09 | `POST /otp/routers/default/index/graphql` | Optional walking previews | On demand | none | Optional |
| API-10 | `GET /api/fare-products` | More → Fares | TTL 7 d | DB | v1.0 |
| API-11 | `GET /api/tscs/recharge-points` | More → Recharge points | TTL 7 d | DB | v1.0 |
| API-12 | `GET /api/cms/{news,sections,cards,pies,rrss}` | More → News, Info; notice banner | 15 min in the foreground | DB | v1.0 |
| API-13 | `GET /assets/data/schedules.json` | **Not used** (DD-14) | none | none | none |
| GEO-01 | CartoCiudad `candidates` | Place search | Debounced 300 ms | none | MVP |
| GEO-02 | CartoCiudad `find` | Resolve a candidate without coordinates | On selection | none | MVP |

## 3. Decoding strategy

```kotlin
internal val SourceJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    isLenient = true
}
```

- Every DTO field is nullable with a default. The mappers, not the decoder, enforce what is required.
- Collections are decoded as `JsonArray` and then element by element, so one bad item is dropped and counted instead of failing the response:

```kotlin
internal inline fun <reified T> JsonArray.decodeEach(json: Json, report: DecodeReport): List<T> =
    mapNotNull { element ->
        runCatching { json.decodeFromJsonElement<T>(element) }
            .onFailure { report.dropped(T::class.simpleName ?: "?", it) }
            .getOrNull()
    }
```

- Envelopes: BFF endpoints use `{ "result": { <key>: [...] } }`, transit endpoints use a bare object, the CMS uses Strapi's `{ data, meta }`, and OTP uses GraphQL's `{ data, errors? }` (spec §4.2). A missing `result` or collection key is a `Data` error.

## 4. Source endpoints

### API-01 Lines

`GET /api/linesDiscovery/lines` returns `result.lines` (13 items on 22 Oct 2026, about 283 KB uncompressed).

```kotlin
@Serializable internal data class LinesResultDto(val lines: JsonArray? = null)
@Serializable internal data class LineDto(
    val id: Int? = null,
    val name: String? = null,          // "B3-LARDERO-EL CAMPILLO"
    val color: String? = null,         // "rgba(1,255,1,1)"
    val status: Int? = null,           // only 1 observed (OQ-02)
    val stops: LineStopsDto? = null,
)
@Serializable internal data class LineStopsDto(
    val asc: List<LineStopDto> = emptyList(),    // OUTBOUND (Ida)
    val desc: List<LineStopDto> = emptyList(),   // INBOUND (Vuelta)
)
@Serializable internal data class LineStopDto(
    val id: Int? = null, val name: String? = null,
    val lat: Double? = null, val lng: Double? = null, val status: Int? = null,
    val onwardShape: List<LatLngDto> = emptyList(),   // segment to the next stop; empty on the last stop
)
@Serializable internal data class LatLngDto(val lat: Double? = null, val lng: Double? = null)
```

Mapping:
- `name` gives the code and display name through the regex and fallback of spec §5.5, after the remote-config name overrides (DD-11). `kind` is NIGHT when the code starts with `B`.
- `color` is parsed from `rgba()` or hex (DD-08). `onColor` is black or white, whichever reaches at least 4.5:1 contrast (NFR-A11Y-06).
- `asc` and `desc` become patterns. The headsign is the name of the last stop. The shape is the concatenation of `onwardShape` in sequence. `shapeIndex` is the index where each stop's segment starts in that shape.
- Pattern stops that are absent from API-02 become synthetic stops with `isProvisional = true` (DD-10, ids 383–385).
- Sync validation: at least 5 lines, otherwise the payload is rejected as suspicious (spec §8.5).

Fixture: `fixtures/lines_2026-10-22.json`.

### API-02 Stops

`GET /api/linesDiscovery/stops` returns `result.stops` (254 items).

```kotlin
@Serializable internal data class StopDto(
    val id: Int? = null, val name: String? = null,
    val lat: Double? = null, val lng: Double? = null,
    val lines: List<Int> = emptyList(), val status: Int? = null,
)
```

Mapping:
- Names are trimmed and given overrides. `normalized_name` is lower case with accents stripped and `ñ` folded to `n`, for FTS (NFR-I18N-03).
- `lines` is authoritative for the `lines` parameter of API-06. Pattern membership from API-01 is authoritative for drawing (spec §5.2).
- Items outside the service bounding box (lat 42.4219 to 42.4925, lng -2.5206 to -2.4050, expanded by 2 km) are dropped and logged.
- Sync validation: at least 100 stops.

Fixture: `fixtures/stops_2026-10-22.json`.

### API-03 and API-04 Vehicle monitoring (SIRI VM)

`GET /api/vehicleMonitoring/all` and `GET /api/vehicleMonitoring/byLine/{lineId}` both return `result.activities`.

```kotlin
@Serializable internal data class VehicleActivityDto(
    val vehicleRef: String? = null,                  // fleet number; NOT the ET id space (DD-03)
    val vehicleMonitoringRef: String? = null,
    val lineRef: String? = null,                     // may be "[object Object]" (DD-06)
    val publishedLineName: String? = null,
    val directionRef: String? = null,                // "Ida" | "Vuelta" | "" (DD-04, DD-07)
    val latitude: Double? = null, val longitude: Double? = null,
    val locationRecordedAtTime: String? = null,      // correct UTC
    val nextStopRef: String? = null,                 // "0632" or "" (DD-01)
    val nextStopAimedArrivalTime: String? = null,    // local time mislabelled Z (DD-05)
    val nextStopExpectedArrivalTime: String? = null, // same defect
    val delaySeconds: Int? = null,
    val confidenceLevel: String? = null,
    val monitored: Boolean? = null,
    val predictionInaccurate: Boolean? = null,
)
```

The DD-05 correction applies only when the heuristic triggers (risk R4):

```kotlin
// DD-05: VM next-stop times carry Madrid wall-clock time with a "Z" suffix.
internal fun correctNextStopTime(raw: Instant, recordedAt: Instant): Instant =
    if (raw - recordedAt > 30.minutes) {
        raw.toLocalDateTime(TimeZone.UTC).toInstant(TimeZone.of("Europe/Madrid"))
    } else {
        raw
    }
```

Other rules:
- Bearing is computed client-side from the last two positions of the same `vehicleRef` (FR-LINE-06).
- A vehicle is hidden 45 s (`vehicle_grace_ms`) after its last report (FR-LINE-07).
- A `null` `lineId` means a grey marker on the network map, and the vehicle is hidden from line views (FR-LIVE-03).

Fixtures: `vm_all_with_object_lineRef.json`, `vm_line2.json`.

### API-05 and API-06 Estimated timetable (SIRI ET)

`GET /api/estimatedTimetable/byLine/{lineId}?previewMinutes=30` and `GET /api/estimatedTimetable/byStop/{stopId}?lines=1,33&previewMinutes=60` both return `result.arrivals`.

```kotlin
@Serializable internal data class EstimatedCallDto(
    val lineRef: String? = null,
    val directionRef: String? = null,          // opaque numeric code, e.g. "401" (DD-04)
    val stopPointRef: String? = null,          // "38" (DD-01)
    val order: Int? = null,
    val vehicleRef: String? = null,            // internal id; never join with VM (DD-03)
    val journeyCode: String? = null,
    val aimedArrivalTime: String? = null,      // correct offset
    val expectedArrivalTime: String? = null,
    val aimedDepartureTime: String? = null,    // "" when unknown (DD-07)
    val expectedDepartureTime: String? = null,
    val arrivalStatus: String? = null,         // only NO_REPORT observed (DD-09)
    val departureStatus: String? = null,
    val delaySeconds: Int? = null,
    val cancellation: Boolean? = null,
    val predictionInaccurate: Boolean? = null,
)
```

Mapping:
- Sort by `expectedArrivalTime`, falling back to `aimedArrivalTime`; the source order is not guaranteed.
- Infer direction with the algorithm of spec §5.6, using the persisted `direction_code_cache`. When the result is ambiguous, the direction is `null` and no headsign is shown.
- Quality is `REALTIME_LOW_CONFIDENCE` when `predictionInaccurate` is true (DD-09). `cancellation` set to true gives a cancelled arrival.
- For API-06, `lines` is always the stop's served lines from API-02, and `previewMinutes` is the remote-config key `eta_preview_minutes` (default 60).

Fixtures: `et_line1_60min.json` (directions 401 and 402), `et_stop38.json`.

### API-07 Transit route

`POST /api/transit/route`

```json
{ "origin": { "lat": 42.4650, "lng": -2.4450 },
  "destination": { "lat": 42.4560, "lng": -2.4300 },
  "departureAt": "2026-10-22T06:19:00.000Z",
  "maxResults": 5 }
```

The response is `201` with a bare object:

```kotlin
@Serializable internal data class RouteResponseDto(
    val routes: JsonArray? = null,
    val criteria: CriteriaDto? = null,     // maxWalkToBusM 700, maxWalkFromBusM 600, maxBusWaitMin 30, directMarginMin 15
    val rejection: RejectionDto? = null,   // may coexist with routes (DD-17)
)
@Serializable internal data class RouteDto(
    val totalDurationSec: Int? = null, val totalDistanceM: Int? = null,
    val bounds: BoundsDto? = null, val legs: List<LegDto> = emptyList(),
)
@Serializable @JsonClassDiscriminator("mode")
internal sealed interface LegDto {
    @Serializable @SerialName("walk") data class Walk(
        val distanceM: Int? = null, val durationSec: Int? = null,
        val polyline: List<LatLngDto> = emptyList(),    // decoded coordinates, not encoded
    ) : LegDto
    @Serializable @SerialName("bus") data class Bus(
        val distanceM: Int? = null, val durationSec: Int? = null,
        val polyline: List<LatLngDto> = emptyList(),
        val lineId: Int? = null, val lineName: String? = null,
        val lineColor: String? = null,                  // hex (DD-08)
        val direction: String? = null,                  // "asc" | "desc"
        val fromStop: StopRefDto? = null, val toStop: StopRefDto? = null,   // ids may be unknown (DD-18)
        val intermediateStops: List<StopRefDto> = emptyList(),
        val departureEpochMs: Long? = null, val arrivalEpochMs: Long? = null,   // planned, not real-time
    ) : LegDto
}
@Serializable internal data class RejectionDto(val reason: String? = null, val details: Map<String, Double>? = null)
```

Mapping:
- **If any leg fails to decode, drop the whole route.** A route with a missing leg is misleading.
- Use the line colour from API-01 by `lineId`, and fall back to the hex value.
- Resolve stops by id, then by name plus the nearest coordinate on the leg polyline. If both fail, show the name without a link (DD-18).
- Show the routes if there are any. Show the rejection message only when `routes` is empty (DD-17).
- Sort itineraries by arrival time (FR-PLAN-04).
- For the first bus leg departing within 60 min, overlay the real-time ETA from API-06 at the boarding stop (FR-PLAN-06).
- Reject points outside the service bounding box expanded by 5 km on the client, before calling (FR-PLAN-10).

| `rejection.reason` | Message key | Default English |
| --- | --- | --- |
| `no-route` | `plan.error.no_route` | No bus connects these points at this time. |
| `walk-too-far-destination` | `plan.error.walk_far_destination` | The destination is more than {maxWalkFromBusM} m from any stop. |
| `walk-too-far-origin` (assumed) | `plan.error.walk_far_origin` | The origin is more than {maxWalkToBusM} m from any stop. |
| anything else | `plan.error.generic` | No route could be calculated. |

Fixtures: `route_ok.json`, `route_rejected.json`.

### API-08 Walk distances

`POST /api/transit/walk-distances`

```json
{ "origin": { "lat": 42.4650, "lng": -2.4450 },
  "destinations": [ { "id": 38, "lat": 42.426317, "lng": -2.461335 } ] }
```

The response is `{ "items": [ { "id": 38, "distanceM": 512, "durationSec": 410 } ] }`.

- Send at most **40** destinations, pre-filtered by straight-line distance within 800 m and taking the nearest first.
- Re-request only when the user has moved more than 50 m. Until the response arrives, sort by straight-line distance (FR-HOME-03, FR-STOP-11).
- Never log the coordinates (NFR-PRIV-02).

Fixture: `walk_distances.json`.

### API-09 OTP GraphQL (optional, not in the MVP)

`POST /otp/routers/default/index/graphql` is used only for walking-path previews. `legGeometry.points` is a Google encoded polyline with precision 5. The query and schema notes are in spec §4.9.

### API-10 Fare products (v1.0)

`GET /api/fare-products` returns `result.cachedAt` and `result.products` (57 items). The fields are `productCode`, `name`, `description`, `literalDescription`, `price` (cents), `maxQuantity`, `startPeriod` (the sentinel `2000-01-01` means always valid) and `endPeriod?`. Products are classified by the ordered rules of spec §6.7. QR and EMV variants are hidden while `feature_qr` is false. Fixture: `fares.json`.

### API-11 Recharge points (v1.0)

`GET /api/tscs/recharge-points` returns `result.rechargePoints` (121 items) with `{ id, name, type, address, lat, lng }`. Use this endpoint only; the static file has a different id space (DD-12). Fixture: `recharge_points.json`.

### API-12 CMS (v1.0)

Strapi v5 `{ data, meta.pagination }` under `/api/cms/news` (`page`, `pageSize=10`, `header=true`), `/sections`, `/cards`, `/pies` and `/rrss`.

- Absolute media URL = `https://transporteurbano.logrono.es/cms` + `url`. Without `/cms` the URL returns 404. The app caches images itself.
- News bodies are HTML inside a rich-text node (DD-13). Concatenate the text nodes, then sanitise with the allow-list `p, br, strong, b, em, i, u, ul, ol, li, a[href], h2–h4` (NFR-SEC-02). Open only `https` links (NFR-SEC-03).
- `affectedLineIds` comes from matching patterns such as "línea 3", "líneas 3 y 9", "L3" and "B1" (FR-NEWS-05).

Fixtures: `news_page1.json`, `news_header.json`, `cards.json`, `pies.json`, `rrss.json`.

## 5. CartoCiudad geocoder (FR-PLAN-02)

Base URL: `https://www.cartociudad.es/geocoder/api/geocoder/`. **The parameter and field names below come from public CartoCiudad documentation and still need confirming against a live capture (LB-302).**

| Id | Call | Use |
| --- | --- | --- |
| GEO-01 | `GET candidates?q={text}&limit=8&provincia_filter=La Rioja` | Address suggestions: at least 3 characters, debounced 300 ms, at most 8 shown |
| GEO-02 | `GET find?q={address}&type={type}&id={id}` | Resolve a selected candidate that comes back without usable coordinates |

- Map each candidate to `PlaceSuggestion(id, title = address, subtitle = municipality, type, position?)`.
- Merge geocoder suggestions with local stop matches and saved places in `SearchPlaces`, in this order: saved places, stops, addresses.
- Requests carry only the typed text. Disclose geocoding in the privacy policy (NFR-PRIV-02).
- Attribution: CartoCiudad and IGN data are published under CC BY 4.0. Credit them on the About data screen (to be confirmed under OQ-11).
- Fixtures to capture: `geocoder_candidates_logrono.json`, `geocoder_find.json`.

## 6. Data defect implementation map

The pipeline order is spec §5.4. Each rule has a home and a test prefix.

| DD | Handled in | Test prefix |
| --- | --- | --- |
| DD-01 stop id formats | `IdParser.stopId()` | `dd01_` |
| DD-02 line id formats | `IdParser.lineId()` | `dd02_` |
| DD-03 VM and ET vehicle ids differ | Not joined anywhere; VM fleet number shown only in the vehicle sheet | `dd03_` |
| DD-04 direction encodings | `DirectionMapper`, `EtDirectionInferrer` + `direction_code_cache` | `dd04_` |
| DD-05 VM local time labelled Z | `VehicleMapper.correctNextStopTime()` | `dd05_` (CET and CEST) |
| DD-06 `"[object Object]"` lineRef | `IdParser.lineId()` → `null` | `dd06_` |
| DD-07 `""` instead of null | `String?.blankToNull()` applied in every mapper | `dd07_` |
| DD-08 colour formats | `ColorParser` (rgba and hex), `ContrastColor` | `dd08_` |
| DD-09 low-confidence predictions | `ArrivalMapper` → `PredictionQuality` | `dd09_` |
| DD-10 pattern stops missing from the catalogue | `NetworkNormaliser.synthesiseProvisionalStops()` | `dd10_` |
| DD-11 typos and trailing spaces | `NameNormaliser` + remote-config `name_overrides` | `dd11_` |
| DD-12 two recharge datasets | `ContentApi` calls API-11 only | `dd12_` |
| DD-13 HTML inside rich text | `CmsHtmlSanitiser` (v1.0) | `dd13_` |
| DD-14 stale timetable snapshot | Never fetched | none |
| DD-15 POST returns 201 | `HttpClient` success check | `dd15_` |
| DD-16 shared stop names | `StopHeaderUi` headsigns, search result subtitles | `dd16_` |
| DD-17 rejection together with routes | `PlannerMapper` | `dd17_` |
| DD-18 unknown planner stop ids | `PlannerStopResolver` | `dd18_` |

## 7. Error mapping

| Condition | `AppError` | UI (spec §7.3) |
| --- | --- | --- |
| No connectivity, DNS failure, timeout | `Network` | Error state with reason "network" and Retry; previous data is kept if present. The offline banner is v1.0 (ADR-0008) |
| Non-2xx | `Server(status)` | Error state, reason "server", Retry |
| Non-JSON body, missing envelope, undecodable root | `Data` | Error state, reason "data", Retry; counted in the `DataQualityReport` |
| Items dropped during decoding or validation | none (partial) | Content shown silently; counted in analytics |
| Deep-link id not found | `NotFound` | Parent list with a "Not found" toast |
| Static sync failure | logged only | Nothing shown (FR-ERR-04) |

## 8. Remote configuration

The keys and defaults are spec §8.9, plus the additions marked *new*. Defaults are compiled into `:shared:core:config` (`RemoteConfigKey`, `CompiledDefaultsRemoteConfig`, LB-004) so the app works with no remote config at all. The backend is undecided (OQ-16). Polling intervals are read through `pollInterval`, which applies the 10 s floor of spec §8.6. Line ETAs (spec §8.6, 20 s) have no key of their own and reuse `poll_arrivals_ms`. The owner confirmed the defaults of `fare_tier_texts`, `night_service_text` and `map_style_url` on 2026-10-26.

| Key | Type | Default | Use |
| --- | --- | --- | --- |
| `api_base_url` | String | `https://transporteurbano.logrono.es` | Switch to the proxy |
| `poll_vehicles_ms` | Int | 10000 | FR-LIVE-02 |
| `poll_arrivals_ms` | Int | 20000 | FR-STOP-05 |
| `vehicle_grace_ms` | Int | 45000 | FR-LINE-07 |
| `eta_preview_minutes` | Int | 60 | FR-STOP-03 |
| `feature_live_map` | Bool | true | Kill switch for the buses layer |
| `feature_planner` | Bool | true | Kill switch for How to Get There |
| `feature_qr` | Bool | false | FR-FARE-05 |
| `name_overrides` | JSON map | `{"PRADOVIEJOO":"PRADOVIEJO"}` | DD-11 |
| `fare_tier_texts` | JSON map | `FN20` and `FN50`: neutral es and en texts ("Descuento del 20 %", "20 % discount"), with no eligibility until OQ-09 is answered | FR-FARE-06 |
| `night_service_text` | JSON map | The FR-STOP-06 sentence in es and en, until OQ-08 is answered | FR-STOP-06 |
| `min_supported_version` | String | `1.0.0` | Force-update prompt |
| `map_style_url` *(new)* | String | Empty: no provider until the owner decides OQ-10 (LB-013) | Switch tile provider without a release |
| `geocoder_province_filter` *(new)* | String | `La Rioja` | FR-PLAN-02 |

## 9. Fixtures

Golden payloads live in `fixtures/` and are captured with `scripts/record-fixtures.sh` (LB-003). The set is spec §10.2 plus the geocoder captures. A weekly CI job re-records them and fails on schema diffs. Synthetic fixtures for the CET/CEST tests of DD-05 are hand-written and prefixed `synthetic_`.

## 10. Contract change log

| Date | Endpoint | Change observed | Action |
| --- | --- | --- | --- |
| 2026-10-22 | All | Initial capture (spec v0.1) | Baseline |
