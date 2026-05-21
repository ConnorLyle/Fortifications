package com.example.fortifications.events;

import com.example.fortifications.FortificationsMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class RelicsEffectHandler {

    private static final String RELICS_STUN_EFFECT = "relics:stun";

    private RelicsEffectHandler() {}

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (isRelicsStun(event.getEffectInstance())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (isRelicsStun(event.getEffectInstance())) {
            event.getEntity().removeEffect(event.getEffectInstance().getEffect());
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }

        MobEffectInstance stunEffect = livingEntity.getActiveEffects().stream()
                .filter(RelicsEffectHandler::isRelicsStun)
                .findFirst()
                .orElse(null);

        if (stunEffect != null) {
            livingEntity.removeEffect(stunEffect.getEffect());
        }
    }

    private static boolean isRelicsStun(MobEffectInstance effectInstance) {
        return effectInstance.getEffect().unwrapKey()
                .map(key -> key.location().toString())
                .orElse("")
                .equals(RELICS_STUN_EFFECT);
    }
}
