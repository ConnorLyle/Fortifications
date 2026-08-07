# 0. Agent Workflow and Canonical TODO Rules

## Required reading order

1. Read `AGENTS.md` for repository-wide instructions.
2. Read this `HANDOFF.md` for the current implementation, active or blocked work, known risks, verification state, and next steps.
3. Before choosing, discussing, or implementing War Day work, read `WARDAY_TODO.md` in full. It is the canonical backlog; unchecked items are the active queue, while completed struck-through items and their notes are project history and may describe dependencies or prior verification.
4. Inspect the relevant implementation before starting an item and check nearby TODO entries for dependencies, overlap, or behavior that requires separate verification.

## How to use `WARDAY_TODO.md`

- Do not copy or maintain the backlog checklist in this handoff, `AGENTS.md`, or source comments. Task status belongs only in `WARDAY_TODO.md`.
- Refer to a queue item in plans, notes, and handoffs by its section and exact task text so another agent can locate it unambiguously.
- Treat implementation notes beneath an unchecked item as partial-work evidence, not proof that the item is complete.
- Keep an item unchecked while it is only partially implemented, merely compiles, lacks required functional or in-game verification, or has unresolved edge cases.
- If one queue item appears to fix another, verify the second behavior independently before marking it complete.
- Never delete completed items. Preserve them as struck-through project history.

## Required assessment before implementation

When the user asks to bring up, discuss, start, or work on a backlog item, provide a pre-implementation assessment before editing code or changing project state. Include:

- feasible implementation approaches and their tradeoffs;
- expected technical or gameplay challenges and edge cases;
- dependencies or interactions with other backlog items;
- a recommended approach plus useful scope or design suggestions;
- how the change should be verified.

Do not begin implementation until that assessment has been presented. If the user only asks to bring up or discuss an item, stop after the assessment and wait for an explicit request to implement it.

## Completing and documenting a queue item

- Once an item is implemented and verified, change `- [ ] Description` in `WARDAY_TODO.md` to `- [x] ~~Description~~` and add a short indented completion note stating the verification performed.
- Build the changed project after each completed fix: run `gradle build` from `Warday-Mod/Warday` for Warday, or `.\gradlew.bat build` from `Fortifications Mod/Fortifications Mod` for Fortifications.
- After a successful Warday build, replace only `MYTH MODS FOR DEREK/warday-1.0.0.jar` with `Warday-Mod/Warday/build/libs/warday-1.0.0.jar`. After a successful Fortifications build, replace only `MYTH MODS FOR DEREK/fortifications-1.0.0.jar` with `Fortifications Mod/Fortifications Mod/build/libs/fortifications-1.0.0.jar`.
- Verify that each refreshed distributable matches its build artifact by file size or cryptographic hash, and mention the refreshed JAR in the completion report. Do not alter other files in `MYTH MODS FOR DEREK`.
- Prefer focused automated coverage for calculations and state transitions, but never treat compilation alone as proof of Minecraft runtime behavior.
- For gameplay changes, include a concise manual test recipe and result beneath the completed queue item.
- If in-game verification is unavailable, record the build/test evidence and the remaining manual test explicitly, and leave the item unchecked.
- Recheck match end, death/respawn, logout/reconnect, and server-restart behavior whenever work touches persisted event or player state.
- Keep this `HANDOFF.md` updated as substantive work changes implementation state, verification evidence, active or blocked work, risks, dependencies, or next steps. Revise stale statements rather than accumulating contradictory history.

# 1. Current State of the Build

## Mission Accomplished

