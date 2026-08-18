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
- Build all current Fortifications and War Day changes with `.\gradlew.bat build` from `Fortifications Mod/Fortifications Mod`.
- After a successful build, replace `MYTH MODS FOR DEREK/fortifications-1.0.0.jar` with `Fortifications Mod/Fortifications Mod/build/libs/fortifications-1.0.0.jar`. The bundle must not contain the retired standalone `warday-1.0.0.jar`.
- Verify that each refreshed distributable matches its build artifact by file size or cryptographic hash, and mention the refreshed JAR in the completion report. Do not alter other files in `MYTH MODS FOR DEREK`.
- Prefer focused automated coverage for calculations and state transitions, but never treat compilation alone as proof of Minecraft runtime behavior.
- For gameplay changes, include a concise manual test recipe and result beneath the completed queue item.
- If in-game verification is unavailable, record the build/test evidence and the remaining manual test explicitly, and leave the item unchecked.
- Recheck match end, death/respawn, logout/reconnect, and server-restart behavior whenever work touches persisted event or player state.
- Keep this `HANDOFF.md` updated as substantive work changes implementation state, verification evidence, active or blocked work, risks, dependencies, or next steps. Revise stale statements rather than accumulating contradictory history.

# 1. Current State of the Build

## Mission Accomplished

- Fortifications and War Day are now one NeoForge mod and one JAR named `fortifications`, built from `Fortifications Mod/Fortifications Mod` with `.\gradlew.bat build`. `Warday-Mod/Warday` remains only as pre-merge rollback/reference source.
- The merged metadata exposes one mod entry with ID/display name `fortifications`/`Fortifications`. `/warday` remains the War Day command root, while current registries, packets, assets, translations, and the dimension use the `fortifications:` namespace.
- Current active War Day IDs are `fortifications:nexus` and `fortifications:war_day`. The obsolete `fortifications:forward_marker` and `fortifications:attacker_spawn` blocks/items remain registered only for existing-world compatibility, with aliases from the old `warday:` IDs. The saved-data filename remains `warday_state` so player recovery data is not silently abandoned; loading an inactive legacy `warday:war_day` state rewrites the dimension and invalidates the old prepared arena.
- The Warday mod has a command surface under `/warday` in `WarDayCommands`:
  - `/warday team1 <name>` and `/warday team2 <name>` configure FTB team names.
  - `/warday blocks` and `/warday kit` give setup blocks to an operator player.
  - `/warday validate` scans for exactly one defender-owned nexus in its connected claim cluster. No orientation or attacker marker is required; Team 2 still comes from configuration.
  - `/warday scan` reports claimed-cluster and guardrail information without changing the world.
  - `/warday prepare` previews the copy plan.
  - `/warday prepare confirm` starts a tick-budgeted job that copies the nexus-centered 256x256 source square in its original orientation, reserves/copies the claim in place, transfers source biomes, and validates four safe attacker corner spawns before mutation. `/warday prepare status` reports its phase and `/warday prepare cancel` stops it.
  - `/warday clear` invalidates prepared state, loads every arena chunk, removes non-player entities, and tick-budget wipes the full 256x256 arena build height. `/warday status` reports its progress; it refuses to overlap preparation or an active match and leaves warned players untouched.
  - `/warday status` reports prepared/active state and configured dimension status.
  - `/warday start` snapshots online players, teleports team members into the War Day dimension, puts non-participants in spectator, and assigns match blocks.
  - `/warday end` restores online players from saved snapshots.
