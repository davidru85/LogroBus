<!--
Lightweight template for docs, chore, CI and build-only changes, with no app behaviour change.
Title: a Conventional Commit, e.g. `docs(adr): record SKIE decision` or `ci(policy): split doc checks`. Write everything in technical English.
-->

## Summary

<!-- What changes and why, in 1–3 bullets. -->

-

**Related:** LB-xxx / ADR-nnnn / OQ-nn (if any)

## Local quality gates

<!-- CI runs the policy checks; the Gradle checks join in LB-002 (docs/ci_cd.md §1). Paste the final summary of scripts/checks/run-all.sh. -->

```text

```

## Checklist

- [ ] This PR changes no app behaviour; otherwise use the default template
- [ ] `README.md` and `README.es.md` are updated with equivalent content (mandatory)
- [ ] `docs/project_log.md` entry is updated
- [ ] Links and the docs index are updated (`docs/README.md`, the `AGENTS.md` documentation map) if docs were added or renamed