- `Warday-Mod/Warday` currently compiles with `gradle build` from `Warday-Mod/Warday`.
- `Fortifications Mod/Fortifications Mod` currently compiles with `./gradlew.bat build` from `Fortifications Mod/Fortifications Mod`.
- The Warday mod has a command surface under `/warday` in `WarDayCommands`:
  - `/warday team1 <name>` and `/warday team2 <name>` configure FTB team names.
  - `/warday blocks` and `/warday kit` give setup blocks to an operator player.
  - `/warday validate` scans for one defender-owned nexus, one defender-owned forward marker, and one attacker-owned spawn marker when Team B exists.
  - `/warday scan` reports claimed-cluster and guardrail information without changing the world.
  - `/warday prepare` previews the copy plan.
  - `/warday prepare confirm` copies the defender claimed cluster and optional attacker spawn cluster into the configured War Day dimension.
  - `/warday status` reports prepared/active state and configured dimension status.
  - `/warday start` snapshots online players, teleports team members into the War Day dimension, puts non-participants in spectator, and assigns match blocks.
  - `/warday end` restores online players from saved snapshots.
- Warday setup blocks are registered:
  - `warday:nexus`
  - `warday:forward_marker`
  - `warday:attacker_spawn`
- `ForwardMarkerBlock` persists horizontal facing, which is used for reporting the base orientation plan.
- Defender base rotation has been started in `Warday-Mod/Warday/src/main/java/com/trove/warday/WarDayCommands.java`:
  - `PlacementPlan` now rotates defender copied positions around the nexus so the forward marker faces east, toward the current attacker-side arena placement at positive X.
  - Rotated target positions are used by destination conflict checks, full-height destination wiping, block copying, copied nexus state, decorative entity copying, and safe spawn searches.
  - Copied block states are rotated with `BlockState.rotate(...)`.
  - Copied decorative entity positions and yaw are rotated, and painting/item-frame tile coordinates are translated through the same rotated block-position path.
  - `/warday prepare` now reports the rotation plan, and `/warday prepare confirm` no longer says copied bases are unrotated.
- `WarDayState` persists prepared state, active state, copied nexus position, attacker spawn position, configured dimension, team labels, and player snapshots in Minecraft `SavedData`.
- Nexus destruction in the active War Day dimension starts the persisted victory-fanfare phase; restoration follows when the configured fanfare finishes, while `/warday end` remains an immediate operator override.
- Respawn handling exists for active participants: players briefly enter spectator mode, are teleported above their spawn, then restored after `respawnDelaySeconds`.
- Active-match lifecycle edge cases have been implemented, including login handling during an active match, deferred restoration after the match, persisted pending respawn/death state, and active-role reassignment.
- A server-owned boss-bar timer displays the authoritative combat countdown to all online match players, reconstructs from persisted state after restart, synchronizes login/logout, and disappears before the victory fanfare or any match cleanup path.
- A server-owned `warday_roster` sidebar lists both persisted participant teams, paginates large rosters every five seconds, reconstructs after restart, and restores the pre-match sidebar objective during cleanup.
- `/warday validate` and `/warday scan` now narrow marker scanning to loaded claimed chunks owned by the configured teams within the validation radius instead of walking the entire full-height radius volume.
- `/warday prepare confirm` now clears only positions mapped from the actual source claim cluster before paste. It no longer wipes unclaimed holes inside the cluster's rectangular bounding footprint.
- The Fortifications mod registers a synced `fortifications:unarmed_damage` player attribute and has a global `FortificationsMod.GLOBAL_ACTIVE` flag.
- `FortificationBlockRules` excludes Warday setup blocks from Fortifications hardening so admin/event marker blocks are not accidentally fortified.
- Fortifications' nine Relics/reliquified item mixins now target the Relics 0.12 `AbilityStatTemplate.StatTemplateBuilder.targetValue(...)` API. Their frozen and additive balance values were converted from the removed 0.11 `upgradeModifier(...)` API so they retain the intended per-level behavior.
- Seven Relics/reliquified event-mixin classes containing nine redirects now target `AbilityRankModifierData.isEnabled()` instead of the removed Relics 0.11 `AbilityData.isRankModifierUnlocked(String)` method.
- The Fortifications compile-only dependencies now match the current pack versions of Relics 0.12.8, Iron's Spells 3.16.2, and Tunes & Tomes 1.1.0-HOTFIX.
- Recipe/loot datapack overrides exist under the Fortifications mod resources for several third-party mods, including `artifacts`, `relics`, `alexscaves`, and `sophisticatedbackpacks`.
- The refreshed Warday and Fortifications distributables are present at `MYTH MODS FOR DEREK/warday-1.0.0.jar` and `MYTH MODS FOR DEREK/fortifications-1.0.0.jar`.