- `/warday blocks` and `/warday kit` now issue only `fortifications:nexus`. The retired forward marker is hidden from the creative setup tab and ignored by validation and scanning; its registry entry, item, model, and legacy alias remain so old worlds do not lose the block.
- Arena placement preserves the source square's world orientation around the nexus. `PlacementPlan` uses no rotation, and old forward-marker blocks encountered in the source are skipped by destination checks and copying so they do not appear in freshly prepared arenas.
- `WarDayState` persists prepared/active state, copied nexus position, four attacker corner spawns, pending corner choices, configured dimension, team labels, and player snapshots in Minecraft `SavedData`.
- Nexus destruction in the active War Day dimension starts the persisted victory-fanfare phase; restoration follows when the configured fanfare finishes, while `/warday end` remains an immediate operator override.
- Respawn handling exists for active participants: players enter spectator mode during the scaling cooldown, then defenders return nexus-side while attackers choose one of four persisted corner spawns from a vanilla inventory popup. Attacker cooldowns have a five-second minimum selection window.
- During respawn cooldown, participants automatically spectate an eligible living teammate in entity-camera POV. A synchronized client flag gates raw LMB/RMB press handling for validated previous/next teammate requests; menu clicks and ordinary spectators are unaffected.
- Active-match lifecycle edge cases have been implemented, including login handling during an active match, deferred restoration after the match, persisted pending respawn/death state, and active-role reassignment.
- A server-owned boss-bar timer displays the authoritative combat countdown to all online match players, reconstructs from persisted state after restart, synchronizes login/logout, and disappears before the victory fanfare or any match cleanup path.
- Separate server-owned defender/attacker roster objectives list both persisted teams, paginate every five seconds, and select a personalized view for each participant. Only teammates show a right-aligned health/status column; opponents retain names with blank status.
- Online teammate health is displayed as numeric Minecraft health points such as `20/20`, with percentage coloring. Teammate pending respawns show `RESP Ns`, disconnected teammates show `OFF`, and cleanup returns clients to the current global sidebar.
- Terrain preparation copies the exact nexus-centered 256x256 source square in its original orientation, reserves claimed chunks out of the surrounding pass, and copies the claim into those same columns. Each corner spawn searches outward from its preferred inset position in nearest-first rings across the complete arena when necessary. Before copy, preparation discards every loaded non-player entity in the dedicated dimension and tick-budget wipes every block across columns `-128..127` and the full build height. After wiping, it requires two consecutive later-tick entity scans to find nothing before fresh terrain copying begins. Loading, validation, checks, clearing, copying, and source-biome transfer remain resumable with a 10 ms tick budget.
- The War Day dimension border is permanently enforced at center `(0, 0)` with size `256`, including during preparation, match start, and post-match cleanup. Each match creates one shared scale-1 vanilla map centered at `(0, 0)`, covering exactly `-128..127`; its persisted map ID lets every player and late joiner receive the same map without duplicates. Snapshot restoration and explicit ID-based cleanup remove issued maps after the match.
- The match nexus has hardness `22.5` and blast resistance `30`. A successful `/warday prepare confirm` ends with a bold green completion message only after prepared state is saved; it names the completed arena components and points to `/warday start`.
- Active-combat friendly fire is canceled between players on the same persisted War Day side, including player-fired projectile sources; enemy and environmental damage remain unchanged.
- Rapid-breaking strikes use cumulative exact slowdown tiers of 25%, 45%, 55%, and 60%, capped at 40% remaining mining speed, with the existing duration refreshed on every trigger.
- Match-issued team blocks now carry `minecraft:custom_data` key `WardayTeamBlock` with side value `defender` or `attacker`. Replenishment counts only correctly marked stacks, and right-click/place validation rejects ordinary matching wool or the opposing side's tagged blocks.
- During an active match, Warday blocks `enderstorage:ender_pouch`, block storage, vanilla chest boats/minecarts, item frames, and entities exposing NeoForge item handlers. Cleanup clears vanilla/mutable entity inventories before discarding storage entities.
- Both `/warday end` and normal victory completion restore players, remove the issued arena map, wipe the full bounded arena through the dimension build height, and discard every loaded non-player entity in the Warday dimension. Cleanup counts are logged; the wipe covers the complete accessible 256x256 dimension rather than attempting an impossible infinite-chunk deletion.
- `/warday validate` and `/warday scan` now narrow nexus scanning to loaded claimed chunks owned by the configured teams within the validation radius instead of walking the entire full-height radius volume.
- `/warday prepare confirm` now clears only positions mapped from the actual source claim cluster before paste. It no longer wipes unclaimed holes inside the cluster's rectangular bounding footprint.
- The Fortifications mod registers a synced `fortifications:unarmed_damage` player attribute and has a global `FortificationsMod.GLOBAL_ACTIVE` flag.
- `FortificationBlockRules` excludes Warday setup blocks from Fortifications hardening so admin/event marker blocks are not accidentally fortified.
- Fortifications' nine Relics/reliquified item mixins now target the Relics 0.12 `AbilityStatTemplate.StatTemplateBuilder.targetValue(...)` API. Their frozen and additive balance values were converted from the removed 0.11 `upgradeModifier(...)` API so they retain the intended per-level behavior.
- Seven Relics/reliquified event-mixin classes containing nine redirects now target `AbilityRankModifierData.isEnabled()` instead of the removed Relics 0.11 `AbilityData.isRankModifierUnlocked(String)` method.
- The Fortifications compile-only dependencies now match the current pack versions of Relics 0.12.8, Iron's Spells 3.16.2, and Tunes & Tomes 1.1.0-HOTFIX.
- Recipe/loot datapack overrides exist under the Fortifications mod resources for several third-party mods, including `artifacts`, `relics`, `alexscaves`, and `sophisticatedbackpacks`.
- Fortifications registers `fortifications:fort_chest`, displayed as **The FortChest**. It is a single-only 27-slot chest with vanilla chest opening, obstruction, hopper, comparator, naming, drop, and piglin behavior; its placed and inventory renderers use the current user-supplied custom 64x64 RGBA chest sheet.
- Fortifications registers a rare, stackable `fortifications:class_reset_token`. Right-clicking it opens a vanilla confirmation inventory; confirmation resets only the `fortifications_classes:example` Puffish skill allocations, preserves the category's experience/available points, removes the class-specific `is_monk` tag, consumes one token outside Creative, and opens the class tree for reselection. It has no crafting recipe and is available only through Creative mode or commands.
- Class resets are refused while War Day is active or the player still has a saved War Day inventory awaiting restoration. A persisted per-player flag suppresses the next class Starter Kit grant after a reset; the Puffish pre-reward hook snapshots inventory/cursor/effects and restores them at server-tick post after `sk give` has run, while leaving the newly selected class attributes and commands active. A normal first-ever class selection remains unchanged.
- Global player chat displays the sender as `<[FTB Team] PlayerName>`, using the current FTB team display name/color. The implementation changes only vanilla chat's bound sender component, preserving the signed message, filtering, spam checks, and body. Automatic personal teams are omitted to avoid redundant `[PlayerName] PlayerName` prefixes; party and server teams are shown. Nametags and the tab list are unchanged.
- The only current distributable is `MYTH MODS FOR DEREK/fortifications-1.0.0.jar`; the standalone Warday jar was removed from the bundle after the merge.

