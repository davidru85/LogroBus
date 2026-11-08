#!/usr/bin/env bash
# Usage: scripts/record-fixtures.sh [--list | --check] [endpoint-id...]
#
# The only place in the project that may call the live source (spec §8.7, docs/testing_strategy.md §4.3). Unit and
# integration tests decode the captures in fixtures/; they never call it.
#
#   (no mode)   Captures every endpoint below into fixtures/ and prints one line per fixture:
#               "<id>  <file>  <bytes>  unchanged|written|DRIFT". Captures are staged in a temporary directory and
#               moved into place only when the whole run succeeded, so a failure leaves fixtures/ untouched.
#               DRIFT means the shape (scripts/ci/fixture-shape.sh) of the new capture differs from the committed one;
#               the files are still written, the human decides with docs/playbooks/record-fixtures.md, and the exit
#               code is 1 (docs/testing_strategy.md §4.3).
#   --check     Records into a temporary directory and compares the shape with the committed fixture, without
#               writing anything. Prints the same lines plus the diff; exits 1 on any drift or a missing fixture.
#               This is the drift gate the weekly CI job runs (risk R1).
#   --list      Prints the endpoint table ("<id>  <method>  <file>") and exits without touching the network.
#
# Endpoint ids restrict the run: scripts/record-fixtures.sh API-03 GEO-01. An unknown id is an error.
#
# Every response body is capped at MAX_BODY_BYTES (16 MiB, an order of magnitude above the largest fixture the source
# serves today): --max-filesize stops an oversized transfer on the wire, and the size on disk is checked again after
# decompression, because --compressed makes the wire cap approximate. A body above the cap fails its endpoint with a
# message naming it, and nothing reaches fixtures/.
#
# Environment, for tests only (never for a real capture):
#   RECORD_BASE_URL           Source base URL, default https://transporteurbano.logrono.es (api_spec.md §1).
#   RECORD_GEOCODER_BASE_URL  CartoCiudad base URL, default https://www.cartociudad.es/geocoder/api/geocoder.
#   RECORD_FIXTURES_DIR       Directory the captures are written to and compared against, default <repo>/fixtures.
#   RECORD_MIN_INTERVAL       Seconds between requests, default 1. A real capture never lowers it.
# These overrides exist so the architecture test can point the recorder at a local stub server that binds 127.0.0.1
# and pace it faster; the contract tests must never reach the live source (AGENTS.md §9).
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd -P)"
shape_script="$(cd "$(dirname "$0")" && pwd -P)/ci/fixture-shape.sh"

source_base="${RECORD_BASE_URL:-https://transporteurbano.logrono.es}"
geocoder_base="${RECORD_GEOCODER_BASE_URL:-https://www.cartociudad.es/geocoder/api/geocoder}"
fixtures_dir="${RECORD_FIXTURES_DIR:-$root/fixtures}"
min_interval="${RECORD_MIN_INTERVAL:-1}"

# The largest fixture the source serves today is sections.json at about 1.1 MB. The cap leaves an order of magnitude
# of headroom and exists so that a broken or hostile source cannot fill the disk beside fixtures/: every capture is
# written into the staging directory before anyone looks at it (risk R2, NFR-REL-02).
MAX_BODY_BYTES=16777216