## Active/Halted Work

- Fortifications Relics 0.12 compatibility was implemented after the 2026-08-06 client crashes showed required mixin injections scanning zero targets. The first launch exposed the removed `upgradeModifier(...)` target in item mixins; the next launch exposed the removed `isRankModifierUnlocked(...)` target in Sealed Sword and Shock Pendant event mixins. Source audit migrated every occurrence of both removed APIs rather than stopping at the reported failures.
- `./gradlew.bat build --no-daemon --no-problems-report` succeeded on 2026-08-06 after transient OneDrive access-denial retries in NeoForge's generated `build/tmp` output. Static `javap` inspection confirmed every current Relics 0.12.8, Reliquified Artifacts 1.0.7, and Reliquified Iron's Spells 0.2.7 target method contains enough `initialValue(...)`, `targetValue(...)`, and `AbilityRankModifierData.isEnabled()` calls for all referenced mixin ordinals.
- The build artifact and refreshed `MYTH MODS FOR DEREK/fortifications-1.0.0.jar` are both 78,129 bytes with SHA-256 `60556314F177BDA3E892989A88C2377196467422BBCB86E61229C68DBC63BD5A`.
- The same verified jar was deployed to the affected CurseForge instance at `fortif update 8 we're so back/mods/fortifications-1.0.0.jar`; its post-copy size and SHA-256 match the build artifact.
- Both custom projects were rebuilt successfully and their distributables refreshed together on 2026-08-06. Warday is 116,294 bytes with SHA-256 `C5AEEBC0A78E339416520390263C884BE80AE9BE3EADA53751B94392DC59CD9A`; Fortifications retains the size and hash recorded above.
- Minecraft runtime verification is still required in the exact CurseForge instance. Launch through mod construction, enter a world, and check affected Relics/reliquified item stats; a later incompatible mixin could surface only after the previously fatal injections are fixed.