## Active/Halted Work

- The FortChest implementation adds a dedicated block entity type, vanilla chest renderer registration, a client material override, and a custom item renderer because Minecraft 1.21.1's built-in item renderer recognizes only literal vanilla chest blocks. Player placement and neighbor updates force `ChestType.SINGLE`, so adjacent FortChests never merge.
- FortChests now bind to the placing player's current FTB Team ID. Only current members of that exact team can open or break the chest; team changes take effect dynamically. Existing unbound FortChests are claimed by the first FTB team member to interact with or break them. Explosions cannot destroy FortChests, and hopper-style insertion/extraction is disabled so non-player automation cannot bypass team access.
- Every FortChest bound to the same team delegates to one 27-slot inventory stored in that FTB team's persistent extra data. Different team IDs remain isolated. Breaking one physical chest drops only the block and leaves shared contents intact; the inventory survives having no placed chest and returns when the team places another. Legacy per-block contents migrate into the team's storage on first binding, with overflow returned to the claiming player as drops.
- FortChest texture provenance: the latest user-supplied `dark's textures/fortchest.png` was copied byte-for-byte to `Fortifications Mod/Fortifications Mod/src/main/resources/assets/fortifications/textures/entity/chest/fort_chest.png`; both source and project texture are 64x64 PNG files with SHA-256 `A92A02423B85E5EDF232F3F7FEC6AB17E5D644241803CDDC631135A400F3928A`. The packaged texture was independently hashed to the same value.
- Class Reset Token texture provenance: `dark's textures/class_gem.png` is copied byte-for-byte to `assets/fortifications/textures/item/class_reset_token.png`. It is a 16x128 eight-frame vertical sheet with SHA-256 `A584D6F234D7D599A51A397508AC7DD892DF0364A5B770FE16B049B936F5EDE2`. Its supplied `class_gem.png.mcmeta` is copied to the matching token texture metadata with SHA-256 `13594935224A83FB39219E863A118BB5D3ADE3A8D7A6BE664AA74F513322B566`; it plays frames 0-7 for two ticks each. Static JAR inspection verified the texture, exact metadata, and item-model reference are packaged; in-game animation still needs client verification.
- Animated Nexus texture correction: the current files come directly from `dark's textures/nexus.png` and `nexus.png.mcmeta`. The 16x256 sixteen-frame sheet has SHA-256 `58E60E192A9A53F691F1CC104D5906BE34FE9ABA0C6DD7D5908D9ABBBA139383`, and its metadata cycles frames 0-15 for two ticks each. Static JAR inspection verified both current resources are packaged. In-game animation and appearance still need runtime verification after a client restart or resource reload.
- The latest merged `.\gradlew.bat build --no-daemon --no-problems-report` succeeded on 2026-08-17 with the FTB Teams chat sender prefix and all prior texture/class-reset work. It ran `TeamChatPrefixPolicyTest`, `ClassResetPolicyTest`, `FortChestTeamAccessTest`, and every Warday focused check. The artifact and sole refreshed distributable are both 307,996 bytes with SHA-256 `7B40BF40A9F81CA6CFA90F5494D61833F6A780B88FCA121E5622603D872B63BF`; the bundle contains no standalone `warday-1.0.0.jar`. Static JAR inspection confirmed `TeamChatPrefixMixin`, its helper/policy classes, and the required mixin configuration entry are packaged.
- Chat-prefix runtime verification remains: test two named party teams, team color changes, join/leave/switch without restart, a solo personal team, operators, signed-chat indicators/reporting, chat filtering, reconnect, dedicated-server restart, FTB team-chat redirection, and compatibility with any installed chat-formatting mods. The focused policy test and static injection-target audit do not replace an in-game mixin launch.
- Class-reset runtime verification remains: test confirm versus cancel, Survival versus Creative consumption, no-class and missing-datapack errors, class experience/point preservation, old reward removal/new attribute activation, Monk tag removal and re-addition, no Starter Kit item/effect duplication, a normal new player's first kit, full inventories/cursor stacks, reset followed by logout/restart before reselection, repeated resets, and both active-match and pending-War-Day-restoration denial. Compilation and policy tests do not prove the external Puffish/Starter Kit interaction in Minecraft.
- FortChest runtime verification remains: use at least two FTB teams and multiple chests per team; verify placement binding, same-team shared live updates, cross-team inventory isolation, open/break denial, team leave/join behavior, legacy chest claiming/migration, breaking the last chest without item loss, replacement after restart, explosion immunity, hopper denial, comparator output, texture/rendering, adjacency, naming, opening animation/sound, obstruction/cat blocking, and server restart. Also copy a FortChest through `/warday prepare`, start/end the match, and confirm Warday never clears the external team inventory.
- Fortifications Relics 0.12 compatibility was implemented after the 2026-08-06 client crashes showed required mixin injections scanning zero targets. The first launch exposed the removed `upgradeModifier(...)` target in item mixins; the next launch exposed the removed `isRankModifierUnlocked(...)` target in Sealed Sword and Shock Pendant event mixins. Source audit migrated every occurrence of both removed APIs rather than stopping at the reported failures.
- `./gradlew.bat build --no-daemon --no-problems-report` succeeded on 2026-08-06 after transient OneDrive access-denial retries in NeoForge's generated `build/tmp` output. Static `javap` inspection confirmed every current Relics 0.12.8, Reliquified Artifacts 1.0.7, and Reliquified Iron's Spells 0.2.7 target method contains enough `initialValue(...)`, `targetValue(...)`, and `AbilityRankModifierData.isEnabled()` calls for all referenced mixin ordinals.
- The earlier Relics-only Fortifications artifact was deployed to the affected CurseForge instance at `fortif update 8 we're so back/mods/fortifications-1.0.0.jar`; that deployed copy is now superseded by the FortChest build recorded above and has not yet been refreshed in the instance.
- Both custom projects were rebuilt successfully and their distributables refreshed together on 2026-08-06. That historical Fortifications artifact and its hashes are superseded by the current FortChest artifact recorded above.
- Minecraft runtime verification is still required in the exact CurseForge instance. Launch through mod construction, enter a world, and check affected Relics/reliquified item stats; a later incompatible mixin could surface only after the previously fatal injections are fixed.

