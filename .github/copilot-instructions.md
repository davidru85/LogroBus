# Copilot instructions

All instructions for AI agents in this repository live in [`AGENTS.md`](../AGENTS.md) at the repository root. Read it before making any change; it is canonical and overrides anything else. Agent workflows live in [`docs/playbooks/`](../docs/playbooks/), and the harness is described in [`docs/agent_harness.md`](../docs/agent_harness.md).

Four rules are easy to miss, so they are repeated here. `AGENTS.md` remains the authoritative version.

1. Talk to the owner in Peninsular Spanish. Write everything in the repository in technical English.
2. Every pull request updates both `README.md` and `README.es.md`, with equivalent content.
3. Run `scripts/checks/run-all.sh` before you finish, and report its real result.
4. Never push to `main`. Work on a story branch and open a pull request.