- Current implementation item: `WARDAY_TODO.md` section **Match presentation and HUD**, exact task **“Add a sidebar scoreboard that lists Team 1 and Team 2 and their participating players.”**
- The sidebar uses custom score display components and blank number formatting rather than altering real player scoreboard teams. It renders both team headers plus up to six alphabetically sorted names per team and rotates five-second pages when needed.
- Persisted `previousSidebarObjective` state allows cleanup to restore the objective that owned the sidebar before War Day. If an external system replaces the sidebar mid-match, Warday records that newer objective before reclaiming the slot and restores it during cleanup if Warday still owns the slot.
- Pagination calculations passed for every 0-30 player combination on both teams with full coverage and no page above 14 lines. `gradle build --no-daemon --no-problems-report` succeeded on 2026-08-06. The build artifact and refreshed `MYTH MODS FOR DEREK/warday-1.0.0.jar` are both 116,294 bytes with SHA-256 `C5AEEBC0A78E339416520390263C884BE80AE9BE3EADA53751B94392DC59CD9A`.
- The sidebar item remains unchecked pending visual and lifecycle verification in Minecraft.
- Previous implementation item: `WARDAY_TODO.md` section **Match presentation and HUD**, exact task **“Add a boss-bar match timer at the top of the screen and keep it synchronized with the authoritative match clock.”**
- The boss bar is implemented with `ServerBossEvent`. Its name and progress are derived once per second from the persisted `matchEndGameTime`; `matchDurationTicks` is now persisted for restart-stable progress normalization, with a configuration fallback for legacy saves.
- Boss-bar player membership is synchronized at match start, login, logout, and periodic updates. The bar is removed at fanfare transition, `/warday end`, inactive-state recovery, missing-level recovery, and cross-server static cleanup.
- Focused timer-boundary calculations passed. Its earlier build artifact at SHA-256 `5FFBF88DC42834CF97CCB94D8F33962FF26EA383655A06D49272A27992399488` has been superseded by the sidebar build recorded above.
- The boss-bar item remains unchecked pending visual and lifecycle verification in Minecraft.
- First still-unverified canonical item: `WARDAY_TODO.md` section **Player and entity state**, exact task **“Make non-player entities carry over into the War Day dimension and define which entity categories must be copied or transferred.”**
- Source review on 2026-08-06 found that continuous entity coordinates were rotated around the anchor block corner, which could shift entities into an adjacent transformed block. `PlacementPlan.targetX/targetZ` now rotate around the anchor block center.
- Focused calculation verification checked 900 points across all four rotations and confirmed that every entity point remains inside the block produced by the integer block transform.
- The entity-position correction previously built successfully at SHA-256 `05277987197345962CA438506ED73E64AD9EEAD4E34CDE7DCBD14D75B4E183EA`; that artifact has now been superseded by the boss-bar build recorded above.
- A preceding `gradle clean build --no-daemon` attempt compiled the source but encountered a transient Windows access denial in NeoForge's generated `build/tmp` output; a normal retry compiled and produced the JAR, then hit a OneDrive collision replacing Gradle's generated problems report. Disabling that optional report produced the clean successful build above.
- The entity task and nested tamed-entity task remain unchecked because no Minecraft runtime test was performed. NBT fidelity, AI/tame behavior, leash recreation, passenger trees, entity caps, cleanup, and restart behavior remain manual-verification requirements.
- The rotation implementation assumes the defender target facing is east because the defender anchor is `[0, warDayBaseY, 0]` and the attacker anchor is `[baseSpacingBlocks, warDayBaseY, 0]`.
- The most likely half-finished area is Warday gameplay completion, not compilation:
  - Defender base rotation compiles and has calculation-level coverage, but still needs in-game validation.
  - Attacker spawn areas remain unrotated because they do not have a forward marker/orientation marker.
  - Persistent non-player entities use the prepared-template path; decorative `Painting` and `ItemFrame` entities use a separate copy path.
  - Item frames are copied but their items are cleared.
  - Containers are copied structurally but their contents are cleared.
  - Prepared/copied War Day claim shapes are cleared over the source dimension's mapped vertical range before paste.
  - Team B can be absent for validation/prepare one-team testing, but `/warday start` requires both configured teams.
- The worktree contains pre-existing uncommitted Warday implementation and documentation changes. Preserve them and inspect `git diff` before further edits.

## Next Immediate Steps

1. Launch the updated CurseForge instance through mod construction. If it still fails, inspect the newly generated `latest.log`/crash report because the next failure may have been masked by the three fatal Relics mixin errors.
2. In a world, verify Kinetic/Hunting Belt slots and Cloud in a Bottle jumps remain fixed at 2, sealed-weapon respawn time remains 120 seconds, and Night Vision Goggles, Power Glove, and Vampiric Glove additive stats still progress at their intended rates.
3. Manually verify the sidebar layout, team membership, page rotation, offline-name fallback, restart reconstruction, cleanup, and restoration of a pre-existing sidebar objective.
4. Manually verify the boss bar during start/countdown, death/respawn, logout/reconnect, server restart, defender timeout, nexus victory, `/warday end`, fanfare transition, and a second match. Confirm there are no stale or duplicate HUD elements.
5. Run the entity carryover manual matrix from the first `WARDAY_TODO.md` item, including tamed wolves, passenger trees, leashes, all rotations, an unclaimed-hole entity, the configured cap, repeated matches, and restart cleanup.
6. Record successful runtime results beneath each applicable queue item before striking it through; keep any item with a failure or untested edge case unchecked.
7. If the sidebar is readable in-game, implement the separate live-health queue item by extending the existing roster-line renderer, then continue the remaining manual and automated lifecycle coverage.