- Current implementation item: `WARDAY_TODO.md` section **Anti-grief rule**, exact task **“Add a configurable rapid-block-breaking punishment.”**
- The rule triggers on the configurable 15th successful break in an inclusive rolling 30-second window. Match-scoped strikes update one named transient modifier to cumulative 25%, 45%, 55%, then 60% slowdown by default; later strikes remain capped and every trigger refreshes the 60-second default duration and Glowing.
- A lowest-priority successful-break listener counts only persisted Survival participants during active combat inside the Warday dimension/border. Canceled/protected/out-of-bounds breaks, outsiders, Creative/Spectator, the nexus action, and setup markers are exempt. Histories prune once per second; penalties clean on expiry, logout, fanfare, match end, inactive recovery, and clean server stop, restoring a prior Glowing effect for its remaining duration when cleanup is early.
- `WarDayRapidBreakRuleTest` covers the 15th-break boundary, rolling-window expiry, retrigger reset, exact modifier conversion, all four tiers, and the cap. The 2026-08-10 full build passed; current artifact evidence is recorded below.
- The rapid-break item remains unchecked pending the full gameplay/exemption/lifecycle/configuration matrix recorded in `WARDAY_TODO.md`.
- Current user-requested combat update: `WARDAY_TODO.md` section **Combat rules**, exact task **“Disable friendly fire between participants on the same persisted War Day team during active combat.”** `LivingIncomingDamageEvent` now cancels same-side player damage in the Warday dimension, including projectile sources. `WarDayFriendlyFireTest` covers both teams, opponents, and outsiders; runtime weapon/mod interaction checks remain required.
- The merged build preserves `/warday`, all Warday focused tests, the two-pass entity cleanup rule, exact map/border and spawn-fallback calculations, FortChest shared-storage exemption, and the 16-frame Nexus animation. Minecraft runtime verification is still required after the loader and namespace merge.
- Previous implementation item: `WARDAY_TODO.md` section **Skills and map privacy**, exact task **“Investigate and implement the strongest feasible JourneyMap integration/configuration that hides non-teammate player icons while retaining teammate icons.”**
- Installed JourneyMap 6.0.3/API 2.0.0 exposes a server-side per-receiver/per-target `PlayerRadarUpdateEvent`. Warday now subscribes when JourneyMap is loaded and preserves visibility only when both players resolve to the same FTB Team ID. It never makes a JourneyMap-hidden player visible, applies globally including operators, and fails closed when team state is unavailable.
- `WarDayTeamVisibilityTest` covers same/different/missing teams and preservation of stricter JourneyMap decisions. `gradle build --no-daemon --no-problems-report` succeeded with all four focused checks; packaged metadata confirms the optional 6.0.x dependency. The artifact and refreshed `MYTH MODS FOR DEREK/warday-1.0.0.jar` are both 139,208 bytes with SHA-256 `79A9AB4ED1BE5D313940A9CA4AB5826266F8948EF18C2AC381ABA30B0C7CA651`.
- The JourneyMap item remains unchecked pending multiplayer minimap/fullscreen, team-change, operator, dimension, reconnect/restart, server-option, and optional-mod-absence verification recorded in `WARDAY_TODO.md`.
- Previous implementation item: `WARDAY_TODO.md` section **Skills and map privacy**, exact task **“Remove the combat and mining skill trees from the shipped gameplay/configuration.”**
- The Fortifications JAR now overrides `data/puffish_skills/puffish_skills/config.json` with a version-3 empty category list and declares an optional load-after relationship with `default_skill_trees`. This suppresses only that pack's combat/mining registration and presentation; the independent `fortifications_classes` tree remains available, and legacy player category data is left inert rather than deleted.
- `.\gradlew.bat build --no-daemon --no-problems-report` succeeded on 2026-08-07. Static JAR inspection confirmed both the override and metadata ordering; that earlier artifact is superseded by the current FortChest build recorded above.
- This item remains unchecked pending runtime verification with existing/new player data, `/reload`, Classes-tree visibility, inactive legacy rewards, and with the optional default-trees JAR absent.
- Current user-requested arena update: `WARDAY_TODO.md` section **Arena preparation**, exact task **“Rework the attacker-side arena copy strategy so substantially more surrounding terrain/chunks are brought into War Day instead of relying on terrain generated between a small attacker spawn copy and the defender base.”**
- Preparation copies the exact 256x256 square centered on the defender nexus without rotation. Claimed chunks are reserved out of the surrounding-terrain pass and copied separately, while the nexus maps to arena origin. The forward marker is no longer required, scanned, or copied. Remote biome search and its exhaustive grid were removed; the old search config keys remain only for config compatibility.
- The exact source biome grid is transferred to the target. Four safe attacker landings are resolved near the inset northwest/northeast/southwest/southeast corners from source terrain before world mutation. All four positions are persisted; initial attackers distribute across them.
- Attacker cooldowns open a vanilla one-row four-button corner picker and last at least five seconds so the first death can choose. Choice state persists across reconnect/restart; no choice falls back by death count. Defender respawn remains nexus-side, and existing teammate-camera cycling remains active after closing the picker.
- The watchdog-safe job now phases through local source/target chunk loading, corner validation, destination checks, clearing, surrounding terrain, claim blocks, source biomes, and finalization. `WarDayAttackerTerrainPlanTest` covers the exact 256 windows, rotations, four corner targets, fallback rotation, negative coordinates, borders, and guardrail. Live preparation, popup/menu interaction, terrain fidelity, and lifecycle verification remain required by `WARDAY_TODO.md`.
- The arena item remains unchecked pending the in-game matrix recorded beneath the canonical item in `WARDAY_TODO.md`.
- Previous implementation item: `WARDAY_TODO.md` section **Respawn experience**, exact task **“During respawn cooldown, make the dead player spectate a living teammate in first-person POV when one is available.”**
- Respawn spectators select only online, living, non-spectator, non-respawning members of the viewer's persisted team in the War Day dimension. Ordering is stable by name/UUID; invalid targets are replaced automatically, and a viewer with no target remains above team spawn until a teammate becomes eligible.
- A clientbound active-state payload gates raw mouse interception to Warday cooldowns only, while a serverbound cycle payload requests previous/next selection. LMB/RMB presses are handled directly because the normal attack/use mapping is unreliable in spectator mode; menu clicks are ignored. Every request is revalidated server-side. Camera/input state is disabled before cooldown restoration, fanfare, exact match restoration, and `/warday end`.
- `WarDaySpectatorCycleTest` covers candidate selection plus LMB/RMB press mapping and rejection of releases/other buttons. The 2026-08-10 full build passed; current artifact evidence is recorded below.
- The respawn-spectating item remains unchecked pending multiplayer camera/input and lifecycle verification. Required coverage is recorded beneath the canonical item in `WARDAY_TODO.md`.
- Previous implementation item: `WARDAY_TODO.md` section **Match presentation and HUD**, exact task **“If Minecraft scoreboard constraints allow a readable implementation, show each listed player's live health beside their name.”**
- Player-line health uses Minecraft 1.21.1 per-score `FixedFormat`, leaving the integer score available for roster ordering. Online teammate values are numeric health points (`20/20` at vanilla full health), respect modded maximum health, and are colored by percentage; absorption is excluded. Opponent number formats are blank.
- Sidebar synchronization remains once per second and reuses stable score holders across separate defender/attacker objectives. Each participant receives their side's objective; the global display slot is not commandeered, and cleanup returns clients to the current global objective before removing Warday objectives.
- `WarDayRosterHealthTest` covers numeric formatting, clamping, invalid/boosted health, color thresholds, offline status, and respawn countdown boundaries. The 2026-08-10 full build passed; current artifact evidence is recorded below.
- Warday's configured compile-only Curios dependency was absent from the repository-level `mods` directory at the start of this work. A matching local `curios-neoforge-9.5.1+1.21.1.jar` was restored from the active CurseForge instance; the source and local copy both have SHA-256 `A45DF2125C26219974ABA7507FFC9AFE7B83ACC941A386AF3FAACB1CC0056FDE`. The `mods` directory remains an untracked local build dependency.
- The live-health item remains unchecked pending in-game readability and lifecycle verification. Required coverage includes health changes, modded maximum health, pending respawn, disconnect/reconnect, restart, pagination, fanfare, both cleanup paths, restoration of an external sidebar, and a second match.
- Earlier implementation item: `WARDAY_TODO.md` section **Match presentation and HUD**, exact task **“Add a sidebar scoreboard that lists Team 1 and Team 2 and their participating players.”**
- The sidebar uses custom score display components and stable dummy ordering scores rather than altering real player scoreboard teams. Headers and empty-page lines have blank number formatting, while player lines use the live-health format described above. It renders both team headers plus up to six alphabetically sorted names per team and rotates five-second pages when needed.
- Persisted `previousSidebarObjective` state allows cleanup to restore the objective that owned the sidebar before War Day. If an external system replaces the sidebar mid-match, Warday records that newer objective before reclaiming the slot and restores it during cleanup if Warday still owns the slot.
- Pagination calculations passed for every 0-30 player combination on both teams with full coverage and no page above 14 lines. `gradle build --no-daemon --no-problems-report` succeeded on 2026-08-06. The build artifact and refreshed `MYTH MODS FOR DEREK/warday-1.0.0.jar` are both 116,294 bytes with SHA-256 `C5AEEBC0A78E339416520390263C884BE80AE9BE3EADA53751B94392DC59CD9A`.
- The sidebar item remains unchecked pending visual and lifecycle verification in Minecraft.
- Earlier implementation item: `WARDAY_TODO.md` section **Match presentation and HUD**, exact task **“Add a boss-bar match timer at the top of the screen and keep it synchronized with the authoritative match clock.”**
- The boss bar is implemented with `ServerBossEvent`. Its name and progress are derived once per second from the persisted `matchEndGameTime`; `matchDurationTicks` is now persisted for restart-stable progress normalization, with a configuration fallback for legacy saves.
- Boss-bar player membership is synchronized at match start, login, logout, and periodic updates. The bar is removed at fanfare transition, `/warday end`, inactive-state recovery, missing-level recovery, and cross-server static cleanup.
- Focused timer-boundary calculations passed. Its earlier build artifact at SHA-256 `5FFBF88DC42834CF97CCB94D8F33962FF26EA383655A06D49272A27992399488` has been superseded by the live-health build recorded above.
- The boss-bar item remains unchecked pending visual and lifecycle verification in Minecraft.
- First still-unverified canonical item: `WARDAY_TODO.md` section **Player and entity state**, exact task **“Make non-player entities carry over into the War Day dimension and define which entity categories must be copied or transferred.”**
- Source review on 2026-08-06 found that continuous entity coordinates were rotated around the anchor block corner, which could shift entities into an adjacent transformed block. `PlacementPlan.targetX/targetZ` now rotate around the anchor block center.
- Focused calculation verification checked 900 points across all four rotations and confirmed that every entity point remains inside the block produced by the integer block transform.
- The entity-position correction previously built successfully at SHA-256 `05277987197345962CA438506ED73E64AD9EEAD4E34CDE7DCBD14D75B4E183EA`; that artifact has now been superseded by the boss-bar build recorded above.
- A preceding `gradle clean build --no-daemon` attempt compiled the source but encountered a transient Windows access denial in NeoForge's generated `build/tmp` output; a normal retry compiled and produced the JAR, then hit a OneDrive collision replacing Gradle's generated problems report. Disabling that optional report produced the clean successful build above.
- The entity task and nested tamed-entity task remain unchecked because no Minecraft runtime test was performed. NBT fidelity, AI/tame behavior, leash recreation, passenger trees, entity caps, cleanup, and restart behavior remain manual-verification requirements.
- The copied nexus is at arena origin, the source orientation is retained, and attacker targets use all four corners at a 16-block inset.
- The most likely half-finished area is Warday gameplay completion, not compilation:
  - Nexus-only fixed-orientation copying compiles and has calculation-level coverage, but still needs in-game validation.
  - Terrain comes from the defender nexus's exact 256x256 surroundings; there is no separately copied attacker base or required attacker marker.
  - Persistent non-player entities use the prepared-template path; decorative `Painting` and `ItemFrame` entities use a separate copy path.
  - Item frames are copied but their items are cleared.
  - Containers are copied structurally but their contents are cleared.
  - Prepared/copied War Day claim shapes are cleared over the source dimension's mapped vertical range before paste.
  - Team B can be absent for validation/prepare one-team testing, but `/warday start` requires both configured teams.
