# Worklog

Use this prompt to write a concise team worklog for the current project state.

## Goal

- Preserve enough context for another teammate or Codex session to continue smoothly.
- Keep the worklog factual and useful for handoff.
- Do not turn the worklog into a long diary.

## Location

- Write the worklog under `context/worklog/`.
- Use `YYYY-MM-DD.md` as the default filename.
- If a worklog for the same date already exists, append a new section instead of rewriting unrelated entries.

## Format

```md
# 작업 일지 YYYY-MM-DD

## 완료한 작업

- 완료한 작업.

## 결정 사항

- 결정:
  - 고려한 대안:
    - 대안 A:
    - 대안 B:
  - 선택한 안:
  - 선택 이유:
  - 트레이드오프:

## 변경한 파일

- `path/to/file`: 간단한 설명.

## 검증

- 실행한 확인:
- 결과:
- 실행하지 못한 확인:

## 다음 작업

- 바로 이어서 할 일.

## 리스크 / 메모

- 남은 우려나 이어받을 때 필요한 맥락.
```

## Writing Rules

- Write in Korean unless the user asks otherwise.
- In `Decisions`, include the alternatives considered and the concrete reason for the chosen option.
- In `Verification`, distinguish checks that actually ran from checks that were skipped or could not run.
- Do not include secrets, API keys, tokens, credentials, customer names, or sensitive internal details.
- If there were no meaningful decisions, write `- 주요 결정 없음.` under `결정 사항`.
