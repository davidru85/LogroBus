# Playbook: finish a story and open the pull request

**Input:** the story id `LB-xxx`, on its branch.
**Used by:** humans, every AI agent, and the Claude Code skill `/finish-story`.

1. **Verify the acceptance criteria.** For each criterion in [`acceptance_criteria.md`](../acceptance_criteria.md), note the evidence: a test name, a screenshot or a manual check. A criterion without evidence is not met.
2. **Review.** Self-review the diff against [`review-checklists.md`](review-checklists.md). With Claude Code, run the matching reviewer subagents.
3. **Update the docs in the same branch:**
   - `backlog.md`: set the story status to `done`.
   - `project_log.md`: a dated entry with Done, Decisions and Open / next.
   - **`README.md` and `README.es.md` (mandatory):**
     - Add a "Recent changes" / "Cambios recientes" entry.
     - Refresh "Project status" / "Estado del proyecto".
     - Update any other section the change affects.
     - The two files must stay equivalent.
   - Any other doc or ADR the change affects. If the source contract changed, add a row to the change log in `api_spec.md`.
4. **Run every gate:** `scripts/checks/run-all.sh`. Fix all failures and report the real results.
5. **Commit** with a Conventional Commit subject that ends in the story id: `feat(near-me): stop sheet arrivals [LB-205]`.
6. **Push the branch:** `git push -u origin HEAD`. Never push to `main`.
7. **Open the pull request** against `main`:
   - The title is the Conventional Commit subject.
   - The body follows `.github/pull_request_template.md` (or the `docs-chore.md` template for changes that do not alter app behaviour).
   - List the criteria and tick them, and add screenshots for UI changes.
   - **Paste the real `run-all.sh` summary.** It is the evidence for every check that CI does not run yet (`ci_cd.md` §1).

   ```bash
   gh pr create --base main --title "<type(scope): summary [LB-xxx]>" --body-file <filled-template.md>
   ```

   Use `--draft` if anything is still pending.
8. **Hand over.** Tell the owner, in Peninsular Spanish:
   - the PR link, what to look at, and any follow-up;
   - the task you will start once the PR is merged.

   **Do not merge** unless the owner explicitly asks. When asked, and only once the checks are green, run `gh pr merge --merge --delete-branch`.
9. **Watch the pull request until it is merged** (owner rule, 2026-10-25). The owner approves and merges without saying so in the conversation.
   - Poll its state in the background, a few minutes apart, not in a tight loop:

     ```bash
     gh pr view <number> --json state,reviewDecision
     ```

   - **`MERGED`:** run `git checkout main && git pull --ff-only`, tell the owner in one line that the merge was detected, and start the task named in the hand-over. If none was named, start the next open item of the story in `backlog.md`, through `start-story.md` when it is a new story.
   - **`CLOSED` without a merge, `CHANGES_REQUESTED`, or new review comments:** stop and report to the owner; do not move on.
   - If the session ends while the PR is open, the next session resumes the watch.