- The worktree contains uncommitted personalized-health, respawn-spectating, exact attacker-terrain/automatic-spawn, friendly-fire, tagged team-block anti-cheat, Ender pouch/entity-storage blocking, end cleanup wipe, skill-tree removal, JourneyMap privacy, and cumulative rapid-break implementations plus tests/resources/docs and refreshed distributables. Preserve unrelated local dependencies and inspect `git diff` before further edits.

## Next Immediate Steps

1. Back up the world, remove both old standalone jars, install only the merged `fortifications-1.0.0.jar` on client and server, and launch through mod construction. Confirm NeoForge lists one Fortifications mod and no duplicate registration/mixin/network error occurs.
2. In a world, verify Kinetic/Hunting Belt slots and Cloud in a Bottle jumps remain fixed at 2, sealed-weapon respawn time remains 120 seconds, and Night Vision Goggles, Power Glove, and Vampiric Glove additive stats still progress at their intended rates.
3. In the backed-up world, confirm old `warday:war_day` prepared state is invalidated and run a fresh `/warday prepare confirm` into `fortifications:war_day`. Verify `/warday kit` gives only the nexus, validation succeeds without a forward marker, old placed markers are not copied, and existing custom team/config values survive in `fortifications-server.toml`; then test status/cancel/restart, full coverage, irregular claims, surface safety, defender overlap, and repeat preparation.
4. Test tagged team blocks with pre-owned wool, stolen opposing stacks, split/drop/pickup, start/respawn/reconnect replenishment, then exercise Ender pouches and vanilla/modded entity storage.
5. Populate the arena and storage entities, run `/warday clear`, `/warday end`, and both victory paths, and confirm the arena is air, all non-player entities are gone, progress/counts are credible, a warned player is preserved, and a second prepare starts cleanly.
6. Verify only the Classes Puffish tree remains, then run the JourneyMap privacy matrix, all four rapid-break tiers, and the friendly-fire weapon/projectile matrix from `WARDAY_TODO.md`.
7. Manually verify respawn teammate POV and both mouse-cycle directions with multiple teammates and an opponent, including target loss, no-target recovery, reconnect/restart, cooldown completion, fanfare, cleanup, ordinary spectators, and a second match.
8. Manually verify personalized sidebars: each participant sees only teammate health/status, vanilla full health reads `20/20`, opponent names remain visible without values, and external sidebar coexistence/restoration survives lifecycle paths.
9. Manually verify the boss bar during start/countdown, death/respawn, logout/reconnect, server restart, defender timeout, nexus victory, `/warday end`, fanfare transition, and a second match. Confirm there are no stale or duplicate HUD elements.
10. Run the entity carryover manual matrix from the first `WARDAY_TODO.md` item, including tamed wolves, passenger trees, leashes, all rotations, an unclaimed-hole entity, the configured cap, repeated matches, and restart cleanup.
11. Record successful runtime results beneath each applicable queue item before striking it through; keep any item with a failure or untested edge case unchecked.

