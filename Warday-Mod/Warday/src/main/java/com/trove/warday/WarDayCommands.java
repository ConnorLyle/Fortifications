package com.trove.warday;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WarDayCommands {
    private static final int VALIDATE_PERMISSION_LEVEL = 2;
    private static final int MATCH_BLOCK_TARGET_COUNT = 32;
    private static final int NEXUS_SHELL_RADIUS_XZ = 3;
    private static final int NEXUS_SHELL_MIN_Y_OFFSET = -3;
    private static final int NEXUS_SHELL_MAX_Y_OFFSET = 5;
    private static final SuggestionProvider<CommandSourceStack> TEAM_NAME_SUGGESTIONS = WarDayCommands::suggestTeamNames;
    private static final Map<UUID, PendingRespawn> PENDING_RESPAWNS = new HashMap<>();
    private static final Map<UUID, Integer> DEATH_COUNTS = new HashMap<>();
    private static final Map<UUID, Deque<Long>> DIG_HISTORY = new HashMap<>();

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("warday")
                .requires(source -> source.hasPermission(VALIDATE_PERMISSION_LEVEL))
                .then(Commands.literal("validate")
                        .executes(context -> validate(context.getSource())))
                .then(Commands.literal("scan")
                        .executes(context -> scan(context.getSource())))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource())))
                .then(Commands.literal("blocks")
                        .executes(context -> giveSetupBlocks(context.getSource())))
                .then(Commands.literal("kit")
                        .executes(context -> giveSetupBlocks(context.getSource())))
                .then(Commands.literal("start")
                        .executes(context -> start(context.getSource())))
                .then(Commands.literal("end")
                        .executes(context -> end(context.getSource())))
                .then(Commands.literal("prepare")
                        .executes(context -> preparePreview(context.getSource()))
                        .then(Commands.literal("confirm")
                                .executes(context -> prepareConfirm(context.getSource()))))
                .then(Commands.literal("team1")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(TEAM_NAME_SUGGESTIONS)
                                .executes(context -> setTeamName(
                                        context.getSource(),
                                        WarDayConfig.TEAM_A_NAME,
                                        "Team 1",
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("team2")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(TEAM_NAME_SUGGESTIONS)
                                .executes(context -> setTeamName(
                                        context.getSource(),
                                        WarDayConfig.TEAM_B_NAME,
                                        "Team 2",
                                        StringArgumentType.getString(context, "name"))))));
    }

    private int setTeamName(
            CommandSourceStack source,
            net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<String> configValue,
            String label,
            String name
    ) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            source.sendFailure(message(ChatFormatting.RED, label + " name cannot be empty."));
            return 0;
        }

        configValue.set(trimmed);
        configValue.save();
        source.sendSuccess(() -> message(ChatFormatting.GREEN, label + " set to " + trimmed), true);

        if (FTBTeamsAPI.api().isManagerLoaded()) {
            boolean exists = findTeamByConfiguredName(FTBTeamsAPI.api().getManager(), trimmed).isPresent();
            ChatFormatting color = exists ? ChatFormatting.GRAY : ChatFormatting.YELLOW;
            String status = exists ? "Found matching FTB team." : "No matching FTB team found yet.";
            source.sendSuccess(() -> message(color, status), false);
        }

        return 1;
    }

    private int giveSetupBlocks(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(message(ChatFormatting.RED, "Only a player can receive War Day setup blocks."));
            return 0;
        }

        giveOrDrop(player, new ItemStack(WarDayMod.NEXUS_ITEM.get()));
        giveOrDrop(player, new ItemStack(WarDayMod.FORWARD_MARKER_ITEM.get()));
        giveOrDrop(player, new ItemStack(WarDayMod.ATTACKER_SPAWN_ITEM.get()));
        source.sendSuccess(() -> message(ChatFormatting.GREEN, "Added War Day setup blocks to your inventory."), true);
        return 1;
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static CompletableFuture<Suggestions> suggestTeamNames(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return builder.buildFuture();
        }

        String remaining = builder.getRemainingLowerCase();
        for (Team team : FTBTeamsAPI.api().getManager().getTeams()) {
            suggestIfMatches(builder, remaining, team.getShortName());
            suggestIfMatches(builder, remaining, team.getName().getString());
        }

        return builder.buildFuture();
    }

    private static void suggestIfMatches(SuggestionsBuilder builder, String remaining, String value) {
        if (value != null && !value.isBlank() && value.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {
            builder.suggest(value);
        }
    }

    private int validate(CommandSourceStack source) {
        Optional<ScanContext> scanContext = createScanContext(source);
        if (scanContext.isEmpty()) {
            return 0;
        }

        ScanContext context = scanContext.get();

        source.sendSuccess(() -> message(ChatFormatting.AQUA,
                "War Day validation for " + WarDayConfig.TEAM_A_NAME.get() + " vs " + WarDayConfig.TEAM_B_NAME.get()), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "Copied defender base rotates around its nexus so the forward marker faces the attacker side."), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "Scanning " + context.radius() + " blocks around " + context.level().dimension().location() + " " + formatPos(context.center())), false);

        reportBlocks(source, "nexus", context.nexuses());
        reportBlocks(source, "forward marker", context.forwardMarkers());

        TeamValidation teamAValidation = validateTeamMarkers(context.teamA(), context.nexuses(), context.forwardMarkers(), context.chunkManager());
        Optional<AttackerValidation> attackerValidation = context.teamB().map(team -> validateAttackerSpawn(team, context.attackerSpawns()));
        reportTeamValidation(source, teamAValidation);
        attackerValidation.ifPresent(validation -> reportAttackerValidation(source, validation));

        boolean passed = teamAValidation.passed() && attackerValidation.map(AttackerValidation::passed).orElse(true);
        if (passed) {
            source.sendSuccess(() -> message(ChatFormatting.GREEN,
                    context.teamB().isPresent()
                            ? "Validation passed: defender base and attacker spawn are configured."
                            : "Validation passed in one-team testing mode."), true);
            return 1;
        }

        source.sendFailure(message(ChatFormatting.RED,
                "Validation failed: fix the team-owned nexus/forward marker counts or claim cluster placement."));
        return 0;
    }

    private int scan(CommandSourceStack source) {
        Optional<ScanContext> scanContext = createScanContext(source);
        if (scanContext.isEmpty()) {
            return 0;
        }

        ScanContext context = scanContext.get();
        source.sendSuccess(() -> message(ChatFormatting.AQUA,
                "War Day scan for defender " + context.teamA().getName().getString()
                        + context.teamB().map(team -> " vs attacker " + team.getName().getString()).orElse(" one-team test")), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "Scanning " + context.radius() + " blocks around " + context.level().dimension().location() + " " + formatPos(context.center())), false);

        TeamValidation teamAValidation = validateTeamMarkers(context.teamA(), context.nexuses(), context.forwardMarkers(), context.chunkManager());
        Optional<AttackerValidation> attackerValidation = context.teamB().map(team -> validateAttackerSpawn(team, context.attackerSpawns()));
        reportTeamScan(source, teamAValidation, context.chunkManager());
        attackerValidation.ifPresent(validation -> reportAttackerValidation(source, validation));
        reportGuardrails(source, teamAValidation, context.chunkManager());

        boolean passed = teamAValidation.passed() && attackerValidation.map(AttackerValidation::passed).orElse(true);
        if (!passed) {
            source.sendFailure(message(ChatFormatting.RED,
                    "Scan could not resolve both base areas. Run /warday validate for marker-specific failures."));
            return 0;
        }

        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "Scan complete: both base areas resolved without changing the world."), false);
        return 1;
    }

    private int preparePreview(CommandSourceStack source) {
        Optional<ResolvedBases> resolved = resolveBases(source);
        if (resolved.isEmpty()) {
            return 0;
        }

        ResolvedBases bases = resolved.get();
        source.sendSuccess(() -> message(ChatFormatting.AQUA, "War Day prepare preview only. No blocks were copied."), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY, "Target dimension: " + WarDayConfig.WAR_DAY_DIMENSION.get()), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY, "Defender nexus targets [0, warDayBaseY, 0]; attacker spawn targets [baseSpacingBlocks, warDayBaseY, 0]."), false);

        reportPlacementPlan(source, PlacementPlan.from(bases.teamA()));
        bases.attackerArea().ifPresent(area -> reportPlacementPlan(source, PlacementPlan.from(area)));

        source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                "Run /warday prepare confirm to copy these source chunks into the target placements."), false);
        return 1;
    }

    private int prepareConfirm(CommandSourceStack source) {
        Optional<ResolvedBases> resolved = resolveBases(source);
        if (resolved.isEmpty()) {
            return 0;
        }

        Optional<ResourceKey<Level>> dimensionKey = warDayDimensionKey(source);
        if (dimensionKey.isEmpty()) {
            return 0;
        }

        ServerLevel targetLevel = source.getServer().getLevel(dimensionKey.get());
        if (targetLevel == null) {
            source.sendFailure(message(ChatFormatting.RED, "War Day dimension is not loaded: " + WarDayConfig.WAR_DAY_DIMENSION.get()));
            source.sendFailure(message(ChatFormatting.RED, "Restart the server after adding this mod jar so the bundled dimension data can load."));
            return 0;
        }

        ResolvedBases bases = resolved.get();
        ServerLevel defenderSourceLevel = source.getServer().getLevel(bases.teamA().dimension());
        if (defenderSourceLevel == null) {
            source.sendFailure(message(ChatFormatting.RED, "Defender source dimension is not loaded: " + bases.teamA().dimension().location()));
            return 0;
        }

        PlacementPlan teamAPlan = PlacementPlan.from(bases.teamA());
        Optional<PlacementPlan> attackerPlan = bases.attackerArea().map(PlacementPlan::from);
        ServerLevel attackerSourceLevel = null;
        if (attackerPlan.isPresent()) {
            attackerSourceLevel = source.getServer().getLevel(attackerPlan.get().dimension());
            if (attackerSourceLevel == null) {
                source.sendFailure(message(ChatFormatting.RED, "Attacker source dimension is not loaded: " + attackerPlan.get().dimension().location()));
                return 0;
            }
        }

        CopyCheck teamACheck = checkDestinationEmpty(defenderSourceLevel, targetLevel, teamAPlan);
        reportCopyCheck(source, bases.teamA().team().getName().getString(), teamACheck);
        CopyCheck attackerCheck = null;
        if (attackerPlan.isPresent()) {
            attackerCheck = checkDestinationEmpty(attackerSourceLevel, targetLevel, attackerPlan.get());
            reportCopyCheck(source, WarDayConfig.TEAM_B_NAME.get() + " spawn area", attackerCheck);
        }

        if (!teamACheck.passed() || (attackerCheck != null && !attackerCheck.passed())) {
            source.sendSuccess(() -> message(ChatFormatting.YELLOW, "Destination conflicts found; wiping computed War Day destination areas before paste."), true);
        }

        int teamAWiped = wipeDestinationArea(targetLevel, teamAPlan);
        source.sendSuccess(() -> message(ChatFormatting.YELLOW, "Wiped " + teamAWiped + " defender destination blocks from War Day target area."), true);
        attackerPlan.ifPresent(plan -> {
            int attackerWiped = wipeDestinationArea(targetLevel, plan);
            source.sendSuccess(() -> message(ChatFormatting.YELLOW, "Wiped " + attackerWiped + " attacker destination blocks from War Day target area."), true);
        });

        CopyResult teamAResult = copyBase(defenderSourceLevel, targetLevel, teamAPlan);
        EntityCopyResult teamAEntityResult = copyDecorativeEntities(defenderSourceLevel, targetLevel, teamAPlan);
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "Copied " + bases.teamA().team().getName().getString() + ": " + teamAResult.blocksCopied()
                        + " blocks, " + teamAResult.blockEntitiesCopied() + " block entities, "
                        + teamAResult.containersCleared() + " containers cleared, "
                        + teamAEntityResult.entitiesCopied() + " decorative entities, "
                        + teamAEntityResult.itemFramesCleared() + " item frames cleared."), true);
        Optional<BlockPos> safeAttackerSpawn = Optional.empty();
        if (attackerPlan.isPresent()) {
            PlacementPlan plan = attackerPlan.get();
            CopyResult attackerResult = copyBase(attackerSourceLevel, targetLevel, plan);
            EntityCopyResult attackerEntityResult = copyDecorativeEntities(attackerSourceLevel, targetLevel, plan);
            safeAttackerSpawn = findSafeSpawnPos(targetLevel, plan);
            if (safeAttackerSpawn.isEmpty()) {
                source.sendFailure(message(ChatFormatting.RED,
                        "Copied attacker spawn area, but no safe two-block-tall landing spot was found near "
                                + formatPos(plan.targetAnchorPos()) + ". Move the attacker spawn marker or clear space above it, then rerun /warday prepare confirm."));
                return 0;
            }
            BlockPos spawnPos = safeAttackerSpawn.get();
            source.sendSuccess(() -> message(ChatFormatting.GREEN,
                    "Copied attacker spawn area: " + attackerResult.blocksCopied()
                            + " blocks, " + attackerResult.blockEntitiesCopied() + " block entities, "
                            + attackerResult.containersCleared() + " containers cleared, "
                            + attackerEntityResult.entitiesCopied() + " decorative entities, "
                            + attackerEntityResult.itemFramesCleared() + " item frames cleared. Safe spawn at "
                            + formatPos(spawnPos)), true);
        }
        WarDayState state = WarDayState.get(source.getServer());
        BlockPos copiedNexusPos = teamAPlan.targetPos(bases.teamA().nexus().pos());
        buildNexusShell(targetLevel, copiedNexusPos);
        state.markPrepared(
                WarDayConfig.WAR_DAY_DIMENSION.get(),
                bases.teamA().team().getName().getString(),
                bases.attackerArea().isPresent() ? WarDayConfig.TEAM_B_NAME.get() : "",
                copiedNexusPos,
                safeAttackerSpawn.orElse(null)
        );
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "Built protected nexus shell at " + formatPos(copiedNexusPos) + "."), true);
        source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                "Copied defender base rotation applied. Nexus win tracking will be added in a later pass."), false);
        return 1;
    }

    private int status(CommandSourceStack source) {
        WarDayState state = WarDayState.get(source.getServer());
        Optional<ResourceKey<Level>> dimensionKey = warDayDimensionKey(source);
        boolean configuredDimensionLoaded = dimensionKey.map(key -> source.getServer().getLevel(key) != null).orElse(false);

        source.sendSuccess(() -> message(ChatFormatting.AQUA, "War Day status"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY, "Configured defender team: " + WarDayConfig.TEAM_A_NAME.get()), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY, "Configured attacker team: " + WarDayConfig.TEAM_B_NAME.get()), false);
        source.sendSuccess(() -> message(configuredDimensionLoaded ? ChatFormatting.GREEN : ChatFormatting.RED,
                "Configured dimension loaded: " + configuredDimensionLoaded + " (" + WarDayConfig.WAR_DAY_DIMENSION.get() + ")"), false);
        source.sendSuccess(() -> message(state.isPrepared() ? ChatFormatting.GREEN : ChatFormatting.YELLOW,
                "Prepared state saved: " + state.isPrepared()), false);
        source.sendSuccess(() -> message(state.isActive() ? ChatFormatting.GREEN : ChatFormatting.GRAY,
                "Active: " + state.isActive()), false);

        if (state.isPrepared()) {
            source.sendSuccess(() -> message(ChatFormatting.GRAY, "Saved dimension: " + state.warDayDimension()), false);
            source.sendSuccess(() -> message(ChatFormatting.GRAY, "Saved defender team: " + state.defenderTeam()), false);
            source.sendSuccess(() -> message(ChatFormatting.GRAY, "Saved attacker team: " + (state.attackerTeam().isBlank() ? "none" : state.attackerTeam())), false);
            source.sendSuccess(() -> message(ChatFormatting.GRAY,
                    "Copied nexus: " + state.copiedNexusPos().map(WarDayCommands::formatPos).orElse("missing")), false);
            source.sendSuccess(() -> message(ChatFormatting.GRAY,
                    "Attacker spawn: " + state.attackerSpawnPos().map(WarDayCommands::formatPos).orElse("missing")), false);
            if (state.isActive()) {
                long secondsRemaining = Math.max(0L, (state.matchEndGameTime() - source.getLevel().getGameTime()) / 20L);
                source.sendSuccess(() -> message(ChatFormatting.GRAY, "Match time remaining: " + secondsRemaining + " seconds"), false);
            }
            source.sendSuccess(() -> message(ChatFormatting.GREEN, "Next command: /warday start"), false);
        } else {
            source.sendSuccess(() -> message(ChatFormatting.YELLOW, "Next command: /warday prepare confirm"), false);
        }

        return state.isPrepared() ? 1 : 0;
    }

    private int start(CommandSourceStack source) {
        WarDayState state = WarDayState.get(source.getServer());
        if (!state.isPrepared()) {
            source.sendFailure(message(ChatFormatting.RED, "War Day is not prepared. Run /warday prepare confirm first."));
            return 0;
        }
        if (state.isActive()) {
            source.sendFailure(message(ChatFormatting.RED, "War Day is already active."));
            return 0;
        }
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            source.sendFailure(message(ChatFormatting.RED, "FTB Teams manager is not loaded."));
            return 0;
        }

        Optional<ResourceKey<Level>> dimensionKey = warDayDimensionKey(source);
        if (dimensionKey.isEmpty()) {
            return 0;
        }
        ServerLevel warDayLevel = source.getServer().getLevel(dimensionKey.get());
        if (warDayLevel == null) {
            source.sendFailure(message(ChatFormatting.RED, "War Day dimension is not loaded: " + WarDayConfig.WAR_DAY_DIMENSION.get()));
            return 0;
        }

        TeamManager teamManager = FTBTeamsAPI.api().getManager();
        Optional<Team> defenderTeam = findTeamByConfiguredName(teamManager, WarDayConfig.TEAM_A_NAME.get());
        Optional<Team> attackerTeam = findTeamByConfiguredName(teamManager, WarDayConfig.TEAM_B_NAME.get());
        if (defenderTeam.isEmpty()) {
            source.sendFailure(message(ChatFormatting.RED, "Defender team not found: " + WarDayConfig.TEAM_A_NAME.get()));
            return 0;
        }
        if (attackerTeam.isEmpty()) {
            source.sendFailure(message(ChatFormatting.RED, "Attacker team not found: " + WarDayConfig.TEAM_B_NAME.get()));
            return 0;
        }
        if (state.copiedNexusPos().isEmpty() || state.attackerSpawnPos().isEmpty()) {
            source.sendFailure(message(ChatFormatting.RED, "Prepared state is missing nexus or attacker spawn position. Rerun /warday prepare confirm."));
            return 0;
        }

        Map<UUID, WarDayState.PlayerSnapshot> snapshots = new HashMap<>();
        int defenders = 0;
        int attackers = 0;
        int spectators = 0;
        Set<UUID> defenderIds = defenderTeam.get().getMembers();
        Set<UUID> attackerIds = attackerTeam.get().getMembers();
        Item defenderBlock = defenderMatchBlock();
        Item attackerBlock = attackerMatchBlock();

        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            snapshots.put(id, snapshotPlayer(player));

            if (defenderIds.contains(id)) {
                teleportPlayer(player, warDayLevel, state.copiedNexusPos().get().offset(0, 1, 0));
                setPlayerSpawn(player, warDayLevel, state.copiedNexusPos().get().offset(0, 1, 0));
                player.setGameMode(GameType.SURVIVAL);
                ensureInventoryHasAtLeast(player, defenderBlock, MATCH_BLOCK_TARGET_COUNT);
                defenders++;
            } else if (attackerIds.contains(id)) {
                teleportPlayer(player, warDayLevel, state.attackerSpawnPos().get());
                setPlayerSpawn(player, warDayLevel, state.attackerSpawnPos().get());
                player.setGameMode(GameType.SURVIVAL);
                ensureInventoryHasAtLeast(player, attackerBlock, MATCH_BLOCK_TARGET_COUNT);
                attackers++;
            } else {
                player.setGameMode(GameType.SPECTATOR);
                teleportPlayer(player, warDayLevel, state.attackerSpawnPos().get().offset(0, 11, 0));
                spectators++;
            }
        }

        boolean originalKeepInventory = warDayLevel.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        warDayLevel.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, source.getServer());
        configureWorldBorder(warDayLevel, state.copiedNexusPos().get());
        spawnNexusMarker(warDayLevel, state, state.copiedNexusPos().get());
        DEATH_COUNTS.clear();
        DIG_HISTORY.clear();
        PENDING_RESPAWNS.clear();

        long matchEndGameTime = warDayLevel.getGameTime() + WarDayConfig.MATCH_DURATION_SECONDS.getAsInt() * 20L;
        state.start(snapshots, matchEndGameTime, originalKeepInventory);
        source.getServer().getPlayerList().broadcastSystemMessage(
                message(ChatFormatting.GREEN, "War Day started for " + WarDayConfig.MATCH_DURATION_SECONDS.getAsInt()
                        + " seconds. Defenders=" + defenders + ", attackers=" + attackers + ", spectators=" + spectators),
                false
        );
        return 1;
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Optional<ParticipantRespawn> respawn = participantRespawn(player);
        if (respawn.isEmpty()) {
            giveRespawnMatchBlocks(player);
            return;
        }

        ParticipantRespawn participant = respawn.get();
        int deathCount = DEATH_COUNTS.merge(player.getUUID(), 1, Integer::sum);
        int delaySeconds = respawnDelaySecondsForDeath(deathCount);
        int delayTicks = delaySeconds * 20;
        if (delayTicks <= 0) {
            restoreDelayedRespawn(player, participant);
            return;
        }

        PENDING_RESPAWNS.put(player.getUUID(), new PendingRespawn(delayTicks, participant));
        player.setGameMode(GameType.SPECTATOR);
        teleportPlayer(player, participant.level(), participant.spawnPos().offset(0, 11, 0));
        player.sendSystemMessage(message(ChatFormatting.YELLOW,
                "Respawning in " + delaySeconds + " seconds."));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        WarDayState state = WarDayState.get(event.getServer());
        if (!state.isActive()) {
            PENDING_RESPAWNS.clear();
            return;
        }

        Optional<ServerLevel> warDayLevel = warDayLevel(event.getServer(), state);
        if (warDayLevel.isPresent() && state.matchEndGameTime() > 0L && warDayLevel.get().getGameTime() >= state.matchEndGameTime()) {
            int restored = endActiveWarDay(event.getServer(), state);
            String defenderTeam = state.defenderTeam().isBlank() ? WarDayConfig.TEAM_A_NAME.get() : state.defenderTeam();
            event.getServer().getPlayerList().broadcastSystemMessage(
                    message(ChatFormatting.GOLD,
                            "War Day complete: " + defenderTeam + " defended the nexus until time expired. Restored gamemodes for "
                                    + restored + " online players."),
                    false
            );
            return;
        }

        if (PENDING_RESPAWNS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, PendingRespawn>> iterator = PENDING_RESPAWNS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingRespawn> entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            PendingRespawn pending = entry.getValue().tick();
            if (pending.ticksRemaining() > 0) {
                entry.setValue(pending);
                teleportPlayer(player, pending.participant().level(), pending.participant().spawnPos().offset(0, 11, 0));
                continue;
            }

            Optional<ParticipantRespawn> currentRespawn = participantRespawn(player);
            restoreDelayedRespawn(player, currentRespawn.orElse(pending.participant()));
            iterator.remove();
        }
    }

    private int end(CommandSourceStack source) {
        WarDayState state = WarDayState.get(source.getServer());
        if (!state.isActive()) {
            source.sendFailure(message(ChatFormatting.RED, "War Day is not active."));
            return 0;
        }

        int restored = endActiveWarDay(source.getServer(), state);
        source.getServer().getPlayerList().broadcastSystemMessage(
                message(ChatFormatting.YELLOW, "War Day ended. Restored gamemodes for " + restored + " online players."),
                false
        );
        return 1;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        WarDayState state = WarDayState.get(level.getServer());
        if (!state.isActive() || state.copiedNexusPos().isEmpty()) {
            return;
        }

        if (!level.dimension().location().toString().equals(state.warDayDimension())) {
            return;
        }

        Optional<ParticipantRespawn> participant = participantRespawn(player);
        if (participant.isEmpty()) {
            event.setCanceled(true);
            player.displayClientMessage(message(ChatFormatting.RED, "Only War Day participants can break blocks during the match."), true);
            return;
        }

        BlockPos nexusPos = state.copiedNexusPos().get();
        if (!isInMatchBounds(event.getPos(), nexusPos)) {
            event.setCanceled(true);
            player.displayClientMessage(message(ChatFormatting.RED, "You cannot break blocks outside the War Day bounds."), true);
            return;
        }

        if (isInNexusShell(event.getPos(), nexusPos) && !event.getPos().equals(nexusPos)) {
            event.setCanceled(true);
            player.displayClientMessage(message(ChatFormatting.RED, "The nexus shell is protected."), true);
            return;
        }

        if (!event.getState().is(WarDayMod.NEXUS.get()) || !event.getPos().equals(nexusPos)) {
            trackDiggingPenalty(player, level.getGameTime());
            return;
        }

        if (!isAttacker(player)) {
            event.setCanceled(true);
            player.displayClientMessage(message(ChatFormatting.RED, "Only attackers can destroy the nexus."), true);
            return;
        }

        int restored = endActiveWarDay(level.getServer(), state);
        String breaker = player.getName().getString();
        String attackerTeam = state.attackerTeam().isBlank() ? WarDayConfig.TEAM_B_NAME.get() : state.attackerTeam();
        level.getServer().getPlayerList().broadcastSystemMessage(
                message(ChatFormatting.GOLD,
                        "War Day complete: " + attackerTeam + " destroyed the nexus. Final break by " + breaker
                                + ". Restored gamemodes for " + restored + " online players."),
                false
        );
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        WarDayState state = WarDayState.get(level.getServer());
        if (!state.isActive() || state.copiedNexusPos().isEmpty()) {
            return;
        }
        if (!level.dimension().location().toString().equals(state.warDayDimension())) {
            return;
        }

        BlockPos nexusPos = state.copiedNexusPos().get();
        if (!isInMatchBounds(event.getPos(), nexusPos)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(message(ChatFormatting.RED, "You cannot place blocks outside the War Day bounds."), true);
            }
            return;
        }

        if (isInNexusShell(event.getPos(), nexusPos)) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(message(ChatFormatting.RED, "You cannot place blocks in the nexus shell."), true);
            }
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            event.setCanceled(true);
            return;
        }

        Optional<ParticipantRespawn> participant = participantRespawn(player);
        if (participant.isEmpty() || !event.getPlacedBlock().is(Block.byItem(participant.get().matchBlock()))) {
            event.setCanceled(true);
            player.displayClientMessage(message(ChatFormatting.RED, "Only your team blocks can be placed during War Day."), true);
        }
    }

    private static int endActiveWarDay(MinecraftServer server, WarDayState state) {
        int restored = 0;
        Map<UUID, WarDayState.PlayerSnapshot> snapshots = state.savedPlayers();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            WarDayState.PlayerSnapshot snapshot = snapshots.get(player.getUUID());
            if (snapshot != null) {
                restorePlayer(server, player, snapshot);
                restored++;
            }
        }

        if (state.keepInventoryCaptured()) {
            warDayLevel(server, state).ifPresent(level ->
                    level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(state.originalKeepInventory(), server)
            );
        }
        warDayLevel(server, state).ifPresent(level -> removeNexusMarker(level, state));
        state.end();
        PENDING_RESPAWNS.clear();
        DEATH_COUNTS.clear();
        DIG_HISTORY.clear();
        return restored;
    }

    private static void giveRespawnMatchBlocks(ServerPlayer player) {
        WarDayState state = WarDayState.get(player.getServer());
        if (!state.isActive() || !FTBTeamsAPI.api().isManagerLoaded()) {
            return;
        }

        TeamManager teamManager = FTBTeamsAPI.api().getManager();
        Optional<Team> defenderTeam = findTeamByConfiguredName(teamManager, WarDayConfig.TEAM_A_NAME.get());
        Optional<Team> attackerTeam = findTeamByConfiguredName(teamManager, WarDayConfig.TEAM_B_NAME.get());
        UUID playerId = player.getUUID();

        if (defenderTeam.map(team -> team.getMembers().contains(playerId)).orElse(false)) {
            ensureInventoryHasAtLeast(player, defenderMatchBlock(), MATCH_BLOCK_TARGET_COUNT);
        } else if (attackerTeam.map(team -> team.getMembers().contains(playerId)).orElse(false)) {
            ensureInventoryHasAtLeast(player, attackerMatchBlock(), MATCH_BLOCK_TARGET_COUNT);
        }
    }

    private static void ensureInventoryHasAtLeast(ServerPlayer player, Item item, int targetCount) {
        int currentCount = player.getInventory().countItem(item);
        if (currentCount >= targetCount) {
            return;
        }

        giveOrDrop(player, new ItemStack(item, targetCount - currentCount));
    }

    private static WarDayState.PlayerSnapshot snapshotPlayer(ServerPlayer player) {
        return new WarDayState.PlayerSnapshot(
                player.gameMode.getGameModeForPlayer(),
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }

    private static void restorePlayer(MinecraftServer server, ServerPlayer player, WarDayState.PlayerSnapshot snapshot) {
        if (!snapshot.dimension().isBlank()) {
            ServerLevel restoreLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(snapshot.dimension())));
            if (restoreLevel != null) {
                player.teleportTo(restoreLevel, snapshot.x(), snapshot.y(), snapshot.z(), Set.of(), snapshot.yRot(), snapshot.xRot());
            }
        }
        player.setGameMode(snapshot.gameMode());
    }

    private static void teleportPlayer(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, Set.of(), player.getYRot(), player.getXRot());
    }

    private static void setPlayerSpawn(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.setRespawnPosition(level.dimension(), pos, player.getYRot(), true, false);
    }

    private static int respawnDelaySecondsForDeath(int deathCount) {
        if (deathCount <= 1) {
            return 0;
        }
        if (deathCount == 2) {
            return 5;
        }
        if (deathCount == 3) {
            return 10;
        }
        return 15;
    }

    private static Item defenderMatchBlock() {
        return configuredItem(WarDayConfig.DEFENDER_MATCH_BLOCK.get(), Blocks.BLUE_WOOL.asItem());
    }

    private static Item attackerMatchBlock() {
        return configuredItem(WarDayConfig.ATTACKER_MATCH_BLOCK.get(), Blocks.RED_WOOL.asItem());
    }

    private static Item configuredItem(String id, Item fallback) {
        try {
            return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Optional<ServerLevel> warDayLevel(MinecraftServer server, WarDayState state) {
        try {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(state.warDayDimension()));
            return Optional.ofNullable(server.getLevel(dimension));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static void configureWorldBorder(ServerLevel level, BlockPos nexusPos) {
        double size = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt() * 2.0D;
        level.getWorldBorder().setCenter(nexusPos.getX() + 0.5D, nexusPos.getZ() + 0.5D);
        level.getWorldBorder().setSize(size);
    }

    private static void spawnNexusMarker(ServerLevel level, WarDayState state, BlockPos nexusPos) {
        removeNexusMarker(level, state);

        Display.BlockDisplay marker = EntityType.BLOCK_DISPLAY.create(level);
        if (marker == null) {
            return;
        }

        CompoundTag blockStateTag = new CompoundTag();
        blockStateTag.putString("Name", WarDayMod.MODID + ":nexus");
        CompoundTag markerTag = new CompoundTag();
        markerTag.put("block_state", blockStateTag);
        marker.load(markerTag);
        marker.setPos(nexusPos.getX(), nexusPos.getY(), nexusPos.getZ());
        marker.setGlowingTag(true);
        marker.setNoGravity(true);
        marker.setInvulnerable(true);

        level.addFreshEntity(marker);
        state.setNexusMarkerId(marker.getUUID());
    }

    private static void removeNexusMarker(ServerLevel level, WarDayState state) {
        state.nexusMarkerId()
                .map(level::getEntity)
                .ifPresent(Entity::discard);
    }

    private static boolean isAttacker(ServerPlayer player) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return false;
        }

        Optional<Team> attackerTeam = findTeamByConfiguredName(FTBTeamsAPI.api().getManager(), WarDayConfig.TEAM_B_NAME.get());
        return attackerTeam.map(team -> team.getMembers().contains(player.getUUID())).orElse(false);
    }

    private static boolean isInMatchBounds(BlockPos pos, BlockPos nexusPos) {
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        return pos.getX() >= nexusPos.getX() - halfSize
                && pos.getX() < nexusPos.getX() + halfSize
                && pos.getZ() >= nexusPos.getZ() - halfSize
                && pos.getZ() < nexusPos.getZ() + halfSize;
    }

    private static void trackDiggingPenalty(ServerPlayer player, long gameTime) {
        int windowTicks = WarDayConfig.DIG_LIMIT_WINDOW_SECONDS.getAsInt() * 20;
        int limit = WarDayConfig.DIG_LIMIT_BLOCKS.getAsInt();
        Deque<Long> digs = DIG_HISTORY.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        while (!digs.isEmpty() && gameTime - digs.peekFirst() > windowTicks) {
            digs.removeFirst();
        }

        digs.addLast(gameTime);
        if (digs.size() <= limit) {
            return;
        }

        int durationTicks = WarDayConfig.DIG_PENALTY_SECONDS.getAsInt() * 20;
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, 0, false, true, true));
        player.displayClientMessage(message(ChatFormatting.YELLOW, "Digging penalty applied."), true);
        digs.clear();
    }

    private static void buildNexusShell(ServerLevel level, BlockPos nexusPos) {
        clearNexusShell(level, nexusPos);

        BlockState quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState quartzSlab = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
        BlockState chain = Blocks.CHAIN.defaultBlockState();

        fill(level, nexusPos.offset(-2, -3, -1), nexusPos.offset(2, -3, 1), quartz);
        fill(level, nexusPos.offset(-1, -2, -1), nexusPos.offset(1, -2, 1), quartzSlab);

        level.setBlock(nexusPos.below(), chain, 3);
        level.setBlock(nexusPos.above(), chain, 3);
        level.setBlock(nexusPos.above(2), chain, 3);
        level.setBlock(nexusPos.above(3), chain, 3);

        level.setBlock(nexusPos, WarDayMod.NEXUS.get().defaultBlockState(), 3);

        fill(level, nexusPos.offset(-2, 4, -2), nexusPos.offset(2, 4, 2), quartzSlab);
        fill(level, nexusPos.offset(-2, 5, -2), nexusPos.offset(2, 5, 2), quartz);
    }

    private static void clearNexusShell(ServerLevel level, BlockPos nexusPos) {
        for (int x = -NEXUS_SHELL_RADIUS_XZ; x <= NEXUS_SHELL_RADIUS_XZ; x++) {
            for (int y = NEXUS_SHELL_MIN_Y_OFFSET; y <= NEXUS_SHELL_MAX_Y_OFFSET; y++) {
                for (int z = -NEXUS_SHELL_RADIUS_XZ; z <= NEXUS_SHELL_RADIUS_XZ; z++) {
                    BlockPos pos = nexusPos.offset(x, y, z);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void fill(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
        BlockPos min = new BlockPos(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ())
        );

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            level.setBlock(pos, state, 3);
        }
    }

    private static boolean isInNexusShell(BlockPos pos, BlockPos nexusPos) {
        return Math.abs(pos.getX() - nexusPos.getX()) <= NEXUS_SHELL_RADIUS_XZ
                && Math.abs(pos.getZ() - nexusPos.getZ()) <= NEXUS_SHELL_RADIUS_XZ
                && pos.getY() >= nexusPos.getY() + NEXUS_SHELL_MIN_Y_OFFSET
                && pos.getY() <= nexusPos.getY() + NEXUS_SHELL_MAX_Y_OFFSET;
    }

    private static Optional<ParticipantRespawn> participantRespawn(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || !FTBTeamsAPI.api().isManagerLoaded()) {
            return Optional.empty();
        }

        WarDayState state = WarDayState.get(server);
        if (!state.isActive()) {
            return Optional.empty();
        }

        Optional<ServerLevel> warDayLevel = warDayLevel(server, state);
        if (warDayLevel.isEmpty()) {
            return Optional.empty();
        }

        TeamManager teamManager = FTBTeamsAPI.api().getManager();
        Optional<Team> defenderTeam = findTeamByConfiguredName(teamManager, WarDayConfig.TEAM_A_NAME.get());
        Optional<Team> attackerTeam = findTeamByConfiguredName(teamManager, WarDayConfig.TEAM_B_NAME.get());
        UUID playerId = player.getUUID();

        if (defenderTeam.map(team -> team.getMembers().contains(playerId)).orElse(false) && state.copiedNexusPos().isPresent()) {
            return Optional.of(new ParticipantRespawn(warDayLevel.get(), state.copiedNexusPos().get().offset(0, 1, 0), defenderMatchBlock()));
        }
        if (attackerTeam.map(team -> team.getMembers().contains(playerId)).orElse(false) && state.attackerSpawnPos().isPresent()) {
            return Optional.of(new ParticipantRespawn(warDayLevel.get(), state.attackerSpawnPos().get(), attackerMatchBlock()));
        }

        return Optional.empty();
    }

    private static void restoreDelayedRespawn(ServerPlayer player, ParticipantRespawn respawn) {
        teleportPlayer(player, respawn.level(), respawn.spawnPos());
        setPlayerSpawn(player, respawn.level(), respawn.spawnPos());
        player.setGameMode(GameType.SURVIVAL);
        ensureInventoryHasAtLeast(player, respawn.matchBlock(), MATCH_BLOCK_TARGET_COUNT);
    }

    private Optional<ResolvedBases> resolveBases(CommandSourceStack source) {
        Optional<ScanContext> scanContext = createScanContext(source);
        if (scanContext.isEmpty()) {
            return Optional.empty();
        }

        ScanContext context = scanContext.get();
        TeamValidation teamAValidation = validateTeamMarkers(context.teamA(), context.nexuses(), context.forwardMarkers(), context.chunkManager());
        Optional<AttackerValidation> attackerValidation = context.teamB().map(team -> validateAttackerSpawn(team, context.attackerSpawns()));

        if (!teamAValidation.passed() || attackerValidation.map(validation -> !validation.passed()).orElse(false)) {
            reportTeamValidation(source, teamAValidation);
            attackerValidation.ifPresent(validation -> reportAttackerValidation(source, validation));
            source.sendFailure(message(ChatFormatting.RED, "Prepare requires the defender base and attacker spawn to pass validation."));
            return Optional.empty();
        }

        BaseArea teamA = BaseArea.from(teamAValidation, context.chunkManager());
        Optional<AttackerArea> attackerArea = attackerValidation.map(validation -> AttackerArea.from(validation, context.chunkManager()));
        boolean teamAWithinLimits = reportAndCheckGuardrails(source, teamA);

        if (!teamAWithinLimits) {
            return Optional.empty();
        }

        return Optional.of(new ResolvedBases(teamA, attackerArea));
    }

    private Optional<ResourceKey<Level>> warDayDimensionKey(CommandSourceStack source) {
        try {
            ResourceLocation location = ResourceLocation.parse(WarDayConfig.WAR_DAY_DIMENSION.get());
            return Optional.of(ResourceKey.create(Registries.DIMENSION, location));
        } catch (Exception ex) {
            source.sendFailure(message(ChatFormatting.RED, "Invalid warDayDimension config value: " + WarDayConfig.WAR_DAY_DIMENSION.get()));
            return Optional.empty();
        }
    }

    private Optional<ScanContext> createScanContext(CommandSourceStack source) {
        if (!FTBTeamsAPI.api().isManagerLoaded() || !FTBChunksAPI.api().isManagerLoaded()) {
            source.sendFailure(message(ChatFormatting.RED, "FTB Teams or FTB Chunks manager is not loaded yet."));
            return Optional.empty();
        }

        TeamManager teamManager = FTBTeamsAPI.api().getManager();
        ClaimedChunkManager chunkManager = FTBChunksAPI.api().getManager();
        Optional<Team> teamA = findTeamByConfiguredName(teamManager, WarDayConfig.TEAM_A_NAME.get());
        Optional<Team> teamB = findTeamByConfiguredName(teamManager, WarDayConfig.TEAM_B_NAME.get());

        if (teamA.isEmpty()) {
            source.sendFailure(message(ChatFormatting.RED, "Could not find configured Team A. Team A is required."));
            source.sendFailure(message(ChatFormatting.RED, "Team A: " + WarDayConfig.TEAM_A_NAME.get() + " found=" + teamA.isPresent()));
            return Optional.empty();
        }
        if (teamB.isEmpty()) {
            source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                    "Team B not found; continuing in one-team testing mode. Team B config=" + WarDayConfig.TEAM_B_NAME.get()), false);
        }

        List<LocatedBlock> nexuses = new ArrayList<>();
        List<LocatedBlock> forwardMarkers = new ArrayList<>();
        List<AttackerSpawn> attackerSpawns = new ArrayList<>();
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        int radius = WarDayConfig.VALIDATION_RADIUS_BLOCKS.getAsInt();
        scanArea(level, center, radius, chunkManager, nexuses, forwardMarkers, attackerSpawns);

        return Optional.of(new ScanContext(level, center, radius, chunkManager, teamA.get(), teamB, nexuses, forwardMarkers, attackerSpawns));
    }

    private static Optional<Team> findTeamByConfiguredName(TeamManager teamManager, String configuredName) {
        Optional<Team> exact = teamManager.getTeamByName(configuredName);
        if (exact.isPresent()) {
            return exact;
        }

        String normalized = configuredName.trim();
        for (Team team : teamManager.getTeams()) {
            if (team.getShortName().equalsIgnoreCase(normalized) || team.getName().getString().equalsIgnoreCase(normalized)) {
                return Optional.of(team);
            }
        }

        return Optional.empty();
    }

    private static void scanArea(
            ServerLevel level,
            BlockPos center,
            int radius,
            ClaimedChunkManager chunkManager,
            List<LocatedBlock> nexuses,
            List<LocatedBlock> forwardMarkers,
            List<AttackerSpawn> attackerSpawns
    ) {
        BlockPos min = new BlockPos(center.getX() - radius, level.getMinBuildHeight(), center.getZ() - radius);
        BlockPos max = new BlockPos(center.getX() + radius, level.getMaxBuildHeight() - 1, center.getZ() + radius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.is(WarDayMod.NEXUS.get())) {
                nexuses.add(new LocatedBlock(level.dimension(), pos.immutable(), null, getClaimOwner(chunkManager, level, pos)));
            } else if (state.is(WarDayMod.FORWARD_MARKER.get())) {
                Direction facing = state.getValue(ForwardMarkerBlock.FACING);
                forwardMarkers.add(new LocatedBlock(level.dimension(), pos.immutable(), facing, getClaimOwner(chunkManager, level, pos)));
            } else if (state.is(WarDayMod.ATTACKER_SPAWN.get())) {
                attackerSpawns.add(new AttackerSpawn(level.dimension(), pos.immutable(), getClaimOwner(chunkManager, level, pos)));
            }
        }
    }

    private static Optional<Team> getClaimOwner(ClaimedChunkManager chunkManager, ServerLevel level, BlockPos pos) {
        ClaimedChunk chunk = chunkManager.getChunk(new ChunkDimPos(level.dimension(), new ChunkPos(pos)));
        return chunk == null ? Optional.empty() : Optional.of(chunk.getTeamData().getTeam());
    }

    private static TeamValidation validateTeamMarkers(
            Team team,
            List<LocatedBlock> nexuses,
            List<LocatedBlock> forwardMarkers,
            ClaimedChunkManager chunkManager
    ) {
        List<LocatedBlock> teamNexuses = nexuses.stream().filter(block -> block.isOwnedBy(team)).toList();
        List<LocatedBlock> teamForwardMarkers = forwardMarkers.stream().filter(block -> block.isOwnedBy(team)).toList();
        int clusterSize = 0;
        boolean markerInCluster = false;

        if (teamNexuses.size() == 1) {
            Set<ChunkDimPos> cluster = connectedClaimCluster(team, teamNexuses.getFirst().chunkDimPos(), chunkManager);
            clusterSize = cluster.size();
            markerInCluster = teamForwardMarkers.size() == 1 && cluster.contains(teamForwardMarkers.getFirst().chunkDimPos());
        }

        boolean passed = teamNexuses.size() == 1 && teamForwardMarkers.size() == 1 && markerInCluster;
        return new TeamValidation(team, teamNexuses, teamForwardMarkers, clusterSize, markerInCluster, passed);
    }

    private static AttackerValidation validateAttackerSpawn(Team team, List<AttackerSpawn> attackerSpawns) {
        List<AttackerSpawn> teamSpawns = attackerSpawns.stream().filter(spawn -> spawn.isOwnedBy(team)).toList();
        return new AttackerValidation(team, teamSpawns, teamSpawns.size() == 1);
    }

    private static Set<ChunkDimPos> connectedClaimCluster(Team team, ChunkDimPos start, ClaimedChunkManager chunkManager) {
        Collection<? extends ClaimedChunk> claimedChunks = chunkManager.getOrCreateData(team).getClaimedChunks();
        Set<ChunkDimPos> teamClaims = new HashSet<>();
        for (ClaimedChunk chunk : claimedChunks) {
            teamClaims.add(chunk.getPos());
        }

        if (!teamClaims.contains(start)) {
            return Set.of();
        }

        Set<ChunkDimPos> visited = new HashSet<>();
        Queue<ChunkDimPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            ChunkDimPos current = queue.remove();
            for (ChunkDimPos next : neighbors(current)) {
                if (teamClaims.contains(next) && visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return visited;
    }

    private static List<ChunkDimPos> neighbors(ChunkDimPos pos) {
        return List.of(
                pos.offset(1, 0),
                pos.offset(-1, 0),
                pos.offset(0, 1),
                pos.offset(0, -1)
        );
    }

    private static void reportBlocks(CommandSourceStack source, String label, List<LocatedBlock> blocks) {
        ChatFormatting color = blocks.size() == 2 ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> message(color, "Found " + blocks.size() + " " + label + (blocks.size() == 1 ? "" : "s") + "."), false);
        for (LocatedBlock block : blocks) {
            String suffix = block.facing() == null ? "" : " facing " + block.facing();
            String owner = block.ownerName().map(name -> " owner " + name).orElse(" unclaimed");
            source.sendSuccess(() -> message(ChatFormatting.GRAY,
                    "- " + block.dimension().location() + " " + formatPos(block.pos()) + suffix + owner), false);
        }
    }

    private static void reportTeamValidation(CommandSourceStack source, TeamValidation validation) {
        ChatFormatting color = validation.passed() ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> message(color,
                validation.team().getName().getString()
                        + ": nexuses=" + validation.nexuses().size()
                        + ", forwardMarkers=" + validation.forwardMarkers().size()
                        + ", nexusClusterChunks=" + validation.clusterSize()
                        + ", markerInNexusCluster=" + validation.markerInCluster()), false);
    }

    private static void reportAttackerValidation(CommandSourceStack source, AttackerValidation validation) {
        ChatFormatting color = validation.passed() ? ChatFormatting.GREEN : ChatFormatting.RED;
        String location = validation.spawns().stream()
                .findFirst()
                .map(spawn -> " at " + spawn.dimension().location() + " " + formatPos(spawn.pos()))
                .orElse("");
        source.sendSuccess(() -> message(color,
                validation.team().getName().getString() + ": attackerSpawns=" + validation.spawns().size() + location), false);
    }

    private static void reportTeamScan(CommandSourceStack source, TeamValidation validation, ClaimedChunkManager chunkManager) {
        if (!validation.passed()) {
            reportTeamValidation(source, validation);
            return;
        }

        LocatedBlock nexus = validation.nexuses().getFirst();
        LocatedBlock forwardMarker = validation.forwardMarkers().getFirst();
        Set<ChunkDimPos> cluster = connectedClaimCluster(validation.team(), nexus.chunkDimPos(), chunkManager);
        ClusterBounds bounds = ClusterBounds.from(cluster);

        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                validation.team().getName().getString() + " base area"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- dimension: " + nexus.dimension().location()), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- chunks: " + cluster.size()
                        + " bounds [" + bounds.minX() + ", " + bounds.minZ() + "] to [" + bounds.maxX() + ", " + bounds.maxZ() + "]"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- footprint: " + bounds.blockWidth() + " x " + bounds.blockDepth() + " blocks"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- nexus: " + formatPos(nexus.pos())), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- forward marker: " + formatPos(forwardMarker.pos()) + " facing " + forwardMarker.facing()), false);
    }

    private static void reportGuardrails(CommandSourceStack source, TeamValidation validation, ClaimedChunkManager chunkManager) {
        if (!validation.passed()) {
            return;
        }

        reportAndCheckGuardrails(source, BaseArea.from(validation, chunkManager));
    }

    private static boolean reportAndCheckGuardrails(CommandSourceStack source, BaseArea baseArea) {
        int maxChunks = WarDayConfig.MAX_BASE_CHUNKS.getAsInt();
        int maxFootprint = WarDayConfig.MAX_BASE_FOOTPRINT_BLOCKS.getAsInt();
        boolean chunkOk = baseArea.cluster().size() <= maxChunks;
        boolean widthOk = baseArea.bounds().blockWidth() <= maxFootprint;
        boolean depthOk = baseArea.bounds().blockDepth() <= maxFootprint;
        boolean passed = chunkOk && widthOk && depthOk;
        ChatFormatting color = passed ? ChatFormatting.GREEN : ChatFormatting.RED;

        source.sendSuccess(() -> message(color,
                baseArea.team().getName().getString()
                        + " guardrails: chunks " + baseArea.cluster().size() + "/" + maxChunks
                        + ", footprint " + baseArea.bounds().blockWidth() + "x" + baseArea.bounds().blockDepth()
                        + " max " + maxFootprint + "x" + maxFootprint), false);
        return passed;
    }

    private static void reportPlacementPlan(CommandSourceStack source, PlacementPlan plan) {
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                plan.label() + " placement"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- source: " + plan.dimension().location()
                        + " chunks [" + plan.bounds().minX() + ", " + plan.bounds().minZ() + "] to ["
                        + plan.bounds().maxX() + ", " + plan.bounds().maxZ() + "]"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- target anchor: " + formatPos(plan.targetAnchorPos())), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- target footprint: [" + plan.targetMinX() + ", " + plan.targetMinZ() + "] to ["
                        + plan.targetMaxX() + ", " + plan.targetMaxZ() + "]"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- rotation: " + plan.rotationDescription()), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- anchor offset from source min: " + formatPos(plan.anchorOffset())), false);
        plan.baseArea().ifPresent(baseArea -> source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- forward marker offset from source min: " + formatPos(plan.offsetFromSourceMin(baseArea.forwardMarker().pos()))
                        + " facing " + baseArea.forwardMarker().facing()), false));
    }

    private static CopyCheck checkDestinationEmpty(ServerLevel sourceLevel, ServerLevel targetLevel, PlacementPlan plan) {
        if (sourceLevel == null) {
            return new CopyCheck(false, 0, 0, BlockPos.ZERO);
        }

        int checked = 0;
        for (ChunkDimPos chunk : plan.cluster()) {
            int minX = chunk.x() * 16;
            int minZ = chunk.z() * 16;
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    for (int y = sourceLevel.getMinBuildHeight(); y < sourceLevel.getMaxBuildHeight(); y++) {
                        BlockPos sourcePos = new BlockPos(x, y, z);
                        BlockState sourceState = sourceLevel.getBlockState(sourcePos);
                        if (sourceState.isAir()) {
                            continue;
                        }

                        checked++;
                        BlockPos targetPos = plan.targetPos(sourcePos);
                        if (targetPos.getY() < targetLevel.getMinBuildHeight() || targetPos.getY() >= targetLevel.getMaxBuildHeight()) {
                            continue;
                        }
                        if (!targetLevel.getBlockState(targetPos).isAir()) {
                            return new CopyCheck(false, checked, 1, targetPos);
                        }
                    }
                }
            }
        }

        return new CopyCheck(true, checked, 0, BlockPos.ZERO);
    }

    private static void reportCopyCheck(CommandSourceStack source, String teamName, CopyCheck check) {
        ChatFormatting color = check.passed() ? ChatFormatting.GREEN : ChatFormatting.RED;
        String detail = check.passed()
                ? teamName + " destination empty for " + check.sourceBlocksChecked() + " non-air source blocks."
                : teamName + " destination conflict at " + formatPos(check.firstConflict());
        source.sendSuccess(() -> message(color, detail), false);
    }

    private static CopyResult copyBase(ServerLevel sourceLevel, ServerLevel targetLevel, PlacementPlan plan) {
        int blocksCopied = 0;
        int blockEntitiesCopied = 0;
        int containersCleared = 0;

        for (ChunkDimPos chunk : plan.cluster()) {
            int minX = chunk.x() * 16;
            int minZ = chunk.z() * 16;
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    for (int y = sourceLevel.getMinBuildHeight(); y < sourceLevel.getMaxBuildHeight(); y++) {
                        BlockPos sourcePos = new BlockPos(x, y, z);
                        BlockState sourceState = sourceLevel.getBlockState(sourcePos);
                        if (sourceState.isAir()) {
                            continue;
                        }

                        BlockPos targetPos = plan.targetPos(sourcePos);
                        if (targetPos.getY() < targetLevel.getMinBuildHeight() || targetPos.getY() >= targetLevel.getMaxBuildHeight()) {
                            continue;
                        }
                        targetLevel.setBlock(targetPos, plan.targetState(sourceState), 3);
                        blocksCopied++;

                        BlockEntity sourceBlockEntity = sourceLevel.getBlockEntity(sourcePos);
                        BlockEntity targetBlockEntity = targetLevel.getBlockEntity(targetPos);
                        if (sourceBlockEntity != null && targetBlockEntity != null) {
                            CompoundTag tag = sourceBlockEntity.saveWithFullMetadata(sourceLevel.registryAccess());
                            tag.putInt("x", targetPos.getX());
                            tag.putInt("y", targetPos.getY());
                            tag.putInt("z", targetPos.getZ());
                            targetBlockEntity.loadWithComponents(tag, targetLevel.registryAccess());
                            targetBlockEntity.setChanged();
                            blockEntitiesCopied++;

                            if (targetBlockEntity instanceof Container container) {
                                container.clearContent();
                                targetBlockEntity.setChanged();
                                containersCleared++;
                            }
                        }
                    }
                }
            }
        }

        return new CopyResult(blocksCopied, blockEntitiesCopied, containersCleared);
    }

    private static int wipeDestinationArea(ServerLevel targetLevel, PlacementPlan plan) {
        int wiped = 0;
        for (int x = plan.targetMinX(); x <= plan.targetMaxX(); x++) {
            for (int z = plan.targetMinZ(); z <= plan.targetMaxZ(); z++) {
                for (int y = targetLevel.getMinBuildHeight(); y < targetLevel.getMaxBuildHeight(); y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!targetLevel.getBlockState(pos).isAir()) {
                        targetLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        wiped++;
                    }
                }
            }
        }
        return wiped;
    }

    private static EntityCopyResult copyDecorativeEntities(ServerLevel sourceLevel, ServerLevel targetLevel, PlacementPlan plan) {
        if (sourceLevel == null) {
            return new EntityCopyResult(0, 0);
        }

        int copied = 0;
        int itemFramesCleared = 0;
        AABB sourceBounds = plan.sourceEntityBounds(sourceLevel);
        List<Entity> entities = sourceLevel.getEntities((Entity) null, sourceBounds, WarDayCommands::isAllowedDecorativeEntity);

        for (Entity sourceEntity : entities) {
            CompoundTag tag = new CompoundTag();
            if (!sourceEntity.save(tag)) {
                continue;
            }

            tag.remove("UUID");
            tag.put("Pos", newDoubleList(
                    plan.targetX(sourceEntity.getX(), sourceEntity.getZ()),
                    plan.targetY(sourceEntity.getY()),
                    plan.targetZ(sourceEntity.getX(), sourceEntity.getZ())
            ));
            rotateEntityYaw(tag, plan);
            translateHangingEntityTile(tag, plan);

            Optional<Entity> copiedEntity = EntityType.create(tag, targetLevel);
            if (copiedEntity.isEmpty()) {
                continue;
            }

            Entity entity = copiedEntity.get();
            targetLevel.addFreshEntity(entity);
            copied++;

            if (entity instanceof ItemFrame itemFrame) {
                itemFrame.setItem(ItemStack.EMPTY, false);
                itemFramesCleared++;
            }
        }

        return new EntityCopyResult(copied, itemFramesCleared);
    }

    private static Optional<BlockPos> findSafeSpawnPos(ServerLevel level, PlacementPlan plan) {
        BlockPos preferred = plan.targetAnchorPos().above();
        if (isSafeSpawnPos(level, preferred)) {
            return Optional.of(preferred);
        }

        int minX = Math.max(plan.targetMinX(), preferred.getX() - 8);
        int maxX = Math.min(plan.targetMaxX(), preferred.getX() + 8);
        int minZ = Math.max(plan.targetMinZ(), preferred.getZ() - 8);
        int maxZ = Math.min(plan.targetMaxZ(), preferred.getZ() + 8);
        int minY = Math.max(level.getMinBuildHeight() + 1, preferred.getY() - 4);
        int maxY = Math.min(level.getMaxBuildHeight() - 2, preferred.getY() + 4);

        for (int distance = 1; distance <= 8; distance++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (Math.max(Math.abs(x - preferred.getX()), Math.abs(z - preferred.getZ())) != distance) {
                            continue;
                        }

                        BlockPos candidate = new BlockPos(x, y, z);
                        if (isSafeSpawnPos(level, candidate)) {
                            return Optional.of(candidate);
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isSafeSpawnPos(ServerLevel level, BlockPos feetPos) {
        if (feetPos.getY() <= level.getMinBuildHeight() || feetPos.getY() + 1 >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockPos groundPos = feetPos.below();
        return hasCollision(level, groundPos) && isClearForPlayer(level, feetPos) && isClearForPlayer(level, feetPos.above());
    }

    private static boolean hasCollision(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isClearForPlayer(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    private static boolean isAllowedDecorativeEntity(Entity entity) {
        return entity instanceof Painting || entity instanceof ItemFrame;
    }

    private static void rotateEntityYaw(CompoundTag tag, PlacementPlan plan) {
        if (!tag.contains("Rotation")) {
            return;
        }

        ListTag rotation = tag.getList("Rotation", net.minecraft.nbt.Tag.TAG_FLOAT);
        if (rotation.isEmpty()) {
            return;
        }

        rotation.set(0, net.minecraft.nbt.FloatTag.valueOf(rotation.getFloat(0) + plan.rotationDegrees()));
    }

    private static void translateHangingEntityTile(CompoundTag tag, PlacementPlan plan) {
        if (tag.contains("TileX") && tag.contains("TileY") && tag.contains("TileZ")) {
            BlockPos targetPos = plan.targetPos(new BlockPos(tag.getInt("TileX"), tag.getInt("TileY"), tag.getInt("TileZ")));
            tag.putInt("TileX", targetPos.getX());
            tag.putInt("TileY", targetPos.getY());
            tag.putInt("TileZ", targetPos.getZ());
        }
    }

    private static ListTag newDoubleList(double x, double y, double z) {
        ListTag list = new ListTag();
        list.add(net.minecraft.nbt.DoubleTag.valueOf(x));
        list.add(net.minecraft.nbt.DoubleTag.valueOf(y));
        list.add(net.minecraft.nbt.DoubleTag.valueOf(z));
        return list;
    }

    private static net.minecraft.network.chat.MutableComponent message(ChatFormatting color, String text) {
        return net.minecraft.network.chat.Component.literal(text).withStyle(color);
    }

    private static String formatPos(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    private record LocatedBlock(ResourceKey<Level> dimension, BlockPos pos, Direction facing, Optional<Team> owner) {
        private boolean isOwnedBy(Team team) {
            return owner.map(value -> value.getId().equals(team.getId())).orElse(false);
        }

        private Optional<String> ownerName() {
            return owner.map(team -> team.getName().getString());
        }

        private ChunkDimPos chunkDimPos() {
            return new ChunkDimPos(dimension, new ChunkPos(pos));
        }
    }

    private record AttackerSpawn(ResourceKey<Level> dimension, BlockPos pos, Optional<Team> owner) {
        private boolean isOwnedBy(Team team) {
            return owner.map(value -> value.getId().equals(team.getId())).orElse(false);
        }

        private ChunkDimPos chunkDimPos() {
            return new ChunkDimPos(dimension, new ChunkPos(pos));
        }
    }

    private record TeamValidation(
            Team team,
            List<LocatedBlock> nexuses,
            List<LocatedBlock> forwardMarkers,
            int clusterSize,
            boolean markerInCluster,
            boolean passed
    ) {
    }

    private record AttackerValidation(Team team, List<AttackerSpawn> spawns, boolean passed) {
        private AttackerSpawn spawn() {
            return spawns.getFirst();
        }
    }

    private record ScanContext(
            ServerLevel level,
            BlockPos center,
            int radius,
            ClaimedChunkManager chunkManager,
            Team teamA,
            Optional<Team> teamB,
            List<LocatedBlock> nexuses,
            List<LocatedBlock> forwardMarkers,
            List<AttackerSpawn> attackerSpawns
    ) {
    }

    private record ClusterBounds(int minX, int minZ, int maxX, int maxZ) {
        private static ClusterBounds from(Set<ChunkDimPos> cluster) {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (ChunkDimPos pos : cluster) {
                minX = Math.min(minX, pos.x());
                minZ = Math.min(minZ, pos.z());
                maxX = Math.max(maxX, pos.x());
                maxZ = Math.max(maxZ, pos.z());
            }

            return new ClusterBounds(minX, minZ, maxX, maxZ);
        }

        private int blockWidth() {
            return (maxX - minX + 1) * 16;
        }

        private int blockDepth() {
            return (maxZ - minZ + 1) * 16;
        }

        private int minBlockX() {
            return minX * 16;
        }

        private int minBlockZ() {
            return minZ * 16;
        }
    }

    private record BaseArea(
            Team team,
            LocatedBlock nexus,
            LocatedBlock forwardMarker,
            Set<ChunkDimPos> cluster,
            ClusterBounds bounds,
            ResourceKey<Level> dimension
    ) {
        private static BaseArea from(TeamValidation validation, ClaimedChunkManager chunkManager) {
            LocatedBlock nexus = validation.nexuses().getFirst();
            LocatedBlock forwardMarker = validation.forwardMarkers().getFirst();
            Set<ChunkDimPos> cluster = connectedClaimCluster(validation.team(), nexus.chunkDimPos(), chunkManager);
            return new BaseArea(validation.team(), nexus, forwardMarker, cluster, ClusterBounds.from(cluster), nexus.dimension());
        }
    }

    private record AttackerArea(
            Team team,
            AttackerSpawn spawn,
            Set<ChunkDimPos> cluster,
            ClusterBounds bounds,
            ResourceKey<Level> dimension
    ) {
        private static AttackerArea from(AttackerValidation validation, ClaimedChunkManager chunkManager) {
            AttackerSpawn spawn = validation.spawn();
            Set<ChunkDimPos> cluster = attackerSpawnCluster(validation.team(), spawn, chunkManager);
            return new AttackerArea(validation.team(), spawn, cluster, ClusterBounds.from(cluster), spawn.dimension());
        }

        private static Set<ChunkDimPos> attackerSpawnCluster(Team team, AttackerSpawn spawn, ClaimedChunkManager chunkManager) {
            ChunkDimPos spawnChunk = spawn.chunkDimPos();
            Set<ChunkDimPos> cluster = connectedClaimCluster(team, spawnChunk, chunkManager);
            if (cluster.contains(spawnChunk)) {
                return cluster;
            }

            Set<ChunkDimPos> clusterWithSpawn = new HashSet<>(cluster);
            clusterWithSpawn.add(spawnChunk);
            return clusterWithSpawn;
        }
    }

    private record ResolvedBases(BaseArea teamA, Optional<AttackerArea> attackerArea) {
    }

    private record PlacementPlan(
            String label,
            ResourceKey<Level> dimension,
            BlockPos anchorPos,
            Set<ChunkDimPos> cluster,
            ClusterBounds bounds,
            BlockPos targetAnchorPos,
            Optional<BaseArea> baseArea
    ) {
        private static final Direction DEFENDER_TARGET_FACING = Direction.EAST;

        private static PlacementPlan from(BaseArea baseArea, BlockPos targetAnchorPos) {
            return new PlacementPlan(
                    baseArea.team().getName().getString(),
                    baseArea.dimension(),
                    baseArea.nexus().pos(),
                    baseArea.cluster(),
                    baseArea.bounds(),
                    targetAnchorPos,
                    Optional.of(baseArea)
            );
        }

        private static PlacementPlan from(BaseArea baseArea) {
            return from(baseArea, new BlockPos(0, WarDayConfig.WAR_DAY_BASE_Y.getAsInt(), 0));
        }

        private static PlacementPlan from(AttackerArea area) {
            return new PlacementPlan(
                    area.team().getName().getString() + " spawn area",
                    area.dimension(),
                    area.spawn().pos(),
                    area.cluster(),
                    area.bounds(),
                    new BlockPos(WarDayConfig.BASE_SPACING_BLOCKS.getAsInt(), WarDayConfig.WAR_DAY_BASE_Y.getAsInt(), 0),
                    Optional.empty()
            );
        }

        private int targetMinX() {
            return targetFootprint().minX();
        }

        private int targetMinZ() {
            return targetFootprint().minZ();
        }

        private int targetMaxX() {
            return targetFootprint().maxX();
        }

        private int targetMaxZ() {
            return targetFootprint().maxZ();
        }

        private BlockPos anchorOffset() {
            return offsetFromSourceMin(anchorPos);
        }

        private BlockPos offsetFromSourceMin(BlockPos sourcePos) {
            return new BlockPos(
                    sourcePos.getX() - bounds.minBlockX(),
                    sourcePos.getY(),
                    sourcePos.getZ() - bounds.minBlockZ()
            );
        }

        private BlockPos targetPos(BlockPos sourcePos) {
            BlockPos rotatedOffset = rotateOffset(
                    sourcePos.getX() - anchorPos.getX(),
                    sourcePos.getY() - anchorPos.getY(),
                    sourcePos.getZ() - anchorPos.getZ()
            );
            return new BlockPos(
                    targetAnchorPos.getX() + rotatedOffset.getX(),
                    targetAnchorPos.getY() + rotatedOffset.getY(),
                    targetAnchorPos.getZ() + rotatedOffset.getZ()
            );
        }

        private BlockState targetState(BlockState sourceState) {
            return sourceState.rotate(rotation());
        }

        private AABB sourceEntityBounds(ServerLevel sourceLevel) {
            return new AABB(
                    bounds.minBlockX(),
                    sourceLevel.getMinBuildHeight(),
                    bounds.minBlockZ(),
                    bounds.minBlockX() + bounds.blockWidth(),
                    sourceLevel.getMaxBuildHeight(),
                    bounds.minBlockZ() + bounds.blockDepth()
            );
        }

        private double targetY(double sourceY) {
            return sourceY + targetAnchorPos.getY() - anchorPos.getY();
        }

        private double targetX(double sourceX, double sourceZ) {
            return targetAnchorPos.getX() + rotatedOffsetX(sourceX - anchorPos.getX(), sourceZ - anchorPos.getZ());
        }

        private double targetZ(double sourceX, double sourceZ) {
            return targetAnchorPos.getZ() + rotatedOffsetZ(sourceX - anchorPos.getX(), sourceZ - anchorPos.getZ());
        }

        private Rotation rotation() {
            return baseArea
                    .map(area -> rotationBetween(area.forwardMarker().facing(), DEFENDER_TARGET_FACING))
                    .orElse(Rotation.NONE);
        }

        private String rotationDescription() {
            return baseArea
                    .map(area -> area.forwardMarker().facing().getName() + " -> " + DEFENDER_TARGET_FACING.getName()
                            + " (" + rotationDegrees() + " degrees)")
                    .orElse("none");
        }

        private int rotationDegrees() {
            return switch (rotation()) {
                case NONE -> 0;
                case CLOCKWISE_90 -> 90;
                case CLOCKWISE_180 -> 180;
                case COUNTERCLOCKWISE_90 -> -90;
            };
        }

        private TargetFootprint targetFootprint() {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            int sourceMinX = bounds.minBlockX();
            int sourceMinZ = bounds.minBlockZ();
            int sourceMaxX = sourceMinX + bounds.blockWidth() - 1;
            int sourceMaxZ = sourceMinZ + bounds.blockDepth() - 1;
            int[] xs = {sourceMinX, sourceMaxX};
            int[] zs = {sourceMinZ, sourceMaxZ};
            for (int x : xs) {
                for (int z : zs) {
                    BlockPos target = targetPos(new BlockPos(x, anchorPos.getY(), z));
                    minX = Math.min(minX, target.getX());
                    minZ = Math.min(minZ, target.getZ());
                    maxX = Math.max(maxX, target.getX());
                    maxZ = Math.max(maxZ, target.getZ());
                }
            }

            return new TargetFootprint(minX, minZ, maxX, maxZ);
        }

        private BlockPos rotateOffset(int x, int y, int z) {
            return switch (rotation()) {
                case NONE -> new BlockPos(x, y, z);
                case CLOCKWISE_90 -> new BlockPos(-z, y, x);
                case CLOCKWISE_180 -> new BlockPos(-x, y, -z);
                case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, -x);
            };
        }

        private double rotatedOffsetX(double x, double z) {
            return switch (rotation()) {
                case NONE -> x;
                case CLOCKWISE_90 -> -z;
                case CLOCKWISE_180 -> -x;
                case COUNTERCLOCKWISE_90 -> z;
            };
        }

        private double rotatedOffsetZ(double x, double z) {
            return switch (rotation()) {
                case NONE -> z;
                case CLOCKWISE_90 -> x;
                case CLOCKWISE_180 -> -z;
                case COUNTERCLOCKWISE_90 -> -x;
            };
        }

        private static Rotation rotationBetween(Direction source, Direction target) {
            int turns = Math.floorMod(horizontalIndex(target) - horizontalIndex(source), 4);
            return switch (turns) {
                case 0 -> Rotation.NONE;
                case 1 -> Rotation.CLOCKWISE_90;
                case 2 -> Rotation.CLOCKWISE_180;
                case 3 -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            };
        }

        private static int horizontalIndex(Direction direction) {
            return switch (direction) {
                case NORTH -> 0;
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0;
            };
        }

    }

    private record TargetFootprint(int minX, int minZ, int maxX, int maxZ) {
    }

    private record CopyCheck(boolean passed, int sourceBlocksChecked, int conflicts, BlockPos firstConflict) {
    }

    private record CopyResult(int blocksCopied, int blockEntitiesCopied, int containersCleared) {
    }

    private record EntityCopyResult(int entitiesCopied, int itemFramesCleared) {
    }

    private record ParticipantRespawn(ServerLevel level, BlockPos spawnPos, Item matchBlock) {
    }

    private record PendingRespawn(int ticksRemaining, ParticipantRespawn participant) {
        private PendingRespawn tick() {
            return new PendingRespawn(ticksRemaining - 1, participant);
        }
    }
}
