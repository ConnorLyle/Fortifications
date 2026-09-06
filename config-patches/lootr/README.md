# Lootr chest protection

Optional configuration counterpart for `FORTIFICATIONS_TODO.md`, **Requested changes**, “Make Lootr chests unbreakable.” Fortifications now enforces the same behavior itself, so deploying these external Lootr settings is unnecessary.

In the active instance/server's `config/lootr-common.toml`, set these existing keys within `[breaking]`:

```toml
disable_break = true
enable_break = false
enable_fake_player_break = false
blast_immune = true
```

Merge these values into the existing file; do not replace the whole configuration with this fragment. Preserve all other options. These keys were confirmed in the local Fortifications Lootr 1.21.1 configurations (1.11.38.123 and 1.11.38.124).

If these settings are used independently, apply them to the dedicated server and distributed client instance and restart the affected instance/server. This configuration is separate from the Fortifications JAR.

Sneaking in Creative retains Lootr's administrative removal exception. Configuration-based protection does not promise immunity to commands or mods that directly replace blocks.

Manual verification: test ordinary/sneak mining in Survival, the Creative exception, TNT/creeper and modded explosions, fake-player machines, two players opening the same chest with independent loot, and persistence after restart. Check that War Day preparation and intentional arena cleanup still function if Lootr containers are present. Keep the queue item unchecked until runtime checks pass.