# 2. Architectural Decisions & Patterns

## Structural Choices

- Warday currently keeps most command and event behavior in `WarDayCommands`. This is not elegant, but it keeps the event lifecycle in one place while the feature is still changing quickly. The class owns command registration, validation, preparation, start/end, respawn, and nexus-break completion.
- Data that must survive server restart is separated into `WarDayState`, backed by Minecraft `SavedData`. Runtime-only delay bookkeeping remains in the static `PENDING_RESPAWNS` map because it is short-lived and only meaningful while the server process is active.
- Setup blocks are deliberately simple block classes. `NexusBlock` is the only active setup marker. `ForwardMarkerBlock` remains solely as a compatibility block for worlds that already contain it.
- Config is centralized in `WarDayConfig` using NeoForge `ModConfigSpec`, so team names, scan radius, base limits, target dimension, target Y, spacing, and respawn delay can be changed without recompiling.
- `PlacementPlan` is a record local to `WarDayCommands` because the copy algorithm currently needs only a compact value object: source anchor, target anchor, cluster bounds, rotation, and coordinate translation helpers.
- Fortifications uses `FortificationBlockRules` as a small policy class instead of baking block checks directly into event/mixin code. This makes the Warday exemption easy to see and test later.

## Cross-Boundary Communication

- Commands enter through NeoForge's `RegisterCommandsEvent` and Brigadier command registration in `WarDayCommands.registerCommands`.
- Warday communicates with FTB Teams through `FTBTeamsAPI.api().getManager()` and resolves teams by configured name, short name, or display name.
- Warday communicates with FTB Chunks through `FTBChunksAPI.api().getManager()`, `ClaimedChunkManager`, `ClaimedChunk`, and `ChunkDimPos`.
- Team ownership is inferred from the FTB Chunks claim owner for the chunk containing each setup block.
- Persistent server state is serialized to NBT in `WarDayState.save` and deserialized in `WarDayState.load`.
- Block entities are copied by saving source block entity NBT with `saveWithFullMetadata`, rewriting `x/y/z`, then loading that data into the target block entity with `loadWithComponents`. Target block states retain their source orientation.
- Decorative entities are copied by serializing entity NBT, removing `UUID`, translating `Pos`, rotating yaw, translating hanging entity tile coordinates, then recreating the entity with `EntityType.create`.
- Player snapshots serialize game mode, dimension id, coordinates, and rotation. Restoration parses the saved dimension id back into a `ResourceKey<Level>`.

