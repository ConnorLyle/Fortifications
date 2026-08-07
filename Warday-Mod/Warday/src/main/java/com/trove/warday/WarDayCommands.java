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
import net.minecraft.nbt.Tag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ExperienceOrb;
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
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
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
    private static final int MATCH_BORDER_SPAWN_MARGIN = 16;
    private static final double HANGING_ENTITY_SCAN_MARGIN = 8.0D;
    private static final String MATCH_ENTITY_MARKER = "warday_match_entity";
    private static final String MATCH_ENTITY_BATCH = "warday_match_entity_batch";
    private static final String PREPARED_DECORATIVE_MARKER = "warday_prepared_decorative";
    private static final String WARDAY_ROSTER_OBJECTIVE = "warday_roster";
    private static final int ROSTER_PLAYERS_PER_TEAM_PAGE = 6;
    private static final long ROSTER_PAGE_TICKS = 100L;
    private static final SuggestionProvider<CommandSourceStack> TEAM_NAME_SUGGESTIONS = WarDayCommands::suggestTeamNames;
    private static final Map<UUID, PendingRespawn> PENDING_RESPAWNS = new HashMap<>();
    private static final Map<UUID, Integer> DEATH_COUNTS = new HashMap<>();
    private static final Map<UUID, Deque<Long>> DIG_HISTORY = new HashMap<>();
    private static ServerBossEvent matchTimerBossBar;
    private static MinecraftServer matchTimerBossBarServer;
    private static long lastBossBarSeconds = Long.MIN_VALUE;
    private static MinecraftServer rosterScoreboardServer;
    private static String lastRosterSignature = "";

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
                "Scanning loaded configured-team claims within " + context.radius() + " blocks around "
                        + context.level().dimension().location() + " " + formatPos(context.center())), false);

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
                "Scanning loaded configured-team claims within " + context.radius() + " blocks around "
                        + context.level().dimension().location() + " " + formatPos(context.center())), false);

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
        WarDayState state = WarDayState.get(source.getServer());
        if (state.isActive()) {
            source.sendFailure(message(ChatFormatting.RED, "War Day cannot be prepared while a match is active."));
            return 0;
        }

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
            source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                    "Destination conflicts found; clearing transformed claimed areas before paste."), true);
        }

        int teamAWiped = wipeDestinationArea(defenderSourceLevel, targetLevel, teamAPlan);
        int teamADecorationsCleared = clearDestinationDecorativeEntities(targetLevel, teamAPlan);
        source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                "Cleared " + teamAWiped + " defender destination blocks and " + teamADecorationsCleared
                        + " existing hanging entities from transformed claimed chunks."), true);
        if (attackerPlan.isPresent()) {
            int attackerWiped = wipeDestinationArea(attackerSourceLevel, targetLevel, attackerPlan.get());
            int attackerDecorationsCleared = clearDestinationDecorativeEntities(targetLevel, attackerPlan.get());
            source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                    "Cleared " + attackerWiped + " attacker destination blocks and " + attackerDecorationsCleared
                            + " existing hanging entities from transformed claimed chunks."), true);
        }

        CopyResult teamAResult = copyBase(defenderSourceLevel, targetLevel, teamAPlan);
        EntityCopyResult teamAEntityResult = copyDecorativeEntities(defenderSourceLevel, targetLevel, teamAPlan);
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "Copied " + bases.teamA().team().getName().getString() + ": " + teamAResult.blocksCopied()
                        + " blocks, " + teamAResult.blockEntitiesCopied() + " block entities, "
                        + teamAResult.containersCleared() + " containers cleared, "
                        + teamAEntityResult.entitiesCopied() + " decorative entities, "
                        + teamAEntityResult.itemFramesCleared() + " item frames cleared, "
                        + teamAEntityResult.entitiesFailed() + " decorative entities failed validation/copy."), true);
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
                            + attackerEntityResult.itemFramesCleared() + " item frames cleared, "
                            + attackerEntityResult.entitiesFailed() + " decorative entities failed validation/copy. Safe spawn at "
                            + formatPos(spawnPos)), true);
        }
        int entityLimit = WarDayConfig.MAX_PREPARED_ENTITIES.getAsInt();
        Set<UUID> capturedEntityRoots = new HashSet<>();
        EntityTemplateCapture defenderEntityCapture = capturePreparedEntityTemplates(
                defenderSourceLevel,
                targetLevel,
                teamAPlan,
                entityLimit,
                capturedEntityRoots
        );
        List<CompoundTag> preparedEntityTemplates = new ArrayList<>(defenderEntityCapture.templates());
        EntityTemplateCapture attackerEntityCapture = EntityTemplateCapture.empty();
        if (attackerPlan.isPresent()) {
            int remainingEntities = entityLimit - defenderEntityCapture.entityCount();
            attackerEntityCapture = capturePreparedEntityTemplates(
                    attackerSourceLevel,
                    targetLevel,
                    attackerPlan.get(),
                    Math.max(0, remainingEntities),
                    capturedEntityRoots
            );
            preparedEntityTemplates.addAll(attackerEntityCapture.templates());
        }
        int preparedEntityCount = defenderEntityCapture.entityCount() + attackerEntityCapture.entityCount();
        int entityTemplatesSkipped = defenderEntityCapture.skippedCount() + attackerEntityCapture.skippedCount();

        BlockPos copiedNexusPos = teamAPlan.targetPos(bases.teamA().nexus().pos());
        buildNexusShell(targetLevel, copiedNexusPos);
        state.markPrepared(
                WarDayConfig.WAR_DAY_DIMENSION.get(),
                bases.teamA().team().getName().getString(),
                bases.attackerArea().isPresent() ? WarDayConfig.TEAM_B_NAME.get() : "",
                copiedNexusPos,
                safeAttackerSpawn.orElse(null),
                preparedEntityTemplates
        );
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "Built protected nexus shell at " + formatPos(copiedNexusPos) + "."), true);
        source.sendSuccess(() -> message(
                entityTemplatesSkipped == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW,
                "Prepared " + preparedEntityCount + " non-player entities in " + preparedEntityTemplates.size()
                        + " entity groups for match-time cloning; skipped " + entityTemplatesSkipped + "."
        ), true);
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
            source.sendSuccess(() -> message(ChatFormatting.GRAY,
                    "Prepared non-player entity groups: " + state.preparedEntityTemplates().size()), false);
            if (state.isActive()) {
                long gameTime = warDayLevel(source.getServer(), state)
                        .map(ServerLevel::getGameTime)
                        .orElse(source.getLevel().getGameTime());
                if (state.isFanfareActive()) {
                    long secondsRemaining = secondsRemaining(state.fanfareEndGameTime(), gameTime);
                    source.sendSuccess(() -> message(ChatFormatting.GOLD,
                            "Victory fanfare: " + state.winningTeam() + " wins; returning players in "
                                    + secondsRemaining + " seconds"), false);
                } else {
                    long secondsRemaining = secondsRemaining(state.matchEndGameTime(), gameTime);
                    source.sendSuccess(() -> message(ChatFormatting.GRAY,
                            "Match time remaining: " + secondsRemaining + " seconds"), false);
                }
            }
            if (state.isActive()) {
                source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                        "Operator override: /warday end immediately restores players"), false);
            } else {
                source.sendSuccess(() -> message(ChatFormatting.GREEN, "Next command: /warday start"), false);
            }
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

        BlockPos copiedNexusPos = state.copiedNexusPos().get();
        Optional<BlockPos> defenderSpawn = findSafeSpawnNear(warDayLevel, copiedNexusPos.offset(0, 1, 0), 8, 4);
        Optional<BlockPos> attackerSpawn = findSafeSpawnNear(warDayLevel, state.attackerSpawnPos().get(), 8, 4);
        if (defenderSpawn.isEmpty()) {
            source.sendFailure(message(ChatFormatting.RED,
                    "No safe defender spawn exists near the copied nexus. Repair the platform and rerun /warday prepare confirm."));
            return 0;
        }
        if (attackerSpawn.isEmpty()) {
            source.sendFailure(message(ChatFormatting.RED,
                    "No safe attacker spawn exists. Repair the platform and rerun /warday prepare confirm."));
            return 0;
        }
        if (!isInsideConfiguredMatchBorder(copiedNexusPos, attackerSpawn.get())) {
            source.sendFailure(message(ChatFormatting.RED,
                    "The prepared attacker spawn is outside the configured match border. Rerun /warday prepare confirm with this updated mod before starting."));
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
        List<ServerPlayer> onlinePlayers = List.copyOf(source.getServer().getPlayerList().getPlayers());

        for (ServerPlayer player : onlinePlayers) {
            UUID id = player.getUUID();
            try {
                snapshots.put(id, snapshotPlayer(player));
            } catch (IllegalStateException exception) {
                source.sendFailure(message(ChatFormatting.RED,
                        "Could not safely snapshot " + player.getName().getString() + " inventory. War Day was not started; check the server log."));
                return 0;
            }
        }

        boolean originalKeepInventory = warDayLevel.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        double originalWorldBorderCenterX = warDayLevel.getWorldBorder().getCenterX();
        double originalWorldBorderCenterZ = warDayLevel.getWorldBorder().getCenterZ();
        double originalWorldBorderSize = warDayLevel.getWorldBorder().getSize();
        long matchDurationTicks = WarDayConfig.MATCH_DURATION_SECONDS.getAsInt() * 20L;
        long matchEndGameTime = warDayLevel.getGameTime() + matchDurationTicks;
        String previousSidebarObjective = currentSidebarObjectiveName(source.getServer());
        state.start(
                snapshots,
                defenderIds,
                attackerIds,
                matchEndGameTime,
                matchDurationTicks,
                previousSidebarObjective,
                originalKeepInventory,
                originalWorldBorderCenterX,
                originalWorldBorderCenterZ,
                originalWorldBorderSize
        );

        warDayLevel.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, source.getServer());
        configureWorldBorder(warDayLevel, state.copiedNexusPos().get());
        spawnNexusMarker(warDayLevel, state, state.copiedNexusPos().get());
        DEATH_COUNTS.clear();
        DIG_HISTORY.clear();
        PENDING_RESPAWNS.clear();
        int staleEntityClonesRemoved = clearPreparedMatchEntities(warDayLevel);
        UUID entityBatchId = state.matchEntityBatchId().orElseThrow();
        EntityTemplateSpawn entitySpawn = spawnPreparedEntityTemplates(
                warDayLevel,
                state.preparedEntityTemplates(),
                entityBatchId
        );

        for (ServerPlayer player : onlinePlayers) {
            player.closeContainer();
            UUID id = player.getUUID();
            if (defenderIds.contains(id)) {
                BlockPos spawn = defenderSpawn.get();
                teleportPlayer(player, warDayLevel, spawn);
                setPlayerSpawn(player, warDayLevel, spawn);
                player.setGameMode(GameType.SURVIVAL);
                ensureInventoryHasAtLeast(player, defenderBlock, MATCH_BLOCK_TARGET_COUNT);
                defenders++;
            } else if (attackerIds.contains(id)) {
                BlockPos spawn = attackerSpawn.get();
                teleportPlayer(player, warDayLevel, spawn);
                setPlayerSpawn(player, warDayLevel, spawn);
                player.setGameMode(GameType.SURVIVAL);
                ensureInventoryHasAtLeast(player, attackerBlock, MATCH_BLOCK_TARGET_COUNT);
                attackers++;
            } else {
                player.setGameMode(GameType.SPECTATOR);
                teleportPlayer(player, warDayLevel, attackerSpawn.get().offset(0, 11, 0));
                spectators++;
            }
        }
        syncMatchTimerBossBar(source.getServer(), warDayLevel, state, true);
        syncWarDaySidebar(source.getServer(), state, warDayLevel.getGameTime(), true);
        source.getServer().getPlayerList().broadcastSystemMessage(
                message(ChatFormatting.GREEN, "War Day started for " + WarDayConfig.MATCH_DURATION_SECONDS.getAsInt()
                        + " seconds. Defenders=" + defenders + ", attackers=" + attackers + ", spectators=" + spectators
                        + ", prepared entities=" + entitySpawn.spawnedCount() + "."),
                false
        );
        if (staleEntityClonesRemoved > 0 || entitySpawn.failedCount() > 0) {
            int staleRemoved = staleEntityClonesRemoved;
            source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                    "Entity clone cleanup/spawn details: stale removed=" + staleRemoved
                            + ", failed to spawn=" + entitySpawn.failedCount() + "."), true);
        }
        return 1;
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !event.getEntity().getPersistentData().getBoolean(MATCH_ENTITY_MARKER)) {
            return;
        }

        WarDayState state = WarDayState.get(level.getServer());
        CompoundTag persistentData = event.getEntity().getPersistentData();
        Optional<UUID> activeBatchId = state.matchEntityBatchId();
        boolean belongsToActiveMatch = state.isActive()
                && level.dimension().location().toString().equals(state.warDayDimension())
                && persistentData.hasUUID(MATCH_ENTITY_BATCH)
                && activeBatchId.map(id -> id.equals(persistentData.getUUID(MATCH_ENTITY_BATCH))).orElse(false);
        if (!belongsToActiveMatch) {
            event.setCanceled(true);
            event.getEntity().discard();
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        WarDayState state = WarDayState.get(player.getServer());
        if (state.isFanfareActive() && state.savedPlayer(player.getUUID()).isPresent()) {
            applyFanfareRole(player, state);
            return;
        }

        Optional<ParticipantRespawn> respawn = participantRespawn(player);
        if (respawn.isEmpty()) {
            giveRespawnMatchBlocks(player);
            return;
        }

        ParticipantRespawn participant = respawn.get();
        int deathCount = state.incrementDeathCount(player.getUUID());
        DEATH_COUNTS.put(player.getUUID(), deathCount);
        int delaySeconds = respawnDelaySecondsForDeath(deathCount);
        int delayTicks = delaySeconds * 20;
        if (delayTicks <= 0) {
            restoreDelayedRespawn(player, participant);
            return;
        }

        PendingRespawn pending = new PendingRespawn(delayTicks, participant, null);
        state.setPendingRespawnTicks(player.getUUID(), delayTicks);
        pending = beginRespawnSpectating(player, state, pending, true);
        PENDING_RESPAWNS.put(player.getUUID(), pending);
        player.sendSystemMessage(message(ChatFormatting.YELLOW,
                "Respawning in " + delaySeconds + " seconds. Left click views the previous living teammate; right click views the next."));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        WarDayState state = WarDayState.get(player.getServer());
        if (state.isActive()) {
            if (state.isFanfareActive()) {
                if (state.savedPlayer(player.getUUID()).isPresent()) {
                    applyFanfareRole(player, state);
                } else {
                    player.sendSystemMessage(message(ChatFormatting.YELLOW,
                            "War Day is in its victory celebration. You will remain outside this match."));
                }
                return;
            }
            if (state.savedPlayer(player.getUUID()).isEmpty()) {
                try {
                    state.savePlayerIfAbsent(player.getUUID(), snapshotPlayer(player));
                } catch (IllegalStateException exception) {
                    player.sendSystemMessage(message(ChatFormatting.RED,
                            "Your inventory could not be safely snapshotted, so you were not moved into the active War Day. Contact an operator."));
                    return;
                }
            }
            applyActiveMatchRole(player, state);
            warDayLevel(player.getServer(), state).ifPresent(level ->
                    syncMatchTimerBossBar(player.getServer(), level, state, true)
            );
            long rosterGameTime = warDayLevel(player.getServer(), state)
                    .map(ServerLevel::getGameTime)
                    .orElse(player.getServer().overworld().getGameTime());
            syncWarDaySidebar(player.getServer(), state, rosterGameTime, true);
            return;
        }

        state.savedPlayer(player.getUUID()).ifPresent(snapshot -> {
            if (restorePlayer(player.getServer(), player, snapshot)) {
                state.removeSavedPlayer(player.getUUID());
                player.sendSystemMessage(message(ChatFormatting.YELLOW, "Your complete pre-War Day player state was restored."));
            } else {
                player.sendSystemMessage(message(ChatFormatting.RED,
                        "Your location was restored, but inventory restoration could not be verified. Your recovery snapshot was retained; contact an operator."));
            }
        });
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && matchTimerBossBar != null) {
            matchTimerBossBar.removePlayer(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        WarDayState state = WarDayState.get(event.getServer());
        if (!state.isActive()) {
            PENDING_RESPAWNS.clear();
            hideMatchTimerBossBar(event.getServer());
            return;
        }

        Optional<ServerLevel> warDayLevel = warDayLevel(event.getServer(), state);
        long rosterGameTime = warDayLevel
                .map(ServerLevel::getGameTime)
                .orElse(event.getServer().overworld().getGameTime());
        if (rosterScoreboardServer != event.getServer() || rosterGameTime % 20L == 0L) {
            syncWarDaySidebar(event.getServer(), state, rosterGameTime, false);
        }
        if (state.isFanfareActive()) {
            hideMatchTimerBossBar(event.getServer());
            warDayLevel.ifPresent(level -> tickVictoryFanfare(event.getServer(), level, state));
            return;
        }

        if (warDayLevel.isPresent()) {
            syncMatchTimerBossBar(event.getServer(), warDayLevel.get(), state, false);
        } else {
            hideMatchTimerBossBar(event.getServer());
        }

        if (warDayLevel.isPresent() && state.matchEndGameTime() > 0L && warDayLevel.get().getGameTime() >= state.matchEndGameTime()) {
            String defenderTeam = state.defenderTeam().isBlank() ? WarDayConfig.TEAM_A_NAME.get() : state.defenderTeam();
            beginVictoryFanfare(
                    event.getServer(),
                    warDayLevel.get(),
                    state,
                    defenderTeam,
                    "The nexus survived until time expired.",
                    ""
            );
            return;
        }

        for (Map.Entry<UUID, Integer> entry : state.pendingRespawns().entrySet()) {
            if (PENDING_RESPAWNS.containsKey(entry.getKey())) {
                continue;
            }

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }

            Optional<ParticipantRespawn> respawn = participantRespawn(player);
            respawn.ifPresent(participant -> {
                PendingRespawn pending = new PendingRespawn(entry.getValue(), participant, null);
                PENDING_RESPAWNS.put(entry.getKey(), beginRespawnSpectating(player, state, pending, false));
            });
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
                state.setPendingRespawnTicks(player.getUUID(), pending.ticksRemaining());
                pending = maintainRespawnSpectating(player, state, pending, false);
                entry.setValue(pending);
                continue;
            }

            Optional<ParticipantRespawn> currentRespawn = participantRespawn(player);
            restoreDelayedRespawn(player, currentRespawn.orElse(pending.participant()));
            state.removePendingRespawn(player.getUUID());
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

        if (state.isFanfareActive()) {
            event.setCanceled(true);
            player.displayClientMessage(message(ChatFormatting.GOLD, "Combat has ended. Enjoy the victory celebration!"), true);
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

        String breaker = player.getName().getString();
        String attackerTeam = state.attackerTeam().isBlank() ? WarDayConfig.TEAM_B_NAME.get() : state.attackerTeam();
        beginVictoryFanfare(
                level.getServer(),
                level,
                state,
                attackerTeam,
                breaker + " destroyed the nexus.",
                breaker
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

        if (state.isFanfareActive()) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(message(ChatFormatting.GOLD, "Building is disabled during the victory celebration."), true);
            }
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

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
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

        if (state.isFanfareActive()) {
            event.getAffectedBlocks().clear();
            return;
        }

        BlockPos nexusPos = state.copiedNexusPos().get();
        event.getAffectedBlocks().removeIf(pos -> isInNexusShell(pos, nexusPos));
    }

    @SubscribeEvent
    public void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
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

        if (isInNexusShell(event.getPos(), state.copiedNexusPos().get())) {
            event.setCanceled(true);
        }
    }

    private static boolean beginVictoryFanfare(
            MinecraftServer server,
            ServerLevel level,
            WarDayState state,
            String winningTeam,
            String victoryReason,
            String victoryActor
    ) {
        long fanfareEndGameTime = level.getGameTime() + WarDayConfig.VICTORY_FANFARE_SECONDS.getAsInt() * 20L;
        if (!state.beginFanfare(fanfareEndGameTime, winningTeam, victoryReason, victoryActor)) {
            return false;
        }

        hideMatchTimerBossBar(server);
        PENDING_RESPAWNS.clear();
        DEATH_COUNTS.clear();
        DIG_HISTORY.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (state.savedPlayer(player.getUUID()).isPresent()) {
                applyFanfareRole(player, state);
            } else {
                sendVictoryTitle(player, state);
            }
        }
        playVictoryEffects(level, state);

        int durationSeconds = WarDayConfig.VICTORY_FANFARE_SECONDS.getAsInt();
        server.getPlayerList().broadcastSystemMessage(
                message(ChatFormatting.GOLD,
                        "War Day victory: " + winningTeam + "! " + victoryReason
                                + " Players return in " + durationSeconds + " seconds."),
                false
        );
        return true;
    }

    private static void tickVictoryFanfare(MinecraftServer server, ServerLevel level, WarDayState state) {
        long gameTime = level.getGameTime();
        if (gameTime >= state.fanfareEndGameTime()) {
            String winningTeam = state.winningTeam();
            String victoryReason = state.victoryReason();
            int restored = endActiveWarDay(server, state);
            server.getPlayerList().broadcastSystemMessage(
                    message(ChatFormatting.GOLD,
                            "War Day complete: " + winningTeam + " won. " + victoryReason
                                    + " Restored " + restored + " online players."),
                    false
            );
            return;
        }

        if (gameTime % 20L == 0L) {
            long secondsRemaining = secondsRemaining(state.fanfareEndGameTime(), gameTime);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (state.savedPlayer(player.getUUID()).isEmpty()) {
                    continue;
                }
                if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                    player.setGameMode(GameType.SPECTATOR);
                }
                player.displayClientMessage(message(ChatFormatting.GOLD,
                        state.winningTeam() + " wins! Returning in " + secondsRemaining + " seconds."), true);
            }
        }

        if (gameTime % 40L == 0L) {
            playVictoryEffects(level, state);
        }
    }

    private static void applyFanfareRole(ServerPlayer player, WarDayState state) {
        stopRespawnSpectating(player);
        player.setGameMode(GameType.SPECTATOR);
        warDayLevel(player.getServer(), state).ifPresent(level -> {
            if (player.level() != level) {
                BlockPos focus = state.copiedNexusPos().orElse(BlockPos.ZERO).offset(0, 8, 0);
                teleportPlayer(player, level, focus);
            }
        });
        sendVictoryTitle(player, state);
        long gameTime = warDayLevel(player.getServer(), state)
                .map(ServerLevel::getGameTime)
                .orElse(0L);
        long remaining = secondsRemaining(state.fanfareEndGameTime(), gameTime);
        player.sendSystemMessage(message(ChatFormatting.GOLD,
                "Combat has ended. " + state.winningTeam() + " won; returning in " + remaining + " seconds."));
    }

    private static void sendVictoryTitle(ServerPlayer player, WarDayState state) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(
                message(ChatFormatting.GOLD, state.winningTeam() + " wins!")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                message(ChatFormatting.YELLOW, state.victoryReason())));
    }

    private static void playVictoryEffects(ServerLevel level, WarDayState state) {
        if (state.copiedNexusPos().isEmpty()) {
            return;
        }
        BlockPos nexusPos = state.copiedNexusPos().get();
        double x = nexusPos.getX() + 0.5D;
        double y = nexusPos.getY() + 2.0D;
        double z = nexusPos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 80, 5.0D, 3.0D, 5.0D, 0.15D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 40, 4.0D, 2.0D, 4.0D, 0.1D);
        level.playSound(null, nexusPos, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.MASTER, 2.0F, 1.0F);
    }

    private static long secondsRemaining(long endGameTime, long gameTime) {
        return Math.max(0L, (endGameTime - gameTime + 19L) / 20L);
    }

    private static void syncMatchTimerBossBar(
            MinecraftServer server,
            ServerLevel level,
            WarDayState state,
            boolean force
    ) {
        if (!state.isCombatActive() || state.matchEndGameTime() <= 0L) {
            hideMatchTimerBossBar(server);
            return;
        }

        ServerBossEvent bossBar = matchTimerBossBar(server);
        long remainingTicks = Math.max(0L, state.matchEndGameTime() - level.getGameTime());
        long remainingSeconds = (remainingTicks + 19L) / 20L;
        if (force || remainingSeconds != lastBossBarSeconds) {
            long totalTicks = state.matchDurationTicks() > 0L
                    ? state.matchDurationTicks()
                    : WarDayConfig.MATCH_DURATION_SECONDS.getAsInt() * 20L;
            float progress = Math.max(0.0F, Math.min(1.0F, (float) remainingTicks / Math.max(1L, totalTicks)));
            long minutes = remainingSeconds / 60L;
            long seconds = remainingSeconds % 60L;
            bossBar.setName(message(ChatFormatting.GOLD,
                    "War Day - " + minutes + ":" + (seconds < 10L ? "0" : "") + seconds));
            bossBar.setProgress(progress);
            syncMatchTimerPlayers(server, bossBar);
            lastBossBarSeconds = remainingSeconds;
        }
        bossBar.setVisible(true);
    }

    private static ServerBossEvent matchTimerBossBar(MinecraftServer server) {
        if (matchTimerBossBar == null || matchTimerBossBarServer != server) {
            if (matchTimerBossBar != null) {
                matchTimerBossBar.removeAllPlayers();
            }
            matchTimerBossBar = new ServerBossEvent(
                    message(ChatFormatting.GOLD, "War Day"),
                    BossEvent.BossBarColor.YELLOW,
                    BossEvent.BossBarOverlay.PROGRESS
            );
            matchTimerBossBarServer = server;
            lastBossBarSeconds = Long.MIN_VALUE;
        }
        return matchTimerBossBar;
    }

    private static void syncMatchTimerPlayers(MinecraftServer server, ServerBossEvent bossBar) {
        Set<ServerPlayer> onlinePlayers = new HashSet<>(server.getPlayerList().getPlayers());
        for (ServerPlayer player : List.copyOf(bossBar.getPlayers())) {
            if (!onlinePlayers.contains(player)) {
                bossBar.removePlayer(player);
            }
        }
        for (ServerPlayer player : onlinePlayers) {
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }
    }

    private static void hideMatchTimerBossBar(MinecraftServer server) {
        if (matchTimerBossBar == null) {
            return;
        }
        if (matchTimerBossBarServer != server) {
            matchTimerBossBar.removeAllPlayers();
            matchTimerBossBar = null;
            matchTimerBossBarServer = null;
            lastBossBarSeconds = Long.MIN_VALUE;
            return;
        }
        matchTimerBossBar.setVisible(false);
        matchTimerBossBar.removeAllPlayers();
        lastBossBarSeconds = Long.MIN_VALUE;
    }

    private static String currentSidebarObjectiveName(MinecraftServer server) {
        Objective current = server.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        if (current == null || WARDAY_ROSTER_OBJECTIVE.equals(current.getName())) {
            return "";
        }
        return current.getName();
    }

    private static void syncWarDaySidebar(
            MinecraftServer server,
            WarDayState state,
            long gameTime,
            boolean force
    ) {
        if (!state.isActive()) {
            return;
        }

        List<RosterPlayer> defenders = rosterPlayers(server, state, state.defenderParticipants());
        List<RosterPlayer> attackers = rosterPlayers(server, state, state.attackerParticipants());
        int pageCount = Math.max(1, Math.max(
                pagesForRoster(defenders.size()),
                pagesForRoster(attackers.size())
        ));
        int page = Math.floorMod(gameTime / ROSTER_PAGE_TICKS, pageCount);
        String signature = rosterSignature(state, defenders, attackers, page, pageCount);

        ServerScoreboard scoreboard = server.getScoreboard();
        Objective objective = scoreboard.getObjective(WARDAY_ROSTER_OBJECTIVE);
        Objective displayed = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (displayed != null && !WARDAY_ROSTER_OBJECTIVE.equals(displayed.getName())) {
            state.setPreviousSidebarObjective(displayed.getName());
        }
        boolean wrongServer = rosterScoreboardServer != server;
        boolean notDisplayed = displayed != objective;
        boolean objectiveMissing = objective == null;
        if (!force && !wrongServer && !notDisplayed && !objectiveMissing && signature.equals(lastRosterSignature)) {
            return;
        }

        if (objective == null) {
            objective = scoreboard.addObjective(
                    WARDAY_ROSTER_OBJECTIVE,
                    ObjectiveCriteria.DUMMY,
                    message(ChatFormatting.GOLD, "War Day Teams"),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    BlankFormat.INSTANCE
            );
        }
        objective.setDisplayName(message(ChatFormatting.GOLD,
                pageCount > 1 ? "War Day Teams " + (page + 1) + "/" + pageCount : "War Day Teams"));

        List<RosterLine> lines = new ArrayList<>();
        addRosterTeamLines(lines, state.defenderTeam(), defenders, page, ChatFormatting.AQUA);
        addRosterTeamLines(lines, state.attackerTeam(), attackers, page, ChatFormatting.RED);
        removeUnexpectedRosterScores(scoreboard, objective, lines.size());
        for (int index = 0; index < lines.size(); index++) {
            RosterLine line = lines.get(index);
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(
                    ScoreHolder.forNameOnly("wd_line_" + index),
                    objective
            );
            score.display(line.display());
            score.numberFormatOverride(line.numberFormat());
            score.set(15 - index);
        }

        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
        rosterScoreboardServer = server;
        lastRosterSignature = signature;
    }

    private static List<RosterPlayer> rosterPlayers(
            MinecraftServer server,
            WarDayState state,
            Set<UUID> playerIds
    ) {
        List<RosterPlayer> players = new ArrayList<>();
        for (UUID playerId : playerIds) {
            ServerPlayer online = server.getPlayerList().getPlayer(playerId);
            String name = online != null
                    ? online.getGameProfile().getName()
                    : server.getProfileCache().get(playerId)
                            .map(profile -> profile.getName())
                            .orElse(playerId.toString().substring(0, 8));
            WarDayRosterHealth.Snapshot health;
            Optional<Integer> pendingRespawnTicks = state.pendingRespawnTicks(playerId);
            if (online == null) {
                health = WarDayRosterHealth.offline();
            } else if (pendingRespawnTicks.isPresent() && pendingRespawnTicks.get() > 0) {
                health = WarDayRosterHealth.respawning(pendingRespawnTicks.get());
            } else {
                health = WarDayRosterHealth.online(online.getHealth(), online.getMaxHealth());
            }
            players.add(new RosterPlayer(playerId, name, health));
        }
        players.sort(Comparator.comparing(RosterPlayer::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(player -> player.id().toString()));
        return List.copyOf(players);
    }

    private static int pagesForRoster(int playerCount) {
        return Math.max(1, (playerCount + ROSTER_PLAYERS_PER_TEAM_PAGE - 1) / ROSTER_PLAYERS_PER_TEAM_PAGE);
    }

    private static String rosterSignature(
            WarDayState state,
            List<RosterPlayer> defenders,
            List<RosterPlayer> attackers,
            int page,
            int pageCount
    ) {
        return state.defenderTeam() + "|" + defenders + "|"
                + state.attackerTeam() + "|" + attackers + "|" + page + "/" + pageCount;
    }

    private static void addRosterTeamLines(
            List<RosterLine> lines,
            String teamName,
            List<RosterPlayer> players,
            int page,
            ChatFormatting color
    ) {
        String displayTeamName = teamName == null || teamName.isBlank() ? "Unconfigured team" : teamName;
        lines.add(new RosterLine(
                message(color, displayTeamName).withStyle(ChatFormatting.BOLD),
                BlankFormat.INSTANCE
        ));
        int fromIndex = page * ROSTER_PLAYERS_PER_TEAM_PAGE;
        int toIndex = Math.min(players.size(), fromIndex + ROSTER_PLAYERS_PER_TEAM_PAGE);
        if (fromIndex >= players.size()) {
            lines.add(new RosterLine(
                    message(ChatFormatting.DARK_GRAY, "  (no players this page)"),
                    BlankFormat.INSTANCE
            ));
            return;
        }
        for (RosterPlayer player : players.subList(fromIndex, toIndex)) {
            lines.add(new RosterLine(
                    message(color, "  " + player.name()),
                    new FixedFormat(message(rosterHealthColor(player.health()), WarDayRosterHealth.displayText(player.health())))
            ));
        }
    }

    private static ChatFormatting rosterHealthColor(WarDayRosterHealth.Snapshot health) {
        return switch (WarDayRosterHealth.band(health)) {
            case HEALTHY -> ChatFormatting.GREEN;
            case HURT -> ChatFormatting.YELLOW;
            case LOW -> ChatFormatting.GOLD;
            case CRITICAL, EMPTY -> ChatFormatting.RED;
            case RESPAWNING -> ChatFormatting.YELLOW;
            case OFFLINE -> ChatFormatting.DARK_GRAY;
        };
    }

    private static void removeUnexpectedRosterScores(
            ServerScoreboard scoreboard,
            Objective objective,
            int expectedLineCount
    ) {
        for (PlayerScoreEntry entry : List.copyOf(scoreboard.listPlayerScores(objective))) {
            String owner = entry.owner();
            if (!owner.startsWith("wd_line_")) {
                scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(owner), objective);
                continue;
            }
            try {
                int index = Integer.parseInt(owner.substring("wd_line_".length()));
                if (index < 0 || index >= expectedLineCount) {
                    scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(owner), objective);
                }
            } catch (NumberFormatException ignored) {
                scoreboard.resetSinglePlayerScore(ScoreHolder.forNameOnly(owner), objective);
            }
        }
    }

    private static void restoreWarDaySidebar(MinecraftServer server, WarDayState state) {
        ServerScoreboard scoreboard = server.getScoreboard();
        Objective wardayObjective = scoreboard.getObjective(WARDAY_ROSTER_OBJECTIVE);
        Objective current = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (current == wardayObjective) {
            String previousName = state.previousSidebarObjective();
            Objective previous = previousName.isBlank() ? null : scoreboard.getObjective(previousName);
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, previous);
        }
        if (wardayObjective != null) {
            scoreboard.removeObjective(wardayObjective);
        }
        rosterScoreboardServer = null;
        lastRosterSignature = "";
    }

    private static int endActiveWarDay(MinecraftServer server, WarDayState state) {
        hideMatchTimerBossBar(server);
        int restored = 0;
        Map<UUID, WarDayState.PlayerSnapshot> snapshots = state.savedPlayers();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            WarDayState.PlayerSnapshot snapshot = snapshots.get(player.getUUID());
            if (snapshot != null) {
                stopRespawnSpectating(player);
                if (restorePlayer(server, player, snapshot)) {
                    state.removeSavedPlayer(player.getUUID());
                    restored++;
                } else {
                    player.sendSystemMessage(message(ChatFormatting.RED,
                            "Inventory restoration could not be verified. Your recovery snapshot was retained; contact an operator."));
                }
            }
        }

        if (state.keepInventoryCaptured()) {
            warDayLevel(server, state).ifPresent(level ->
                    level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(state.originalKeepInventory(), server)
            );
        }
        warDayLevel(server, state).ifPresent(level -> {
            clearLoadedMatchStorage(level, state);
            clearPreparedMatchEntities(level);
            clearTransientMatchEntities(level, state);
            restoreWorldBorder(level, state);
            removeNexusMarker(level, state);
        });
        restoreWarDaySidebar(server, state);
        state.end();
        PENDING_RESPAWNS.clear();
        DEATH_COUNTS.clear();
        DIG_HISTORY.clear();
        return restored;
    }

    private static boolean isActiveWarDayLevel(ServerLevel level) {
        WarDayState state = WarDayState.get(level.getServer());
        return state.isActive() && level.dimension().location().toString().equals(state.warDayDimension());
    }

    private static void clearLoadedMatchStorage(ServerLevel level, WarDayState state) {
        if (state.copiedNexusPos().isEmpty()) {
            return;
        }

        BlockPos nexusPos = state.copiedNexusPos().get();
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        int minChunkX = Math.floorDiv(nexusPos.getX() - halfSize, 16);
        int maxChunkX = Math.floorDiv(nexusPos.getX() + halfSize, 16);
        int minChunkZ = Math.floorDiv(nexusPos.getZ() - halfSize, 16);
        int maxChunkZ = Math.floorDiv(nexusPos.getZ() + halfSize, 16);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    if (!isInMatchBounds(entry.getKey(), nexusPos)) {
                        continue;
                    }
                    if (entry.getValue() instanceof Container container) {
                        container.clearContent();
                    }
                    clearBlockItemHandlers(level, entry.getKey());
                    entry.getValue().setChanged();
                }
            }
        }

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemFrame itemFrame && isInMatchBounds(itemFrame.blockPosition(), nexusPos)) {
                itemFrame.setItem(ItemStack.EMPTY, false);
            }
        }
    }

    private static void clearBlockItemHandlers(ServerLevel level, BlockPos pos) {
        Set<IItemHandler> handlers = new HashSet<>();
        IItemHandler unsided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (unsided != null) {
            handlers.add(unsided);
        }
        for (Direction direction : Direction.values()) {
            IItemHandler sided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
            if (sided != null) {
                handlers.add(sided);
            }
        }

        for (IItemHandler handler : handlers) {
            if (!(handler instanceof IItemHandlerModifiable modifiable)) {
                continue;
            }
            for (int slot = 0; slot < modifiable.getSlots(); slot++) {
                modifiable.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private static void giveRespawnMatchBlocks(ServerPlayer player) {
        WarDayState state = WarDayState.get(player.getServer());
        if (!state.isCombatActive() || !FTBTeamsAPI.api().isManagerLoaded()) {
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

    private static void applyActiveMatchRole(ServerPlayer player, WarDayState state) {
        Optional<ParticipantRespawn> participant = participantRespawn(player);
        if (participant.isPresent()) {
            ParticipantRespawn respawn = participant.get();
            Optional<Integer> pendingTicks = state.pendingRespawnTicks(player.getUUID());
            if (pendingTicks.isPresent() && pendingTicks.get() > 0) {
                PendingRespawn pending = new PendingRespawn(pendingTicks.get(), respawn, null);
                PENDING_RESPAWNS.put(player.getUUID(), beginRespawnSpectating(player, state, pending, true));
                player.sendSystemMessage(message(ChatFormatting.YELLOW,
                        "War Day is active. Respawning in " + Math.max(1, (pendingTicks.get() + 19) / 20)
                                + " seconds. Left/right click cycles living teammates."));
                return;
            }

            restoreDelayedRespawn(player, respawn);
            player.sendSystemMessage(message(ChatFormatting.GREEN, "War Day is active. You have been moved to your team spawn."));
            return;
        }

        warDayLevel(player.getServer(), state).ifPresent(level -> {
            BlockPos spectatorPos = state.attackerSpawnPos()
                    .or(() -> state.copiedNexusPos())
                    .orElse(BlockPos.ZERO)
                    .offset(0, 11, 0);
            player.setGameMode(GameType.SPECTATOR);
            teleportPlayer(player, level, spectatorPos);
            player.sendSystemMessage(message(ChatFormatting.YELLOW, "War Day is active. You are spectating this match."));
        });
    }

    private static void ensureInventoryHasAtLeast(ServerPlayer player, Item item, int targetCount) {
        int currentCount = player.getInventory().countItem(item);
        if (currentCount >= targetCount) {
            return;
        }

        giveOrDrop(player, new ItemStack(item, targetCount - currentCount));
    }

    private static WarDayState.PlayerSnapshot snapshotPlayer(ServerPlayer player) {
        BlockPos respawnPos = player.getRespawnPosition();
        return new WarDayState.PlayerSnapshot(
                player.gameMode.getGameModeForPlayer(),
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                player.getRespawnDimension().location().toString(),
                respawnPos != null,
                respawnPos == null ? 0 : respawnPos.getX(),
                respawnPos == null ? 0 : respawnPos.getY(),
                respawnPos == null ? 0 : respawnPos.getZ(),
                player.getRespawnAngle(),
                player.isRespawnForced(),
                true,
                player.getInventory().save(new ListTag()),
                player.getInventory().selected,
                player.getEnderChestInventory().createTag(player.registryAccess()),
                saveItemStack(player.containerMenu.getCarried(), player),
                captureCuriosInventory(player)
        );
    }

    private static boolean restorePlayer(MinecraftServer server, ServerPlayer player, WarDayState.PlayerSnapshot snapshot) {
        boolean inventoryRestored = restorePlayerInventory(player, snapshot);
        if (!snapshot.dimension().isBlank()) {
            ServerLevel restoreLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(snapshot.dimension())));
            if (restoreLevel != null) {
                player.teleportTo(restoreLevel, snapshot.x(), snapshot.y(), snapshot.z(), Set.of(), snapshot.yRot(), snapshot.xRot());
            }
        }
        restorePlayerRespawn(player, snapshot);
        player.setGameMode(snapshot.gameMode());
        return inventoryRestored;
    }

    private static CompoundTag saveItemStack(ItemStack stack, ServerPlayer player) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        Tag saved = stack.saveOptional(player.registryAccess());
        return saved instanceof CompoundTag compoundTag ? compoundTag.copy() : new CompoundTag();
    }

    private static CompoundTag captureCuriosInventory(ServerPlayer player) {
        if (!ModList.get().isLoaded("curios")) {
            return new CompoundTag();
        }
        try {
            CompoundTag snapshot = CuriosInventoryBridge.capture(player);
            if (!CuriosInventoryBridge.wasCaptured(snapshot)) {
                throw new IllegalStateException("Curios inventory capability was unavailable");
            }
            return snapshot;
        } catch (LinkageError | RuntimeException exception) {
            WarDayMod.LOGGER.error("Could not snapshot Curios inventory for {}", player.getGameProfile().getName(), exception);
            throw new IllegalStateException("Could not snapshot Curios inventory", exception);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !isActiveWarDayLevel(level)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        boolean exposesItemStorage = blockEntity instanceof Container
                || level.getCapability(Capabilities.ItemHandler.BLOCK, event.getPos(), null) != null
                || level.getCapability(Capabilities.ItemHandler.BLOCK, event.getPos(), event.getFace()) != null;
        if (!exposesItemStorage) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(message(ChatFormatting.RED,
                "Storage blocks are disabled during War Day so match items cannot alter persistent inventories."), true);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !isActiveWarDayLevel(level)
                || !(event.getTarget() instanceof ItemFrame)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(message(ChatFormatting.RED,
                "Item frames cannot hold items during War Day."), true);
    }

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !isActiveWarDayLevel(level)
                || !(event.getTarget() instanceof ItemFrame)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(message(ChatFormatting.RED,
                "Item frames cannot hold items during War Day."), true);
    }

    private static boolean restorePlayerInventory(ServerPlayer player, WarDayState.PlayerSnapshot snapshot) {
        if (!snapshot.hasInventorySnapshot()) {
            return true;
        }

        player.closeContainer();
        player.getInventory().load(snapshot.inventory());
        player.getInventory().selected = Math.max(0, Math.min(8, snapshot.selectedSlot()));
        player.getEnderChestInventory().fromTag(snapshot.enderChest(), player.registryAccess());
        player.inventoryMenu.setCarried(ItemStack.parseOptional(player.registryAccess(), snapshot.carriedItem()));
        boolean curiosRestored = restoreCuriosInventory(player, snapshot.curiosInventory());
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastFullState();
        return verifyRestoredInventory(player, snapshot, curiosRestored);
    }

    private static boolean verifyRestoredInventory(
            ServerPlayer player,
            WarDayState.PlayerSnapshot snapshot,
            boolean curiosRestored
    ) {
        List<String> mismatches = new ArrayList<>();
        if (!snapshot.inventory().equals(player.getInventory().save(new ListTag()))) {
            mismatches.add("vanilla inventory");
        }
        if (snapshot.selectedSlot() != player.getInventory().selected) {
            mismatches.add("selected hotbar slot");
        }
        if (!snapshot.enderChest().equals(player.getEnderChestInventory().createTag(player.registryAccess()))) {
            mismatches.add("Ender Chest");
        }
        if (!snapshot.carriedItem().equals(saveItemStack(player.inventoryMenu.getCarried(), player))) {
            mismatches.add("carried cursor stack");
        }

        CompoundTag expectedCurios = snapshot.curiosInventory();
        if (!curiosRestored) {
            mismatches.add("Curios inventory");
        } else if (!expectedCurios.isEmpty()) {
            try {
                if (!expectedCurios.equals(captureCuriosInventory(player))) {
                    mismatches.add("Curios inventory");
                }
            } catch (IllegalStateException exception) {
                mismatches.add("Curios inventory");
            }
        }

        if (!mismatches.isEmpty()) {
            WarDayMod.LOGGER.error(
                    "Post-restore inventory verification failed for {}: {}",
                    player.getGameProfile().getName(),
                    String.join(", ", mismatches)
            );
            return false;
        }
        return true;
    }

    private static boolean restoreCuriosInventory(ServerPlayer player, CompoundTag snapshot) {
        if (snapshot.isEmpty()) {
            return true;
        }
        if (!ModList.get().isLoaded("curios")) {
            return false;
        }
        try {
            return CuriosInventoryBridge.restore(player, snapshot);
        } catch (LinkageError | RuntimeException exception) {
            WarDayMod.LOGGER.error("Could not restore Curios inventory for {}", player.getGameProfile().getName(), exception);
            return false;
        }
    }

    private static void restorePlayerRespawn(ServerPlayer player, WarDayState.PlayerSnapshot snapshot) {
        ResourceKey<Level> respawnDimension = Level.OVERWORLD;
        if (!snapshot.respawnDimension().isBlank()) {
            respawnDimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(snapshot.respawnDimension()));
        }

        BlockPos respawnPos = snapshot.hasRespawnPosition()
                ? new BlockPos(snapshot.respawnX(), snapshot.respawnY(), snapshot.respawnZ())
                : null;
        player.setRespawnPosition(respawnDimension, respawnPos, snapshot.respawnAngle(), snapshot.respawnForced(), false);
    }

    private static void teleportPlayer(ServerPlayer player, ServerLevel level, BlockPos pos) {
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, Set.of(), player.getYRot(), player.getXRot());
    }

    private static void teleportPlayerSafely(ServerPlayer player, ServerLevel level, BlockPos preferred) {
        BlockPos target = findSafeSpawnNear(level, preferred, 8, 4).orElse(preferred);
        teleportPlayer(player, level, target);
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

    private static boolean isInsideConfiguredMatchBorder(BlockPos nexusPos, BlockPos pos) {
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        return Math.abs(pos.getX() - nexusPos.getX()) < halfSize
                && Math.abs(pos.getZ() - nexusPos.getZ()) < halfSize;
    }

    private static void restoreWorldBorder(ServerLevel level, WarDayState state) {
        if (!state.worldBorderCaptured()) {
            return;
        }

        level.getWorldBorder().setCenter(state.originalWorldBorderCenterX(), state.originalWorldBorderCenterZ());
        level.getWorldBorder().setSize(state.originalWorldBorderSize());
    }

    private static int clearPreparedMatchEntities(ServerLevel level) {
        List<Entity> preparedEntities = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity.getPersistentData().getBoolean(MATCH_ENTITY_MARKER)) {
                preparedEntities.add(entity);
            }
        }
        preparedEntities.forEach(Entity::discard);
        return preparedEntities.size();
    }

    private static void clearTransientMatchEntities(ServerLevel level, WarDayState state) {
        if (state.copiedNexusPos().isEmpty()) {
            return;
        }

        BlockPos nexusPos = state.copiedNexusPos().get();
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        AABB bounds = new AABB(
                nexusPos.getX() - halfSize,
                level.getMinBuildHeight(),
                nexusPos.getZ() - halfSize,
                nexusPos.getX() + halfSize,
                level.getMaxBuildHeight(),
                nexusPos.getZ() + halfSize
        );

        for (Entity entity : level.getEntities((Entity) null, bounds, WarDayCommands::isTransientMatchEntity)) {
            entity.discard();
        }
    }

    private static boolean isTransientMatchEntity(Entity entity) {
        return entity instanceof ItemEntity
                || entity instanceof ExperienceOrb
                || entity instanceof Projectile;
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
        if (!state.isCombatActive()) {
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

        boolean defender = state.isDefenderParticipant(playerId)
                || defenderTeam.map(team -> team.getMembers().contains(playerId)).orElse(false);
        boolean attacker = state.isAttackerParticipant(playerId)
                || attackerTeam.map(team -> team.getMembers().contains(playerId)).orElse(false);

        if (defender && state.copiedNexusPos().isPresent()) {
            return Optional.of(new ParticipantRespawn(warDayLevel.get(), state.copiedNexusPos().get().offset(0, 1, 0), defenderMatchBlock()));
        }
        if (attacker && state.attackerSpawnPos().isPresent()) {
            return Optional.of(new ParticipantRespawn(warDayLevel.get(), state.attackerSpawnPos().get(), attackerMatchBlock()));
        }

        return Optional.empty();
    }

    static void cycleRespawnSpectator(ServerPlayer player, int direction) {
        if (direction != -1 && direction != 1) {
            return;
        }

        WarDayState state = WarDayState.get(player.getServer());
        PendingRespawn pending = PENDING_RESPAWNS.get(player.getUUID());
        if (!state.isCombatActive()
                || pending == null
                || state.pendingRespawnTicks(player.getUUID()).orElse(0) <= 0
                || !player.isSpectator()) {
            return;
        }

        PENDING_RESPAWNS.put(
                player.getUUID(),
                selectRespawnSpectatorTarget(player, state, pending, direction, true)
        );
    }

    private static PendingRespawn beginRespawnSpectating(
            ServerPlayer player,
            WarDayState state,
            PendingRespawn pending,
            boolean announce
    ) {
        player.setGameMode(GameType.SPECTATOR);
        player.setCamera(player);
        WarDayNetwork.syncRespawnSpectatorState(player, true);
        return maintainRespawnSpectating(player, state, pending, announce);
    }

    private static PendingRespawn maintainRespawnSpectating(
            ServerPlayer player,
            WarDayState state,
            PendingRespawn pending,
            boolean announce
    ) {
        List<ServerPlayer> candidates = livingTeammatesForRespawnSpectator(player, state);
        UUID currentTargetId = pending.spectatorTargetId();
        ServerPlayer currentTarget = currentTargetId == null
                ? null
                : candidates.stream()
                        .filter(candidate -> candidate.getUUID().equals(currentTargetId))
                        .findFirst()
                        .orElse(null);
        if (currentTarget != null) {
            if (player.getCamera() != currentTarget) {
                player.setCamera(currentTarget);
            }
            return pending;
        }

        return selectRespawnSpectatorTarget(player, state, pending, 1, announce || currentTargetId != null);
    }

    private static PendingRespawn selectRespawnSpectatorTarget(
            ServerPlayer player,
            WarDayState state,
            PendingRespawn pending,
            int direction,
            boolean announce
    ) {
        List<ServerPlayer> candidates = livingTeammatesForRespawnSpectator(player, state);
        List<UUID> candidateIds = candidates.stream().map(ServerPlayer::getUUID).toList();
        Optional<UUID> selectedId = WarDaySpectatorCycle.select(
                candidateIds,
                pending.spectatorTargetId(),
                direction
        );
        if (selectedId.isEmpty()) {
            boolean targetChanged = pending.spectatorTargetId() != null || player.getCamera() != player;
            player.setCamera(player);
            teleportPlayer(player, pending.participant().level(), pending.participant().spawnPos().offset(0, 11, 0));
            if (announce || targetChanged) {
                player.displayClientMessage(message(ChatFormatting.YELLOW,
                        "No living teammates are available to spectate."), true);
            }
            return pending.withSpectatorTarget(null);
        }

        ServerPlayer selected = candidates.stream()
                .filter(candidate -> candidate.getUUID().equals(selectedId.get()))
                .findFirst()
                .orElseThrow();
        player.setCamera(selected);
        if (announce || !selected.getUUID().equals(pending.spectatorTargetId())) {
            player.displayClientMessage(message(ChatFormatting.AQUA,
                    "Spectating " + selected.getGameProfile().getName() + " - left/right click to cycle."), true);
        }
        return pending.withSpectatorTarget(selected.getUUID());
    }

    private static List<ServerPlayer> livingTeammatesForRespawnSpectator(
            ServerPlayer viewer,
            WarDayState state
    ) {
        Set<UUID> teamMembers;
        if (state.isDefenderParticipant(viewer.getUUID())) {
            teamMembers = state.defenderParticipants();
        } else if (state.isAttackerParticipant(viewer.getUUID())) {
            teamMembers = state.attackerParticipants();
        } else {
            return List.of();
        }

        Optional<ServerLevel> level = warDayLevel(viewer.getServer(), state);
        if (level.isEmpty()) {
            return List.of();
        }

        List<ServerPlayer> candidates = new ArrayList<>();
        for (UUID memberId : teamMembers) {
            if (memberId.equals(viewer.getUUID()) || state.pendingRespawnTicks(memberId).orElse(0) > 0) {
                continue;
            }
            ServerPlayer candidate = viewer.getServer().getPlayerList().getPlayer(memberId);
            if (candidate == null
                    || candidate.level() != level.get()
                    || candidate.isSpectator()
                    || !candidate.isAlive()
                    || candidate.isDeadOrDying()
                    || candidate.getHealth() <= 0.0F) {
                continue;
            }
            candidates.add(candidate);
        }
        candidates.sort(Comparator
                .comparing((ServerPlayer candidate) -> candidate.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(candidate -> candidate.getUUID().toString()));
        return List.copyOf(candidates);
    }

    private static void stopRespawnSpectating(ServerPlayer player) {
        player.setCamera(player);
        WarDayNetwork.syncRespawnSpectatorState(player, false);
    }

    private static void restoreDelayedRespawn(ServerPlayer player, ParticipantRespawn respawn) {
        stopRespawnSpectating(player);
        teleportPlayerSafely(player, respawn.level(), respawn.spawnPos());
        setPlayerSpawn(player, respawn.level(), findSafeSpawnNear(respawn.level(), respawn.spawnPos(), 8, 4).orElse(respawn.spawnPos()));
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
        List<Team> scanTeams = new ArrayList<>();
        scanTeams.add(teamA.get());
        teamB.ifPresent(scanTeams::add);
        scanArea(level, center, radius, chunkManager, scanTeams, nexuses, forwardMarkers, attackerSpawns);

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
            Collection<Team> scanTeams,
            List<LocatedBlock> nexuses,
            List<LocatedBlock> forwardMarkers,
            List<AttackerSpawn> attackerSpawns
    ) {
        int minBlockX = center.getX() - radius;
        int maxBlockX = center.getX() + radius;
        int minBlockZ = center.getZ() - radius;
        int maxBlockZ = center.getZ() + radius;
        int minChunkX = Math.floorDiv(minBlockX, 16);
        int maxChunkX = Math.floorDiv(maxBlockX, 16);
        int minChunkZ = Math.floorDiv(minBlockZ, 16);
        int maxChunkZ = Math.floorDiv(maxBlockZ, 16);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                ClaimedChunk claimedChunk = chunkManager.getChunk(
                        new ChunkDimPos(level.dimension(), new ChunkPos(chunkX, chunkZ))
                );
                if (claimedChunk == null) {
                    continue;
                }

                Team owner = claimedChunk.getTeamData().getTeam();
                boolean relevantOwner = scanTeams.stream().anyMatch(team -> team.getId().equals(owner.getId()));
                if (!relevantOwner) {
                    continue;
                }

                int chunkMinX = Math.max(minBlockX, chunkX * 16);
                int chunkMaxX = Math.min(maxBlockX, chunkX * 16 + 15);
                int chunkMinZ = Math.max(minBlockZ, chunkZ * 16);
                int chunkMaxZ = Math.min(maxBlockZ, chunkZ * 16 + 15);
                scanClaimedChunk(
                        level,
                        owner,
                        chunkMinX,
                        chunkMaxX,
                        chunkMinZ,
                        chunkMaxZ,
                        nexuses,
                        forwardMarkers,
                        attackerSpawns
                );
            }
        }
    }

    private static void scanClaimedChunk(
            ServerLevel level,
            Team owner,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            List<LocatedBlock> nexuses,
            List<LocatedBlock> forwardMarkers,
            List<AttackerSpawn> attackerSpawns
    ) {
        Optional<Team> claimedOwner = Optional.of(owner);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(WarDayMod.NEXUS.get())) {
                        nexuses.add(new LocatedBlock(level.dimension(), pos.immutable(), null, claimedOwner));
                    } else if (state.is(WarDayMod.FORWARD_MARKER.get())) {
                        Direction facing = state.getValue(ForwardMarkerBlock.FACING);
                        forwardMarkers.add(new LocatedBlock(level.dimension(), pos.immutable(), facing, claimedOwner));
                    } else if (state.is(WarDayMod.ATTACKER_SPAWN.get())) {
                        attackerSpawns.add(new AttackerSpawn(level.dimension(), pos.immutable(), claimedOwner));
                    }
                }
            }
        }
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

    private static int wipeDestinationArea(ServerLevel sourceLevel, ServerLevel targetLevel, PlacementPlan plan) {
        int wiped = 0;
        for (ChunkDimPos chunk : plan.cluster()) {
            int minX = chunk.x() * 16;
            int minZ = chunk.z() * 16;
            for (int x = minX; x < minX + 16; x++) {
                for (int z = minZ; z < minZ + 16; z++) {
                    for (int y = sourceLevel.getMinBuildHeight(); y < sourceLevel.getMaxBuildHeight(); y++) {
                        BlockPos targetPos = plan.targetPos(new BlockPos(x, y, z));
                        if (targetPos.getY() < targetLevel.getMinBuildHeight()
                                || targetPos.getY() >= targetLevel.getMaxBuildHeight()) {
                            continue;
                        }
                        if (!targetLevel.getBlockState(targetPos).isAir()) {
                            targetLevel.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                            wiped++;
                        }
                    }
                }
            }
        }
        return wiped;
    }

    private static int clearDestinationDecorativeEntities(ServerLevel targetLevel, PlacementPlan plan) {
        AABB targetBounds = plan.targetEntityBounds(targetLevel).inflate(HANGING_ENTITY_SCAN_MARGIN);
        List<Entity> existingDecorations = targetLevel.getEntities(
                (Entity) null,
                targetBounds,
                entity -> isAllowedDecorativeEntity(entity)
                        && entity instanceof HangingEntity hangingEntity
                        && plan.containsTargetBlock(hangingEntity.getPos())
        );
        existingDecorations.forEach(Entity::discard);
        return existingDecorations.size();
    }

    private static EntityTemplateCapture capturePreparedEntityTemplates(
            ServerLevel sourceLevel,
            ServerLevel targetLevel,
            PlacementPlan plan,
            int remainingCapacity,
            Set<UUID> capturedRoots
    ) {
        List<CompoundTag> templates = new ArrayList<>();
        int capturedEntities = 0;
        int skippedEntities = 0;
        AABB sourceBounds = plan.sourceEntityBounds(sourceLevel);
        List<Entity> candidates = sourceLevel.getEntities(
                (Entity) null,
                sourceBounds,
                entity -> !entity.isPassenger()
                        && plan.containsSourceChunk(entity.blockPosition())
                        && entity.getSelfAndPassengers().allMatch(WarDayCommands::isAllowedPreparedEntity)
        );

        for (Entity sourceEntity : candidates) {
            if (!capturedRoots.add(sourceEntity.getUUID())) {
                continue;
            }

            CompoundTag tag = new CompoundTag();
            if (!savePreparedEntityTree(sourceEntity, tag)) {
                skippedEntities += Math.toIntExact(sourceEntity.getSelfAndPassengers().count());
                continue;
            }

            int entityTreeSize = countEntityTree(tag);
            boolean targetPositionsValid = sourceEntity.getSelfAndPassengers().allMatch(entity -> {
                double targetY = plan.targetY(entity.getY());
                return targetY >= targetLevel.getMinBuildHeight() && targetY < targetLevel.getMaxBuildHeight();
            });
            if (!targetPositionsValid || capturedEntities + entityTreeSize > remainingCapacity) {
                skippedEntities += entityTreeSize;
                continue;
            }

            transformPreparedEntityTree(tag, plan);
            templates.add(tag);
            capturedEntities += entityTreeSize;
        }

        return new EntityTemplateCapture(List.copyOf(templates), capturedEntities, skippedEntities);
    }

    private static boolean savePreparedEntityTree(Entity entity, CompoundTag tag) {
        if (!entity.saveAsPassenger(tag)) {
            return false;
        }

        ListTag passengerTags = new ListTag();
        for (Entity passenger : entity.getPassengers()) {
            CompoundTag passengerTag = new CompoundTag();
            if (!savePreparedEntityTree(passenger, passengerTag)) {
                return false;
            }
            passengerTags.add(passengerTag);
        }
        if (!passengerTags.isEmpty()) {
            tag.put(Entity.PASSENGERS_TAG, passengerTags);
        }
        return true;
    }

    private static boolean isAllowedPreparedEntity(Entity entity) {
        if (!entity.shouldBeSaved() || entity.isRemoved()
                || entity instanceof ServerPlayer
                || entity instanceof Painting
                || entity instanceof ItemFrame
                || entity instanceof ItemEntity
                || entity instanceof ExperienceOrb
                || entity instanceof Projectile
                || entity instanceof Display) {
            return false;
        }

        EntityType<?> type = entity.getType();
        return type != EntityType.AREA_EFFECT_CLOUD
                && type != EntityType.ENDER_DRAGON
                && type != EntityType.END_CRYSTAL
                && type != EntityType.EVOKER_FANGS
                && type != EntityType.FALLING_BLOCK
                && type != EntityType.INTERACTION
                && type != EntityType.LEASH_KNOT
                && type != EntityType.LIGHTNING_BOLT
                && type != EntityType.MARKER
                && type != EntityType.TNT
                && type != EntityType.WITHER;
    }

    private static void transformPreparedEntityTree(CompoundTag tag, PlacementPlan plan) {
        ListTag pos = tag.getList("Pos", Tag.TAG_DOUBLE);
        if (pos.size() >= 3) {
            double sourceX = pos.getDouble(0);
            double sourceY = pos.getDouble(1);
            double sourceZ = pos.getDouble(2);
            tag.put("Pos", newDoubleList(
                    plan.targetX(sourceX, sourceZ),
                    plan.targetY(sourceY),
                    plan.targetZ(sourceX, sourceZ)
            ));
        }
        ListTag motion = tag.getList("Motion", Tag.TAG_DOUBLE);
        if (motion.size() >= 3) {
            double sourceMotionX = motion.getDouble(0);
            double sourceMotionZ = motion.getDouble(2);
            tag.put("Motion", newDoubleList(
                    plan.rotatedOffsetX(sourceMotionX, sourceMotionZ),
                    motion.getDouble(1),
                    plan.rotatedOffsetZ(sourceMotionX, sourceMotionZ)
            ));
        }
        rotateEntityYaw(tag, plan);
        transformFenceLeash(tag, plan);

        ListTag passengers = tag.getList(Entity.PASSENGERS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < passengers.size(); i++) {
            transformPreparedEntityTree(passengers.getCompound(i), plan);
        }
    }

    private static void transformFenceLeash(CompoundTag tag, PlacementPlan plan) {
        if (!tag.contains("leash", Tag.TAG_INT_ARRAY)) {
            return;
        }

        int[] sourceLeash = tag.getIntArray("leash");
        if (sourceLeash.length != 3) {
            tag.remove("leash");
            return;
        }

        BlockPos targetLeash = plan.targetPos(new BlockPos(sourceLeash[0], sourceLeash[1], sourceLeash[2]));
        tag.putIntArray("leash", new int[]{targetLeash.getX(), targetLeash.getY(), targetLeash.getZ()});
    }

    private static EntityTemplateSpawn spawnPreparedEntityTemplates(
            ServerLevel targetLevel,
            List<CompoundTag> templates,
            UUID entityBatchId
    ) {
        Map<UUID, UUID> entityIds = new HashMap<>();
        templates.forEach(template -> collectEntityUuidMappings(template, entityIds));

        int spawnedEntities = 0;
        int failedEntities = 0;
        for (CompoundTag template : templates) {
            CompoundTag spawnTag = template.copy();
            rewriteEntityTreeUuids(spawnTag, entityIds);
            try {
                Entity entity = EntityType.loadEntityRecursive(spawnTag, targetLevel, loaded -> {
                    loaded.getPersistentData().putBoolean(MATCH_ENTITY_MARKER, true);
                    loaded.getPersistentData().putUUID(MATCH_ENTITY_BATCH, entityBatchId);
                    return loaded;
                });
                if (entity == null) {
                    failedEntities += countEntityTree(spawnTag);
                    continue;
                }

                int entityTreeSize = Math.toIntExact(entity.getSelfAndPassengers().count());
                if (targetLevel.tryAddFreshEntityWithPassengers(entity)) {
                    spawnedEntities += entityTreeSize;
                } else {
                    failedEntities += entityTreeSize;
                }
            } catch (RuntimeException exception) {
                int failedTreeSize = countEntityTree(spawnTag);
                failedEntities += failedTreeSize;
                WarDayMod.LOGGER.warn("Could not create prepared War Day entity template {}", template.getString("id"), exception);
            }
        }

        return new EntityTemplateSpawn(spawnedEntities, failedEntities);
    }

    private static void collectEntityUuidMappings(CompoundTag tag, Map<UUID, UUID> entityIds) {
        if (tag.hasUUID(Entity.UUID_TAG)) {
            entityIds.computeIfAbsent(tag.getUUID(Entity.UUID_TAG), ignored -> UUID.randomUUID());
        }
        ListTag passengers = tag.getList(Entity.PASSENGERS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < passengers.size(); i++) {
            collectEntityUuidMappings(passengers.getCompound(i), entityIds);
        }
    }

    private static void rewriteEntityTreeUuids(CompoundTag tag, Map<UUID, UUID> entityIds) {
        if (tag.hasUUID(Entity.UUID_TAG)) {
            UUID originalId = tag.getUUID(Entity.UUID_TAG);
            tag.putUUID(Entity.UUID_TAG, entityIds.computeIfAbsent(originalId, ignored -> UUID.randomUUID()));
        } else {
            tag.putUUID(Entity.UUID_TAG, UUID.randomUUID());
        }

        if (tag.contains("leash", Tag.TAG_COMPOUND)) {
            CompoundTag leash = tag.getCompound("leash");
            if (leash.hasUUID(Entity.UUID_TAG)) {
                UUID replacement = entityIds.get(leash.getUUID(Entity.UUID_TAG));
                if (replacement == null) {
                    tag.remove("leash");
                } else {
                    leash.putUUID(Entity.UUID_TAG, replacement);
                }
            }
        }

        ListTag passengers = tag.getList(Entity.PASSENGERS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < passengers.size(); i++) {
            rewriteEntityTreeUuids(passengers.getCompound(i), entityIds);
        }
    }

    private static int countEntityTree(CompoundTag tag) {
        int count = 1;
        ListTag passengers = tag.getList(Entity.PASSENGERS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < passengers.size(); i++) {
            count += countEntityTree(passengers.getCompound(i));
        }
        return count;
    }

    private static EntityCopyResult copyDecorativeEntities(ServerLevel sourceLevel, ServerLevel targetLevel, PlacementPlan plan) {
        if (sourceLevel == null) {
            return new EntityCopyResult(0, 0, 0);
        }

        int copied = 0;
        int itemFramesCleared = 0;
        int failed = 0;
        AABB sourceBounds = plan.sourceEntityBounds(sourceLevel).inflate(HANGING_ENTITY_SCAN_MARGIN);
        List<Entity> entities = sourceLevel.getEntities(
                (Entity) null,
                sourceBounds,
                entity -> isAllowedDecorativeEntity(entity)
                        && entity instanceof HangingEntity hangingEntity
                        && plan.containsSourceChunk(hangingEntity.getPos())
        );

        for (Entity sourceEntity : entities) {
            CompoundTag tag = new CompoundTag();
            if (!sourceEntity.save(tag)) {
                failed++;
                continue;
            }

            tag.remove("UUID");
            tag.put("Pos", newDoubleList(
                    plan.targetX(sourceEntity.getX(), sourceEntity.getZ()),
                    plan.targetY(sourceEntity.getY()),
                    plan.targetZ(sourceEntity.getX(), sourceEntity.getZ())
            ));
            rotateEntityYaw(tag, plan);
            rotateHangingEntityFacing(tag, plan);
            translateHangingEntityTile(tag, plan);

            Optional<Entity> copiedEntity = EntityType.create(tag, targetLevel);
            if (copiedEntity.isEmpty()) {
                failed++;
                continue;
            }

            Entity entity = copiedEntity.get();
            if (!(sourceEntity instanceof HangingEntity sourceHanging)
                    || !(entity instanceof HangingEntity copiedHanging)
                    || copiedHanging.getDirection() != plan.rotation().rotate(sourceHanging.getDirection())
                    || sourceEntity instanceof Painting sourcePainting
                    && (!(entity instanceof Painting copiedPainting)
                    || !copiedPainting.getVariant().equals(sourcePainting.getVariant()))
                    || !copiedHanging.survives()) {
                entity.discard();
                failed++;
                continue;
            }

            boolean clearedItemFrame = entity instanceof ItemFrame;
            if (entity instanceof ItemFrame itemFrame) {
                itemFrame.setItem(ItemStack.EMPTY, false);
            }

            entity.getPersistentData().putBoolean(PREPARED_DECORATIVE_MARKER, true);
            if (targetLevel.addFreshEntity(entity)) {
                copied++;
                if (clearedItemFrame) {
                    itemFramesCleared++;
                }
            } else {
                entity.discard();
                failed++;
            }
        }

        return new EntityCopyResult(copied, itemFramesCleared, failed);
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

    private static Optional<BlockPos> findSafeSpawnNear(ServerLevel level, BlockPos preferred, int horizontalRadius, int verticalRadius) {
        if (isSafeSpawnPos(level, preferred)) {
            return Optional.of(preferred);
        }

        int minY = Math.max(level.getMinBuildHeight() + 1, preferred.getY() - verticalRadius);
        int maxY = Math.min(level.getMaxBuildHeight() - 2, preferred.getY() + verticalRadius);
        for (int distance = 1; distance <= horizontalRadius; distance++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = preferred.getX() - distance; x <= preferred.getX() + distance; x++) {
                    for (int z = preferred.getZ() - distance; z <= preferred.getZ() + distance; z++) {
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

    private static void rotateHangingEntityFacing(CompoundTag tag, PlacementPlan plan) {
        if (tag.contains("facing", Tag.TAG_BYTE)) {
            Direction sourceFacing = Direction.from2DDataValue(tag.getByte("facing"));
            Direction targetFacing = plan.rotation().rotate(sourceFacing);
            tag.putByte("facing", (byte) targetFacing.get2DDataValue());
        }

        if (tag.contains("Facing", Tag.TAG_BYTE)) {
            Direction sourceFacing = Direction.from3DDataValue(tag.getByte("Facing"));
            Direction targetFacing = plan.rotation().rotate(sourceFacing);
            tag.putByte("Facing", (byte) targetFacing.get3DDataValue());
        }
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
                    new BlockPos(attackerTargetOffset(), WarDayConfig.WAR_DAY_BASE_Y.getAsInt(), 0),
                    Optional.empty()
            );
        }

        private static int attackerTargetOffset() {
            int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
            int margin = Math.min(MATCH_BORDER_SPAWN_MARGIN, Math.max(1, halfSize / 4));
            int maximumSafeOffset = Math.max(1, halfSize - margin);
            return Math.min(WarDayConfig.BASE_SPACING_BLOCKS.getAsInt(), maximumSafeOffset);
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

        private AABB targetEntityBounds(ServerLevel targetLevel) {
            TargetFootprint footprint = targetFootprint();
            return new AABB(
                    footprint.minX(),
                    targetLevel.getMinBuildHeight(),
                    footprint.minZ(),
                    footprint.maxX() + 1.0D,
                    targetLevel.getMaxBuildHeight(),
                    footprint.maxZ() + 1.0D
            );
        }

        private boolean containsSourceChunk(BlockPos sourcePos) {
            return cluster.contains(new ChunkDimPos(dimension, new ChunkPos(sourcePos)));
        }

        private boolean containsTargetBlock(BlockPos targetPos) {
            return containsSourceChunk(sourcePos(targetPos));
        }

        private BlockPos sourcePos(BlockPos targetPos) {
            int targetOffsetX = targetPos.getX() - targetAnchorPos.getX();
            int targetOffsetY = targetPos.getY() - targetAnchorPos.getY();
            int targetOffsetZ = targetPos.getZ() - targetAnchorPos.getZ();
            BlockPos sourceOffset = switch (rotation()) {
                case NONE -> new BlockPos(targetOffsetX, targetOffsetY, targetOffsetZ);
                case CLOCKWISE_90 -> new BlockPos(targetOffsetZ, targetOffsetY, -targetOffsetX);
                case CLOCKWISE_180 -> new BlockPos(-targetOffsetX, targetOffsetY, -targetOffsetZ);
                case COUNTERCLOCKWISE_90 -> new BlockPos(-targetOffsetZ, targetOffsetY, targetOffsetX);
            };
            return anchorPos.offset(sourceOffset);
        }

        private double targetY(double sourceY) {
            return sourceY + targetAnchorPos.getY() - anchorPos.getY();
        }

        private double targetX(double sourceX, double sourceZ) {
            double sourceCenterX = anchorPos.getX() + 0.5D;
            double sourceCenterZ = anchorPos.getZ() + 0.5D;
            return targetAnchorPos.getX() + 0.5D
                    + rotatedOffsetX(sourceX - sourceCenterX, sourceZ - sourceCenterZ);
        }

        private double targetZ(double sourceX, double sourceZ) {
            double sourceCenterX = anchorPos.getX() + 0.5D;
            double sourceCenterZ = anchorPos.getZ() + 0.5D;
            return targetAnchorPos.getZ() + 0.5D
                    + rotatedOffsetZ(sourceX - sourceCenterX, sourceZ - sourceCenterZ);
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

    private record EntityCopyResult(int entitiesCopied, int itemFramesCleared, int entitiesFailed) {
    }

    private record EntityTemplateCapture(List<CompoundTag> templates, int entityCount, int skippedCount) {
        private static EntityTemplateCapture empty() {
            return new EntityTemplateCapture(List.of(), 0, 0);
        }
    }

    private record EntityTemplateSpawn(int spawnedCount, int failedCount) {
    }

    private record RosterPlayer(UUID id, String name, WarDayRosterHealth.Snapshot health) {
    }

    private record RosterLine(net.minecraft.network.chat.Component display, NumberFormat numberFormat) {
    }

    private record ParticipantRespawn(ServerLevel level, BlockPos spawnPos, Item matchBlock) {
    }

    private record PendingRespawn(int ticksRemaining, ParticipantRespawn participant, UUID spectatorTargetId) {
        private PendingRespawn tick() {
            return new PendingRespawn(ticksRemaining - 1, participant, spectatorTargetId);
        }

        private PendingRespawn withSpectatorTarget(UUID targetId) {
            return new PendingRespawn(ticksRemaining, participant, targetId);
        }
    }
}
