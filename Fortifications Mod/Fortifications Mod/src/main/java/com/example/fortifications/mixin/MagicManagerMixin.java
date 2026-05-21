package com.example.fortifications.mixin;

import com.example.fortifications.spellbalance.SpellCastInterceptor;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.network.casting.SyncCooldownPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MagicManager.class, remap = false)
public abstract class MagicManagerMixin {

    @Inject(method = "addCooldown", at = @At("HEAD"), cancellable = true, remap = false)
    private void fortifications$addBalancedCooldown(ServerPlayer player, AbstractSpell spell, CastSource castSource, CallbackInfo ci) {
        int cooldownTicks = SpellCastInterceptor.getBalancedCooldownTicks(player, spell.getSpellId());
        if (cooldownTicks < 0) {
            return;
        }

        SpellCooldownAddedEvent.Pre preEvent = NeoForge.EVENT_BUS.post(
                new SpellCooldownAddedEvent.Pre(cooldownTicks, spell, player, castSource)
        );
        if (castSource == CastSource.SCROLL || preEvent.isCanceled()) {
            ci.cancel();
            return;
        }

        int effectiveCooldown = preEvent.getEffectiveCooldown();
        MagicData.getPlayerMagicData(player).getPlayerCooldowns().addCooldown(spell, effectiveCooldown);
        PacketDistributor.sendToPlayer(
                player,
                new SyncCooldownPacket(spell.getSpellId(), effectiveCooldown),
                new CustomPacketPayload[0]
        );
        NeoForge.EVENT_BUS.post(new SpellCooldownAddedEvent.Post(effectiveCooldown, spell, player, castSource));
        ci.cancel();
    }
}
