# ADR-0006: Agent-agnostic harness with thin per-agent adapters

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-10-22 |
| Deciders | Project owner |
| Related | NFR-MNT-05, `agent_harness.md`, `ci_cd.md`, `engineering_workflow.md` |

## Context

The owner will use several AI coding agents on this repository: Claude Code and others such as Codex, Gemini CLI and GitHub Copilot. Each agent has its own instruction file and its own optional extensions (hooks, subagents, skills). Copying rules into each agent's format would make them drift apart. The repository is private on GitHub Free, so there is no branch protection, and the owner has ruled that CI does not run while the repository is private.

## Decision

- **`AGENTS.md` is the only rule book.** Per-agent files (`CLAUDE.md`, `GEMINI.md`, `.github/copilot-instructions.md`) only import or point to it.
- **Every quality gate is a script** in `scripts/checks/`, with `scripts/checks/run-all.sh` as the single local entry point.
- **Multi-step workflows are documents** in `docs/playbooks/`. Agent-specific skills or commands are thin wrappers around them.
- **Enforcement is layered and agent-independent:**
  - git hooks in `.githooks/`, installed by `scripts/bootstrap.sh`;
  - CI jobs that call the same scripts, active once the repository is public;
  - agent hooks (Claude Code) that call the same scripts for earlier feedback.

## Consequences

- One edit updates the behaviour of every agent. Adding an agent costs one pointer file.
- Agents without hook support depend on following `AGENTS.md`, and on the git hooks, to run the gates. The PR template requires the `run-all.sh` output as evidence.
- Every script must stay POSIX-friendly (bash 3.2) and fast, because hooks and CI run them often.
- Enforcement is weaker while the repository is private: the git hooks can be skipped with `--no-verify`. The owner's review of the pasted gate output is the backstop.

## Alternatives considered

- **Claude Code only, with rules in `.claude/`.** Simpler, but it locks the project to one agent and contradicts the owner's choice.
- **Duplicated instructions per agent.** Rejected: rules would drift between agents.
- **Running CI on the private repository.** Rejected by the owner's rule (`ci_cd.md` §1).
