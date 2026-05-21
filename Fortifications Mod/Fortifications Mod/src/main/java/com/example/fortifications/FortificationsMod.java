package com.example.fortifications;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(FortificationsMod.MOD_ID)
public class FortificationsMod {

    public static final String MOD_ID = "fortifications";

    /**
     * When true, Fortifications mode is active for ALL players.
     * Flipped by the /fortifications all command (OP level 2).
     */
    public static volatile boolean GLOBAL_ACTIVE = false;

    public FortificationsMod(IEventBus modEventBus, ModContainer container) {}
}
