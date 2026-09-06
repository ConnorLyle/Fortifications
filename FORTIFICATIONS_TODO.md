# Fortifications balance and crash-prevention backlog

User-provided queue, recorded 2026-09-06. Preserve these requests and update this file as work proceeds. The existing War Day queue remains in [WARDAY_TODO.md](WARDAY_TODO.md).

## Workflow

- Before implementing each item, inspect the installed mod version and relevant code/configuration, then present a write-up explaining whether and how it is possible, approaches and tradeoffs, edge cases, dependencies, the recommended scope, and verification.
- Adding this queue is not authorization to implement all items. When asked only to discuss an item, stop after its assessment.
- Keep requests unchecked until implemented and functionally verified. Record partial implementation, build evidence, and remaining manual tests beneath the item.
- On completion use `- [x] ~~Original task text~~` and add a concise verification note. Preserve completed entries as history.
- Confirm exact registry IDs and ability/rank meanings during assessment; names and IDs below preserve the user's intent and may contain spelling errors. Assess how bans affect existing items as well as new acquisition; do not assume recipe removal alone enforces a ban.
- Follow AGENTS.md for combined-project builds, distributable refreshes, and HANDOFF.md maintenance. Keep task status here rather than duplicating this checklist elsewhere.

## Requested changes

- [ ] Make Lootr chests unbreakable.
  - Implemented 2026-09-06 inside Fortifications after the user requested mod-level enforcement. Lootr chests, trapped chests, barrels, converted inventories, shulkers, and decorated pots reject player and fake-player breaking and are removed from every explosion's affected-block list. A real player sneaking in Creative retains the administrative removal exception. Lootr suspicious sand/gravel and trophies are intentionally unaffected; vanilla containers are unaffected. The implementation uses registry IDs and has no hard Lootr class dependency, so Fortifications can load without Lootr.
  - Automated verification: installed Lootr 1.11.38.124 archive inspection confirmed the exact six protected registry paths. `LootrProtectionPolicyTest` covers every protected/unprotected ID and the Survival, Creative, sneaking, and fake-player decisions. `.\gradlew.bat build --no-problems-report` succeeded with all focused checks on 2026-09-06. The refreshed artifact/distributable are 4,151,338 bytes with SHA-256 `F307E6831E178724FC3168EA8FF65B474ECF5535C06EBE5859FFD26C7AE55DDC`.
  - Manual verification remains: test Survival and Creative mining, the sneaking-Creative exception, fake-player breakers, TNT/creepers/modded explosions, all six protected container types, independent two-player loot, restart, intentional War Day arena clearing, and Fortifications startup without Lootr. The item stays unchecked until those runtime checks pass.
- [ ] Ban `reliquified_irons_spells_and_spellbooks:sinner_crown` for crashing.
  - Implemented 2026-09-06 using the confirmed registry ID from Reliquified Iron's Spells and Spellbooks 0.2.7. The crown is in Fortifications' central banned-item registry, so dropped copies are discarded, pickup/use is denied, and copies in inventory, armor, offhand, or Curios slots are deleted with player feedback.
  - Crash protection was strengthened for this and existing bans: `CurioCanEquipEvent` now rejects banned stacks before Curios activation, and server-side player cleanup runs at highest priority every tick so an already-equipped crown is removed at the first player tick after login.
  - Automated verification: `BannedItemRegistryTest` confirms the exact Sinner Crown ID is banned while nearby/unrelated IDs remain allowed. `.\gradlew.bat build --no-problems-report` succeeded with all focused checks on 2026-09-06. The artifact and refreshed distributable are 4,151,459 bytes with SHA-256 `A59BE2DD813D88BEE7346783B1595FC691717628866EA7EC4FCD30403A1C0C1E`.
  - Manual verification remains: test `/give`, ground pickup, normal and automated Curios insertion, crafting/loot acquisition, login with the crown already equipped, reconnect, and server restart while confirming no crown behavior or crash occurs. The item stays unchecked until runtime verification passes.
- [ ] Remove the shield function from the umbrella; if that is not feasible, ban it.
  - Confirm the exact umbrella item and isolate its blocking function during assessment.
- [ ] Ban all sealed swords for crashing.
  - Identify every applicable sword variant during assessment.
- [ ] Re-add the Experience Disperser.
  - Coordinate with the following balance changes before restoring availability.
- [ ] Change `relics:experience_disperer` experience dispersion from min 5%, max 100% to min 5%, max 50%, and remove its rank 3 ability.
  - Registry ID is preserved as supplied; verify its spelling before implementation.
- [ ] Change `irons_spellbooks:dead_king_phylactery` from requiring 8 shards to 4.
- [ ] For `relics:glitchy_mantle`, entirely disable Glitchy Copy and Surface Tear, and nerf Perception Glitch from min 10%, max 50% to min 5%, max 25%.
- [ ] Ban Ghostly Mantle.
- [ ] For `relics:shield_of_retaliation`, change the highest-level shield cooldown from 0.25 seconds to 0.75 seconds; change the rank 1 maximum duration increase from 2.5 seconds to 0.25 seconds and maximum damage increase from 250% to 25%.
- [ ] Ban Scroll of the Enraged Dead King.
