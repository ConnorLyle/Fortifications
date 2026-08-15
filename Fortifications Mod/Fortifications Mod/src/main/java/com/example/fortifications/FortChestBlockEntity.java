package com.example.fortifications;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.UUID;

public class FortChestBlockEntity extends ChestBlockEntity {
    private static final String OWNER_TEAM_ID_TAG = "OwnerTeamId";
    private static final String OWNER_TEAM_NAME_TAG = "OwnerTeamName";
    private NonNullList<ItemStack> localItems = NonNullList.withSize(27, ItemStack.EMPTY);
    private UUID ownerTeamId;
    private String ownerTeamName = "";
    private boolean useLocalItems;
    private boolean detachedFromSharedInventory;

    public FortChestBlockEntity(BlockPos pos, BlockState state) {
        super(FortificationsMod.FORT_CHEST_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fortifications.fort_chest");
    }

    public boolean bindToPlayerTeam(ServerPlayer player) {
        Optional<Team> playerTeam = playerTeam(player);
        if (playerTeam.isEmpty()) {
            return false;
        }
        if (ownerTeamId == null) {
            Team team = playerTeam.get();
            ownerTeamId = team.getId();
            ownerTeamName = team.getName().getString();
            migrateLegacyItems(player);
            setChanged();
        }
        return FortChestTeamAccess.canAccess(ownerTeamId, playerTeam.get().getId());
    }

    public boolean canAccess(ServerPlayer player) {
        UUID playerTeamId = playerTeam(player).map(Team::getId).orElse(null);
        return FortChestTeamAccess.canAccess(ownerTeamId, playerTeamId);
    }

    public boolean isBound() {
        return ownerTeamId != null;
    }

    public String ownerTeamName() {
        if (ownerTeamId != null && FTBTeamsAPI.api().isManagerLoaded()) {
            return FTBTeamsAPI.api().getManager().getTeamByID(ownerTeamId)
                    .map(team -> team.getName().getString())
                    .orElse(ownerTeamName);
        }
        return ownerTeamName;
    }

    public void detachFromSharedInventory() {
        detachedFromSharedInventory = true;
        localItems = NonNullList.withSize(27, ItemStack.EMPTY);
    }

    @Override
    public boolean canOpen(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && canAccess(serverPlayer)
                && super.canOpen(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && canAccess(serverPlayer)
                && super.stillValid(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItem(Container destination, int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!useLocalItems && !detachedFromSharedInventory) {
            sharedInventory().ifPresent(FortChestTeamStorage.TeamInventory::persist);
        }
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        if (useLocalItems || detachedFromSharedInventory) {
            return localItems;
        }
        return sharedInventory()
                .map(FortChestTeamStorage.TeamInventory::getItems)
                .orElse(localItems);
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        if (useLocalItems || detachedFromSharedInventory) {
            localItems = items;
            return;
        }
        Optional<FortChestTeamStorage.TeamInventory> shared = sharedInventory();
        if (shared.isEmpty()) {
            localItems = items;
            return;
        }
        NonNullList<ItemStack> target = shared.get().getItems();
        for (int slot = 0; slot < target.size(); slot++) {
            target.set(slot, slot < items.size() ? items.get(slot) : ItemStack.EMPTY);
        }
        shared.get().setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        ownerTeamId = tag.hasUUID(OWNER_TEAM_ID_TAG) ? tag.getUUID(OWNER_TEAM_ID_TAG) : null;
        ownerTeamName = tag.getString(OWNER_TEAM_NAME_TAG);
        useLocalItems = true;
        try {
            super.loadAdditional(tag, provider);
        } finally {
            useLocalItems = false;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        useLocalItems = true;
        try {
            super.saveAdditional(tag, provider);
        } finally {
            useLocalItems = false;
        }
        if (ownerTeamId != null) {
            tag.putUUID(OWNER_TEAM_ID_TAG, ownerTeamId);
            tag.putString(OWNER_TEAM_NAME_TAG, ownerTeamName);
            tag.remove("Items");
        }
    }

    private Optional<FortChestTeamStorage.TeamInventory> sharedInventory() {
        if (ownerTeamId == null || !(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        return FortChestTeamStorage.inventory(serverLevel.getServer(), ownerTeamId);
    }

    private static Optional<Team> playerTeam(ServerPlayer player) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return Optional.empty();
        }
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player);
    }

    private void migrateLegacyItems(ServerPlayer player) {
        if (localItems.stream().allMatch(ItemStack::isEmpty)) {
            return;
        }
        sharedInventory().ifPresent(shared -> {
            for (int slot = 0; slot < localItems.size(); slot++) {
                ItemStack stack = localItems.get(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack remainder = shared.addItem(stack.copy());
                if (!remainder.isEmpty()) {
                    player.drop(remainder, false);
                }
                localItems.set(slot, ItemStack.EMPTY);
            }
            shared.persist();
        });
    }
}
