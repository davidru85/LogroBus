# Engineering workflow

| Field | Value |
| --- | --- |
| Status | Active |
| Last updated | 2026-10-22 |
| Related | `AGENTS.md` §10–12, `ci_cd.md`, `agent_harness.md`, `playbooks/` |

This document covers how work moves from a backlog story to `main`: branching, commits, pull requests, review, merge, and the rules that keep `main` releasable. It applies to humans and to every AI agent.

## 1. Branching model

- Development is trunk-based. `main` is always buildable and releasable.
- Every change goes through a short-lived branch and a pull request. **Nobody pushes directly to `main`.**
- Branch names use `<type>/LB-<id>-<short-slug>`, for example `feat/LB-205-stop-sheet`. Branches without a story use `chore/<slug>` or `docs/<slug>`.
- One story per branch. A branch should live at most a few days; split larger stories.

### 1.1 Enforcement while the repository is on GitHub Free

GitHub Free does not offer branch protection or rulesets for private repositories (checked 2026-10-22). In addition, **CI does not run while the repository is private**, by the owner's rule (`ci_cd.md` §1). Until either changes (OQ-20), the rules are enforced locally:

| Rule | How it is enforced |
| --- | --- |
| No direct pushes to `main` | The `.githooks/pre-push` hook rejects them, and agents have deny rules. The owner may bypass the hook with `LOGROBUS_ALLOW_MAIN_PUSH=1` only for emergencies, and must record any bypass in `project_log.md` |
| Gates green before merging | `scripts/checks/run-all.sh` runs locally, and its summary is pasted in the PR. The owner merges only PRs with a clean summary |
| Conventional Commits | The `.githooks/commit-msg` hook. The PR title is checked by the author with `scripts/checks/check-conventional-title.sh` |
| Frozen spec, doc links | The `.githooks/pre-push` hook (blocking) |
| README parity | `scripts/checks/run-all.sh` before opening or updating a PR (blocking; its output is pasted into the PR) and the Claude Code Stop hook. The pre-push hook only warns, because TDD pushes after every cycle (`testing_strategy.md` §7.1) |
| No secrets | The `.githooks/pre-commit` hook |

Every developer and agent environment must run `scripts/bootstrap.sh` once, to install the hooks. When the repository becomes public, the same gates also run in CI (`policy-checks.yml`).

