package com.example.fortifications.mixin;

import com.example.fortifications.spellbalance.SpellCastInterceptor;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = TooltipsUtils.class, remap = false)
public abstract class TooltipsUtilsMixin {

    @Inject(method = "formatScrollTooltip", at = @At("RETURN"), cancellable = true, remap = false)
    private static void fortifications$formatScrollTooltip(ItemStack stack, Player player, CallbackInfoReturnable<List<Component>> cir) {
        if (!ISpellContainer.isSpellContainer(stack)) {
            return;
        }

        ISpellContainer container = ISpellContainer.get(stack);
        if (container.isEmpty()) {
            return;
        }

        SpellData spellData = container.getSpellAtIndex(0);
        int spellLevel = spellData.getSpell().getLevelFor(spellData.getLevel(), player);
        replaceCooldownLine(cir.getReturnValue(), spellData.getSpell(), spellLevel);
    }

    @Inject(method = "formatActiveSpellTooltip", at = @At("RETURN"), cancellable = true, remap = false)
    private static void fortifications$formatActiveSpellTooltip(
            ItemStack stack,
            SpellData spellData,
            CastSource castSource,
            LocalPlayer player,
            CallbackInfoReturnable<List<Component>> cir
    ) {
        int spellLevel = spellData.getSpell().getLevelFor(spellData.getLevel(), player);
        replaceCooldownLine(cir.getReturnValue(), spellData.getSpell(), spellLevel);
    }

    private static void replaceCooldownLine(List<Component> tooltip, AbstractSpell spell, int spellLevel) {
        int cooldownTicks = SpellCastInterceptor.getBalancedCooldownTicks(spell.getSpellId(), spellLevel);
        if (cooldownTicks < 0) {
            return;
        }

        Component cooldownComponent = Component.translatable(
                "tooltip.irons_spellbooks.cooldown_length_seconds",
                Utils.timeFromTicks(cooldownTicks, 2)
        ).withStyle(ChatFormatting.BLUE);

        for (int index = 0; index < tooltip.size(); index++) {
            if (tooltip.get(index).getString().contains("Cooldown")) {
                tooltip.set(index, cooldownComponent);
                return;
            }
        }
    }
}
