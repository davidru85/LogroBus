# Playbook: record an architecture decision

**Input:** a decision that is significant or hard to reverse (see [`engineering_workflow.md`](../engineering_workflow.md) §10).
**Used by:** humans, every AI agent, and the Claude Code skill `/new-adr`.

1. **Check for an existing ADR** in [`adr/README.md`](../adr/README.md). If the new decision changes an accepted ADR, the new ADR supersedes it; never rewrite an accepted one.
2. **Take the next free number** `NNNN` and create `docs/adr/NNNN-kebab-case-title.md` from the template in [`adr/README.md`](../adr/README.md).
3. **Fill it in:** Context, Decision, Consequences, Alternatives considered. Set the status to `Proposed` unless the owner has already approved it.
4. **Add it to the index** table in `docs/adr/README.md`. If it supersedes another ADR, change that ADR's status to `Superseded by ADR-NNNN`.
5. **Link it** from the docs it affects, such as `architecture.md`, `AGENTS.md` or `design_guidelines.md`, and add a `project_log.md` entry.
6. **Ask the owner to approve it.** After approval, set the status to `Accepted` with the date.
