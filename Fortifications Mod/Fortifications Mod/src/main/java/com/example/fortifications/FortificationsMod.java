package com.example.fortifications;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FortificationsMod.MOD_ID)
public class FortificationsMod {

    public static final String MOD_ID = "fortifications";
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, MOD_ID);
    public static final DeferredHolder<Attribute, Attribute> UNARMED_DAMAGE = ATTRIBUTES.register(
            "unarmed_damage",
            () -> new RangedAttribute("attribute.name.fortifications.unarmed_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true)
    );

    /**
     * When true, Fortifications mode is active for ALL players.
     * Flipped by the /fortifications all command (OP level 2).
     */
    public static volatile boolean GLOBAL_ACTIVE = false;

    public FortificationsMod(IEventBus modEventBus, ModContainer container) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.register(this);
    }

    @SubscribeEvent
    public void onEntityAttributeModification(net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent event) {
        event.add(net.minecraft.world.entity.EntityType.PLAYER, UNARMED_DAMAGE, 0.0D);
    }
}
