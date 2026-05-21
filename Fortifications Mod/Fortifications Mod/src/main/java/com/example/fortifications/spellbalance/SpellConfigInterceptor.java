package com.example.fortifications.spellbalance;

import com.example.fortifications.FortificationsMod;
import io.redspace.ironsspellbooks.api.config.ModifyDefaultConfigValuesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class SpellConfigInterceptor {

    private SpellConfigInterceptor() {}

    @SubscribeEvent
    public static void onModifyDefaultConfigValues(ModifyDefaultConfigValuesEvent event) {
        FortificationsSpellBalance.applyDefaultConfigOverrides(event);
    }
}
