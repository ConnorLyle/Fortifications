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
- `WarDayState` persists prepared state, active state, copied nexus position, attacker spawn position, configured dimension, team labels, and player snapshots in Minecraft `SavedData`.
- Nexus destruction in the active War Day dimension ends the event and restores online players.
- Respawn handling exists for active participants: players briefly enter spectator mode, are teleported above their spawn, then restored after `respawnDelaySeconds`.
- The Fortifications mod registers a synced `fortifications:unarmed_damage` player attribute and has a global `FortificationsMod.GLOBAL_ACTIVE` flag.
- `FortificationBlockRules` excludes Warday setup blocks from Fortifications hardening so admin/event marker blocks are not accidentally fortified.
- Recipe/loot datapack overrides exist under the Fortifications mod resources for several third-party mods, including `artifacts`, `relics`, `alexscaves`, and `sophisticatedbackpacks`.
- Built jars are present in `MYTH MODS FOR DEREK`, including `warday-1.0.0.jar`, `fortifications-1.0.0.jar`, and `voxy-0.2.9-alpha.jar`.

## Active/Halted Work

- I was not in the middle of editing a function when this handoff prompt arrived. No feature code was changed as part of this handoff.
- Based on the newest timestamps, recent active work appears to have been in `Warday-Mod/Warday/src/main/java/com/trove/warday/WarDayCommands.java` and `Warday-Mod/Warday/src/main/java/com/trove/warday/WarDayConfig.java`.
- The most likely half-finished area is Warday gameplay completion, not compilation:
  - `prepareConfirm` explicitly tells the operator that copied bases are not rotated yet.
  - Base placement preserves source X/Y/Z offsets and does not currently rotate copied bases according to the forward marker.
  - Only decorative entities `Painting` and `ItemFrame` are copied.
  - Item frames are copied but their items are cleared.
  - Containers are copied structurally but their contents are cleared.
  - Only online players are snapshotted/restored when `/warday start` and `/warday end` run.
  - Prepared/copied War Day areas are wiped with broad full-height block clearing before paste.
  - Team B can be absent for validation/prepare one-team testing, but `/warday start` requires both configured teams.
- I could not obtain `git status` because `git` is not available in the PowerShell environment used by Codex. Dirty/untracked files should be checked manually from a developer shell with Git installed.

## Next Immediate Steps

1. Manually play-test the full Warday flow on a local NeoForge server with FTB Teams and FTB Chunks installed: create teams, claim chunks, place setup blocks, run `/warday validate`, `/warday prepare confirm`, `/warday start`, break the copied nexus, and confirm restoration.
2. Implement or explicitly defer base rotation. The current `PlacementPlan` only translates positions from the source anchor to target anchor; the forward marker facing is only reported, not applied to block/entity transforms.
3. Harden active-match edge cases: offline player restoration, players joining during an active match, dimension unload/reload behavior, and respawn behavior if prepared positions become invalid.

# 2. Architectural Decisions & Patterns

## Structural Choices

- Warday currently keeps most command and event behavior in `WarDayCommands`. This is not elegant, but it keeps the event lifecycle in one place while the feature is still changing quickly. The class owns command registration, validation, preparation, start/end, respawn, and nexus-break completion.
- Data that must survive server restart is separated into `WarDayState`, backed by Minecraft `SavedData`. Runtime-only delay bookkeeping remains in the static `PENDING_RESPAWNS` map because it is short-lived and only meaningful while the server process is active.
- Setup blocks are deliberately simple block classes. `NexusBlock` is just a marker block. `ForwardMarkerBlock` extends `HorizontalDirectionalBlock` only because the facing is needed to express intended base orientation.
- Config is centralized in `WarDayConfig` using NeoForge `ModConfigSpec`, so team names, scan radius, base limits, target dimension, target Y, spacing, and respawn delay can be changed without recompiling.
- `PlacementPlan` is a record local to `WarDayCommands` because the copy algorithm currently needs only a compact value object: source anchor, target anchor, cluster bounds, and coordinate translation helpers.
- Fortifications uses `FortificationBlockRules` as a small policy class instead of baking block checks directly into event/mixin code. This makes the Warday exemption easy to see and test later.

