package com.example.fortifications;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class FortChestTeamStorage {
    private static final String TEAM_DATA_KEY = "FortificationsFortChestItems";
    private static final int INVENTORY_SIZE = 27;
    private static final Map<UUID, TeamInventory> INVENTORIES = new HashMap<>();
    private static MinecraftServer activeServer;

    private FortChestTeamStorage() {
    }

    public static Optional<TeamInventory> inventory(MinecraftServer server, UUID teamId) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return Optional.empty();
        }
        if (activeServer != server) {
            INVENTORIES.clear();
            activeServer = server;
        }
        Team team = FTBTeamsAPI.api().getManager().getTeamByID(teamId).orElse(null);
        if (team == null || !team.isValid()) {
            INVENTORIES.remove(teamId);
            return Optional.empty();
        }
        return Optional.of(INVENTORIES.computeIfAbsent(
                teamId, ignored -> new TeamInventory(server, team)));
    }

    public static void clear(MinecraftServer server) {
        if (activeServer == server) {
            INVENTORIES.clear();
            activeServer = null;
        }
    }

    public static final class TeamInventory extends SimpleContainer {
        private final MinecraftServer server;
        private final Team team;
        private boolean loading;

        private TeamInventory(MinecraftServer server, Team team) {
            super(INVENTORY_SIZE);
            this.server = server;
            this.team = team;
            CompoundTag extraData = team.getExtraData();
            if (extraData.contains(TEAM_DATA_KEY, Tag.TAG_LIST)) {
                loading = true;
                try {
                    fromTag(extraData.getList(TEAM_DATA_KEY, Tag.TAG_COMPOUND), server.registryAccess());
                } finally {
                    loading = false;
                }
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) {
                persist();
            }
        }

        public void persist() {
            team.getExtraData().put(TEAM_DATA_KEY, createTag(server.registryAccess()));
            team.markDirty();
        }
    }
}
