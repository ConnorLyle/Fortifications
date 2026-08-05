# War Day TODO

This is the canonical War Day backlog. Agents must follow the queue and completion rules in [`AGENTS.md`](AGENTS.md).

## Player and entity state

- [ ] Make non-player entities carry over into the War Day dimension and define which entity categories must be copied or transferred.
  - Implementation added 2026-08-04: `/warday prepare confirm` now captures transformed, persistent non-player entity templates from the actual claimed chunks, saves them in `WarDayState`, and `/warday start` creates fresh temporary clones for that match. Match end removes loaded clones, and a persisted per-match batch marker rejects stale clones when unloaded chunks later load.
  - Supported by default: saveable non-player entities, including living mobs, tamed animals, armor stands, vehicles, passengers, and compatible modded entities. Excluded: players, paintings/item frames handled by the separate decorative path, drops, XP, projectiles, displays, transient hazards, leash knots, the Ender Dragon, and the Wither.
  - Safety/configuration: `prepare.maxPreparedEntities` defaults to 256 across both copied areas; exact claim membership prevents copying entities from unclaimed holes. Entity UUIDs are regenerated per match, owner UUIDs remain intact, vehicle/passenger trees are preserved, and fence-leash positions are transformed with the arena.
  - Build verification: `gradle build --no-daemon` succeeded in `Warday-Mod/Warday` on 2026-08-04. No automated tests exist in this project.
  - Manual verification still required before completion: run prepare/start/end twice with representative vanilla and modded entities, all four defender rotations, a server restart during an active match, an entity in an unclaimed hole, and enough entities to exercise the configured cap. Confirm no originals are changed and no temporary duplicates survive.
  - [ ] Ensure tamed entities, especially tamed wolves, carry over with their owner, tame state, health, equipment, name, and other relevant NBT intact.
    - Source implementation preserves the serialized entity NBT while replacing only entity-instance UUIDs and copied-entity leash references. The player's owner UUID is deliberately not rewritten.
    - Manual verification still required: use a named, damaged, sitting, collar-dyed wolf plus a standing/following wolf; confirm owner, health, collar, name, pose, behavior, leash behavior, cleanup, and recreation in a second match.
- [ ] Fix paintings so they do not break when placed or copied and are present in the War Day dimension with the correct position, facing, and variant.
  - Implementation added 2026-08-04: decorative copying now transforms the painting `facing` value, entity position, attached tile position, and yaw through the same base rotation while preserving the serialized painting variant. Item frames use their separate `Facing` field and are rotated through the same path.
  - Copying is restricted to hanging entities attached inside the actual claimed chunks. Previously prepared paintings/item frames in the transformed destination claim shape are removed before recopying, preventing repeat-prepare collisions and duplicates.
  - Every clone must retain its expected direction and painting variant, pass vanilla `survives()` support/collision validation, and succeed in `addFreshEntity`; failures are counted in `/warday prepare confirm` output instead of being reported as successful copies.
  - Build verification: `gradle build --no-daemon` succeeded in `Warday-Mod/Warday` on 2026-08-04. The rebuilt `MYTH MODS FOR DEREK/warday-1.0.0.jar` has the same SHA-256 hash as the build artifact: `DB808B4055AF8AC5421FA9C01A4553C71B19F95B2DA488C6C82EA7C503CB4552`.
  - Manual verification still required before completion: test direct placement and copied 1x1, wide, tall, and large paintings on all wall directions; prepare all four defender rotations twice; verify exact variants, attachment positions, no item drops/duplicates, survival beyond 100 ticks, an unclaimed-hole exclusion, and persistence after a server restart.