## Cross-Boundary Communication

- Commands enter through NeoForge's `RegisterCommandsEvent` and Brigadier command registration in `WarDayCommands.registerCommands`.
- Warday communicates with FTB Teams through `FTBTeamsAPI.api().getManager()` and resolves teams by configured name, short name, or display name.
- Warday communicates with FTB Chunks through `FTBChunksAPI.api().getManager()`, `ClaimedChunkManager`, `ClaimedChunk`, and `ChunkDimPos`.
- Team ownership is inferred from the FTB Chunks claim owner for the chunk containing each setup block.
- Persistent server state is serialized to NBT in `WarDayState.save` and deserialized in `WarDayState.load`.
- Block entities are copied by saving source block entity NBT with `saveWithFullMetadata`, rewriting `x/y/z`, then loading that data into the target block entity with `loadWithComponents`.
- Decorative entities are copied by serializing entity NBT, removing `UUID`, translating `Pos` and hanging entity tile coordinates, then recreating the entity with `EntityType.create`.
- Player snapshots serialize game mode, dimension id, coordinates, and rotation. Restoration parses the saved dimension id back into a `ResourceKey<Level>`.

## Complex Logic

- Claim cluster resolution is a breadth-first search over 4-neighbor chunk adjacency. `connectedClaimCluster` starts at the nexus chunk and walks only chunks owned by the same team.
- Defender validation requires exactly one owned nexus, exactly one owned forward marker, and the marker chunk must be inside the connected claim cluster rooted at the nexus.
- Attacker validation requires exactly one owned attacker spawn marker.
- Guardrails check connected cluster size and footprint width/depth against config values before preparation.
- Copy preparation computes a source cluster bounding box, an anchor offset from source min, and translated target coordinates. It preserves vertical coordinates relative to the source anchor and target Y.
- Destination checking scans all non-air source blocks and detects whether translated target positions are already occupied.
- Destination wiping clears every non-air block in the computed destination footprint across the target dimension's full build height.
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
- Base rotation is not implemented. Forward marker facing is stored and reported but not used to rotate blocks, block entities, or entities.
- Several gameplay values are effectively hardcoded:
  - Match block target count is `32`.
  - Defender target anchor is `[0, warDayBaseY, 0]`.
  - Attacker target anchor is `[baseSpacingBlocks, warDayBaseY, 0]`.
  - Spectators/respawning players are placed 11 blocks above spawn.
  - Safe spawn search radius is 8 blocks horizontally and 4 blocks vertically.
- `/warday validate` and `/warday scan` only scan a cube/cylinder-like block area around the command source using `validationRadiusBlocks`; they do not globally scan all claims.
- `scanArea` loops over every block position in the configured radius across the full build height. At the default 512-block radius this is potentially very expensive.
- Destination wiping is destructive within the computed target footprint and full build height. It is intentional for repeatable prepare runs, but risky if the target dimension contains anything important.
- Container contents are intentionally cleared after copy. That avoids giving duplicated loot/resources, but it may surprise players if their defensive build depends on filled containers.
- Item frames are copied but cleared. This avoids duplicating items, but visual signage/maps/items in frames will not survive the copy.
- Only `Painting` and `ItemFrame` decorative entities are copied. Armor stands, display entities, mobs, boats, minecarts, and modded decorative entities are ignored.
- Active-match restore only handles online players captured at `/warday start`. Offline players, players who disconnect mid-match, and players who join mid-match need explicit policy.
- Saved respawn positions are overwritten during `/warday start` and delayed respawn, but original bed/spawn information is not snapshotted/restored in `WarDayState.PlayerSnapshot`.
- `PENDING_RESPAWNS` is static runtime state and is lost on server restart. A restart during an active match may leave delayed respawns unresolved.
- There is no explicit security model beyond command permission level 2. Operators can wipe/paste target areas with `/warday prepare confirm`.
- There are no automated tests for the custom Warday validation/copy/lifecycle logic.
- Git status could not be checked from this environment because Git was not installed on PATH.
- I did not identify any deliberately bypassed authentication, mocked auth, ignored validation, missing database indexes, or database memory leaks. The main risks are gameplay state correctness and expensive world scans.