# 2. Architectural Decisions & Patterns

## Structural Choices

- Warday currently keeps most command and event behavior in `WarDayCommands`. This is not elegant, but it keeps the event lifecycle in one place while the feature is still changing quickly. The class owns command registration, validation, preparation, start/end, respawn, and nexus-break completion.
- Data that must survive server restart is separated into `WarDayState`, backed by Minecraft `SavedData`. Runtime-only delay bookkeeping remains in the static `PENDING_RESPAWNS` map because it is short-lived and only meaningful while the server process is active.
- Setup blocks are deliberately simple block classes. `NexusBlock` is just a marker block. `ForwardMarkerBlock` extends `HorizontalDirectionalBlock` only because the facing is needed to express intended base orientation.
- Config is centralized in `WarDayConfig` using NeoForge `ModConfigSpec`, so team names, scan radius, base limits, target dimension, target Y, spacing, and respawn delay can be changed without recompiling.
- `PlacementPlan` is a record local to `WarDayCommands` because the copy algorithm currently needs only a compact value object: source anchor, target anchor, cluster bounds, rotation, and coordinate translation helpers.
- Fortifications uses `FortificationBlockRules` as a small policy class instead of baking block checks directly into event/mixin code. This makes the Warday exemption easy to see and test later.

## Cross-Boundary Communication

- Commands enter through NeoForge's `RegisterCommandsEvent` and Brigadier command registration in `WarDayCommands.registerCommands`.
- Warday communicates with FTB Teams through `FTBTeamsAPI.api().getManager()` and resolves teams by configured name, short name, or display name.
- Warday communicates with FTB Chunks through `FTBChunksAPI.api().getManager()`, `ClaimedChunkManager`, `ClaimedChunk`, and `ChunkDimPos`.
- Team ownership is inferred from the FTB Chunks claim owner for the chunk containing each setup block.
- Persistent server state is serialized to NBT in `WarDayState.save` and deserialized in `WarDayState.load`.
- Block entities are copied by saving source block entity NBT with `saveWithFullMetadata`, rewriting `x/y/z`, then loading that data into the target block entity with `loadWithComponents`. The target block state is now rotated before placement for defender bases.
- Decorative entities are copied by serializing entity NBT, removing `UUID`, translating `Pos`, rotating yaw, translating hanging entity tile coordinates, then recreating the entity with `EntityType.create`.
- Player snapshots serialize game mode, dimension id, coordinates, and rotation. Restoration parses the saved dimension id back into a `ResourceKey<Level>`.

## Complex Logic

- Claim cluster resolution is a breadth-first search over 4-neighbor chunk adjacency. `connectedClaimCluster` starts at the nexus chunk and walks only chunks owned by the same team.
- Defender validation requires exactly one owned nexus, exactly one owned forward marker, and the marker chunk must be inside the connected claim cluster rooted at the nexus.
- Attacker validation requires exactly one owned attacker spawn marker.
- Guardrails check connected cluster size and footprint width/depth against config values before preparation.
- Copy preparation computes a source cluster bounding box, an anchor offset from source min, rotation from defender forward marker facing to east, and target coordinates. It preserves vertical coordinates relative to the source anchor and target Y.
- Marker validation iterates chunk coordinates in the configured radius, rejects unloaded/unclaimed chunks and claims owned by unrelated teams, then scans only relevant claimed chunks for setup blocks.
- Destination checking scans all non-air source blocks and detects whether rotated target positions are already occupied.
- Destination clearing walks source claim chunks and transforms each source position through `PlacementPlan`, preserving irregular cluster shapes and rotation while clearing only mapped target positions within target build bounds.
- Safe attacker spawn selection first tries one block above the target anchor, then searches nearby positions for solid ground and two collision-free/fluid-free player spaces.
- Respawn delay uses a per-tick countdown map keyed by player UUID and restores survival/game spawn when the delay expires.

# 3. Data & Infrastructure