- [ ] Snapshot each participant's complete pre-War-Day inventory state and restore that exact state after War Day.
  - Items gained, moved, damaged, placed, consumed, or lost during War Day must not permanently alter the player's pre-event inventory.
  - Include armor, offhand, main inventory, selected hotbar slot, and any other player inventory data used by the installed modpack.
  - Implementation added 2026-08-04: the match-start snapshot now persists each online participant before any teleport or match-issued item mutation. It captures the vanilla main inventory, armor, offhand, selected hotbar slot, Ender Chest, carried cursor stack, and the complete Curios inventory tag when Curios is installed.
  - Restoration runs for normal match end and saved-player recovery after logout, reconnect, or server restart. Legacy snapshots remain compatible, and a recovery snapshot is retained instead of discarded if optional Curios data cannot be restored safely.
  - Duplication safeguards added: players cannot interact with arena containers, item-handler blocks, or item frames during an active match; loaded match-area containers, modifiable item handlers, and item frames are cleared during match cleanup before the temporary world is reused.
  - Build verification: `gradle build --no-daemon` succeeded in `Warday-Mod/Warday` on 2026-08-04. The rebuilt `MYTH MODS FOR DEREK/warday-1.0.0.jar` matches the build artifact at SHA-256 `335E29BE22FB513A250F4C09CD6A8F9A747737F4318AE22E1F0C0D25C1FA8D21`.
  - Manual verification still required before completion: test main inventory, armor, offhand, selected slot, Ender Chest, cursor-held items, every Curios slot, nested backpack contents, gained/consumed/damaged/placed items, death and respawn, logout/reconnect, and server restart during an active match. Also test automation deposits and modded storage blocks that expose non-modifiable item handlers.
- [ ] Ensure temporary team blocks and other match-issued items are absent after War Day restoration.
  - This may be resolved by exact inventory restoration, but it must be tested independently before being struck off.
  - Source implementation now restores the exact pre-match snapshot after team blocks are issued, so match-issued stacks should be removed automatically. Independent in-game verification is still required.

## Match presentation and HUD

- [ ] Add a configurable victory fanfare after the nexus is broken instead of immediately teleporting everyone to the Overworld.
  - Target behavior: freeze/end combat, announce the winning team with a title card, play celebratory effects such as fireworks, wait about 30 seconds by default, and then restore/return players safely.
  - Prevent duplicate nexus-break handling, deaths, disconnects, or a server restart during the fanfare from corrupting match state or restoring players more than once.
- [ ] Add a boss-bar match timer at the top of the screen and keep it synchronized with the authoritative match clock.
- [ ] Add a sidebar scoreboard that lists Team 1 and Team 2 and their participating players.
- [ ] If Minecraft scoreboard constraints allow a readable implementation, show each listed player's live health beside their name.
  - Update efficiently and handle death, respawn cooldown, disconnect, reconnect, and match cleanup.

## Respawn experience

- [ ] During respawn cooldown, make the dead player spectate a living teammate in first-person POV when one is available.
  - Allow cycling through living teammates with spectator controls, preferably left click and right click if reliable; at minimum, provide a reliable one-direction cycle.
  - Handle the viewed teammate dying, disconnecting, or being the last living teammate, and restore the respawning player normally when the cooldown ends.

## Arena preparation

- [ ] Rework the attacker-side arena copy strategy so substantially more surrounding terrain/chunks are brought into War Day instead of relying on terrain generated between a small attacker spawn copy and the defender base.
  - Decide whether this is a configurable radius, a connected claim expansion, or another bounded plan before implementation.
  - Keep `/warday prepare` preview, destination conflict checks, rotation/placement logic, performance limits, and repeatable destination clearing accurate for the enlarged copy area.

## Skills and map privacy

- [ ] Remove the combat and mining skill trees from the shipped gameplay/configuration.
  - Remove their registration and presentation cleanly; migrate or safely ignore existing player data without breaking other skill trees.
- [ ] Investigate and implement the strongest feasible JourneyMap integration/configuration that hides non-teammate player icons while retaining teammate icons.
  - Confirm what can be enforced server-side with the installed JourneyMap version/API. If exact team filtering is impossible, document the limitation and use the safest server-enforceable fallback.

## Anti-grief rule

- [ ] Add a configurable rapid-block-breaking punishment.
  - Default threshold: one player breaks 15 blocks within a rolling 30-second window.
  - Default punishment: apply a 25% mining-speed penalty and Glowing for 60 seconds.
  - Define whether the rule applies only during an active War Day and which modes, dimensions, blocks, operators, or protected/admin actions are exempt.
  - Use per-player rolling-window tracking, avoid counting cancelled break events, clean up expired tracking state, and avoid stacking the penalty incorrectly when retriggered.
  - Provide clear player feedback when the threshold triggers and verify cleanup at match end, logout, and server restart as appropriate.
