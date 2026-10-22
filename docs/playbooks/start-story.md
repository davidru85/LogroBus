# Playbook: start a story

**Input:** a backlog story id, `LB-xxx`.
**Used by:** humans, every AI agent, and the Claude Code skill `/start-story`.

1. **Sync `main`:** run `git fetch origin && git switch main && git pull --ff-only`.
2. **Read the story:**
   - Its row in [`backlog.md`](../backlog.md).
   - Its criteria in [`acceptance_criteria.md`](../acceptance_criteria.md).
   - Every requirement id it cites, in [`requirements.md`](../requirements.md) and the functional spec.
   - Any related entry in [`open_questions.md`](../open_questions.md).
3. **Check the definition of ready** ([`engineering_workflow.md`](../engineering_workflow.md) §6). If it is not met, stop and tell the owner exactly what is missing.
4. **Create the branch:** `git switch -c <type>/LB-xxx-<short-slug>`, where `<type>` is `feat`, `fix`, `docs` or `chore`.
5. **Mark it in progress:** set the story status to `in-progress` in `backlog.md`, and commit with `docs(backlog): start LB-xxx [LB-xxx]`.
6. **Plan:** write a short plan for the owner, in Peninsular Spanish, covering:
   - The modules and files to touch.
   - The tests to add, per [`testing_strategy.md`](../testing_strategy.md) §3.
   - The docs to update, and any risk or open question.
   Wait for the owner's go-ahead before large or irreversible changes.
7. **Implement with the TDD commit protocol** ([`testing_strategy.md`](../testing_strategy.md) §7.1). Repeat this cycle for each behaviour of each acceptance criterion:
   1. **Red:** write the test (`lbxxx_…`), run it and see it fail for the expected reason. Commit `test(scope): … [LB-xxx]` with the body `TDD: red`.
   2. **Green:** write the minimum code to pass, then run the tests. Commit `feat|fix(scope): … [LB-xxx]` with the body `TDD: green`.
   3. **Refactor:** improve the code with the tests green, then run `scripts/checks/run-all.sh`. Commit `refactor(scope): … [LB-xxx]` with the body `TDD: refactor`. If nothing needs refactoring, skip this commit and say so in the green commit body.
   4. **Push:** `git push`.
   Report each red run to the owner as evidence.
8. When the work is complete, continue with [`finish-story.md`](finish-story.md).