# The endpoint table: one row per fixture. Ids, paths and bodies come from docs/api_spec.md §2, the fixture names from
# spec §10.2 plus the two geocoder captures of api_spec.md §5. "source" and "geocoder" select the base URL, "-" is
# a row with no body, and the timeout follows api_spec.md §1: 10 s for the live endpoints and the geocoder, 30 s for
# the static ones.
endpoints() {
  cat <<'EOF'
API-01|GET|source|/api/linesDiscovery/lines|-|30|lines_2026-10-22.json
API-02|GET|source|/api/linesDiscovery/stops|-|30|stops_2026-10-22.json
API-03|GET|source|/api/vehicleMonitoring/all|-|10|vm_all_with_object_lineRef.json
API-04|GET|source|/api/vehicleMonitoring/byLine/2|-|10|vm_line2.json
API-05|GET|source|/api/estimatedTimetable/byLine/1?previewMinutes=60|-|10|et_line1_60min.json
API-06|GET|source|/api/estimatedTimetable/byStop/38?lines=1&previewMinutes=60|-|10|et_stop38.json
API-07a|POST|source|/api/transit/route|{"origin":{"lat":42.4650,"lng":-2.4450},"destination":{"lat":42.4560,"lng":-2.4300},"departureAt":"2026-10-19T07:19:00.000Z","maxResults":5}|10|route_ok.json
API-07b|POST|source|/api/transit/route|{"origin":{"lat":42.4650,"lng":-2.4450},"destination":{"lat":42.5000,"lng":-2.3800},"departureAt":"2026-10-19T07:19:00.000Z","maxResults":5}|10|route_rejected.json
API-08|POST|source|/api/transit/walk-distances|{"origin":{"lat":42.4650,"lng":-2.4450},"destinations":[{"id":38,"lat":42.426317,"lng":-2.461335},{"id":61,"lat":42.463321,"lng":-2.447845}]}|10|walk_distances.json
API-10|GET|source|/api/fare-products|-|30|fares.json
API-11|GET|source|/api/tscs/recharge-points|-|30|recharge_points.json
API-12a|GET|source|/api/cms/news?page=1&pageSize=10|-|30|news_page1.json
API-12b|GET|source|/api/cms/news?page=1&pageSize=10&header=true|-|30|news_header.json
API-12c|GET|source|/api/cms/sections|-|30|sections.json
API-12d|GET|source|/api/cms/cards|-|30|cards.json
API-12e|GET|source|/api/cms/pies|-|30|pies.json
API-12f|GET|source|/api/cms/rrss|-|30|rrss.json
GEO-01|GET|geocoder|candidates?q=Logro%C3%B1o&limit=8&provincia_filter=La%20Rioja|-|10|geocoder_candidates_logrono.json
GEO-02|GET|geocoder|find?q=CALLE%20FUEROS%20DE%20LOGRO%C3%91O%2C%20Logro%C3%B1o&type=callejero&id=260890000097|-|10|geocoder_find.json
EOF
}

usage() {
  awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}

die() {
  printf 'record-fixtures: %s\n' "$1" >&2
  exit 1
}

# True when [id] is one of the ids the caller asked for, or none were asked for.
wanted() {
  if [ -z "$selected" ]; then
    return 0
  fi
  case " $selected " in
    *" $1 "*) return 0 ;;
    *) return 1 ;;
  esac
}

