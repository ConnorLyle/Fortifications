package com.example.equalbreak;

import com.example.equalbreak.network.TogglePayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod(EqualBreakMod.MOD_ID)
public class EqualBreakMod {

    public static final String MOD_ID = "equalbreak";

    /**
     * Set of player UUIDs that currently have Equal Break toggled on personally.
     * Managed server-side; synced from client via packet.
     */
    public static final Set<UUID> TOGGLED_PLAYERS = Collections.synchronizedSet(new HashSet<>());

    /**
     * When true, Equal Break is active for ALL players regardless of their
     * personal toggle. Flipped by the /equalbreak all command (OP level 2).
     */
    public static volatile boolean GLOBAL_ACTIVE = false;

    public EqualBreakMod(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                TogglePayload.TYPE,
                TogglePayload.STREAM_CODEC,
                TogglePayload::handle
        );
    }
}
