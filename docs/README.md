# LogroBus documentation

Living project documentation. Start with [`../AGENTS.md`](../AGENTS.md) for the working rules, then read the document that matches your task.

| Document | Purpose |
| --- | --- |
| [`../logrono-bus-app-functional-spec.md`](../logrono-bus-app-functional-spec.md) | Baseline functional specification v0.1: scope, API capture, domain model, requirements. Frozen; deviations are recorded in ADRs |
| [`architecture.md`](architecture.md) | KMP module layout, layers, data flow, polling and sync, Android and iOS integration |
| [`api_spec.md`](api_spec.md) | Client contract for every remote call: source BFF endpoints, DTOs, normalisation, geocoder, remote config, fixtures |
| [`design_guidelines.md`](design_guidelines.md) | The three sections screen by screen, Material 3 Expressive and Liquid Glass rules, tokens, states, accessibility, copy |
| [`backlog.md`](backlog.md) | Milestones, epics and stories with status and spec traceability |
| [`open_questions.md`](open_questions.md) | Open questions, owners, proposed defaults and answers |
| [`project_log.md`](project_log.md) | Dated record of work done and decisions taken |
| [`adr/`](adr/README.md) | Architecture decision records |

## Identifier conventions

| Prefix | Meaning | Defined in |
| --- | --- | --- |
| `FR-<MODULE>-nn` | Functional requirement | Spec §6 |
| `NFR-<AREA>-nn` | Non-functional requirement | Spec §9 |
| `DD-nn` | Data defect at source and its handling rule | Spec §5.3 |
| `API-nn` | Source endpoint | Spec §4, `api_spec.md` |
| `OQ-nn` | Open question | Spec §10.4, `open_questions.md` |
| `SC-nn` | Screen from the spec inventory | Spec §7.2, mapped in `design_guidelines.md` |
| `M<n>` | Milestone | `backlog.md` |
| `E<n>` | Epic | `backlog.md` |
| `LB-nnn` | Backlog story; the hundreds digit matches the epic | `backlog.md` |
| `ADR-nnnn` | Architecture decision record | `adr/` |

## Writing conventions

- English, Markdown, Mermaid for diagrams, ISO 8601 dates (`2026-10-22`).
- Cite requirement ids rather than restating requirements.
- Each living doc carries a "Last updated" date in its header table; update it when you change the doc.
