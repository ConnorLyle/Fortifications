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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private long matchEndGameTime;
    private boolean originalKeepInventory;
    private boolean keepInventoryCaptured;
    private double originalWorldBorderCenterX;
    private double originalWorldBorderCenterZ;
    private double originalWorldBorderSize;
    private boolean worldBorderCaptured;
    private UUID nexusMarkerId;
    private final Map<UUID, PlayerSnapshot> savedPlayers = new HashMap<>();
    private final Set<UUID> defenderParticipants = new HashSet<>();
    private final Set<UUID> attackerParticipants = new HashSet<>();
    private final Map<UUID, Integer> deathCounts = new HashMap<>();
    private final Map<UUID, Integer> pendingRespawnTicks = new HashMap<>();

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
        state.matchEndGameTime = tag.getLong("MatchEndGameTime");
        state.originalKeepInventory = tag.getBoolean("OriginalKeepInventory");
        state.keepInventoryCaptured = tag.getBoolean("KeepInventoryCaptured");
        state.originalWorldBorderCenterX = tag.getDouble("OriginalWorldBorderCenterX");
        state.originalWorldBorderCenterZ = tag.getDouble("OriginalWorldBorderCenterZ");
        state.originalWorldBorderSize = tag.getDouble("OriginalWorldBorderSize");
        state.worldBorderCaptured = tag.getBoolean("WorldBorderCaptured");
        if (tag.hasUUID("NexusMarkerId")) {
            state.nexusMarkerId = tag.getUUID("NexusMarkerId");
        }
        loadUuidSet(tag.getList("DefenderParticipants", 10), state.defenderParticipants);
        loadUuidSet(tag.getList("AttackerParticipants", 10), state.attackerParticipants);
        loadUuidIntMap(tag.getList("DeathCounts", 10), "Deaths", state.deathCounts);
        loadUuidIntMap(tag.getList("PendingRespawns", 10), "Ticks", state.pendingRespawnTicks);

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
                        new PlayerSnapshot(
                                GameType.byName(modeTag.getString("Mode"), GameType.SURVIVAL),
                                "",
                                0.0D,
                                0.0D,
                                0.0D,
                                0.0F,
                                0.0F,
                                "",
                                false,
                                0,
                                0,
                                0,
                                0.0F,
                                false
                        )
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
        tag.putLong("MatchEndGameTime", matchEndGameTime);
        tag.putBoolean("OriginalKeepInventory", originalKeepInventory);
        tag.putBoolean("KeepInventoryCaptured", keepInventoryCaptured);
        tag.putDouble("OriginalWorldBorderCenterX", originalWorldBorderCenterX);
        tag.putDouble("OriginalWorldBorderCenterZ", originalWorldBorderCenterZ);
        tag.putDouble("OriginalWorldBorderSize", originalWorldBorderSize);
        tag.putBoolean("WorldBorderCaptured", worldBorderCaptured);
        if (nexusMarkerId != null) {
            tag.putUUID("NexusMarkerId", nexusMarkerId);
        }
        tag.put("DefenderParticipants", saveUuidSet(defenderParticipants));
        tag.put("AttackerParticipants", saveUuidSet(attackerParticipants));
        tag.put("DeathCounts", saveUuidIntMap(deathCounts, "Deaths"));
        tag.put("PendingRespawns", saveUuidIntMap(pendingRespawnTicks, "Ticks"));

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

    public void start(
            Map<UUID, PlayerSnapshot> players,
            Set<UUID> defenderParticipants,
            Set<UUID> attackerParticipants,
            long matchEndGameTime,
            boolean originalKeepInventory,
            double originalWorldBorderCenterX,
            double originalWorldBorderCenterZ,
            double originalWorldBorderSize
    ) {
        active = true;
        this.matchEndGameTime = matchEndGameTime;
        this.originalKeepInventory = originalKeepInventory;
        this.keepInventoryCaptured = true;
        this.originalWorldBorderCenterX = originalWorldBorderCenterX;
        this.originalWorldBorderCenterZ = originalWorldBorderCenterZ;
        this.originalWorldBorderSize = originalWorldBorderSize;
        this.worldBorderCaptured = true;
        savedPlayers.clear();
        savedPlayers.putAll(players);
        this.defenderParticipants.clear();
        this.defenderParticipants.addAll(defenderParticipants);
        this.attackerParticipants.clear();
        this.attackerParticipants.addAll(attackerParticipants);
        deathCounts.clear();
        pendingRespawnTicks.clear();
        setDirty();
    }

    public Map<UUID, PlayerSnapshot> savedPlayers() {
        return Map.copyOf(savedPlayers);
    }

    public Optional<PlayerSnapshot> savedPlayer(UUID playerId) {
        return Optional.ofNullable(savedPlayers.get(playerId));
    }

    public void savePlayerIfAbsent(UUID playerId, PlayerSnapshot snapshot) {
        if (!savedPlayers.containsKey(playerId)) {
            savedPlayers.put(playerId, snapshot);
            setDirty();
        }
    }

    public void removeSavedPlayer(UUID playerId) {
        if (savedPlayers.remove(playerId) != null) {
            setDirty();
        }
    }

    public Set<UUID> defenderParticipants() {
        return Set.copyOf(defenderParticipants);
    }

    public Set<UUID> attackerParticipants() {
        return Set.copyOf(attackerParticipants);
    }

    public boolean isDefenderParticipant(UUID playerId) {
        return defenderParticipants.contains(playerId);
    }

    public boolean isAttackerParticipant(UUID playerId) {
        return attackerParticipants.contains(playerId);
    }

    public int incrementDeathCount(UUID playerId) {
        int count = deathCounts.getOrDefault(playerId, 0) + 1;
        deathCounts.put(playerId, count);
        setDirty();
        return count;
    }

    public void setPendingRespawnTicks(UUID playerId, int ticks) {
        if (ticks > 0) {
            pendingRespawnTicks.put(playerId, ticks);
        } else {
            pendingRespawnTicks.remove(playerId);
        }
        setDirty();
    }

    public Optional<Integer> pendingRespawnTicks(UUID playerId) {
        return Optional.ofNullable(pendingRespawnTicks.get(playerId));
    }

    public Map<UUID, Integer> pendingRespawns() {
        return Map.copyOf(pendingRespawnTicks);
    }

    public void removePendingRespawn(UUID playerId) {
        if (pendingRespawnTicks.remove(playerId) != null) {
            setDirty();
        }
    }

    public void end() {
        active = false;
        matchEndGameTime = 0L;
        keepInventoryCaptured = false;
        worldBorderCaptured = false;
        nexusMarkerId = null;
        defenderParticipants.clear();
        attackerParticipants.clear();
        deathCounts.clear();
        pendingRespawnTicks.clear();
        setDirty();
    }

    public void clearFinishedSnapshotsIfEmpty() {
        if (!active && savedPlayers.isEmpty()) {
            setDirty();
        }
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

    public long matchEndGameTime() {
        return matchEndGameTime;
    }

    public boolean originalKeepInventory() {
        return originalKeepInventory;
    }

    public boolean keepInventoryCaptured() {
        return keepInventoryCaptured;
    }

    public double originalWorldBorderCenterX() {
        return originalWorldBorderCenterX;
    }

    public double originalWorldBorderCenterZ() {
        return originalWorldBorderCenterZ;
    }

    public double originalWorldBorderSize() {
        return originalWorldBorderSize;
    }

    public boolean worldBorderCaptured() {
        return worldBorderCaptured;
    }

    public Optional<UUID> nexusMarkerId() {
        return Optional.ofNullable(nexusMarkerId);
    }

    public void setNexusMarkerId(UUID nexusMarkerId) {
        this.nexusMarkerId = nexusMarkerId;
        setDirty();
    }

    private static void loadUuidSet(ListTag tags, Set<UUID> target) {
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = tags.getCompound(i);
            if (tag.hasUUID("Player")) {
                target.add(tag.getUUID("Player"));
            }
        }
    }

    private static ListTag saveUuidSet(Set<UUID> values) {
        ListTag tags = new ListTag();
        for (UUID uuid : values) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Player", uuid);
            tags.add(tag);
        }
        return tags;
    }

    private static void loadUuidIntMap(ListTag tags, String valueKey, Map<UUID, Integer> target) {
        for (int i = 0; i < tags.size(); i++) {
            CompoundTag tag = tags.getCompound(i);
            if (tag.hasUUID("Player")) {
                target.put(tag.getUUID("Player"), tag.getInt(valueKey));
            }
        }
    }

    private static ListTag saveUuidIntMap(Map<UUID, Integer> values, String valueKey) {
        ListTag tags = new ListTag();
        values.forEach((uuid, value) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Player", uuid);
            tag.putInt(valueKey, value);
            tags.add(tag);
        });
        return tags;
    }

    public boolean dimensionExists(MinecraftServer server) {
        try {
            ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.parse(warDayDimension));
            return server.getLevel(key) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    public record PlayerSnapshot(
            GameType gameMode,
            String dimension,
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            String respawnDimension,
            boolean hasRespawnPosition,
            int respawnX,
            int respawnY,
            int respawnZ,
            float respawnAngle,
            boolean respawnForced
    ) {
        private static PlayerSnapshot load(CompoundTag tag) {
            return new PlayerSnapshot(
                    GameType.byName(tag.getString("Mode"), GameType.SURVIVAL),
                    tag.getString("Dimension"),
                    tag.getDouble("X"),
                    tag.getDouble("Y"),
                    tag.getDouble("Z"),
                    tag.getFloat("YRot"),
                    tag.getFloat("XRot"),
                    tag.getString("RespawnDimension"),
                    tag.getBoolean("HasRespawnPosition"),
                    tag.getInt("RespawnX"),
                    tag.getInt("RespawnY"),
                    tag.getInt("RespawnZ"),
                    tag.getFloat("RespawnAngle"),
                    tag.getBoolean("RespawnForced")
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
            tag.putString("RespawnDimension", respawnDimension);
            tag.putBoolean("HasRespawnPosition", hasRespawnPosition);
            tag.putInt("RespawnX", respawnX);
            tag.putInt("RespawnY", respawnY);
            tag.putInt("RespawnZ", respawnZ);
            tag.putFloat("RespawnAngle", respawnAngle);
            tag.putBoolean("RespawnForced", respawnForced);
            return tag;
        }
    }
}
