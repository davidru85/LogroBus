<!--
Default template, for features, fixes and refactors.
For docs, chore, CI or build-only changes, use the lightweight template: gh pr create --template docs-chore.md
Title: a Conventional Commit, e.g. `feat(near-me): stop sheet arrivals [LB-205]`. Write everything in technical English.
-->

## Summary

<!-- What changes and why, in 2–5 bullets. -->

-

**Story:** LB-xxx
**Requirements:** FR-…, NFR-…, DD-…

## Acceptance criteria

<!-- Copy the story's criteria from docs/acceptance_criteria.md. Tick each one and give its evidence: a test name, a screenshot or a manual check. -->

- [ ] … (evidence: …)

## Test-driven development

- [ ] The TDD protocol was followed: red, green and refactor commits per cycle, pushed after each cycle (docs/testing_strategy.md §7.1)
- [ ] Every red test was seen failing for the expected reason before the green commit
- [ ] The tests required for this kind of change were added or updated (docs/testing_strategy.md §3)
- [ ] Bug fixes include a regression test that failed before the fix

## Local quality gates

<!-- CI runs the policy checks; the Gradle checks join in LB-002 (docs/ci_cd.md §1). Paste the final summary of scripts/checks/run-all.sh. -->

```text

```

## Screenshots (UI changes only)

| State | Android | iOS |
| --- | --- | --- |
| | | |

## Checklist

- [ ] `README.md` and `README.es.md` are updated with equivalent content (mandatory)
- [ ] `docs/backlog.md` status and `docs/project_log.md` entry are updated
- [ ] Other affected docs and ADRs are updated
- [ ] UI changes: strings exist in es-ES and en, and TalkBack / VoiceOver labels have been checked
- [ ] No secrets, no live-source calls in tests, no new dependency without a justification
