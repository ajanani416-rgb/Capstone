# DEFINITION_OF_DONE.md

A task is DONE only when ALL hold:
1. Requirement understood (traced to PRODUCT.md/FEATURES.md or DECISION logged).
2. Implementation complete (smallest correct change; no scope creep per master prompt §50).
3. Code reviewed (self-review of `git diff`; placeholders removed).
4. Tests pass (commands actually run; output pasted in report — never claimed).
5. Builds pass (`mvn package` / `npm run build` + `lint` where applicable).
6. No obvious regression (affected journeys re-checked).
7. Docs updated (PRODUCT/FEATURES/DATA_API/SCREEN/TASKS as touched).
8. Commit created (Conventional Commits, lowercase, no secrets).
Skipping any step → PARTIAL, not DONE.
