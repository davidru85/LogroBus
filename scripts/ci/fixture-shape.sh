#!/usr/bin/env bash
# Usage: scripts/ci/fixture-shape.sh <file.json>
#        scripts/ci/fixture-shape.sh --diff <old.json> <new.json>
#
# Prints the shape of a JSON document: one "<path><TAB><type>" line per key path, sorted, so that two captures of
# the same endpoint can be compared without their data (docs/api_spec.md §9, docs/testing_strategy.md §4.3).
#
# Rules the diff depends on:
#   - An array collapses to "[]" in the path, so the shape does not depend on how many items the source returned
#     and no array index ever appears.
#   - Types are object, array, string, number, boolean and null. Integers and floats are both "number": the source
#     mixes 42.0 and 42 for the same field.
#   - A path that carries null in some items and a scalar in others reports the scalar type only.
#   - A path whose items carry two different scalar types reports them joined by "|", so it never pretends to be one.
#   - --diff prints "+ path type" for an added path, "- path type" for a removed one and "~ path old->new" for a
#     changed type. A null on either side is never drift: the field is nullable, not changed. Exit 0 when the two
#     shapes are equal (and print nothing), 1 when they differ.
set -euo pipefail

# One implementation for both modes. jq only: the contract is the jq type names, and the recorder needs jq anyway.
# shellcheck disable=SC2016  # The jq program is deliberately literal: "$types" is a jq variable, not shell text.
shape_filter='
def path_text($segments):
  reduce $segments[] as $segment ("";
    if . == "" then $segment
    elif $segment == "[]" then . + "[]"
    else . + "." + $segment
    end)
  | if . == "" then "$" else . end;

def node_type:
  if type == "string" then "string"
  elif type == "number" then "number"
  elif type == "boolean" then "boolean"
  elif type == "array" then "array"
  elif type == "object" then "object"
  else "null"
  end;

# Every node of the document. Arrays contribute "[]" instead of an index, so the path is data-independent.
def shapes($path):
  if type == "object" then
    {path: path_text($path), type: "object"},
    (to_entries[] as $entry | ($entry.value | shapes($path + [$entry.key])))
  elif type == "array" then
    {path: path_text($path), type: "array"},
    (.[] | shapes($path + ["[]"]))
  else
    {path: path_text($path), type: node_type}
  end;

[shapes([])]
| group_by(.path)
| map(
    . as $node
    | ($node | map(.type) | unique) as $types
    | ($types | map(select(. != "null"))) as $known
    | (if ($known | length) == 0 then "null"
       elif ($known | length) == 1 then $known[0]
       else $known | join("|")
       end) as $type
    | "\($node[0].path)\t\($type)"
  )
| .[]
'

usage() {
  awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}

die() {
  printf 'fixture-shape: %s\n' "$1" >&2
  exit 2
}

print_shape() {
  jq -r "$shape_filter" "$1" | LC_ALL=C sort -u
}

# Prints the diff of two captures and fails when they differ.
diff_shapes() {
  local old="$1"
  local new="$2"
  local tab
  local sentinel='@missing@'
  local output
  tab="$(printf '\t')"

  [ -f "$old" ] || die "no such file: $old"
  [ -f "$new" ] || die "no such file: $new"

  tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/fixture-shape.XXXXXX")"
  print_shape "$old" >"$tmp_dir/old"
  print_shape "$new" >"$tmp_dir/new"

  # join needs both inputs sorted on the key; print_shape sorts with the C collation, and so does join here.
  output="$(
    LC_ALL=C join -t "$tab" -a 1 -a 2 -e "$sentinel" -o 0,1.2,2.2 "$tmp_dir/old" "$tmp_dir/new" |
      while IFS="$tab" read -r path before after; do
        if [ "$before" = "$sentinel" ]; then
          printf '%s\n' "+ $path $after"
        elif [ "$after" = "$sentinel" ]; then
          printf '%s\n' "- $path $before"
        elif [ "$before" = "$after" ]; then
          continue
        elif [ "$before" = null ] || [ "$after" = null ]; then
          continue
        else
          printf '%s\n' "~ $path $before->$after"
        fi
      done
  )"

  if [ -z "$output" ]; then
    return 0
  fi
  printf '%s\n' "$output"
  return 1
}

tmp_dir=""
cleanup() {
  if [ -n "$tmp_dir" ]; then
    rm -rf "$tmp_dir"
  fi
}
trap cleanup EXIT

command -v jq >/dev/null 2>&1 || die 'jq is required (brew install jq / apt-get install jq)'

case "${1:-}" in
  --diff)
    [ $# -eq 3 ] || die '--diff takes exactly two files'
    diff_shapes "$2" "$3"
    ;;
  -h | --help)
    usage
    ;;
  '')
    usage >&2
    exit 2
    ;;
  -*)
    die "unknown option: $1"
    ;;
  *)
    [ $# -eq 1 ] || die 'it reads one file at a time'
    [ -f "$1" ] || die "no such file: $1"
    print_shape "$1"
    ;;
esac
