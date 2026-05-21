package com.trove.warday;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WarDayState extends SavedData {
    private static final String DATA_NAME = WarDayMod.MODID + "_state";

    private boolean prepared;
    private String warDayDimension = "warday:war_day";
    private BlockPos copiedNexusPos;
    private BlockPos attackerSpawnPos;
    private String defenderTeam = "";
    private String attackerTeam = "";
    private boolean active;
    private final Map<UUID, GameType> savedGameModes = new HashMap<>();

    public static WarDayState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WarDayState::new, WarDayState::load, null),
                DATA_NAME
        );
    }

    private static WarDayState load(CompoundTag tag, HolderLookup.Provider provider) {
        WarDayState state = new WarDayState();
        state.prepared = tag.getBoolean("Prepared");
        state.warDayDimension = tag.getString("WarDayDimension");
        state.defenderTeam = tag.getString("DefenderTeam");
        state.attackerTeam = tag.getString("AttackerTeam");
        if (tag.contains("CopiedNexusPos")) {
            state.copiedNexusPos = BlockPos.of(tag.getLong("CopiedNexusPos"));
        }
        if (tag.contains("AttackerSpawnPos")) {
            state.attackerSpawnPos = BlockPos.of(tag.getLong("AttackerSpawnPos"));
        }
        state.active = tag.getBoolean("Active");
        ListTag modes = tag.getList("SavedGameModes", 10);
        for (int i = 0; i < modes.size(); i++) {
            CompoundTag modeTag = modes.getCompound(i);
            if (modeTag.hasUUID("Player")) {
                state.savedGameModes.put(modeTag.getUUID("Player"), GameType.byName(modeTag.getString("Mode"), GameType.SURVIVAL));
            }
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("Prepared", prepared);
        tag.putString("WarDayDimension", warDayDimension);
        tag.putString("DefenderTeam", defenderTeam);
        tag.putString("AttackerTeam", attackerTeam);
        if (copiedNexusPos != null) {
            tag.putLong("CopiedNexusPos", copiedNexusPos.asLong());
        }
        if (attackerSpawnPos != null) {
            tag.putLong("AttackerSpawnPos", attackerSpawnPos.asLong());
        }
        tag.putBoolean("Active", active);
        ListTag modes = new ListTag();
        savedGameModes.forEach((uuid, gameType) -> {
            CompoundTag modeTag = new CompoundTag();
            modeTag.putUUID("Player", uuid);
            modeTag.putString("Mode", gameType.getName());
            modes.add(modeTag);
        });
        tag.put("SavedGameModes", modes);
        return tag;
    }

    public void markPrepared(String dimension, String defenderTeam, String attackerTeam, BlockPos copiedNexusPos, BlockPos attackerSpawnPos) {
        this.prepared = true;
        this.warDayDimension = dimension;
        this.defenderTeam = defenderTeam;
        this.attackerTeam = attackerTeam;
        this.copiedNexusPos = copiedNexusPos;
        this.attackerSpawnPos = attackerSpawnPos;
        setDirty();
    }

    public void start(Map<UUID, GameType> gameModes) {
        active = true;
        savedGameModes.clear();
        savedGameModes.putAll(gameModes);
        setDirty();
    }

    public Map<UUID, GameType> savedGameModes() {
        return Map.copyOf(savedGameModes);
    }

    public void end() {
        active = false;
        savedGameModes.clear();
        setDirty();
    }

    public boolean isPrepared() {
        return prepared;
    }

    public boolean isActive() {
        return active;
    }

    public String warDayDimension() {
        return warDayDimension;
    }

    public Optional<BlockPos> copiedNexusPos() {
        return Optional.ofNullable(copiedNexusPos);
    }

    public Optional<BlockPos> attackerSpawnPos() {
        return Optional.ofNullable(attackerSpawnPos);
    }

    public String defenderTeam() {
        return defenderTeam;
    }

    public String attackerTeam() {
        return attackerTeam;
    }

    public boolean dimensionExists(MinecraftServer server) {
        try {
            ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.parse(warDayDimension));
            return server.getLevel(key) != null;
        } catch (Exception ignored) {
            return false;
        }
    }
}
