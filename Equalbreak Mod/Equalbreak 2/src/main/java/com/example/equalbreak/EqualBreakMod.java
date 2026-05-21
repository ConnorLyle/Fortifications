package com.example.equalbreak;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(EqualBreakMod.MOD_ID)
public class EqualBreakMod {

    public static final String MOD_ID = "equalbreak";

    /**
     * When true, Equal Break is active for ALL players.
     * Flipped by the /equalbreak all command (OP level 2).
     */
    public static volatile boolean GLOBAL_ACTIVE = false;

    public EqualBreakMod(IEventBus modEventBus, ModContainer container) {}
}
