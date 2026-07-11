# Team Review

Use this prompt for a team-oriented code review before merging or opening a pull request.

## Scope

- Review uncommitted changes unless the user specifies a branch, commit, or file range.
- If a base branch is needed and the user does not specify one, compare against `develop`.
- Do not edit code during this review unless the user explicitly asks for fixes.

## Review Method

- Use a code-review stance: findings first, ordered by severity.
- Prioritize correctness bugs, regressions, security risks, data-loss risks, missing validation, missing tests, and behavior changes.
- Check whether changes follow `AGENTS.md`, including commit convention, Google style, and no hardcoded environment variables or secrets.
- For non-trivial reviews, delegate focused review tasks to an available Codex subagent when subagents are available.
- Do not delegate trivial single-file reviews unless delegation would clearly improve quality.
- The main agent must verify and synthesize subagent findings before presenting them.

## Output Format

- Start with findings. If there are no findings, say that clearly.
- Include file and line references for each finding when possible.
- Include severity labels such as `[P1]`, `[P2]`, or `[P3]`.
- After findings, list open questions or assumptions.
- Keep the summary brief and secondary to the findings.
- Mention any tests or checks that were run, and any that could not be run.
