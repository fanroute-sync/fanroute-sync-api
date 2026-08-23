# AGENTS.md

This project is for a competition. We use Codex as the main coding assistant, so changes should be small, clear, and easy for teammates to review.

## Project Documentation

The following documents are versioned project sources of truth and must be read before changing related functionality.

- `docs/project/wireframe.md` — screen flows and interaction policies
- `docs/project/planning.md` — MVP scope, domain responsibilities, and product decisions
- `docs/project/coding-rules.md` — project coding conventions and documentation rules

- Keep these documents in sync when a feature, policy, or implementation decision changes.
- If the ERD, wireframe, planning document, API contract, or code conflicts, record the
  discrepancy in the issue or PR and obtain confirmation from the relevant owner before implementation.

## Core Rules

### Think Before Coding

- Do not assume unclear requirements.
- If there are multiple reasonable interpretations, mention them before choosing.
- For non-trivial changes, briefly state the plan and success criteria.
- Ask when the requirement is ambiguous enough that implementation may go in the wrong direction.

### Simplicity First

- Implement the minimum code needed to solve the requested problem.
- Do not add speculative features, abstractions, configs, or future-proofing.
- Prefer readable, direct code over clever code.
- If a solution feels too large for the task, simplify it.

### Surgical Changes

- Touch only files directly related to the request.
- Match the existing style and structure of the project.
- Do not refactor unrelated code.
- Remove only unused code/imports created by your own changes.
- If you notice unrelated problems, mention them instead of fixing them silently.

### Goal-Driven Execution

- Turn tasks into verifiable outcomes.
- For bug fixes, reproduce the bug or identify a concrete failing case when possible.
- For new behavior, add or update focused tests when practical.
- Run the relevant checks before finishing and report what passed or could not be run.

## Competition Bias

- Prioritize working, demo-ready functionality.
- Avoid large rewrites close to deadlines.
- Prefer stable, understandable implementation over perfect architecture.

## Commit Message Convention

### 1. Commit Type

- Write the commit type in English according to the team's convention.

| Type | Meaning |
| --- | --- |
| `Feat` | Add a new feature |
| `Fix` | Fix a bug |
| `Docs` | Documentation changes |
| `Style` | Code formatting, missing semicolons, or changes that do not affect code behavior |
| `Refactor` | Code refactoring |
| `Test` | Add or refactor tests |
| `Chore` | Package manager changes or other miscellaneous changes such as `.gitignore` |
| `Design` | User UI design changes such as CSS |
| `Comment` | Add or update necessary comments |
| `Rename` | Rename or move files or folders only |
| `Remove` | Delete files only |
| `!BREAKING CHANGE` | Large API-breaking changes |
| `!HOTFIX` | Urgent fix for a critical bug |

### 2. Separate Title And Body With A Blank Line

- After the commit type, write the title and body in Korean so the content is easy for the team to understand.
- In the body, explain what changed and why. Focus on what and why rather than how.

### 3. Title Format

- Capitalize the first character of the title.
- Do not end the title with a period.

### 4. Title Length

- Keep the title within 50 English characters when possible.

### 5. Write For Reviewers

- Do not assume that your code is immediately obvious to others.

### 6. Use Bullets For Multiple Items

- If there are multiple changes, use bullet points to improve readability.

### 7. One Concern Per Commit

- Keep each commit focused on one problem or purpose.
- Do not mix unrelated changes in a single commit.
- If a commit contains too many concerns, split it so the history stays easy to track and review.

## Code Style

- Follow Google style when writing code.
- Match existing project style when it is more specific than the general style guide.

## Configuration And Secrets

- Do not hardcode environment variables.
- Do not hardcode secrets, API keys, tokens, credentials, or environment-specific values.
- Read configuration from environment variables or project configuration files.
- Update example environment files when adding required environment variables.

## OpenAPI Documentation

- Keep the Swagger/OpenAPI specification in sync whenever creating or changing a controller,
  whether written by AI or manually by a teammate.
- Add `@Tag` to each controller and `@Operation` to each endpoint with concise Korean summaries.
- Document successful responses with `@ApiResponse` and expected failures with
  `@ApiErrorCodeExamples` so the shared customizer generates response schemas and examples.
- Do not duplicate error response bodies manually when an existing `BaseCode` enum represents
  the failure.
- Add `@SecurityRequirement(name = "bearerAuth")` only to JWT-protected endpoints.
- Annotate non-obvious DTO fields with `@Schema`.
- During `/prompts:team-review`, check that controller diffs have matching Swagger annotations
  before approving.

## Team Review Prompt

- Use `/prompts:team-review` for team-oriented code reviews.
- Treat `team-review` as the project-specific review workflow, separate from the built-in `/review` command.
- During team reviews, prioritize bugs, regressions, security risks, missing tests, and violations of this file.
- For non-trivial reviews, delegate focused review tasks to an available Codex subagent when subagents are available.
- The main agent must verify and synthesize subagent findings before presenting them.
- Do not edit code during review unless the user explicitly asks for fixes.

## Decision Confirmation

- Record cross-domain decisions, policy changes, and unresolved conflicts in the related issue or PR.
- State the alternatives, selected proposal, and impact on existing contracts.
- Obtain confirmation from the relevant domain owner or team before implementing a decision that changes a shared contract.