## Complex Logic

- Claim cluster resolution is a breadth-first search over 4-neighbor chunk adjacency. `connectedClaimCluster` starts at the nexus chunk and walks only chunks owned by the same team.
- Defender validation requires exactly one owned nexus whose chunk resolves into the team's connected claim cluster. Forward and attacker marker validation/scanning are removed; both legacy marker blocks remain registered only for world compatibility and have no preparation role.
- Defender guardrails check connected cluster size and footprint width/depth. Attacker terrain derives exact arena coverage and retains its independent chunk-count guardrail.
- Copy preparation computes an exact `nexus - 128` through `nexus + 127` source window with no rotation and a target anchor at arena origin. It preserves vertical coordinates relative to the nexus and target Y.
- Nexus validation iterates chunk coordinates in the configured radius, rejects unloaded/unclaimed chunks and claims owned by unrelated teams, then scans only relevant claimed chunks for the nexus.
- Destination checking scans non-air source blocks inside the arena and detects whether transformed target positions are already occupied; all-air source sections are skipped.
- Preparation clears every non-player entity plus the full bounded target arena, copies surrounding blocks outside claimed chunks, copies the centered fixed-orientation claim into the reserved columns, and transfers source biome cells. Each expensive block stage is cursor-driven and bounded per server tick; progress is runtime-only and a restart cancels the job. The target dimension must remain dedicated to War Day.
- Four automatic spawn searches target the northwest/northeast/southwest/southeast corners at the standard 16-block inset and validate source-surface collision/fluid safety before any target mutation.
- Respawn delay uses a per-tick UUID map. Attacker corner choices are separately persisted, selected through a server-validated vanilla menu, and consumed when survival restoration occurs; absence of a choice rotates deterministically by death count.

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
  - `TerrainGenerationSequence` non-negative long used to keep the next biome-matched terrain selection stable between preview/confirm and fresh after each successful prepare
  - `Active` boolean
  - `MatchEndGameTime` and `MatchDurationTicks` longs
  - `PreviousSidebarObjective` string
  - `ArenaMapId` integer while a match is active, linking every issued copy to one shared exact-arena map
  - `SavedPlayers` list of player snapshot compounds
