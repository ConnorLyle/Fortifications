package com.example.fortifications;

import com.trove.warday.WarDayState;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

final class ClassResetService {
    private static final ResourceLocation CLASS_CATEGORY =
            ResourceLocation.fromNamespaceAndPath("fortifications_classes", "example");
    private static final String SUPPRESS_NEXT_CLASS_KIT = "FortificationsSuppressNextClassKit";
    private static final String MONK_TAG = "is_monk";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    private static final Map<UUID, PendingInventoryRestore> PENDING_INVENTORY_RESTORES = new HashMap<>();

    private ClassResetService() {
    }

    static void initialize() {
        if (INITIALIZED.compareAndSet(false, true)) {
            SkillsAPI.registerSkillUnlockEvent(ClassResetService::onSkillUnlocked);
            NeoForge.EVENT_BUS.addListener(ClassResetService::onServerTick);
            NeoForge.EVENT_BUS.addListener(ClassResetService::onServerStopping);
        }
    }

    static Validation validate(ServerPlayer player) {
        WarDayState state = WarDayState.get(player.getServer());
        if (ClassResetPolicy.blocksForWarDay(
                state.isActive(), state.savedPlayers().containsKey(player.getUUID()))) {
            return Validation.WARDAY_BLOCKED;
        }

        Optional<Category> category = SkillsAPI.getCategory(CLASS_CATEGORY);
        if (category.isEmpty()) {
            return Validation.MISSING_CLASS_TREE;
        }
        if (category.get().streamUnlockedSkills(player).findAny().isEmpty()) {
            return Validation.NO_CLASS;
        }
        if (!hasResetToken(player)) {
            return Validation.NO_TOKEN;
        }
        return Validation.READY;
    }

    static void resetClass(ServerPlayer player) {
        Validation validation = validate(player);
        if (validation != Validation.READY) {
            player.closeContainer();
            sendValidationMessage(player, validation);
            return;
        }

        Category category = SkillsAPI.getCategory(CLASS_CATEGORY).orElseThrow();
        player.closeContainer();
        category.resetSkills(player);
        player.removeTag(MONK_TAG);
        player.getPersistentData().putBoolean(SUPPRESS_NEXT_CLASS_KIT, true);
        consumeResetToken(player);
        player.displayClientMessage(
                Component.translatable("message.fortifications.class_reset.success"), false);
        category.openScreen(player);
    }

    static void sendValidationMessage(ServerPlayer player, Validation validation) {
        String translationKey = switch (validation) {
            case NO_CLASS -> "message.fortifications.class_reset.no_class";
            case MISSING_CLASS_TREE -> "message.fortifications.class_reset.missing_tree";
            case WARDAY_BLOCKED -> "message.fortifications.class_reset.warday_blocked";
            case NO_TOKEN -> "message.fortifications.class_reset.no_token";
            case READY -> null;
        };
        if (translationKey != null) {
            player.displayClientMessage(Component.translatable(translationKey), false);
        }
    }

    private static boolean hasResetToken(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(FortificationsMod.CLASS_RESET_TOKEN.get())) {
                return true;
            }
        }
        return false;
    }

    private static void consumeResetToken(ServerPlayer player) {
        if (player.isCreative()) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(FortificationsMod.CLASS_RESET_TOKEN.get())) {
                stack.shrink(1);
                player.getInventory().setChanged();
                return;
            }
        }
    }

    private static void onSkillUnlocked(ServerPlayer player, ResourceLocation categoryId, String skillId) {
        boolean pending = player.getPersistentData().getBoolean(SUPPRESS_NEXT_CLASS_KIT);
        if (!ClassResetPolicy.shouldSuppressStarterKit(
                pending, categoryId.getNamespace(), categoryId.getPath())) {
            return;
        }

        player.getPersistentData().remove(SUPPRESS_NEXT_CLASS_KIT);
        ListTag inventory = player.getInventory().save(new ListTag());
        int selectedSlot = player.getInventory().selected;
        ItemStack carried = player.inventoryMenu.getCarried().copy();
        List<MobEffectInstance> effects = player.getActiveEffects().stream()
                .map(MobEffectInstance::new)
                .toList();

        PENDING_INVENTORY_RESTORES.put(player.getUUID(), new PendingInventoryRestore(
                player, inventory, selectedSlot, carried, effects));
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        restorePendingInventories(event.getServer());
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        restorePendingInventories(event.getServer());
        PENDING_INVENTORY_RESTORES.clear();
    }

    private static void restorePendingInventories(MinecraftServer server) {
        PENDING_INVENTORY_RESTORES.entrySet().removeIf(entry -> {
            PendingInventoryRestore restore = entry.getValue();
            if (restore.player().getServer() != server) {
                return false;
            }
            restore.apply();
            return true;
        });
    }

    private record PendingInventoryRestore(
            ServerPlayer player,
            ListTag inventory,
            int selectedSlot,
            ItemStack carried,
            List<MobEffectInstance> effects
    ) {
        private void apply() {
            player.getInventory().load(inventory);
            player.getInventory().selected = Math.max(0, Math.min(8, selectedSlot));
            player.inventoryMenu.setCarried(carried);
            player.removeAllEffects();
            effects.forEach(effect -> player.addEffect(new MobEffectInstance(effect)));
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastFullState();
        }
    }

    enum Validation {
        READY,
        NO_CLASS,
        MISSING_CLASS_TREE,
        WARDAY_BLOCKED,
        NO_TOKEN
    }
}
