---
name: finish-story
description: Finish a LogroBus story - verify acceptance criteria, run reviewer subagents and all quality gates, update backlog, project log and both READMEs, push the branch and open the pull request. Use when the owner says a story is ready, asks to open the PR, or asks to wrap up the current story.
---

# Finish a story

Follow [`docs/playbooks/finish-story.md`](../../../docs/playbooks/finish-story.md) step by step.

- For step 2, run the reviewer subagents that match the diff: `code-reviewer` always; `data-rules-reviewer` for `shared/` changes; `android-ui-reviewer` for `android/`; `ios-ui-reviewer` for `iosApp/`.
- The story id comes from the invocation arguments ($ARGUMENTS) or from the current branch name.
- Never merge the pull request unless the owner explicitly asks.
- Talk to the owner in Peninsular Spanish. Write every repository artefact in technical English.