**`main` will be protected on GitHub** (owner decision, 2026-10-22). This becomes possible when the repository goes public, before the MVP is completed (LB-415). The owner then runs `scripts/github/protect-main.sh`, which configures:
- a pull request required for every change (0 required approvals, because the owner is the only maintainer and cannot approve their own PRs; the owner's merge is the approval);
- the required status checks: the policy jobs, and later the `ci-gate` job (`ci_cd.md` §4), up to date with `main`;
- the rules enforced for administrators too;
- resolved conversations, and no force pushes or deletions. Linear history is **not** required, because PRs are merged with merge commits (§5).

## 2. Commits

- Use [Conventional Commits](https://www.conventionalcommits.org/): `type(scope): summary`.
- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.
- Scopes are listed in `AGENTS.md` §12.
- Code changes end the subject with the story id: `feat(near-me): stop sheet arrivals [LB-205]`.
- **TDD commits (mandatory):** each cycle produces a red commit (`test(…)`), a green commit (`feat(…)` or `fix(…)`) and a refactor commit (`refactor(…)`), followed by a push. Each commit body names its phase: `TDD: red`, `TDD: green` or `TDD: refactor`. See `testing_strategy.md` §7.1.
- Subjects are imperative, in technical English, and at most 100 characters.
- Commits made with an AI agent carry the agent's attribution trailer when the agent provides one.

## 3. Pull requests

- **Title:** a Conventional Commit, because the merge commit takes the PR title as its subject (`scripts/github/configure-merge.sh`).
- **Description:** use one of the templates in §3.1, and fill in every section.
- **Size:** aim for under 400 changed lines, not counting screenshots, fixtures and generated code. Split anything larger.
- **The README rule** (`AGENTS.md` §12) applies to every PR: `README.md` and `README.es.md` are updated together with equivalent content.
- **Documentation travels with the change:** backlog status, project log, and every other affected doc or ADR.
- **Drafts:** open the PR as a draft while work is in progress, and mark it ready when the definition of done is met.

### 3.1 PR templates

| Template | File | Use for | Sections |
| --- | --- | --- | --- |
| Default | `.github/pull_request_template.md` | Features, fixes, refactors: anything that changes app behaviour | Summary with story and requirement ids; acceptance criteria with evidence; TDD checklist; local quality gates output; screenshots (Android and iOS); checklist (READMEs, backlog, log, docs, i18n, a11y, secrets) |
| Docs and chore | `.github/PULL_REQUEST_TEMPLATE/docs-chore.md` | Docs, chore, CI and build changes with no behaviour change | Summary; local quality gates output; checklist (READMEs, log, docs index) |

GitHub pre-fills the default template. To pick the other one, run `gh pr create --template docs-chore.md`, or append `?template=docs-chore.md` to the compare URL. Template changes follow the normal PR process and are documented here.

## 4. Review

- The project owner's approval is required to merge.
- An AI review is a useful first pass but never the only approval. The checklists live in `playbooks/review-checklists.md`; Claude Code exposes them as reviewer subagents (`agent_harness.md`).
- The reviewer checks the architecture rules, the data rules, tests against `testing_strategy.md` §3, UI against `design_guidelines.md`, the language policy, and the docs.
- Address review comments with new commits, not force pushes, so the review history stays readable.

## 5. Merge

- **Merge with a merge commit only:** no squash and no rebase (owner decision, 2026-10-22). The repository is configured for this with `scripts/github/configure-merge.sh`:
  - merge commits are the only allowed method;
  - the merge commit title is the PR title and its message is the PR body;
  - branches are deleted after merging.
- **Why:** the TDD protocol's red, green and refactor commits are preserved in `main`'s history. Red commits therefore exist in `main`, but every merge commit (the first-parent history) is green. Read and bisect `main` with `--first-parent`.
- Do not rewrite the history of a branch under review. Keep the TDD commit sequence intact (no interactive rebase or fixup squashing).
- Merge only when the required checks are green, the PR is approved and the definition of done holds.
- After merging, the next story branches from the updated `main`.

## 6. Definition of ready

A story can be started when:
- It has acceptance criteria in `acceptance_criteria.md`.
- Its requirement ids are listed in `backlog.md`.
- Every open question it depends on is resolved, or has a documented fallback (`open_questions.md`).
- For UI stories, the screen is specified in `design_guidelines.md`.

## 7. Definition of done

See `AGENTS.md` §11. While the repository is private, the pasted `run-all.sh` summary is the evidence for the gates. Once CI is active, the CI checks are authoritative.

## 8. Story lifecycle

| Step | Playbook | Result |
| --- | --- | --- |
| Start | `playbooks/start-story.md` | Branch created, story `in-progress`, plan agreed |
| Implement | none | Small commits with tests |
| Verify | `scripts/checks/run-all.sh` | All gates green locally |
| Finish | `playbooks/finish-story.md` | Docs and READMEs updated, PR opened |
| Review and merge | §4, §5 | Merged by the owner with a merge commit |

## 9. Dependencies

- Every dependency is declared in `gradle/libs.versions.toml` or as a Swift package with an exact version, and needs a one-line justification in the PR. A significant one also needs an ADR.
- **Licence allow-list for code shipped in the apps:** Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC, Zlib. Anything else (MPL, LGPL, GPL, AGPL, or a custom licence) needs the owner's approval until the project licence is decided (OQ-21).
- Dependabot opens weekly update PRs (`ci_cd.md` §6). Kotlin upgrades wait for a compatible SKIE release (ADR-0004).
- **Dependabot PRs follow the README rule, with no exception** (owner decision, 2026-10-22). Before merging, add a commit to the Dependabot branch that updates both READMEs ("Recent changes") and the project log. Once someone else has pushed to its branch, Dependabot stops rebasing it, so resolve conflicts by merging `main` into the branch. Never rewrite its history.
- Do not add analytics, advertising, attribution or fingerprinting SDKs without an ADR and the owner's approval (`security_privacy.md`).

## 10. Decisions and open questions

- If a decision is cheap to reverse, take it, record it in the PR description and in the project log.
- If it is hard to reverse or architectural, write an ADR (`playbooks/new-adr.md`) and get the owner's approval.
- If it is a product, legal or scope decision, raise an open question in `open_questions.md` and ask the owner.
