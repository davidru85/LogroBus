# Playbook: re-record the source fixtures

**Input:** none, or a list of endpoints.
**Used by:** humans, every AI agent, the Claude Code skill `/record-fixtures`, and the weekly CI job.
**Precondition:** `scripts/record-fixtures.sh` exists (LB-003). Until then, this playbook cannot run.

This playbook calls the **live** source. Run it only when it is needed, never in a loop, and respect the 1 request per second limit built into the script.

1. Run `scripts/record-fixtures.sh` (optionally with endpoint ids). It writes dated JSON files to `fixtures/` and prints a shape diff against the previous capture of each endpoint.
2. **If the shape did not change:** keep the new files only if they add value, such as a new edge case. Otherwise discard them.
3. **If the shape changed** (the script exits non-zero):
   - Record the change in the change log of [`api_spec.md`](../api_spec.md) (§10), and update the affected endpoint section.
   - Update the DTOs, mappers and contract tests. Add or adjust `ddNN_` tests if a data defect appeared or disappeared.
   - Add a `project_log.md` entry, and tell the owner. A contract change is risk R1.
4. **Never edit recorded fixtures by hand.** For variations, add `synthetic_*` fixtures ([`testing_strategy.md`](../testing_strategy.md) §4.2).
