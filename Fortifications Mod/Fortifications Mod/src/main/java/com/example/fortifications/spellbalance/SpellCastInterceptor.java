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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = FortificationsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class SpellCastInterceptor {

    private static final Field EFFECT_DURATION_FIELD = findMobEffectField("duration");
    private static final Map<CastKey, Integer> RECENT_PLAYER_CAST_LEVELS = new ConcurrentHashMap<>();

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
        String spellId = event.getSpellId();
        int spellLevel = event.getSpellLevel();
        RECENT_PLAYER_CAST_LEVELS.put(new CastKey(event.getEntity().getUUID(), spellId), spellLevel);

        int[] manaCosts = SpellBalanceConfig.MANA_COSTS.get(spellId);
        if (manaCosts != null) {
            event.setManaCost(intForLevel(manaCosts, spellLevel));
        }
    }

    @SubscribeEvent
    public static void onSpellCooldownAdded(SpellCooldownAddedEvent.Pre event) {
        String spellId = event.getSpell().getSpellId();
        double[] cooldownSeconds = SpellBalanceConfig.COOLDOWN_SECONDS_BY_LEVEL.get(spellId);
        if (cooldownSeconds != null) {
            int spellLevel = RECENT_PLAYER_CAST_LEVELS.getOrDefault(new CastKey(event.getEntity().getUUID(), spellId), 1);
            event.setEffectiveCooldown((int) Math.round(doubleForLevel(cooldownSeconds, spellLevel) * 20.0D));
        }
    }

    @SubscribeEvent
    public static void onSpellDamage(SpellDamageEvent event) {
        if (event.getSpellDamageSource().spell().getSpellId().equals(FortificationsSpellBalance.GTBC_NULLFLARE)) {
            int spellLevel = event.getSpellDamageSource().getEntity() == null
                    ? 1
                    : RECENT_PLAYER_CAST_LEVELS.getOrDefault(
                            new CastKey(event.getSpellDamageSource().getEntity().getUUID(), FortificationsSpellBalance.GTBC_NULLFLARE),
                            1
                    );
            event.setAmount(floatForLevel(new float[] {7.0F, 10.0F, 13.0F, 16.0F, 20.0F}, spellLevel));
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Black Hole is safer to nerf by radius because the spawned entity exposes a public radius setter.
        if (event.getEntity() instanceof BlackHole blackHole) {
            blackHole.setRadius(blackHoleRadiusForVanillaRadius(blackHole.getRadius()));
            blackHole.setDuration(SpellBalanceConfig.BLACK_HOLE_DURATION_TICKS);
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
            setEffectDuration(event.getEffectInstance(), intForLevel(new int[] {15 * 20, 20 * 20, 25 * 20}, event.getEffectInstance().getAmplifier() + 1));
            return;
        }

        if (effectId.equals("irons_spellbooks:hastened")) {
            setEffectDuration(event.getEffectInstance(), SpellBalanceConfig.HASTENED_DURATION_TICKS);
        }
    }

    private static void notifyBanned(Player player) {
        player.displayClientMessage(
                Component.literal("This spell is banned.").withStyle(ChatFormatting.RED),
                true
        );
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

    private static int intForLevel(int[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private static float floatForLevel(float[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private static double doubleForLevel(double[] values, int spellLevel) {
        return values[Math.max(0, Math.min(values.length - 1, spellLevel - 1))];
    }

    private static float blackHoleRadiusForVanillaRadius(float vanillaRadius) {
        int spellLevel = Math.max(1, Math.min(6, Math.round((vanillaRadius - 4.125F) / 2.0F)));
        return floatForLevel(new float[] {2.0F, 4.0F, 5.0F, 6.0F, 7.0F, 8.0F}, spellLevel);
    }

    private record CastKey(UUID playerId, String spellId) {}
}
