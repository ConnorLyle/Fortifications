# Agent Instructions

## Repository scope

- The active combined Fortifications and War Day project is `Fortifications Mod/Fortifications Mod`.
- `Warday-Mod/Warday` is retained only as pre-merge rollback/reference source. Do not implement new work there unless the user explicitly asks to restore the standalone mod.
- Read `HANDOFF.md` before changing the combined project; it describes the current implementation, known risks, and build commands.
- Keep `HANDOFF.md` current whenever substantive work changes implementation state, verification evidence, known risks, active/blocked work, or the recommended next steps. Remove or revise stale statements instead of only appending new notes.
- Preserve unrelated user changes. Do not reset, discard, or overwrite work outside the task being handled.

## Handoff maintenance

- Update `HANDOFF.md` before reporting substantive project work complete or handing the repository to another agent.
- Record what changed, what was actually verified, what still needs manual or automated verification, and any new risks or dependencies.
- When work relates to the War Day backlog, identify the item by its `WARDAY_TODO.md` section and exact task text so the handoff can be reconciled with the canonical queue.
- Keep `HANDOFF.md` concise and current-state focused. Do not copy the backlog checklist into it; link to `WARDAY_TODO.md` and document only the state needed for the next agent to continue safely.

## Work queue protocol

- The canonical backlog is [`WARDAY_TODO.md`](WARDAY_TODO.md). Read it before choosing or implementing War Day work.
- Treat unchecked items in `WARDAY_TODO.md` as the active backlog. Do not maintain a second copy of the backlog checklist in this file, `HANDOFF.md`, or source comments. Workflow guidance may be repeated in `HANDOFF.md`, but task status lives only in `WARDAY_TODO.md`.
- Reference a queue item in agent notes, plans, and handoffs using its section and exact task text so another agent can locate it unambiguously.
- When the user asks to bring up, discuss, start, or work on a backlog item, first give the user a pre-implementation assessment before editing code or changing project state. The assessment must include:
  - the feasible implementation approaches and their tradeoffs;
  - expected technical or gameplay challenges and edge cases;
  - dependencies or interactions with other backlog items;
  - a recommended approach and any useful scope or design suggestions;
  - how the change should be verified.
- Do not begin implementation until that assessment has been presented. If the user's request is only to bring up or discuss the task, stop after the assessment and wait for an explicit request to implement it.
- Before starting an item, inspect the relevant implementation and note dependencies on other queue items.
- Keep an item unchecked while it is only partially implemented, compiles without functional verification, or still has unresolved edge cases.
- Once an item is implemented and verified, edit `WARDAY_TODO.md`: change `- [ ] Description` to `- [x] ~~Description~~` and add a short indented completion note with the verification performed.
- If an apparent fix is supplied by another queue item, verify the behavior separately before striking it off.
- Do not delete completed items. The struck-through entries are the project history.
- When implementation cannot be verified in-game, record the build/test evidence and remaining manual test explicitly; do not strike off the item yet.

## Verification expectations

- Build the combined project after each completed fix with `.\gradlew.bat build` from `Fortifications Mod/Fortifications Mod`.
- After every successful build, copy `Fortifications Mod/Fortifications Mod/build/libs/fortifications-1.0.0.jar` to `MYTH MODS FOR DEREK/fortifications-1.0.0.jar` before reporting the build complete.
- The distributable bundle must contain only the combined `fortifications-1.0.0.jar`; do not recreate `warday-1.0.0.jar`. Do not alter other files in `MYTH MODS FOR DEREK`. Verify the source and copied jar have the same size or cryptographic hash, and mention the refreshed distributable in the completion report.
- Prefer focused automated coverage for state transitions and calculations, but do not treat compilation alone as proof of Minecraft runtime behavior.
- For gameplay changes, record a concise manual test recipe and result beneath the completed queue item.
- Recheck match end, death/respawn, logout/reconnect, and server-restart behavior whenever a change touches persisted event or player state.