- `WarDayState.load` also reads legacy `SavedGameModes` entries and converts them into partial `PlayerSnapshot` entries. This looks like backward compatibility for an older saved state shape.
- Fortifications adds the synced attribute `fortifications:unarmed_damage` to players through `EntityAttributeModificationEvent`.
- The combined mod adds its bundled dimension at `Fortifications Mod/Fortifications Mod/src/main/resources/data/fortifications/dimension/war_day.json`. The configured default dimension ID is `fortifications:war_day`.

## Environment Variables

- No environment variables, API keys, or external connection strings were found or introduced by this handoff.
- Local build assumptions:
  - `Fortifications Mod/Fortifications Mod` is the canonical combined project and builds with `.\gradlew.bat build`.
  - `Warday-Mod/Warday` is rollback/reference source only and must not be used to refresh distributables.
  - A manual Minecraft/NeoForge runtime is still required to validate gameplay behavior.

# 4. Known Tech Debt, Hacks, & AI Shortcuts

- `WarDayCommands` is very large and mixes command registration, validation, copy/paste, event handlers, persistence coordination, and reporting. It should eventually be split into services such as validation, placement/copy, lifecycle, and respawn handling.
- Fixed-orientation arena copying compiles and has calculation-level coverage, but remains untested in-game. Treat runtime behavior as pending verification, especially for modded blocks, block entities with directional NBT, paintings, and item frames.
- The source square is copied without rotation. Both obsolete marker blocks remain registered to avoid breaking existing worlds, but the forward marker is hidden from setup items and skipped during arena copying.
- Several gameplay values are effectively hardcoded:
  - Match block target count is `32`.
  - Arena half-size is fixed at `128`, making the copied/bounded square exactly 256x256.
  - All four attacker targets use a 16-block border inset; corner safety search expands in nearest-first rings across the complete arena when necessary.
  - Attacker respawn selection has a five-second minimum window.
  - Spectators/respawning players are placed 11 blocks above spawn.
  - Safe spawn search radius is 8 blocks horizontally and 4 blocks vertically.
- `/warday validate` and `/warday scan` remain radius-limited and inspect only loaded claims belonging to configured teams. Markers outside the radius, in unloaded chunks, or in unclaimed chunks are not reported.
- Destination clearing is destructive across the complete configured arena and target build height. It is intentional for repeatable prepare runs and radius shrinkage, so the configured War Day target dimension must remain dedicated to generated match areas.
- Container contents are intentionally cleared after copy. That avoids giving duplicated loot/resources, but it may surprise players if their defensive build depends on filled containers.
- Item frames are copied but cleared. This avoids duplicating items, but visual signage/maps/items in frames will not survive the copy.
- Only `Painting` and `ItemFrame` decorative entities are copied. Armor stands, display entities, mobs, boats, minecarts, and modded decorative entities are ignored.
- Active-match login, reconnect, deferred restoration, and pending-respawn state handling have been implemented. These paths still need full multiplayer/server-restart play-testing.
- There is no explicit security model beyond command permission level 2. Operators can wipe/paste target areas with `/warday prepare confirm`.
- Calculation and cursor behavior have focused executable tests, but custom Warday validation, block copying, cancellation, and lifecycle transitions still lack Minecraft runtime automation.
- Fortifications' Relics integration is implemented with required ordinal-based mixins into third-party bytecode. The current Relics/add-on versions were checked statically, but future upstream reordering can break those injections and should be re-audited whenever the pack updates those mods.
- I did not identify any deliberately bypassed authentication, mocked auth, ignored validation, missing database indexes, or database memory leaks. Expensive preparation scans are now tick-budgeted; the main remaining risks are gameplay state correctness, real modded-block copy cost, and unverified live-server cancellation/restart behavior.
