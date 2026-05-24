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
    private final Map<UUID, PlayerSnapshot> savedPlayers = new HashMap<>();

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
        ListTag players = tag.getList("SavedPlayers", 10);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag playerTag = players.getCompound(i);
            if (playerTag.hasUUID("Player")) {
                state.savedPlayers.put(playerTag.getUUID("Player"), PlayerSnapshot.load(playerTag));
            }
        }

        ListTag modes = tag.getList("SavedGameModes", 10);
        for (int i = 0; i < modes.size(); i++) {
            CompoundTag modeTag = modes.getCompound(i);
            if (modeTag.hasUUID("Player")) {
                state.savedPlayers.putIfAbsent(
                        modeTag.getUUID("Player"),
                        new PlayerSnapshot(GameType.byName(modeTag.getString("Mode"), GameType.SURVIVAL), "", 0.0D, 0.0D, 0.0D, 0.0F, 0.0F)
                );
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
        ListTag players = new ListTag();
        savedPlayers.forEach((uuid, snapshot) -> {
            CompoundTag playerTag = snapshot.save();
            playerTag.putUUID("Player", uuid);
            players.add(playerTag);
        });
        tag.put("SavedPlayers", players);
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

    public void start(Map<UUID, PlayerSnapshot> players) {
        active = true;
        savedPlayers.clear();
        savedPlayers.putAll(players);
        setDirty();
    }

    public Map<UUID, PlayerSnapshot> savedPlayers() {
        return Map.copyOf(savedPlayers);
    }

    public void end() {
        active = false;
        savedPlayers.clear();
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

    public record PlayerSnapshot(GameType gameMode, String dimension, double x, double y, double z, float yRot, float xRot) {
        private static PlayerSnapshot load(CompoundTag tag) {
            return new PlayerSnapshot(
                    GameType.byName(tag.getString("Mode"), GameType.SURVIVAL),
                    tag.getString("Dimension"),
                    tag.getDouble("X"),
                    tag.getDouble("Y"),
                    tag.getDouble("Z"),
                    tag.getFloat("YRot"),
                    tag.getFloat("XRot")
            );
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Mode", gameMode.getName());
            tag.putString("Dimension", dimension);
            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);
            tag.putFloat("YRot", yRot);
            tag.putFloat("XRot", xRot);
            return tag;
        }
    }
}