# Joins a base URL and a row's path: an absolute path keeps the base, a relative one (the geocoder's) takes a slash.
join_url() {
  case "$2" in
    /*) printf '%s%s' "$1" "$2" ;;
    *) printf '%s/%s' "$1" "$2" ;;
  esac
}

# Keeps the trailing slash of an override from doubling the one that joins the base and the path.
strip_trailing_slash() {
  local url="$1"
  while [ "$url" != "/" ] && [ "${url%/}" != "$url" ]; do
    url="${url%/}"
  done
  printf '%s' "$url"
}

bytes_of() {
  wc -c <"$1" | tr -d ' '
}

staging=""
# Runs on exit, so a failed or interrupted capture never leaves the temporary staging directory behind.
# shellcheck disable=SC2329
cleanup() {
  if [ -n "$staging" ] && [ -d "$staging" ]; then
    rm -rf "$staging"
  fi
}
trap cleanup EXIT

# Exactly one mode, and it must come first: `--list --check` used to run the first and exit 0, which let a caller
# believe it had checked the fixtures when it had only listed them.
mode='record'
case "${1:-}" in
  --list)
    mode='list'
    shift
    ;;
  --check)
    mode='check'
    shift
    ;;
  -h | --help)
    usage
    exit 0
    ;;
  --*)
    printf 'record-fixtures: unknown option: %s\n' "$1" >&2
    usage >&2
    exit 1
    ;;
esac
case "${1:-}" in
  --*)
    printf 'record-fixtures: one mode at a time: %s\n' "$1" >&2
    usage >&2
    exit 1
    ;;
esac

selected=""
for id in "$@"; do
  row="$(endpoints | awk -F'|' -v id="$id" '$1 == id { print; exit }')"
  if [ -z "$row" ]; then
    die "unknown endpoint id: $id (see --list)"
  fi
  selected="$selected $id"
done

if [ "$mode" = 'list' ]; then
  endpoints | awk -F'|' -v ids="$selected" 'ids == "" || index(ids " ", " " $1 " ") > 0 {
    printf "%s\t%s\t%s\n", $1, $2, $7
  }'
  exit 0
fi

for tool in curl jq; do
  command -v "$tool" >/dev/null 2>&1 || die "$tool is required (it records and pretty-prints the captures)"
done
[ -f "$root/version.properties" ] || die "no version.properties at $root"
version="$(sed -n 's/^VERSION_NAME=//p' "$root/version.properties")"
[ -n "$version" ] || die "version.properties declares no VERSION_NAME"
# The User-Agent of spec §8.7 names the platform and the OS version. The recorder runs under bash, so it reads them
# from the kernel rather than pretending to be a phone; `uname -s` is "Darwin" on macOS and "Linux" on the CI runner.
os="$(uname -s)-$(uname -r)"

source_base="$(strip_trailing_slash "$source_base")"
geocoder_base="$(strip_trailing_slash "$geocoder_base")"

# The staging directory sits beside fixtures/, so moving a capture into place is a rename on one filesystem, not a
# copy: an interrupted run can never leave a half-written fixture behind.
fixtures_parent="$(dirname "$fixtures_dir")"
mkdir -p "$fixtures_parent"
staging="$(mktemp -d "$fixtures_parent/.record-fixtures.XXXXXX")"

# The recorder is the project's polite client: the source sends no rate-limit headers and it can change without
# notice, so one request per second at most (spec §8.7, risk R2). Counted between requests, never before the first.
requests=0
drift=0
missing=0
# The report is buffered and printed once the run is whole, so it never claims a capture that a later failure kept
# out of fixtures/.
# The report goes to files: bash 3.2 has no associative arrays, and a file keeps it ordered and free of quoting.
report_file="$staging/report"
detail_file="$staging/detail"
: >"$report_file"
: >"$detail_file"

while IFS='|' read -r id method base path body timeout file; do
  [ -n "$id" ] || continue
  wanted "$id" || continue

  case "$base" in
    source) url_base="$source_base" ;;
    geocoder) url_base="$geocoder_base" ;;
    *) die "$id: unknown base '$base' in the endpoint table" ;;
  esac
  url="$(join_url "$url_base" "$path")"

  if [ "$requests" -gt 0 ]; then
    sleep "$min_interval"
  fi
  requests=$((requests + 1))

  body_file="$staging/$id.body"
  curl_args=(
    --silent --show-error --compressed
    --max-filesize "$MAX_BODY_BYTES"
    --connect-timeout 5 --max-time "$timeout"
    --output "$body_file" --write-out '%{http_code}'
    --request "$method"
    --header "User-Agent: LogronoBus/$version (fixture recorder $os; +https://github.com/davidru85/LogroBus)"
    --header 'Accept: application/json'
    --header 'Accept-Encoding: gzip'
  )
  # "-" marks a row without a body, so a GET carries neither a body nor the JSON content type.
  if [ "$body" != '-' ]; then
    curl_args+=(--header 'Content-Type: application/json' --data "$body")
  fi
  # Only the body reaches the staging directory, so a failure here can never touch fixtures/.
  curl_status=0
  status="$(curl "${curl_args[@]}" "$url")" || curl_status=$?
  if [ "$curl_status" -ne 0 ]; then
    # curl exits 63 when --max-filesize is what stopped the transfer; name the cap rather than the transfer.
    if [ "$curl_status" -eq 63 ]; then
      die "$id: $url answered a body above the $MAX_BODY_BYTES-byte cap (curl exit 63); fixtures/ is untouched"
    fi
    die "$id: the request to $url failed (curl exit $curl_status); fixtures/ is untouched"
  fi

  case "$status" in
    2*) ;;
    *) die "$id: $url answered $status, not a 2xx; fixtures/ is untouched" ;;
  esac

  # `--compressed` makes `--max-filesize` a cap on the wire, and a gzip bomb decompresses far beyond it: the size
  # that matters is the one on disk, after curl has inflated the body (risk R2, NFR-REL-02).
  body_bytes="$(bytes_of "$body_file")"
  if [ "$body_bytes" -gt "$MAX_BODY_BYTES" ]; then
    die "$id: $url answered $body_bytes bytes, above the $MAX_BODY_BYTES-byte cap; fixtures/ is untouched"
  fi

  # api_spec.md §3: every source response is an object or an array. `jq empty` alone is not enough: it accepts a
  # zero-length body, `null` and a scalar, and canonicalising one of those writes an empty or shapeless fixture that a
  # later `--check` compares against itself and reports unchanged. NFR-REL-02 treats such a body as an error.
  if ! jq -e 'type == "object" or type == "array"' "$body_file" >/dev/null 2>&1; then
    die "$id: $url answered a body that is not a JSON object or array; fixtures/ is untouched"
  fi

  # Canonical form: sorted keys and two-space indent, so re-recording unchanged data produces no diff (LB-003).
  jq --sort-keys --indent 2 . "$body_file" >"$staging/$file"
  rm -f "$body_file"
  size="$(bytes_of "$staging/$file")"

  committed="$fixtures_dir/$file"
  if [ ! -f "$committed" ]; then
    if [ "$mode" = 'check' ]; then
      printf '%s\t%s\t%s\tDRIFT\n' "$id" "$file" "$size" >>"$report_file"
      printf 'no committed fixture at %s\n' "$committed" >>"$detail_file"
      missing=$((missing + 1))
      continue
    fi
    printf '%s\t%s\t%s\twritten\n' "$id" "$file" "$size" >>"$report_file"
    continue
  fi

  # The shape, not the bytes: live data changes every capture, the contract must not (risk R1).
  diff_status=0
  shape_diff="$(bash "$shape_script" --diff "$committed" "$staging/$file")" || diff_status=$?
  if [ "$diff_status" -ne 0 ]; then
    printf '%s\t%s\t%s\tDRIFT\n' "$id" "$file" "$size" >>"$report_file"
    printf '%s\n' "$shape_diff" >>"$detail_file"
    drift=$((drift + 1))
  elif [ "$mode" = 'check' ]; then
    printf '%s\t%s\t%s\tunchanged\n' "$id" "$file" "$size" >>"$report_file"
  elif cmp -s "$committed" "$staging/$file"; then
    printf '%s\t%s\t%s\tunchanged\n' "$id" "$file" "$size" >>"$report_file"
  else
    printf '%s\t%s\t%s\twritten\n' "$id" "$file" "$size" >>"$report_file"
  fi
done < <(endpoints)

# Nothing reached fixtures/ yet: only a whole, successful run replaces the committed captures.
if [ "$mode" = 'record' ]; then
  mkdir -p "$fixtures_dir"
  while IFS='|' read -r id method base path body timeout file; do
    [ -n "$id" ] || continue
    wanted "$id" || continue
    [ -f "$staging/$file" ] || continue
    mv -f "$staging/$file" "$fixtures_dir/$file"
  done < <(endpoints)
fi

# One line per fixture, then the shape diffs (indented) of any that drifted.
cat "$report_file"
sed 's/^/  /' "$detail_file"

if [ "$missing" -gt 0 ]; then
  die "$missing fixture(s) missing from $fixtures_dir"
fi
if [ "$drift" -gt 0 ]; then
  if [ "$mode" = 'check' ]; then
    die "$drift fixture(s) changed shape (risk R1; docs/playbooks/record-fixtures.md)"
  fi
  die "$drift fixture(s) changed shape; the captures are written, review the diff before committing (risk R1)"
fi
exit 0
