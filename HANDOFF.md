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
- Nexus destruction in the active War Day dimension ends the event and restores online players.
- Respawn handling exists for active participants: players briefly enter spectator mode, are teleported above their spawn, then restored after `respawnDelaySeconds`.
- Active-match lifecycle edge cases have been implemented, including login handling during an active match, deferred restoration after the match, persisted pending respawn/death state, and active-role reassignment.
- `/warday validate` and `/warday scan` now narrow marker scanning to loaded claimed chunks owned by the configured teams within the validation radius instead of walking the entire full-height radius volume.
- `/warday prepare confirm` now clears only positions mapped from the actual source claim cluster before paste. It no longer wipes unclaimed holes inside the cluster's rectangular bounding footprint.
- The Fortifications mod registers a synced `fortifications:unarmed_damage` player attribute and has a global `FortificationsMod.GLOBAL_ACTIVE` flag.
- `FortificationBlockRules` excludes Warday setup blocks from Fortifications hardening so admin/event marker blocks are not accidentally fortified.
- Recipe/loot datapack overrides exist under the Fortifications mod resources for several third-party mods, including `artifacts`, `relics`, `alexscaves`, and `sophisticatedbackpacks`.
- Built jars are present in `MYTH MODS FOR DEREK`, including `warday-1.0.0.jar`, `fortifications-1.0.0.jar`, and `voxy-0.2.9-alpha.jar`.

## Active/Halted Work

- Recent active work is in `Warday-Mod/Warday/src/main/java/com/trove/warday/WarDayCommands.java`.
- Defender base rotation has been implemented at source level but has not been compiled or play-tested yet in this Codex shell.
- Claim-scoped marker scanning and transformed claim-shape destination clearing have been implemented at source level but remain uncompiled in this shell.
- Verification was blocked because:
  - `gradle build` from `Warday-Mod/Warday` failed because `gradle` is not on PATH.
  - Reusing the Fortifications Gradle wrapper with `-p Warday-Mod/Warday build` failed because `JAVA_HOME` is not set and `java` is not on PATH.
  - A recursive local search from usual install roots did not find `java.exe`.
  - `git diff`/`git status` could not be obtained because `git` is not on PATH.
- Source-level sanity checks were performed:
  - Brace count matched in `WarDayCommands.java`.
  - No stale references were found for removed one-axis transform helpers such as `targetBlockX`, `targetBlockY`, `targetBlockZ`, `sourceXForZ`, or `sourceZForX`.
- The rotation implementation assumes the defender target facing is east because the defender anchor is `[0, warDayBaseY, 0]` and the attacker anchor is `[baseSpacingBlocks, warDayBaseY, 0]`.
- The most likely half-finished area is Warday gameplay completion, not compilation:
  - Defender base rotation source code is present, but it still needs a real compile and in-game validation.
  - Attacker spawn areas remain unrotated because they do not have a forward marker/orientation marker.
  - Only decorative entities `Painting` and `ItemFrame` are copied.
  - Item frames are copied but their items are cleared.
  - Containers are copied structurally but their contents are cleared.
  - Prepared/copied War Day claim shapes are cleared over the source dimension's mapped vertical range before paste.
  - Team B can be absent for validation/prepare one-team testing, but `/warday start` requires both configured teams.
- I could not obtain `git status` because `git` is not available in the PowerShell environment used by Codex. Dirty/untracked files should be checked manually from a developer shell with Git installed.

## Next Immediate Steps

1. From a developer shell with Java and Gradle available, compile Warday:
   - `cd "C:\Users\Connor.Lyle\Documents\GitHub\Fortifications\Warday-Mod\Warday"`
   - `gradle build`
2. Review `WarDayCommands.java` with `git diff` once Git is available, especially the `PlacementPlan` rotation math and NBT entity yaw/tile-coordinate handling.
3. Manually play-test defender base rotation on a local NeoForge server with FTB Teams and FTB Chunks installed:
   - create teams and claims,
   - place a defender nexus and forward marker,
   - test forward marker facings north, south, east, and west,
   - run `/warday validate`, `/warday prepare`, and `/warday prepare confirm`,
   - confirm the copied defender marker/base faces east toward the attacker side.
4. Manually play-test the full Warday flow: run `/warday start`, break the copied nexus, and confirm restoration.
5. Add focused automated tests for rotation mapping, rotated footprints, and claim-scoped destination clearing.

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
- Git status could not be checked from this environment because Git was not installed on PATH.
- I did not identify any deliberately bypassed authentication, mocked auth, ignored validation, missing database indexes, or database memory leaks. The main risks are gameplay state correctness and expensive world scans.
