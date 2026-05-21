package com.example.fortifications.spellbalance;

import com.example.fortifications.FortificationsMod;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellDamageEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.entity.spells.black_hole.BlackHole;
import io.redspace.ironsspellbooks.entity.spells.portal.PortalEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.lang.reflect.Field;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class SpellCastInterceptor {

    private static final Field EFFECT_DURATION_FIELD = findMobEffectField("duration");

    private SpellCastInterceptor() {}

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (!FortificationsSpellBalance.isBanned(event.getSpellId())) {
            return;
        }

        event.setCanceled(true);
        notifyBanned(event.getEntity());
    }

    @SubscribeEvent
    public static void onSpellOnCast(SpellOnCastEvent event) {
        Double manaMultiplier = SpellBalanceConfig.MANA_MULTIPLIERS.get(event.getSpellId());
        if (manaMultiplier != null) {
            event.setManaCost((int) Math.ceil(event.getManaCost() * manaMultiplier));
        }
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        String spellId = event.getSpell().getSpellId();
        Double cooldownSeconds = SpellBalanceConfig.COOLDOWN_SECONDS.get(spellId);
        if (cooldownSeconds != null) {
            event.setEffectiveCooldown((int) Math.round(cooldownSeconds * 20.0D));
        }
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (event.getSpellDamageSource().spell().getSpellId().equals(FortificationsSpellBalance.GTBC_NULLFLARE)) {
            event.setAmount(event.getAmount() * SpellBalanceConfig.NULLFLARE_DAMAGE_MULTIPLIER);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Black Hole is safer to nerf by radius because the spawned entity exposes a public radius setter.
        if (event.getEntity() instanceof BlackHole blackHole) {
            blackHole.setRadius(blackHole.getRadius() * SpellBalanceConfig.BLACK_HOLE_RADIUS_MULTIPLIER);
        }

        if (event.getEntity() instanceof PortalEntity portalEntity) {
            portalEntity.setTicksToLive(SpellBalanceConfig.PORTAL_DURATION_TICKS);
        }
    }

    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        String effectId = event.getEffectInstance().getEffect().unwrapKey()
                .map(key -> key.location().toString())
                .orElse("");

        if (effectId.equals("irons_spellbooks:true_invisibility")
                && event.getEffectInstance().getDuration() == 100) {
            setEffectDuration(event.getEffectInstance(), SpellBalanceConfig.BLOOD_STEP_TRUE_INVISIBILITY_TICKS);
            return;
        }

        if (effectId.equals("irons_spellbooks:oakskin")) {
            setEffectDuration(event.getEffectInstance(), SpellBalanceConfig.OAKSKIN_DURATION_TICKS);
            return;
        }

        if (effectId.equals("irons_spellbooks:charged")) {
            scaleEffectDuration(event.getEffectInstance(), SpellBalanceConfig.CHARGE_DURATION_MULTIPLIER);
            return;
        }

        if (effectId.equals("irons_spellbooks:hastened")) {
            scaleEffectDuration(event.getEffectInstance(), SpellBalanceConfig.HASTENED_DURATION_MULTIPLIER);
        }
    }

    private static void notifyBanned(Player player) {
        player.displayClientMessage(
                Component.literal("This spell is banned.").withStyle(ChatFormatting.RED),
                true
        );
    }

    private static void scaleEffectDuration(MobEffectInstance effectInstance, double multiplier) {
        setEffectDuration(effectInstance, Math.max(1, (int) Math.round(effectInstance.getDuration() * multiplier)));
    }

    private static void setEffectDuration(MobEffectInstance effectInstance, int durationTicks) {
        try {
            EFFECT_DURATION_FIELD.setInt(effectInstance, durationTicks);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to update mob effect duration", exception);
        }
    }

    private static Field findMobEffectField(String name) {
        try {
            Field field = MobEffectInstance.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Minecraft mob effect field not found: " + name, exception);
        }
    }
}