## Schema Changes

- No SQL/database schema exists in this repository.
- Warday persistent data schema is the NBT structure in `WarDayState`:
  - `Prepared` boolean
  - `WarDayDimension` string
  - `DefenderTeam` string
  - `AttackerTeam` string
  - `CopiedNexusPos` long, optional
  - `AttackerSpawnPos` long, optional
  - `Active` boolean
  - `MatchEndGameTime` and `MatchDurationTicks` longs
  - `PreviousSidebarObjective` string
  - `SavedPlayers` list of player snapshot compounds
- `WarDayState.load` also reads legacy `SavedGameModes` entries and converts them into partial `PlayerSnapshot` entries. This looks like backward compatibility for an older saved state shape.
- Fortifications adds the synced attribute `fortifications:unarmed_damage` to players through `EntityAttributeModificationEvent`.
- Warday adds a bundled dimension data file at `Warday-Mod/Warday/src/main/resources/data/warday/dimension/war_day.json`. The configured default dimension id is `warday:war_day`.

## Environment Variables

- No environment variables, API keys, or external connection strings were found or introduced by this handoff.
- Local build assumptions:
  - `Warday-Mod/Warday` expects a system `gradle` command; there is no `gradlew.bat` in that subproject.
  - `Fortifications Mod/Fortifications Mod` has a Gradle wrapper and builds with `./gradlew.bat build`.
  - A manual Minecraft/NeoForge runtime is still required to validate gameplay behavior.

# 4. Known Tech Debt, Hacks, & AI Shortcuts

- `WarDayCommands` is very large and mixes command registration, validation, copy/paste, event handlers, persistence coordination, and reporting. It should eventually be split into services such as validation, placement/copy, lifecycle, and respawn handling.
- Defender base rotation has source-level implementation, but it is uncompiled and untested in-game. Treat it as pending verification, especially for modded blocks, block entities with directional NBT, paintings, and item frames.
- Attacker spawn-area rotation is not implemented. There is currently no attacker orientation marker, so attacker areas are copied without rotation.
- Several gameplay values are effectively hardcoded:
  - Match block target count is `32`.
  - Defender target anchor is `[0, warDayBaseY, 0]`.
  - Attacker target anchor is `[baseSpacingBlocks, warDayBaseY, 0]`.
  - Spectators/respawning players are placed 11 blocks above spawn.
  - Safe spawn search radius is 8 blocks horizontally and 4 blocks vertically.
- `/warday validate` and `/warday scan` remain radius-limited and inspect only loaded claims belonging to configured teams. Markers outside the radius, in unloaded chunks, or in unclaimed chunks are not reported.
- Destination clearing remains destructive inside transformed source claim chunks over the source dimension's mapped vertical range. It is intentional for repeatable prepare runs, so the configured War Day target dimension should remain dedicated to generated match areas.
- Container contents are intentionally cleared after copy. That avoids giving duplicated loot/resources, but it may surprise players if their defensive build depends on filled containers.
- Item frames are copied but cleared. This avoids duplicating items, but visual signage/maps/items in frames will not survive the copy.
- Only `Painting` and `ItemFrame` decorative entities are copied. Armor stands, display entities, mobs, boats, minecarts, and modded decorative entities are ignored.
- Active-match login, reconnect, deferred restoration, and pending-respawn state handling have been implemented. These paths still need full multiplayer/server-restart play-testing.
- There is no explicit security model beyond command permission level 2. Operators can wipe/paste target areas with `/warday prepare confirm`.
- There are no automated tests for the custom Warday validation/copy/lifecycle logic.
- Fortifications' Relics integration is implemented with required ordinal-based mixins into third-party bytecode. The current Relics/add-on versions were checked statically, but future upstream reordering can break those injections and should be re-audited whenever the pack updates those mods.
- I did not identify any deliberately bypassed authentication, mocked auth, ignored validation, missing database indexes, or database memory leaks. The main risks are gameplay state correctness and expensive world scans.
