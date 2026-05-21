package com.example.fortifications.events;

import com.example.fortifications.FortificationsMod;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class UnarmedDamageHandler {
    private UnarmedDamageHandler() {
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        if (!player.getMainHandItem().isEmpty()) {
            return;
        }

        double bonusDamage = player.getAttributeValue(FortificationsMod.UNARMED_DAMAGE);
        if (bonusDamage <= 0.0D) {
            return;
        }

        event.setAmount(event.getAmount() + (float) bonusDamage);
    }
}
