# Playbook: re-record the source fixtures

**Input:** none, or a list of endpoints.
**Used by:** humans, every AI agent, the Claude Code skill `/record-fixtures`, and the weekly CI job.
**Modes:** `scripts/record-fixtures.sh` takes one mode:
- `--list` prints the 19 endpoints (`<id>  <method>  <file>`) and exits without touching the network. Use it to find an endpoint id.
- no mode (the default) records every endpoint into `fixtures/`, or only the ids given, staging the captures and moving them into place only when the whole run succeeded.
- `--check` records into a temporary directory, writes nothing and compares the shape with the committed fixture, exiting 1 on drift. This is what the weekly job runs: `Fixtures` re-records and `Contract drift` opens one issue per ISO week when it fails ([`ci_cd.md`](../ci_cd.md) §5.1). The job never commits.

This playbook calls the **live** source. Run it only when it is needed, never in a loop, and respect the 1 request per second limit built into the script.

1. Run `scripts/record-fixtures.sh` (optionally with endpoint ids). It writes the captures to `fixtures/` and prints one line per fixture, `<id>  <file>  <bytes>  unchanged|written|DRIFT`. Use `--check` first when you only want to know whether the source moved: it prints the same lines plus the shape diff and writes nothing.
   - A shape diff is one line per change: `+ path type` for an added path, `- path type` for a removed one, `~ path old->new` for a changed type. A null against a value is never a diff.
2. **If the shape did not change:** keep the new files only if they add value, such as a new edge case. Otherwise discard them.
3. **If the shape changed** (the script exits non-zero):
   - Record the change in the change log of [`api_spec.md`](../api_spec.md) (§10), and update the affected endpoint section.
   - Update the DTOs, mappers and contract tests. Add or adjust `ddNN_` tests if a data defect appeared or disappeared.
   - Add a `project_log.md` entry, and tell the owner. A contract change is risk R1.
4. **Never edit recorded fixtures by hand.** For variations, add `synthetic_*` fixtures ([`testing_strategy.md`](../testing_strategy.md) §4.2).
