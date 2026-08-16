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
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
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
    private static final int ATTACKER_CORNER_SELECTION_SECONDS = 5;
    private static final double HANGING_ENTITY_SCAN_MARGIN = 8.0D;
    private static final String MATCH_ENTITY_MARKER = "warday_match_entity";
    private static final String MATCH_ENTITY_BATCH = "warday_match_entity_batch";
    private static final String PREPARED_DECORATIVE_MARKER = "warday_prepared_decorative";
    private static final String MATCH_BLOCK_DATA_KEY = "WardayTeamBlock";
    private static final String MATCH_MAP_DATA_KEY = "WardayArenaMap";
    private static final String DEFENDER_MATCH_BLOCK_MARKER = "defender";
    private static final String ATTACKER_MATCH_BLOCK_MARKER = "attacker";
    private static final ResourceLocation ENDER_POUCH_ID = ResourceLocation.fromNamespaceAndPath("enderstorage", "ender_pouch");
    private static final String WARDAY_ROSTER_LEGACY_OBJECTIVE = "warday_roster";
    private static final String WARDAY_DEFENDER_ROSTER_OBJECTIVE = "warday_roster_d";
    private static final String WARDAY_ATTACKER_ROSTER_OBJECTIVE = "warday_roster_a";
    private static final ResourceLocation RAPID_BREAK_PENALTY_ID = ResourceLocation.fromNamespaceAndPath(WarDayMod.MODID, "rapid_break_penalty");
    private static final int ROSTER_PLAYERS_PER_TEAM_PAGE = 6;
    private static final long ROSTER_PAGE_TICKS = 100L;
    private static final long PREPARATION_TICK_BUDGET_NANOS = 10_000_000L;
    private static final int PREPARATION_MAX_STEPS_PER_TICK = 65_536;
    private static final SuggestionProvider<CommandSourceStack> TEAM_NAME_SUGGESTIONS = WarDayCommands::suggestTeamNames;
    private static final Map<UUID, PendingRespawn> PENDING_RESPAWNS = new HashMap<>();
    private static final Map<UUID, Integer> DEATH_COUNTS = new HashMap<>();
    private static final Map<UUID, Deque<Long>> DIG_HISTORY = new HashMap<>();
    private static final Map<UUID, RapidBreakPenalty> RAPID_BREAK_PENALTIES = new HashMap<>();
    private static final Map<UUID, Integer> RAPID_BREAK_STRIKES = new HashMap<>();
    private static final Set<UUID> ROSTER_OBJECTIVES_KNOWN = new HashSet<>();
    private static ServerBossEvent matchTimerBossBar;
    private static MinecraftServer matchTimerBossBarServer;
    private static long lastBossBarSeconds = Long.MIN_VALUE;
    private static MinecraftServer rosterScoreboardServer;
    private static String lastRosterSignature = "";
    private PreparationJob preparationJob;
    private ClearJob clearJob;

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
                .then(Commands.literal("clear")
                        .executes(context -> clearDimension(context.getSource())))
                .then(Commands.literal("prepare")
                        .executes(context -> preparePreview(context.getSource()))
                        .then(Commands.literal("confirm")
                                .executes(context -> prepareConfirm(context.getSource())))
                        .then(Commands.literal("status")
                                .executes(context -> preparationStatus(context.getSource())))
                        .then(Commands.literal("cancel")
                                .executes(context -> cancelPreparation(context.getSource()))))
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
        if (preparationJob != null || clearJob != null) {
            source.sendFailure(message(ChatFormatting.YELLOW,
                    "Team configuration cannot change while an arena preparation or clear job is running."));
            return 0;
        }
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
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "Added the defender nexus and forward marker. Attacker terrain and spawn are automatic."), true);
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
        reportTeamValidation(source, teamAValidation);

        if (teamAValidation.passed()) {
            source.sendSuccess(() -> message(ChatFormatting.GREEN,
                    context.teamB().isPresent()
                            ? "Validation passed: defender base is configured; the nexus-centered battlefield and four attacker corner spawns will be prepared automatically."
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
        reportTeamScan(source, teamAValidation, context.chunkManager());
        reportGuardrails(source, teamAValidation, context.chunkManager());

        if (!teamAValidation.passed()) {
            source.sendFailure(message(ChatFormatting.RED,
                    "Scan could not resolve the defender base. Run /warday validate for marker-specific failures."));
            return 0;
        }

        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "Scan complete: defender base resolved; its 256x256 surroundings and four attacker corner spawns will be automatic."), false);
        return 1;
    }

    private int preparePreview(CommandSourceStack source) {
        if (preparationJob != null) {
            source.sendFailure(message(ChatFormatting.YELLOW,
                    "A War Day preparation job is already running. Use /warday prepare status or /warday prepare cancel."));
            return 0;
        }
        if (clearJob != null) {
            source.sendFailure(message(ChatFormatting.YELLOW,
                    "The War Day dimension is still being cleared. Use /warday status for progress."));
            return 0;
        }

        Optional<PreparationContext> contextResult = resolvePreparationContext(source);
        if (contextResult.isEmpty()) {
            return 0;
        }

        PreparationContext context = contextResult.get();
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        int terrainChunks = context.attackerArea().cluster().size();
        int claimedColumns = context.defenderBase().cluster().size() * 256;
        int surroundingColumns = halfSize * 2 * halfSize * 2 - claimedColumns;
        source.sendSuccess(() -> message(ChatFormatting.AQUA, "War Day prepare preview only. No blocks were copied."), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY, "Target dimension: " + WarDayConfig.WAR_DAY_DIMENSION.get()), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "The defender claim and its real surrounding terrain form one exact nexus-centered "
                        + (halfSize * 2) + "x" + (halfSize * 2)
                        + " source area with four attacker corner spawns."), false);

        reportPlacementPlan(source, context.defenderPlan());
        reportPlacementPlan(source, context.attackerPlan());
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- exact arena composition: " + claimedColumns + " claimed columns + " + surroundingColumns
                        + " surrounding columns = " + (halfSize * 2 * halfSize * 2) + " total columns"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- source coverage: " + terrainChunks + " local chunks intersect the exact source window"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- preparation execution: source loading, clearing, copying, and biome transfer run in bounded batches across server ticks"), false);

        source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                "Run /warday prepare confirm to start the background preparation job, then /warday prepare status for progress."), false);
        return 1;
    }

    private int prepareConfirm(CommandSourceStack source) {
        if (preparationJob != null) {
            source.sendFailure(message(ChatFormatting.YELLOW,
                    "A War Day preparation job is already running. Use /warday prepare status or /warday prepare cancel."));
            return 0;
        }
        if (clearJob != null) {
            source.sendFailure(message(ChatFormatting.YELLOW,
                    "The War Day dimension is still being cleared. Use /warday status for progress."));
            return 0;
        }

        WarDayState state = WarDayState.get(source.getServer());
        if (state.isActive()) {
            source.sendFailure(message(ChatFormatting.RED, "War Day cannot be prepared while a match is active."));
            return 0;
        }

        Optional<PreparationContext> contextResult = resolvePreparationContext(source);
        if (contextResult.isEmpty()) {
            return 0;
        }

        PreparationContext context = contextResult.get();
        ServerLevel targetLevel = source.getServer().getLevel(context.targetDimension());
        if (targetLevel == null) {
            source.sendFailure(message(ChatFormatting.RED, "War Day dimension is not loaded: " + WarDayConfig.WAR_DAY_DIMENSION.get()));
            source.sendFailure(message(ChatFormatting.RED, "Restart the server after adding this mod jar so the bundled dimension data can load."));
            return 0;
        }

        preparationJob = new PreparationJob(source, context, targetLevel);
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "War Day preparation started. Work is limited per server tick; use /warday prepare status for progress or /warday prepare cancel."), true);
        return 1;
    }

    private int preparationStatus(CommandSourceStack source) {
        if (preparationJob == null) {
            source.sendSuccess(() -> message(ChatFormatting.GRAY, "No War Day preparation job is running."), false);
            return 0;
        }
        preparationJob.reportStatus(source);
        return 1;
    }

    private int cancelPreparation(CommandSourceStack source) {
        if (preparationJob == null) {
            source.sendFailure(message(ChatFormatting.GRAY, "No War Day preparation job is running."));
            return 0;
        }
        boolean worldChanged = preparationJob.worldChanged();
        preparationJob = null;
        source.sendSuccess(() -> message(
                worldChanged ? ChatFormatting.YELLOW : ChatFormatting.GREEN,
                worldChanged
                        ? "War Day preparation cancelled. The prepared flag remains cleared because the arena was partially changed; rerun /warday prepare confirm."
                        : "War Day preparation cancelled before the arena was changed."
        ), true);
        return 1;
    }

    private int clearDimension(CommandSourceStack source) {
        if (preparationJob != null) {
            source.sendFailure(message(ChatFormatting.YELLOW,
                    "War Day preparation is running. Cancel or finish it before clearing the dimension."));
            return 0;
        }
        if (clearJob != null) {
            clearJob.reportStatus(source);
            return 0;
        }

        WarDayState state = WarDayState.get(source.getServer());
        if (state.isActive()) {
            source.sendFailure(message(ChatFormatting.RED,
                    "War Day is active. Use /warday end before clearing the dimension."));
            return 0;
        }

        Optional<ResourceKey<Level>> dimensionKey = warDayDimensionKey(source);
        if (dimensionKey.isEmpty()) {
            return 0;
        }
        ServerLevel level = source.getServer().getLevel(dimensionKey.get());
        if (level == null) {
            source.sendFailure(message(ChatFormatting.RED,
                    "War Day dimension is not loaded: " + WarDayConfig.WAR_DAY_DIMENSION.get()));
            return 0;
        }

        state.invalidatePrepared();
        configureWorldBorder(level);
        clearJob = new ClearJob(source, level);
        int playersPresent = level.players().size();
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "War Day dimension clear started. The full 256x256 arena and all loaded non-player entities "
                        + "will be removed in bounded batches; use /warday status for progress."), true);
        if (playersPresent > 0) {
            source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                    playersPresent + " player(s) are inside the War Day dimension. Players will not be removed, "
                            + "but the terrain beneath them will be cleared."), true);
        }
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
        if (preparationJob != null) {
            preparationJob.reportStatus(source);
        }
        if (clearJob != null) {
            clearJob.reportStatus(source);
        }

        if (state.isPrepared()) {
            source.sendSuccess(() -> message(ChatFormatting.GRAY, "Saved dimension: " + state.warDayDimension()), false);
            source.sendSuccess(() -> message(ChatFormatting.GRAY, "Saved defender team: " + state.defenderTeam()), false);
            source.sendSuccess(() -> message(ChatFormatting.GRAY, "Saved attacker team: " + (state.attackerTeam().isBlank() ? "none" : state.attackerTeam())), false);
            source.sendSuccess(() -> message(ChatFormatting.GRAY,
                    "Copied nexus: " + state.copiedNexusPos().map(WarDayCommands::formatPos).orElse("missing")), false);
            source.sendSuccess(() -> message(ChatFormatting.GRAY,
                    "Attacker corner spawns: " + state.attackerSpawnPositions().stream()
                            .map(WarDayCommands::formatPos).toList()), false);
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
            } else if (preparationJob == null && clearJob == null) {
                source.sendSuccess(() -> message(ChatFormatting.GREEN, "Next command: /warday start"), false);
            }
        } else if (preparationJob == null && clearJob == null) {
            source.sendSuccess(() -> message(ChatFormatting.YELLOW, "Next command: /warday prepare confirm"), false);
        }

        return state.isPrepared() ? 1 : 0;
    }

    private int start(CommandSourceStack source) {
        if (preparationJob != null) {
            source.sendFailure(message(ChatFormatting.YELLOW,
                    "War Day preparation is still running. Use /warday prepare status or /warday prepare cancel."));
            return 0;
        }
        if (clearJob != null) {
            source.sendFailure(message(ChatFormatting.YELLOW,
                    "The War Day dimension is still being cleared. Use /warday status for progress."));
            return 0;
        }
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
        if (state.copiedNexusPos().isEmpty() || state.attackerSpawnPositions().size() != 4) {
            source.sendFailure(message(ChatFormatting.RED,
                    "Prepared state is missing the nexus or four attacker corner spawns. Rerun /warday prepare confirm."));
            return 0;
        }

        BlockPos copiedNexusPos = state.copiedNexusPos().get();
        Optional<BlockPos> defenderSpawn = findSafeSpawnNear(warDayLevel, copiedNexusPos.offset(0, 1, 0), 8, 4);
        List<BlockPos> attackerSpawns = state.attackerSpawnPositions();
        if (defenderSpawn.isEmpty()) {
            source.sendFailure(message(ChatFormatting.RED,
                    "No safe defender spawn exists near the copied nexus. Repair the platform and rerun /warday prepare confirm."));
            return 0;
        }
        for (BlockPos attackerSpawn : attackerSpawns) {
            if (!isSafeSpawnPos(warDayLevel, attackerSpawn) || !isInsideConfiguredMatchBorder(attackerSpawn)) {
                source.sendFailure(message(ChatFormatting.RED,
                        "A prepared attacker corner is no longer safe or inside the border at " + formatPos(attackerSpawn)
                                + ". Repair it and rerun /warday prepare confirm."));
                return 0;
            }
        }

        Map<UUID, WarDayState.PlayerSnapshot> snapshots = new HashMap<>();
        int defenders = 0;
        int attackers = 0;
        int spectators = 0;
        int attackerSpawnIndex = 0;
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
        MapId arenaMapId = createArenaMap(warDayLevel);
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
                originalWorldBorderSize,
                arenaMapId.id()
        );

        warDayLevel.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, source.getServer());
        configureWorldBorder(warDayLevel);
        spawnNexusMarker(warDayLevel, state, state.copiedNexusPos().get());
        DEATH_COUNTS.clear();
        clearRapidBreakPenalties(source.getServer());
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
                ensureInventoryHasAtLeast(
                        player, defenderBlock, DEFENDER_MATCH_BLOCK_MARKER, MATCH_BLOCK_TARGET_COUNT);
                defenders++;
            } else if (attackerIds.contains(id)) {
                BlockPos spawn = attackerSpawns.get(attackerSpawnIndex++ % attackerSpawns.size());
                teleportPlayer(player, warDayLevel, spawn);
                setPlayerSpawn(player, warDayLevel, spawn);
                player.setGameMode(GameType.SURVIVAL);
                ensureInventoryHasAtLeast(
                        player, attackerBlock, ATTACKER_MATCH_BLOCK_MARKER, MATCH_BLOCK_TARGET_COUNT);
                attackers++;
            } else {
                player.setGameMode(GameType.SPECTATOR);
                teleportPlayer(player, warDayLevel, attackerSpawns.getFirst().offset(0, 11, 0));
                spectators++;
            }
            ensureArenaMap(player, state);
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
        state.clearRespawnCornerChoice(player.getUUID());
        int delaySeconds = respawnDelaySecondsForDeath(deathCount);
        if (participant.attacker()) {
            delaySeconds = Math.max(delaySeconds, ATTACKER_CORNER_SELECTION_SECONDS);
        }
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
                "Respawning in " + delaySeconds + " seconds."
                        + (participant.attacker() ? " Choose a corner in the popup." : "")
                        + " Left click views the previous living teammate; right click views the next."));
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
        if (event.getEntity() instanceof ServerPlayer player) {
            if (matchTimerBossBar != null) {
                matchTimerBossBar.removePlayer(player);
            }
            clearRapidBreakPenalty(player, player.serverLevel().getGameTime());
            DIG_HISTORY.remove(player.getUUID());
            ROSTER_OBJECTIVES_KNOWN.remove(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }

        WarDayState state = WarDayState.get(victim.getServer());
        if (!state.isCombatActive()
                || !victim.level().dimension().location().toString().equals(state.warDayDimension())
                || !attacker.level().dimension().location().toString().equals(state.warDayDimension())) {
            return;
        }

        if (WarDayFriendlyFire.areTeammates(
                attacker.getUUID(),
                victim.getUUID(),
                state.defenderParticipants(),
                state.attackerParticipants()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        preparationJob = null;
        clearJob = null;
        clearRapidBreakPenalties(event.getServer());
        DIG_HISTORY.clear();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (preparationJob != null) {
            if (preparationJob.tick(event.getServer())) {
                preparationJob = null;
            }
        }
        if (clearJob != null) {
            if (clearJob.tick(event.getServer())) {
                clearJob = null;
            }
        }

        WarDayState state = WarDayState.get(event.getServer());
        if (!state.isActive()) {
            PENDING_RESPAWNS.clear();
            clearRapidBreakPenalties(event.getServer());
            DIG_HISTORY.clear();
            hideMatchTimerBossBar(event.getServer());
            return;
        }

        Optional<ServerLevel> warDayLevel = warDayLevel(event.getServer(), state);
        long rosterGameTime = warDayLevel
                .map(ServerLevel::getGameTime)
                .orElse(event.getServer().overworld().getGameTime());
        tickRapidBreakPenalties(event.getServer(), rosterGameTime);
        if (rosterGameTime % 20L == 0L) {
            pruneDigHistory(rosterGameTime);
        }
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
        if (!isInMatchBounds(event.getPos())) {
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSuccessfulParticipantBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        WarDayState state = WarDayState.get(level.getServer());
        UUID playerId = player.getUUID();
        if (!state.isCombatActive()
                || state.copiedNexusPos().isEmpty()
                || !level.dimension().location().toString().equals(state.warDayDimension())
                || player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL
                || !(state.isDefenderParticipant(playerId) || state.isAttackerParticipant(playerId))
                || !isInMatchBounds(event.getPos())
                || event.getState().is(WarDayMod.NEXUS.get())
                || event.getState().is(WarDayMod.FORWARD_MARKER.get())
                || event.getState().is(WarDayMod.ATTACKER_SPAWN.get())) {
            return;
        }

        trackDiggingPenalty(player, level.getGameTime());
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
        if (!isInMatchBounds(event.getPos())) {
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
        if (participant.isEmpty()
                || !event.getPlacedBlock().is(Block.byItem(participant.get().matchBlock()))
                || !isHoldingTaggedMatchBlock(player, participant.get())) {
            event.setCanceled(true);
            player.displayClientMessage(message(ChatFormatting.RED,
                    "Only Warday-issued blocks marked for your team can be placed during War Day."), true);
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
        clearRapidBreakPenalties(server);
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
        if (current == null || isWarDayRosterObjective(current.getName())) {
            return "";
        }
        return current.getName();
    }

    private static boolean isWarDayRosterObjective(String name) {
        return WARDAY_ROSTER_LEGACY_OBJECTIVE.equals(name)
                || WARDAY_DEFENDER_ROSTER_OBJECTIVE.equals(name)
                || WARDAY_ATTACKER_ROSTER_OBJECTIVE.equals(name);
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
        Objective displayed = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (displayed != null && !isWarDayRosterObjective(displayed.getName())) {
            state.setPreviousSidebarObjective(displayed.getName());
        }

        Objective legacy = scoreboard.getObjective(WARDAY_ROSTER_LEGACY_OBJECTIVE);
        if (legacy != null) {
            if (displayed == legacy) {
                Objective previous = state.previousSidebarObjective().isBlank()
                        ? null
                        : scoreboard.getObjective(state.previousSidebarObjective());
                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, previous);
            }
            scoreboard.removeObjective(legacy);
        }

        Objective defenderObjective = scoreboard.getObjective(WARDAY_DEFENDER_ROSTER_OBJECTIVE);
        Objective attackerObjective = scoreboard.getObjective(WARDAY_ATTACKER_ROSTER_OBJECTIVE);
        boolean wrongServer = rosterScoreboardServer != server;
        boolean defenderMissing = defenderObjective == null;
        boolean attackerMissing = attackerObjective == null;
        if (defenderMissing) {
            defenderObjective = addRosterObjective(scoreboard, WARDAY_DEFENDER_ROSTER_OBJECTIVE);
        }
        if (attackerMissing) {
            attackerObjective = addRosterObjective(scoreboard, WARDAY_ATTACKER_ROSTER_OBJECTIVE);
        }
        boolean startedTracking = false;
        if (wrongServer || defenderMissing) {
            scoreboard.startTrackingObjective(defenderObjective);
            startedTracking = true;
        }
        if (wrongServer || attackerMissing) {
            scoreboard.startTrackingObjective(attackerObjective);
            startedTracking = true;
        }
        if (startedTracking) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ROSTER_OBJECTIVES_KNOWN.add(player.getUUID());
            }
        }

        if (force || wrongServer || defenderMissing || attackerMissing || !signature.equals(lastRosterSignature)) {
            net.minecraft.network.chat.Component title = message(ChatFormatting.GOLD,
                    pageCount > 1 ? "War Day Teams " + (page + 1) + "/" + pageCount : "War Day Teams");
            defenderObjective.setDisplayName(title);
            attackerObjective.setDisplayName(title);

            List<RosterLine> defenderLines = rosterLines(
                    state, defenders, attackers, page, true);
            List<RosterLine> attackerLines = rosterLines(
                    state, defenders, attackers, page, false);
            updateRosterObjective(scoreboard, defenderObjective, defenderLines);
            updateRosterObjective(scoreboard, attackerObjective, attackerLines);
            lastRosterSignature = signature;
        }

        sendRosterViews(server, state, defenderObjective, attackerObjective);
        rosterScoreboardServer = server;
    }

    private static Objective addRosterObjective(ServerScoreboard scoreboard, String name) {
        return scoreboard.addObjective(
                name,
                ObjectiveCriteria.DUMMY,
                message(ChatFormatting.GOLD, "War Day Teams"),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                BlankFormat.INSTANCE
        );
    }

    private static List<RosterLine> rosterLines(
            WarDayState state,
            List<RosterPlayer> defenders,
            List<RosterPlayer> attackers,
            int page,
            boolean defenderView
    ) {
        List<RosterLine> lines = new ArrayList<>();
        addRosterTeamLines(lines, state.defenderTeam(), defenders, page, ChatFormatting.AQUA, defenderView);
        addRosterTeamLines(lines, state.attackerTeam(), attackers, page, ChatFormatting.RED, !defenderView);
        return lines;
    }

    private static void updateRosterObjective(
            ServerScoreboard scoreboard,
            Objective objective,
            List<RosterLine> lines
    ) {
        removeUnexpectedRosterScores(scoreboard, objective, lines.size());
        for (int index = 0; index < lines.size(); index++) {
            RosterLine line = lines.get(index);
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(
                    ScoreHolder.forNameOnly("wd_line_" + index), objective);
            score.display(line.display());
            score.numberFormatOverride(line.numberFormat());
            score.set(15 - index);
        }
    }

    private static void sendRosterViews(
            MinecraftServer server,
            WarDayState state,
            Objective defenderObjective,
            Objective attackerObjective
    ) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ROSTER_OBJECTIVES_KNOWN.add(player.getUUID())) {
                for (net.minecraft.network.protocol.Packet<?> packet
                        : server.getScoreboard().getStartTrackingPackets(defenderObjective)) {
                    player.connection.send(packet);
                }
                for (net.minecraft.network.protocol.Packet<?> packet
                        : server.getScoreboard().getStartTrackingPackets(attackerObjective)) {
                    player.connection.send(packet);
                }
            }
            Objective view = null;
            if (state.isDefenderParticipant(player.getUUID())) {
                view = defenderObjective;
            } else if (state.isAttackerParticipant(player.getUUID())) {
                view = attackerObjective;
            }
            if (view != null) {
                player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, view));
            }
        }
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
            ChatFormatting color,
            boolean showStatus
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
                    showStatus
                            ? new FixedFormat(message(rosterHealthColor(player.health()), WarDayRosterHealth.displayText(player.health())))
                            : BlankFormat.INSTANCE
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
        Objective current = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (current != null && isWarDayRosterObjective(current.getName())) {
            String previousName = state.previousSidebarObjective();
            Objective previous = previousName.isBlank() ? null : scoreboard.getObjective(previousName);
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, previous);
        }
        Objective restored = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, restored));
        }
        for (String objectiveName : List.of(
                WARDAY_ROSTER_LEGACY_OBJECTIVE,
                WARDAY_DEFENDER_ROSTER_OBJECTIVE,
                WARDAY_ATTACKER_ROSTER_OBJECTIVE
        )) {
            Objective objective = scoreboard.getObjective(objectiveName);
            if (objective != null) {
                scoreboard.removeObjective(objective);
            }
        }
        rosterScoreboardServer = null;
        lastRosterSignature = "";
        ROSTER_OBJECTIVES_KNOWN.clear();
    }

    private static int endActiveWarDay(MinecraftServer server, WarDayState state) {
        hideMatchTimerBossBar(server);
        clearRapidBreakPenalties(server);
        int restored = 0;
        int issuedArenaMapId = state.arenaMapId().orElse(-1);
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
            removeArenaMap(player, issuedArenaMapId);
        }

        if (state.keepInventoryCaptured()) {
            warDayLevel(server, state).ifPresent(level ->
                    level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(state.originalKeepInventory(), server)
            );
        }
        warDayLevel(server, state).ifPresent(level -> {
            clearLoadedMatchStorage(level, state);
            removeNexusMarker(level, state);
            int blocksWiped = wipeDestinationArena(level);
            int entitiesDiscarded = clearAndDiscardWarDayEntities(level);
            WarDayMod.LOGGER.info(
                    "War Day cleanup wiped {} arena blocks and discarded {} non-player entities from {}",
                    blocksWiped,
                    entitiesDiscarded,
                    level.dimension().location()
            );
            configureWorldBorder(level);
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

        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        int minChunkX = Math.floorDiv(-halfSize, 16);
        int maxChunkX = Math.floorDiv(halfSize - 1, 16);
        int minChunkZ = Math.floorDiv(-halfSize, 16);
        int maxChunkZ = Math.floorDiv(halfSize - 1, 16);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    if (!isInMatchBounds(entry.getKey())) {
                        continue;
                    }
                    if (isTeamSharedFortChest(entry.getValue())) {
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
            if (entity instanceof ItemFrame itemFrame && isInMatchBounds(itemFrame.blockPosition())) {
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
            clearItemHandler(handler);
        }
    }

    private static boolean isTeamSharedFortChest(BlockEntity blockEntity) {
        return BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock())
                .equals(ResourceLocation.fromNamespaceAndPath("fortifications", "fort_chest"));
    }

    private static int clearAndDiscardWarDayEntities(ServerLevel level) {
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ServerPlayer) && !entity.isRemoved()) {
                entities.add(entity);
            }
        }
        for (Entity entity : entities) {
            clearEntityInventory(entity);
            entity.discard();
        }
        return entities.size();
    }

    private static void clearEntityInventory(Entity entity) {
        if (entity instanceof Container container) {
            container.clearContent();
        }
        Set<IItemHandler> handlers = new HashSet<>();
        IItemHandler general = entity.getCapability(Capabilities.ItemHandler.ENTITY);
        if (general != null) {
            handlers.add(general);
        }
        for (Direction direction : Direction.values()) {
            IItemHandler automation = entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, direction);
            if (automation != null) {
                handlers.add(automation);
            }
        }
        handlers.forEach(WarDayCommands::clearItemHandler);
    }

    private static void clearItemHandler(IItemHandler handler) {
        if (!(handler instanceof IItemHandlerModifiable modifiable)) {
            return;
        }
        for (int slot = 0; slot < modifiable.getSlots(); slot++) {
            modifiable.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private static void giveRespawnMatchBlocks(ServerPlayer player) {
        WarDayState state = WarDayState.get(player.getServer());
        if (!state.isCombatActive()) {
            return;
        }
        ensureArenaMap(player, state);
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return;
        }

        TeamManager teamManager = FTBTeamsAPI.api().getManager();
        Optional<Team> defenderTeam = findTeamByConfiguredName(teamManager, WarDayConfig.TEAM_A_NAME.get());
        Optional<Team> attackerTeam = findTeamByConfiguredName(teamManager, WarDayConfig.TEAM_B_NAME.get());
        UUID playerId = player.getUUID();

        if (defenderTeam.map(team -> team.getMembers().contains(playerId)).orElse(false)) {
            ensureInventoryHasAtLeast(
                    player, defenderMatchBlock(), DEFENDER_MATCH_BLOCK_MARKER, MATCH_BLOCK_TARGET_COUNT);
        } else if (attackerTeam.map(team -> team.getMembers().contains(playerId)).orElse(false)) {
            ensureInventoryHasAtLeast(
                    player, attackerMatchBlock(), ATTACKER_MATCH_BLOCK_MARKER, MATCH_BLOCK_TARGET_COUNT);
        }
    }

    private static void applyActiveMatchRole(ServerPlayer player, WarDayState state) {
        ensureArenaMap(player, state);
        Optional<ParticipantRespawn> participant = participantRespawn(player);
        if (participant.isPresent()) {
            ParticipantRespawn respawn = participant.get();
            Optional<Integer> pendingTicks = state.pendingRespawnTicks(player.getUUID());
            if (pendingTicks.isPresent() && pendingTicks.get() > 0) {
                PendingRespawn pending = new PendingRespawn(pendingTicks.get(), respawn, null);
                PENDING_RESPAWNS.put(player.getUUID(), beginRespawnSpectating(player, state, pending, true));
                player.sendSystemMessage(message(ChatFormatting.YELLOW,
                        "War Day is active. Respawning in " + Math.max(1, (pendingTicks.get() + 19) / 20)
                                + " seconds."
                                + (respawn.attacker() ? " Choose a corner in the popup." : "")
                                + " Left/right click cycles living teammates."));
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

    private static void ensureInventoryHasAtLeast(
            ServerPlayer player,
            Item item,
            String teamMarker,
            int targetCount
    ) {
        int currentCount = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isTaggedMatchBlock(stack, item, teamMarker)) {
                currentCount += stack.getCount();
            }
        }
        if (currentCount >= targetCount) {
            return;
        }

        giveOrDrop(player, taggedMatchBlockStack(item, teamMarker, targetCount - currentCount));
    }

    private static ItemStack taggedMatchBlockStack(Item item, String teamMarker, int count) {
        ItemStack stack = new ItemStack(item, count);
        CompoundTag marker = new CompoundTag();
        marker.putString(MATCH_BLOCK_DATA_KEY, teamMarker);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        return stack;
    }

    private static boolean isTaggedMatchBlock(ItemStack stack, Item item, String teamMarker) {
        if (!stack.is(item)) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && teamMarker.equals(customData.copyTag().getString(MATCH_BLOCK_DATA_KEY));
    }

    private static boolean isHoldingTaggedMatchBlock(ServerPlayer player, ParticipantRespawn participant) {
        return isTaggedMatchBlock(
                player.getMainHandItem(), participant.matchBlock(), participant.teamMarker())
                || isTaggedMatchBlock(
                player.getOffhandItem(), participant.matchBlock(), participant.teamMarker());
    }

    private static boolean isConfiguredMatchBlockItem(ItemStack stack) {
        return stack.is(defenderMatchBlock()) || stack.is(attackerMatchBlock());
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

        if (isEnderPouch(event.getItemStack())) {
            blockPortableStorageUse(event, player);
            return;
        }

        if (isConfiguredMatchBlockItem(event.getItemStack())) {
            Optional<ParticipantRespawn> participant = participantRespawn(player);
            if (participant.isEmpty() || !isTaggedMatchBlock(
                    event.getItemStack(), participant.get().matchBlock(), participant.get().teamMarker())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                player.displayClientMessage(message(ChatFormatting.RED,
                        "Only Warday-issued blocks marked for your team can be placed during War Day."), true);
                return;
            }
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
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !isActiveWarDayLevel(level)
                || !isEnderPouch(event.getItemStack())) {
            return;
        }
        blockPortableStorageUse(event, player);
    }

    private static boolean isEnderPouch(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ENDER_POUCH_ID);
    }

    private static void blockPortableStorageUse(
            PlayerInteractEvent.RightClickBlock event,
            ServerPlayer player
    ) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(message(ChatFormatting.RED,
                "Ender pouches are disabled during War Day."), true);
    }

    private static void blockPortableStorageUse(
            PlayerInteractEvent.RightClickItem event,
            ServerPlayer player
    ) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(message(ChatFormatting.RED,
                "Ender pouches are disabled during War Day."), true);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !isActiveWarDayLevel(level)
                || !(event.getTarget() instanceof ItemFrame)
                && !entityExposesItemStorage(event.getTarget())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(message(ChatFormatting.RED,
                event.getTarget() instanceof ItemFrame
                        ? "Item frames cannot hold items during War Day."
                        : "Storage entities are disabled during War Day."), true);
    }

    @SubscribeEvent
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !isActiveWarDayLevel(level)
                || !(event.getTarget() instanceof ItemFrame)
                && !entityExposesItemStorage(event.getTarget())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        player.displayClientMessage(message(ChatFormatting.RED,
                event.getTarget() instanceof ItemFrame
                        ? "Item frames cannot hold items during War Day."
                        : "Storage entities are disabled during War Day."), true);
    }

    private static boolean entityExposesItemStorage(Entity entity) {
        return entity instanceof Container
                || entity.getCapability(Capabilities.ItemHandler.ENTITY) != null;
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

    private static void configureWorldBorder(ServerLevel level) {
        double size = WarDayAttackerTerrainPlan.arenaDiameter(
                WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt());
        level.getWorldBorder().setCenter(0.0D, 0.0D);
        level.getWorldBorder().setSize(size);
    }

    private static MapId createArenaMap(ServerLevel level) {
        MapId mapId = level.getFreeMapId();
        level.setMapData(mapId, createArenaMapData(level));
        return mapId;
    }

    private static MapItemSavedData createArenaMapData(ServerLevel level) {
        byte scale = WarDayAttackerTerrainPlan.arenaMapScale(
                WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt());
        return MapItemSavedData.createForClient(scale, false, level.dimension());
    }

    private static void ensureArenaMap(ServerPlayer player, WarDayState state) {
        Optional<ServerLevel> level = warDayLevel(player.getServer(), state);
        if (level.isEmpty() || state.arenaMapId().isEmpty()) {
            return;
        }

        MapId mapId = new MapId(state.arenaMapId().getAsInt());
        MapItemSavedData existing = level.get().getMapData(mapId);
        byte expectedScale = WarDayAttackerTerrainPlan.arenaMapScale(
                WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt());
        if (existing == null
                || existing.centerX != 0
                || existing.centerZ != 0
                || existing.scale != expectedScale
                || !existing.dimension.equals(level.get().dimension())) {
            level.get().setMapData(mapId, createArenaMapData(level.get()));
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.FILLED_MAP) && mapId.equals(stack.get(DataComponents.MAP_ID))) {
                return;
            }
        }

        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(DataComponents.MAP_ID, mapId);
        map.set(DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("War Day Arena Map").withStyle(ChatFormatting.AQUA));
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(MATCH_MAP_DATA_KEY, true);
        map.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        giveOrDrop(player, map);
    }

    private static void removeArenaMap(ServerPlayer player, int mapId) {
        if (mapId < 0) {
            return;
        }
        MapId issuedId = new MapId(mapId);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.FILLED_MAP) && issuedId.equals(stack.get(DataComponents.MAP_ID))) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private static boolean isInsideConfiguredMatchBorder(BlockPos pos) {
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        return WarDayAttackerTerrainPlan.insideArena(pos.getX(), pos.getZ(), halfSize);
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

        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        AABB bounds = new AABB(
                -halfSize,
                level.getMinBuildHeight(),
                -halfSize,
                halfSize,
                level.getMaxBuildHeight(),
                halfSize
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

    private static boolean isInMatchBounds(BlockPos pos) {
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        return WarDayAttackerTerrainPlan.insideArena(pos.getX(), pos.getZ(), halfSize);
    }

    private static void trackDiggingPenalty(ServerPlayer player, long gameTime) {
        int windowTicks = WarDayConfig.DIG_LIMIT_WINDOW_SECONDS.getAsInt() * 20;
        int threshold = WarDayConfig.DIG_LIMIT_BLOCKS.getAsInt();
        Deque<Long> digs = DIG_HISTORY.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        WarDayRapidBreakRule.TrackResult result = WarDayRapidBreakRule.recordBreak(
                digs,
                gameTime,
                windowTicks,
                threshold
        );
        if (!result.triggered()) {
            return;
        }

        int durationTicks = WarDayConfig.DIG_PENALTY_SECONDS.getAsInt() * 20;
        int strike = RAPID_BREAK_STRIKES.merge(player.getUUID(), 1, Integer::sum);
        int penaltyPercent = WarDayRapidBreakRule.cumulativePenaltyPercent(
                strike,
                WarDayConfig.DIG_PENALTY_PERCENT.getAsInt()
        );
        AttributeInstance blockBreakSpeed = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (blockBreakSpeed != null) {
            blockBreakSpeed.addOrUpdateTransientModifier(new AttributeModifier(
                    RAPID_BREAK_PENALTY_ID,
                    WarDayRapidBreakRule.blockBreakSpeedModifier(penaltyPercent),
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }

        RapidBreakPenalty existingPenalty = RAPID_BREAK_PENALTIES.get(player.getUUID());
        MobEffectInstance previousGlowing = existingPenalty == null
                ? Optional.ofNullable(player.getEffect(MobEffects.GLOWING)).map(MobEffectInstance::new).orElse(null)
                : existingPenalty.previousGlowing();
        long startGameTime = existingPenalty == null ? gameTime : existingPenalty.startGameTime();
        RAPID_BREAK_PENALTIES.put(
                player.getUUID(),
                new RapidBreakPenalty(startGameTime, gameTime + durationTicks, previousGlowing)
        );
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0, false, true, true));
        player.displayClientMessage(message(ChatFormatting.YELLOW,
                "Rapid-breaking strike " + Math.min(strike, 4) + ": -" + penaltyPercent
                        + "% block-breaking speed and Glowing for "
                        + WarDayConfig.DIG_PENALTY_SECONDS.getAsInt() + " seconds ("
                        + result.recentBreaks() + " breaks in " + WarDayConfig.DIG_LIMIT_WINDOW_SECONDS.getAsInt()
                        + " seconds)."), false);
    }

    private static void pruneDigHistory(long gameTime) {
        int windowTicks = WarDayConfig.DIG_LIMIT_WINDOW_SECONDS.getAsInt() * 20;
        Iterator<Map.Entry<UUID, Deque<Long>>> iterator = DIG_HISTORY.entrySet().iterator();
        while (iterator.hasNext()) {
            Deque<Long> breaks = iterator.next().getValue();
            WarDayRapidBreakRule.prune(breaks, gameTime, windowTicks);
            if (breaks.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private static void tickRapidBreakPenalties(MinecraftServer server, long gameTime) {
        Iterator<Map.Entry<UUID, RapidBreakPenalty>> iterator = RAPID_BREAK_PENALTIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RapidBreakPenalty> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            if (gameTime >= entry.getValue().endGameTime()) {
                finishRapidBreakPenalty(player, entry.getValue(), gameTime);
                iterator.remove();
            }
        }
    }

    private static void clearRapidBreakPenalty(ServerPlayer player, long gameTime) {
        RapidBreakPenalty penalty = RAPID_BREAK_PENALTIES.remove(player.getUUID());
        if (penalty == null) {
            removeRapidBreakModifier(player);
            return;
        }

        finishRapidBreakPenalty(player, penalty, gameTime);
    }

    private static void finishRapidBreakPenalty(ServerPlayer player, RapidBreakPenalty penalty, long gameTime) {
        removeRapidBreakModifier(player);

        player.removeEffect(MobEffects.GLOWING);
        MobEffectInstance previousGlowing = penalty.previousGlowing();
        if (previousGlowing == null) {
            return;
        }
        long elapsed = Math.max(0L, gameTime - penalty.startGameTime());
        int remaining = (int) Math.max(0L, previousGlowing.getDuration() - elapsed);
        if (remaining > 0) {
            player.addEffect(new MobEffectInstance(
                    previousGlowing.getEffect(),
                    remaining,
                    previousGlowing.getAmplifier(),
                    previousGlowing.isAmbient(),
                    previousGlowing.isVisible(),
                    previousGlowing.showIcon()
            ));
        }
    }

    private static void clearRapidBreakPenalties(MinecraftServer server) {
        for (UUID playerId : List.copyOf(RAPID_BREAK_PENALTIES.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                clearRapidBreakPenalty(player, player.serverLevel().getGameTime());
            }
        }
        RAPID_BREAK_PENALTIES.clear();
        RAPID_BREAK_STRIKES.clear();
    }

    private static void removeRapidBreakModifier(ServerPlayer player) {
        AttributeInstance blockBreakSpeed = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (blockBreakSpeed != null) {
            blockBreakSpeed.removeModifier(RAPID_BREAK_PENALTY_ID);
        }
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
            return Optional.of(new ParticipantRespawn(
                    warDayLevel.get(),
                    state.copiedNexusPos().get().offset(0, 1, 0),
                    defenderMatchBlock(),
                    DEFENDER_MATCH_BLOCK_MARKER,
                    false
            ));
        }
        if (attacker && !state.attackerSpawnPositions().isEmpty()) {
            List<BlockPos> corners = state.attackerSpawnPositions();
            int fallback = WarDayAttackerTerrainPlan.fallbackCornerIndex(
                    state.deathCount(playerId).orElse(1), corners.size());
            int selected = state.respawnCornerChoice(playerId)
                    .filter(index -> index >= 0 && index < corners.size())
                    .orElse(fallback);
            return Optional.of(new ParticipantRespawn(
                    warDayLevel.get(),
                    corners.get(selected),
                    attackerMatchBlock(),
                    ATTACKER_MATCH_BLOCK_MARKER,
                    true
            ));
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
        PendingRespawn maintained = maintainRespawnSpectating(player, state, pending, announce);
        if (pending.participant().attacker()) {
            openAttackerCornerMenu(player);
        }
        return maintained;
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
        player.closeContainer();
        stopRespawnSpectating(player);
        teleportPlayerSafely(player, respawn.level(), respawn.spawnPos());
        setPlayerSpawn(player, respawn.level(), findSafeSpawnNear(respawn.level(), respawn.spawnPos(), 8, 4).orElse(respawn.spawnPos()));
        player.setGameMode(GameType.SURVIVAL);
        ensureInventoryHasAtLeast(
                player, respawn.matchBlock(), respawn.teamMarker(), MATCH_BLOCK_TARGET_COUNT);
        ensureArenaMap(player, WarDayState.get(player.getServer()));
    }

    private static void openAttackerCornerMenu(ServerPlayer player) {
        WarDayState state = WarDayState.get(player.getServer());
        if (!state.isCombatActive()
                || !state.isAttackerParticipant(player.getUUID())
                || state.pendingRespawnTicks(player.getUUID()).orElse(0) <= 0
                || state.attackerSpawnPositions().size() != 4) {
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new CornerSelectionMenu(containerId, inventory, player),
                net.minecraft.network.chat.Component.literal("Choose Respawn Corner")
        ));
    }

    private static void chooseAttackerRespawnCorner(ServerPlayer player, int cornerIndex) {
        WarDayState state = WarDayState.get(player.getServer());
        List<BlockPos> corners = state.attackerSpawnPositions();
        if (!state.isCombatActive()
                || !state.isAttackerParticipant(player.getUUID())
                || state.pendingRespawnTicks(player.getUUID()).orElse(0) <= 0
                || cornerIndex < 0
                || cornerIndex >= corners.size()) {
            return;
        }
        state.setRespawnCornerChoice(player.getUUID(), cornerIndex);
        String cornerName = WarDayAttackerTerrainPlan.cornerSpawnTargets(
                WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt(), MATCH_BORDER_SPAWN_MARGIN).get(cornerIndex).name();
        player.sendSystemMessage(message(ChatFormatting.GREEN,
                cornerName + " selected for this respawn at " + formatPos(corners.get(cornerIndex)) + "."));
    }

    private Optional<PreparationContext> resolvePreparationContext(CommandSourceStack source) {
        Optional<ScanContext> scanContext = createScanContext(source);
        if (scanContext.isEmpty()) {
            return Optional.empty();
        }

        ScanContext context = scanContext.get();
        TeamValidation validation = validateTeamMarkers(
                context.teamA(), context.nexuses(), context.forwardMarkers(), context.chunkManager());
        if (!validation.passed()) {
            reportTeamValidation(source, validation);
            source.sendFailure(message(ChatFormatting.RED,
                    "Prepare requires the defender nexus and forward marker to pass validation."));
            return Optional.empty();
        }

        BaseArea defenderBase = BaseArea.from(validation, context.chunkManager());
        Optional<ResourceKey<Level>> targetDimension = warDayDimensionKey(source);
        if (targetDimension.isEmpty()) {
            return Optional.empty();
        }
        Optional<PlacementPlan> defenderPlan = PlacementPlan.defenderCentered(defenderBase);
        if (defenderPlan.isEmpty()) {
            int arenaSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt() * 2;
            source.sendFailure(message(ChatFormatting.RED,
                    "The rotated defender claim is too large to fit fully inside the nexus-centered "
                            + arenaSize + "x" + arenaSize + " arena."));
            return Optional.empty();
        }
        if (!reportAndCheckGuardrails(source, defenderBase)) {
            return Optional.empty();
        }

        AttackerArea attackerArea = AttackerArea.aroundClaim(defenderBase, defenderPlan.get());
        PlacementPlan attackerPlan = PlacementPlan.from(attackerArea);
        if (!reportAndCheckAttackerGuardrails(source, attackerArea)) {
            return Optional.empty();
        }

        return Optional.of(new PreparationContext(
                defenderBase,
                defenderPlan.get(),
                attackerArea,
                attackerPlan,
                context.teamB(),
                context.level(),
                targetDimension.get()
        ));
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
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        int radius = WarDayConfig.VALIDATION_RADIUS_BLOCKS.getAsInt();
        List<Team> scanTeams = new ArrayList<>();
        scanTeams.add(teamA.get());
        scanArea(level, center, radius, chunkManager, scanTeams, nexuses, forwardMarkers);

        return Optional.of(new ScanContext(level, center, radius, chunkManager, teamA.get(), teamB, nexuses, forwardMarkers));
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
            List<LocatedBlock> forwardMarkers
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
                        forwardMarkers
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
            List<LocatedBlock> forwardMarkers
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

    private static boolean reportAndCheckAttackerGuardrails(CommandSourceStack source, AttackerArea attackerArea) {
        int maxChunks = WarDayConfig.MAX_ATTACKER_TERRAIN_CHUNKS.getAsInt();
        boolean passed = attackerArea.cluster().size() <= maxChunks;
        ChatFormatting color = passed ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> message(color,
                "Claim-surrounding terrain guardrail: exact arena coverage intersects " + attackerArea.cluster().size()
                        + "/" + maxChunks + " local source chunks"), false);
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
        if (plan.automaticSpawnTarget().isPresent()) {
            int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
            source.sendSuccess(() -> message(ChatFormatting.GREEN,
                    "- claim-surrounding terrain coverage: complete " + (halfSize * 2) + "x" + (halfSize * 2)
                            + " arena from [" + -halfSize + ", " + -halfSize + "] to ["
                            + (halfSize - 1) + ", " + (halfSize - 1) + "]"), false);
            source.sendSuccess(() -> message(ChatFormatting.GREEN,
                    "- attacker spawns: four inset corner targets; safe surface positions are validated during confirm"), false);
        } else {
            source.sendSuccess(() -> message(ChatFormatting.GRAY,
                    "- target footprint: [" + plan.targetMinX() + ", " + plan.targetMinZ() + "] to ["
                            + plan.targetMaxX() + ", " + plan.targetMaxZ() + "]"), false);
        }
        int totalColumns = plan.cluster().size() * 256;
        int copyableColumns = plan.copyableColumnCount();
        source.sendSuccess(() -> message(copyableColumns == totalColumns ? ChatFormatting.GRAY : ChatFormatting.YELLOW,
                "- arena clipping: " + copyableColumns + "/" + totalColumns + " source columns inside the configured border"), false);
        ServerLevel sourceLevel = source.getServer().getLevel(plan.dimension());
        if (sourceLevel != null) {
            long unloadedChunks = plan.cluster().stream().filter(chunk -> !sourceLevel.hasChunk(chunk.x(), chunk.z())).count();
            source.sendSuccess(() -> message(unloadedChunks == 0 ? ChatFormatting.GRAY : ChatFormatting.YELLOW,
                    "- source loading: " + (plan.cluster().size() - unloadedChunks) + " loaded, " + unloadedChunks
                            + " will be loaded or generated during confirm"), false);
        }
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
            LevelChunk sourceChunk = sourceLevel.getChunk(chunk.x(), chunk.z());
            for (int sectionY = sourceLevel.getMinBuildHeight(); sectionY < sourceLevel.getMaxBuildHeight(); sectionY += 16) {
                if (sourceChunk.getSection(sourceLevel.getSectionIndex(sectionY)).hasOnlyAir()) {
                    continue;
                }
                int sectionMaxY = Math.min(sectionY + 16, sourceLevel.getMaxBuildHeight());
                for (int x = minX; x < minX + 16; x++) {
                    for (int z = minZ; z < minZ + 16; z++) {
                        for (int y = sectionY; y < sectionMaxY; y++) {
                        BlockPos sourcePos = new BlockPos(x, y, z);
                        BlockState sourceState = sourceChunk.getBlockState(sourcePos);
                        if (sourceState.isAir()) {
                            continue;
                        }

                        BlockPos targetPos = plan.targetPos(sourcePos);
                        if (!plan.containsTargetColumn(targetPos)) {
                            continue;
                        }
                        checked++;
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
            LevelChunk sourceChunk = sourceLevel.getChunk(chunk.x(), chunk.z());
            for (int sectionY = sourceLevel.getMinBuildHeight(); sectionY < sourceLevel.getMaxBuildHeight(); sectionY += 16) {
                if (sourceChunk.getSection(sourceLevel.getSectionIndex(sectionY)).hasOnlyAir()) {
                    continue;
                }
                int sectionMaxY = Math.min(sectionY + 16, sourceLevel.getMaxBuildHeight());
                for (int x = minX; x < minX + 16; x++) {
                    for (int z = minZ; z < minZ + 16; z++) {
                        for (int y = sectionY; y < sectionMaxY; y++) {
                        BlockPos sourcePos = new BlockPos(x, y, z);
                        BlockState sourceState = sourceChunk.getBlockState(sourcePos);
                        if (sourceState.isAir()) {
                            continue;
                        }

                        BlockPos targetPos = plan.targetPos(sourcePos);
                        if (!plan.containsTargetColumn(targetPos)) {
                            continue;
                        }
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

                            if (targetBlockEntity instanceof Container container
                                    && !isTeamSharedFortChest(targetBlockEntity)) {
                                container.clearContent();
                                targetBlockEntity.setChanged();
                                containersCleared++;
                            }
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
                        if (!plan.containsTargetColumn(targetPos)) {
                            continue;
                        }
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

    private static int wipeDestinationArena(ServerLevel targetLevel) {
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        int wiped = 0;
        int minChunk = Math.floorDiv(-halfSize, 16);
        int maxChunk = Math.floorDiv(halfSize - 1, 16);
        for (int chunkX = minChunk; chunkX <= maxChunk; chunkX++) {
            for (int chunkZ = minChunk; chunkZ <= maxChunk; chunkZ++) {
                LevelChunk targetChunk = targetLevel.getChunk(chunkX, chunkZ);
                int minX = Math.max(-halfSize, chunkX * 16);
                int maxX = Math.min(halfSize, chunkX * 16 + 16);
                int minZ = Math.max(-halfSize, chunkZ * 16);
                int maxZ = Math.min(halfSize, chunkZ * 16 + 16);
                for (int sectionY = targetLevel.getMinBuildHeight(); sectionY < targetLevel.getMaxBuildHeight(); sectionY += 16) {
                    if (targetChunk.getSection(targetLevel.getSectionIndex(sectionY)).hasOnlyAir()) {
                        continue;
                    }
                    int sectionMaxY = Math.min(sectionY + 16, targetLevel.getMaxBuildHeight());
                    for (int x = minX; x < maxX; x++) {
                        for (int z = minZ; z < maxZ; z++) {
                            for (int y = sectionY; y < sectionMaxY; y++) {
                                BlockPos targetPos = new BlockPos(x, y, z);
                                if (!targetChunk.getBlockState(targetPos).isAir()) {
                                    targetLevel.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                                    wiped++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return wiped;
    }

    private static int applyArenaBiome(ServerLevel targetLevel, Holder<Biome> arenaBiome) {
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        int minChunk = Math.floorDiv(-halfSize, 16);
        int maxChunk = Math.floorDiv(halfSize - 1, 16);
        List<LevelChunk> updatedChunks = new ArrayList<>();
        var sampler = targetLevel.getChunkSource().randomState().sampler();
        for (int chunkX = minChunk; chunkX <= maxChunk; chunkX++) {
            for (int chunkZ = minChunk; chunkZ <= maxChunk; chunkZ++) {
                LevelChunk targetChunk = targetLevel.getChunk(chunkX, chunkZ);
                targetChunk.fillBiomesFromNoise((quartX, quartY, quartZ, ignoredSampler) -> arenaBiome, sampler);
                targetChunk.setUnsaved(true);
                updatedChunks.add(targetChunk);
            }
        }

        for (int offset = 0; offset < updatedChunks.size(); offset += 16) {
            int end = Math.min(offset + 16, updatedChunks.size());
            ClientboundChunksBiomesPacket packet = ClientboundChunksBiomesPacket.forChunks(updatedChunks.subList(offset, end));
            for (ServerPlayer player : targetLevel.players()) {
                player.connection.send(packet);
            }
        }
        return updatedChunks.size();
    }

    private static String biomeName(Holder<Biome> biome) {
        return biome.unwrapKey()
                .map(key -> key.location().toString())
                .orElse(biome.getRegisteredName());
    }

    private static int clearDestinationArenaDecorativeEntities(ServerLevel targetLevel) {
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        AABB targetBounds = new AABB(
                -halfSize,
                targetLevel.getMinBuildHeight(),
                -halfSize,
                halfSize,
                targetLevel.getMaxBuildHeight(),
                halfSize
        );
        List<Entity> existingDecorations = targetLevel.getEntities(
                (Entity) null,
                targetBounds,
                WarDayCommands::isAllowedDecorativeEntity
        );
        existingDecorations.forEach(Entity::discard);
        return existingDecorations.size();
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
                        && plan.containsSourceBlock(entity.blockPosition())
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
                BlockPos targetPos = BlockPos.containing(
                        plan.targetX(entity.getX(), entity.getZ()),
                        targetY,
                        plan.targetZ(entity.getX(), entity.getZ())
                );
                return targetY >= targetLevel.getMinBuildHeight()
                        && targetY < targetLevel.getMaxBuildHeight()
                        && plan.containsTargetColumn(targetPos);
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
                        && plan.containsSourceBlock(hangingEntity.getPos())
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

    private record TeamValidation(
            Team team,
            List<LocatedBlock> nexuses,
            List<LocatedBlock> forwardMarkers,
            int clusterSize,
            boolean markerInCluster,
            boolean passed
    ) {
    }

    private record ScanContext(
            ServerLevel level,
            BlockPos center,
            int radius,
            ClaimedChunkManager chunkManager,
            Team teamA,
            Optional<Team> teamB,
            List<LocatedBlock> nexuses,
            List<LocatedBlock> forwardMarkers
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
            BaseArea baseArea,
            Set<ChunkDimPos> cluster,
            ClusterBounds bounds,
            ResourceKey<Level> dimension,
            BlockPos sourceAnchorPos,
            List<BlockPos> spawnTargetPositions,
            WarDayAttackerTerrainPlan.SourceWindow sourceWindow
    ) {
        private static AttackerArea aroundClaim(BaseArea defenderBase, PlacementPlan defenderPlan) {
            int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
            BlockPos sourceAnchor = defenderBase.nexus().pos();
            BlockPos targetAnchor = defenderPlan.targetAnchorPos();
            WarDayAttackerTerrainPlan.SourceWindow window = WarDayAttackerTerrainPlan.rotatedSourceWindow(
                    sourceAnchor.getX(), sourceAnchor.getZ(), targetAnchor.getX(), targetAnchor.getZ(),
                    halfSize, defenderPlan.rotationQuarterTurns());
            Set<ChunkDimPos> cluster = new HashSet<>();
            for (int chunkX = window.minChunkX(); chunkX <= window.maxChunkX(); chunkX++) {
                for (int chunkZ = window.minChunkZ(); chunkZ <= window.maxChunkZ(); chunkZ++) {
                    cluster.add(new ChunkDimPos(defenderBase.dimension(), new ChunkPos(chunkX, chunkZ)));
                }
            }
            Set<ChunkDimPos> immutableCluster = Set.copyOf(cluster);
            return new AttackerArea(
                    defenderBase,
                    immutableCluster,
                    ClusterBounds.from(immutableCluster),
                    defenderBase.dimension(),
                    sourceAnchor,
                    WarDayAttackerTerrainPlan.cornerSpawnTargets(halfSize, MATCH_BORDER_SPAWN_MARGIN).stream()
                            .map(corner -> new BlockPos(corner.x(), WarDayConfig.WAR_DAY_BASE_Y.getAsInt(), corner.z()))
                            .toList(),
                    window
            );
        }
    }

    private record PreparationContext(
            BaseArea defenderBase,
            PlacementPlan defenderPlan,
            AttackerArea attackerArea,
            PlacementPlan attackerPlan,
            Optional<Team> attackerTeam,
            ServerLevel sourceLevel,
            ResourceKey<Level> targetDimension
    ) {
    }

    private static List<ChunkPos> configuredArenaChunks() {
        int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
        int minChunk = Math.floorDiv(-halfSize, 16);
        int maxChunk = Math.floorDiv(halfSize - 1, 16);
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = minChunk; x <= maxChunk; x++) {
            for (int z = minChunk; z <= maxChunk; z++) {
                chunks.add(new ChunkPos(x, z));
            }
        }
        return List.copyOf(chunks);
    }

    private static final class ClearJob {
        private final CommandSourceStack source;
        private final ServerLevel level;
        private final List<ChunkPos> chunks = configuredArenaChunks();
        private ClearPhase phase = ClearPhase.LOADING_CHUNKS;
        private int chunkIndex;
        private ArenaBlockCursor arenaCursor;
        private int blocksWiped;
        private int entitiesDiscarded;
        private int consecutiveEmptyEntityPasses;
        private boolean finished;

        private ClearJob(CommandSourceStack source, ServerLevel level) {
            this.source = source;
            this.level = level;
        }

        private boolean tick(MinecraftServer server) {
            if (finished) {
                return true;
            }
            if (server != source.getServer()) {
                fail("Dimension clear stopped because the server instance changed.", null);
                return true;
            }
            if (WarDayState.get(server).isActive()) {
                fail("Dimension clear stopped because a War Day match became active.", null);
                return true;
            }

            long deadline = System.nanoTime() + PREPARATION_TICK_BUDGET_NANOS;
            int steps = 0;
            try {
                boolean continueThisTick = true;
                while (!finished && continueThisTick && steps < PREPARATION_MAX_STEPS_PER_TICK) {
                    continueThisTick = step();
                    steps++;
                    if ((steps & 63) == 0 && System.nanoTime() >= deadline) {
                        break;
                    }
                }
            } catch (RuntimeException exception) {
                fail("Dimension clear failed during " + phase.label + ". Check the server log.", exception);
            }
            return finished;
        }

        private boolean step() {
            return switch (phase) {
                case LOADING_CHUNKS -> stepLoadChunk();
                case CLEARING_ENTITIES -> stepInitialEntityClear();
                case CLEARING_BLOCKS -> stepClearBlock();
                case VERIFYING_ENTITIES -> stepVerifyEntities();
            };
        }

        private boolean stepLoadChunk() {
            if (chunkIndex >= chunks.size()) {
                phase = ClearPhase.CLEARING_ENTITIES;
                source.sendSuccess(() -> message(ChatFormatting.AQUA,
                        "All " + chunks.size() + " arena chunks are loaded. Removing non-player entities."), true);
                return true;
            }
            ChunkPos chunk = chunks.get(chunkIndex++);
            level.getChunk(chunk.x, chunk.z);
            return false;
        }

        private boolean stepInitialEntityClear() {
            entitiesDiscarded += clearAndDiscardWarDayEntities(level);
            arenaCursor = new ArenaBlockCursor(level, true);
            phase = ClearPhase.CLEARING_BLOCKS;
            source.sendSuccess(() -> message(ChatFormatting.AQUA,
                    "Initial entity pass complete. Wiping the full arena build height in bounded batches."), true);
            return false;
        }

        private boolean stepClearBlock() {
            if (!arenaCursor.advance()) {
                phase = ClearPhase.VERIFYING_ENTITIES;
                source.sendSuccess(() -> message(ChatFormatting.AQUA,
                        "Arena blocks are clear. Verifying across later ticks that no non-player entities remain."), true);
                return false;
            }
            BlockState state = arenaCursor.currentState();
            if (!state.isAir()) {
                level.setBlock(arenaCursor.currentPos(), Blocks.AIR.defaultBlockState(), 3);
                blocksWiped++;
            }
            return true;
        }

        private boolean stepVerifyEntities() {
            int discardedThisPass = clearAndDiscardWarDayEntities(level);
            entitiesDiscarded += discardedThisPass;
            consecutiveEmptyEntityPasses = WarDayEntityClearVerification.nextEmptyPasses(
                    consecutiveEmptyEntityPasses, discardedThisPass);
            if (!WarDayEntityClearVerification.isVerified(consecutiveEmptyEntityPasses)) {
                return false;
            }

            configureWorldBorder(level);
            WarDayState.get(source.getServer()).invalidatePrepared();
            source.sendSuccess(() -> message(ChatFormatting.GREEN,
                    "War Day dimension clear complete: wiped " + blocksWiped + " blocks and discarded "
                            + entitiesDiscarded + " non-player entities. The arena is no longer prepared."), true);
            WarDayMod.LOGGER.info(
                    "Manual War Day dimension clear wiped {} arena blocks and discarded {} non-player entities from {}",
                    blocksWiped,
                    entitiesDiscarded,
                    level.dimension().location()
            );
            finished = true;
            return false;
        }

        private void reportStatus(CommandSourceStack output) {
            String detail = switch (phase) {
                case LOADING_CHUNKS -> chunkIndex + "/" + chunks.size() + " chunks";
                case CLEARING_ENTITIES -> "initial entity pass";
                case CLEARING_BLOCKS -> arenaCursor == null ? "starting" : arenaCursor.progressText();
                case VERIFYING_ENTITIES -> consecutiveEmptyEntityPasses + "/"
                        + WarDayEntityClearVerification.REQUIRED_EMPTY_PASSES + " empty verification passes";
            };
            output.sendSuccess(() -> message(ChatFormatting.AQUA,
                    "War Day dimension clear: " + phase.label + " (" + detail + ")"), false);
        }

        private void fail(String text, RuntimeException exception) {
            if (exception != null) {
                WarDayMod.LOGGER.error("War Day dimension clear failed during {}", phase.label, exception);
            } else {
                WarDayMod.LOGGER.warn("War Day dimension clear stopped during {}: {}", phase.label, text);
            }
            source.sendFailure(message(ChatFormatting.RED, text));
            finished = true;
        }
    }

    private enum ClearPhase {
        LOADING_CHUNKS("loading arena chunks"),
        CLEARING_ENTITIES("clearing entities"),
        CLEARING_BLOCKS("clearing arena blocks"),
        VERIFYING_ENTITIES("verifying entity cleanup");

        private final String label;

        ClearPhase(String label) {
            this.label = label;
        }
    }

    private final class PreparationJob {
        private final CommandSourceStack source;
        private final PreparationContext context;
        private final ServerLevel sourceLevel;
        private final ServerLevel targetLevel;
        private final AttackerArea attackerArea;
        private final PlacementPlan attackerPlan;
        private final List<ChunkDimPos> sourceChunks;
        private final List<ChunkPos> targetChunks;
        private PreparationPhase phase = PreparationPhase.LOADING_SOURCE_CHUNKS;
        private int chunkIndex;
        private int cornerSpawnIndex;
        private CornerSpawnSearchCursor cornerSpawnCursor;
        private final List<BlockPos> safeAttackerSpawns = new ArrayList<>();
        private PlanBlockCursor blockCursor;
        private ArenaBlockCursor arenaCursor;
        private int destinationBlocksChecked;
        private int arenaBlocksWiped;
        private int arenaEntitiesCleared;
        private int consecutiveEmptyEntityPasses;
        private final MutableCopyResult terrainCopy = new MutableCopyResult();
        private final MutableCopyResult defenderCopy = new MutableCopyResult();
        private final List<LevelChunk> biomeChunks = new ArrayList<>();
        private boolean worldChanged;
        private boolean finished;

        private PreparationJob(CommandSourceStack source, PreparationContext context, ServerLevel targetLevel) {
            this.source = source;
            this.context = context;
            this.sourceLevel = context.sourceLevel();
            this.targetLevel = targetLevel;
            this.attackerArea = context.attackerArea();
            this.attackerPlan = context.attackerPlan();
            this.sourceChunks = sortedChunks(attackerArea.cluster());
            this.targetChunks = configuredArenaChunks();
            configureWorldBorder(targetLevel);
            announcePhase("Loading " + sourceChunks.size()
                    + " claim-surrounding source chunks one bounded step at a time.");
        }

        private boolean tick(MinecraftServer server) {
            if (finished) {
                return true;
            }
            if (server != source.getServer()) {
                fail("Preparation stopped because the server instance changed.", null);
                return true;
            }
            if (WarDayState.get(server).isActive()) {
                fail("Preparation stopped because a War Day match became active.", null);
                return true;
            }

            long deadline = System.nanoTime() + PREPARATION_TICK_BUDGET_NANOS;
            int steps = 0;
            try {
                boolean continueThisTick = true;
                while (!finished && continueThisTick && steps < PREPARATION_MAX_STEPS_PER_TICK) {
                    continueThisTick = step();
                    steps++;
                    if ((steps & 63) == 0 && System.nanoTime() >= deadline) {
                        break;
                    }
                }
            } catch (RuntimeException exception) {
                fail("Preparation failed during " + phase.label + ". Check the server log; the prepared flag remains cleared if world changes began.", exception);
            }
            return finished;
        }

        private boolean step() {
            return switch (phase) {
                case LOADING_SOURCE_CHUNKS -> stepLoadSourceChunk();
                case LOADING_TARGET_CHUNKS -> stepLoadTargetChunk();
                case VALIDATING_CORNER_SPAWNS -> stepValidateCornerSpawn();
                case CHECKING_DEFENDER_DESTINATION -> stepDestinationCheck(context.defenderPlan(), true);
                case CHECKING_TERRAIN_DESTINATION -> stepDestinationCheck(attackerPlan, false);
                case CLEARING_ARENA -> stepClearArena();
                case VERIFYING_CLEAR_ENTITIES -> stepVerifyClearEntities();
                case COPYING_TERRAIN -> stepCopy(attackerPlan, terrainCopy, true);
                case COPYING_DEFENDER -> stepCopy(context.defenderPlan(), defenderCopy, false);
                case APPLYING_BIOME -> stepApplyBiome();
                case FINALIZING -> stepFinalize();
            };
        }

        private boolean stepLoadSourceChunk() {
            if (chunkIndex >= sourceChunks.size()) {
                chunkIndex = 0;
                phase = PreparationPhase.LOADING_TARGET_CHUNKS;
                announcePhase("Claim-surrounding terrain loaded. Loading " + targetChunks.size() + " target arena chunks.");
                return true;
            }
            ChunkDimPos chunk = sourceChunks.get(chunkIndex++);
            sourceLevel.getChunk(chunk.x(), chunk.z());
            return false;
        }

        private boolean stepLoadTargetChunk() {
            if (chunkIndex >= targetChunks.size()) {
                chunkIndex = 0;
                phase = PreparationPhase.VALIDATING_CORNER_SPAWNS;
                announcePhase("Chunks loaded. Validating all four source-terrain corner landings before changing the arena.");
                return true;
            }
            ChunkPos chunk = targetChunks.get(chunkIndex++);
            targetLevel.getChunk(chunk.x, chunk.z);
            return false;
        }

        private boolean stepValidateCornerSpawn() {
            if (cornerSpawnIndex >= attackerArea.spawnTargetPositions().size()) {
                phase = PreparationPhase.CHECKING_DEFENDER_DESTINATION;
                blockCursor = new PlanBlockCursor(sourceLevel, context.defenderPlan(), true);
                destinationBlocksChecked = 0;
                announcePhase("Four corner landings validated. Checking the defender destination before clearing.");
                return true;
            }
            BlockPos target = attackerArea.spawnTargetPositions().get(cornerSpawnIndex);
            if (cornerSpawnCursor == null) {
                cornerSpawnCursor = new CornerSpawnSearchCursor(
                        sourceLevel, targetLevel, attackerPlan, context.defenderPlan(), target);
            }
            if (cornerSpawnCursor.advance()) {
                return true;
            }
            Optional<BlockPos> safe = cornerSpawnCursor.safeTarget();
            if (safe.isEmpty()) {
                fail("No safe two-block-tall attacker landing spot exists anywhere in the copied 256x256 arena "
                        + "for corner " + formatPos(target) + ". The previous prepared arena was not changed.", null);
                return false;
            }
            BlockPos safeTarget = safe.get();
            if (safeTarget.getY() <= targetLevel.getMinBuildHeight()
                    || safeTarget.getY() + 1 >= targetLevel.getMaxBuildHeight()) {
                fail("The copied source landing for corner " + formatPos(target)
                        + " would be outside the War Day dimension build height. The previous arena was not changed.", null);
                return false;
            }
            safeAttackerSpawns.add(safeTarget);
            cornerSpawnCursor = null;
            cornerSpawnIndex++;
            return false;
        }

        private boolean stepDestinationCheck(PlacementPlan plan, boolean defender) {
            if (!blockCursor.advance()) {
                reportCopyCheck(source, defender ? context.defenderBase().team().getName().getString() : "Claim-surrounding terrain",
                        new CopyCheck(true, destinationBlocksChecked, 0, BlockPos.ZERO));
                if (defender) {
                    phase = PreparationPhase.CHECKING_TERRAIN_DESTINATION;
                    blockCursor = new PlanBlockCursor(sourceLevel, attackerPlan, true);
                    destinationBlocksChecked = 0;
                    announcePhase("Defender destination is empty. Checking the surrounding-terrain destination.");
                } else {
                    beginArenaClearing();
                }
                return true;
            }

            BlockState sourceState = blockCursor.currentState();
            if (sourceState.isAir()) {
                return true;
            }
            BlockPos sourcePos = blockCursor.currentPos();
            if (!plan.containsSourceBlock(sourcePos)) {
                return true;
            }
            BlockPos targetPos = plan.targetPos(sourcePos);
            if (!plan.containsTargetColumn(targetPos)
                    || targetPos.getY() < targetLevel.getMinBuildHeight()
                    || targetPos.getY() >= targetLevel.getMaxBuildHeight()) {
                return true;
            }
            destinationBlocksChecked++;
            if (!targetLevel.getBlockState(targetPos).isAir()) {
                reportCopyCheck(source, defender ? context.defenderBase().team().getName().getString() : "Claim-surrounding terrain",
                        new CopyCheck(false, destinationBlocksChecked, 1, targetPos));
                if (defender) {
                    phase = PreparationPhase.CHECKING_TERRAIN_DESTINATION;
                    blockCursor = new PlanBlockCursor(sourceLevel, attackerPlan, true);
                    destinationBlocksChecked = 0;
                    announcePhase("Checking the surrounding-terrain destination.");
                } else {
                    beginArenaClearing();
                }
            }
            return true;
        }

        private void beginArenaClearing() {
            WarDayState.get(source.getServer()).invalidatePrepared();
            worldChanged = true;
            arenaEntitiesCleared = clearAndDiscardWarDayEntities(targetLevel);
            arenaCursor = new ArenaBlockCursor(targetLevel, true);
            phase = PreparationPhase.CLEARING_ARENA;
            announcePhase("Destination checks complete. Prepared state cleared; all non-player entities removed and the full 256x256 dimension is being wiped in batches.");
        }

        private boolean stepClearArena() {
            if (!arenaCursor.advance()) {
                phase = PreparationPhase.VERIFYING_CLEAR_ENTITIES;
                announcePhase("Arena blocks cleared. Verifying across later ticks that no stale non-player entities remain.");
                return false;
            }
            BlockState state = arenaCursor.currentState();
            if (!state.isAir()) {
                targetLevel.setBlock(arenaCursor.currentPos(), Blocks.AIR.defaultBlockState(), 3);
                arenaBlocksWiped++;
            }
            return true;
        }

        private boolean stepVerifyClearEntities() {
            int discardedThisPass = clearAndDiscardWarDayEntities(targetLevel);
            arenaEntitiesCleared += discardedThisPass;
            consecutiveEmptyEntityPasses = WarDayEntityClearVerification.nextEmptyPasses(
                    consecutiveEmptyEntityPasses, discardedThisPass);
            if (!WarDayEntityClearVerification.isVerified(consecutiveEmptyEntityPasses)) {
                return false;
            }

            source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                    "Cleared " + arenaBlocksWiped + " blocks and " + arenaEntitiesCleared
                            + " non-player entities from the full playable War Day dimension."), true);
            blockCursor = new PlanBlockCursor(sourceLevel, attackerPlan, true);
            phase = PreparationPhase.COPYING_TERRAIN;
            announcePhase("Arena verified empty. Copying surrounding terrain outside the claimed chunks in bounded batches.");
            return true;
        }

        private boolean stepCopy(PlacementPlan plan, MutableCopyResult result, boolean terrain) {
            if (!blockCursor.advance()) {
                EntityCopyResult entities = copyDecorativeEntities(sourceLevel, targetLevel, plan);
                if (terrain) {
                    source.sendSuccess(() -> message(ChatFormatting.GREEN,
                            "Copied claim-surrounding battlefield terrain: " + result.blocksCopied + " blocks, "
                                    + result.blockEntitiesCopied + " block entities, " + result.containersCleared
                                    + " containers cleared, " + entities.entitiesCopied() + " decorative entities, "
                                    + entities.itemFramesCleared() + " item frames cleared, " + entities.entitiesFailed()
                                    + " decorative entities failed validation/copy."), true);
                    blockCursor = new PlanBlockCursor(sourceLevel, context.defenderPlan(), true);
                    phase = PreparationPhase.COPYING_DEFENDER;
                    announcePhase("Surrounding terrain copied. Copying the reserved defender claim columns.");
                } else {
                    source.sendSuccess(() -> message(ChatFormatting.GREEN,
                            "Copied " + context.defenderBase().team().getName().getString() + ": " + result.blocksCopied
                                    + " blocks, " + result.blockEntitiesCopied + " block entities, "
                                    + result.containersCleared + " containers cleared, " + entities.entitiesCopied()
                                    + " decorative entities, " + entities.itemFramesCleared() + " item frames cleared, "
                                    + entities.entitiesFailed() + " decorative entities failed validation/copy."), true);
                    chunkIndex = 0;
                    phase = PreparationPhase.APPLYING_BIOME;
                    announcePhase("Defender claim copied. Transferring source biomes across target chunks.");
                }
                return true;
            }

            BlockState sourceState = blockCursor.currentState();
            if (sourceState.isAir()) {
                return true;
            }
            BlockPos sourcePos = blockCursor.currentPos();
            if (!plan.containsSourceBlock(sourcePos)) {
                return true;
            }
            BlockPos targetPos = plan.targetPos(sourcePos);
            if (!plan.containsTargetColumn(targetPos)
                    || targetPos.getY() < targetLevel.getMinBuildHeight()
                    || targetPos.getY() >= targetLevel.getMaxBuildHeight()) {
                return true;
            }
            targetLevel.setBlock(targetPos, plan.targetState(sourceState), 3);
            result.blocksCopied++;
            BlockEntity sourceBlockEntity = sourceLevel.getBlockEntity(sourcePos);
            BlockEntity targetBlockEntity = targetLevel.getBlockEntity(targetPos);
            if (sourceBlockEntity != null && targetBlockEntity != null) {
                CompoundTag tag = sourceBlockEntity.saveWithFullMetadata(sourceLevel.registryAccess());
                tag.putInt("x", targetPos.getX());
                tag.putInt("y", targetPos.getY());
                tag.putInt("z", targetPos.getZ());
                targetBlockEntity.loadWithComponents(tag, targetLevel.registryAccess());
                targetBlockEntity.setChanged();
                result.blockEntitiesCopied++;
                if (targetBlockEntity instanceof Container container
                        && !isTeamSharedFortChest(targetBlockEntity)) {
                    container.clearContent();
                    targetBlockEntity.setChanged();
                    result.containersCleared++;
                }
            }
            return true;
        }

        private boolean stepApplyBiome() {
            if (chunkIndex >= targetChunks.size()) {
                for (int offset = 0; offset < biomeChunks.size(); offset += 16) {
                    int end = Math.min(offset + 16, biomeChunks.size());
                    ClientboundChunksBiomesPacket packet = ClientboundChunksBiomesPacket.forChunks(biomeChunks.subList(offset, end));
                    for (ServerPlayer player : targetLevel.players()) {
                        player.connection.send(packet);
                    }
                }
                source.sendSuccess(() -> message(ChatFormatting.GREEN,
                        "Transferred the claim area's source biomes across " + biomeChunks.size()
                                + " arena-intersecting chunks."), true);
                phase = PreparationPhase.FINALIZING;
                announcePhase("Biome applied. Validating spawn and preparing entity templates.");
                return true;
            }
            ChunkPos pos = targetChunks.get(chunkIndex++);
            LevelChunk chunk = targetLevel.getChunk(pos.x, pos.z);
            chunk.fillBiomesFromNoise((quartX, quartY, quartZ, ignoredSampler) ->
                            sourceBiomeForTargetQuart(quartX, quartY, quartZ),
                    targetLevel.getChunkSource().randomState().sampler());
            chunk.setUnsaved(true);
            biomeChunks.add(chunk);
            return false;
        }

        private Holder<Biome> sourceBiomeForTargetQuart(int quartX, int quartY, int quartZ) {
            int targetY = Math.max(targetLevel.getMinBuildHeight(),
                    Math.min(targetLevel.getMaxBuildHeight() - 1, quartY * 4 + 2));
            BlockPos sourcePos = attackerPlan.sourcePos(new BlockPos(quartX * 4 + 2, targetY, quartZ * 4 + 2));
            int sourceY = Math.max(sourceLevel.getMinBuildHeight(),
                    Math.min(sourceLevel.getMaxBuildHeight() - 1, sourcePos.getY()));
            return sourceLevel.getBiome(new BlockPos(sourcePos.getX(), sourceY, sourcePos.getZ()));
        }

        private boolean stepFinalize() {
            for (BlockPos spawn : safeAttackerSpawns) {
                if (!isSafeSpawnPos(targetLevel, spawn)) {
                    fail("A previously validated corner landing was not safe after copying at " + formatPos(spawn)
                            + ". The prepared flag remains cleared; inspect that corner and rerun preparation.", null);
                    return false;
                }
            }

            int entityLimit = WarDayConfig.MAX_PREPARED_ENTITIES.getAsInt();
            Set<UUID> capturedEntityRoots = new HashSet<>();
            EntityTemplateCapture defenderCapture = capturePreparedEntityTemplates(
                    sourceLevel, targetLevel, context.defenderPlan(), entityLimit, capturedEntityRoots);
            List<CompoundTag> templates = new ArrayList<>(defenderCapture.templates());
            EntityTemplateCapture terrainCapture = capturePreparedEntityTemplates(
                    sourceLevel, targetLevel, attackerPlan,
                    Math.max(0, entityLimit - defenderCapture.entityCount()), capturedEntityRoots);
            templates.addAll(terrainCapture.templates());
            int preparedEntities = defenderCapture.entityCount() + terrainCapture.entityCount();
            int skippedEntities = defenderCapture.skippedCount() + terrainCapture.skippedCount();

            BlockPos copiedNexusPos = context.defenderPlan().targetPos(context.defenderBase().nexus().pos());
            buildNexusShell(targetLevel, copiedNexusPos);
            WarDayState.get(source.getServer()).markPrepared(
                    WarDayConfig.WAR_DAY_DIMENSION.get(),
                    context.defenderBase().team().getName().getString(),
                    context.attackerTeam().map(team -> team.getName().getString()).orElse(""),
                    copiedNexusPos,
                    safeAttackerSpawns,
                    templates
            );
            source.sendSuccess(() -> message(ChatFormatting.GREEN,
                    "Validated four attacker corner spawns: "
                            + safeAttackerSpawns.stream().map(WarDayCommands::formatPos).toList() + "."), true);
            source.sendSuccess(() -> message(ChatFormatting.GREEN,
                    "Built protected nexus shell at " + formatPos(copiedNexusPos) + "."), true);
            source.sendSuccess(() -> message(
                    skippedEntities == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW,
                    "Prepared " + preparedEntities + " non-player entities in " + templates.size()
                            + " entity groups for match-time cloning; skipped " + skippedEntities + "."), true);
            source.sendSuccess(() -> message(ChatFormatting.GREEN,
                    "War Day preparation complete. The nexus-centered 256x256 arena, source biomes, four attacker "
                            + "corner spawns, nexus, and entity templates are ready. Next: /warday start.")
                    .withStyle(ChatFormatting.BOLD), true);
            finished = true;
            return false;
        }

        private void reportStatus(CommandSourceStack output) {
            String detail = switch (phase) {
                case LOADING_SOURCE_CHUNKS, LOADING_TARGET_CHUNKS, APPLYING_BIOME -> chunkIndex + "/"
                        + (phase == PreparationPhase.LOADING_SOURCE_CHUNKS ? sourceChunks.size() : targetChunks.size()) + " chunks";
                case VALIDATING_CORNER_SPAWNS -> cornerSpawnIndex + "/4 corners"
                        + (cornerSpawnCursor == null ? "" : ", " + cornerSpawnCursor.progressText());
                case CHECKING_DEFENDER_DESTINATION, CHECKING_TERRAIN_DESTINATION,
                     COPYING_TERRAIN, COPYING_DEFENDER -> blockCursor == null
                        ? "starting" : blockCursor.progressText();
                case CLEARING_ARENA -> arenaCursor == null ? "starting" : arenaCursor.progressText();
                case VERIFYING_CLEAR_ENTITIES -> consecutiveEmptyEntityPasses + "/"
                        + WarDayEntityClearVerification.REQUIRED_EMPTY_PASSES + " empty verification passes";
                case FINALIZING -> "final checks";
            };
            output.sendSuccess(() -> message(ChatFormatting.AQUA,
                    "War Day preparation: " + phase.label + " (" + detail + ")"), false);
            if (worldChanged) {
                output.sendSuccess(() -> message(ChatFormatting.YELLOW,
                        "The arena is being rebuilt and is not startable until preparation completes."), false);
            }
        }

        private boolean worldChanged() {
            return worldChanged;
        }

        private void announcePhase(String text) {
            source.sendSuccess(() -> message(ChatFormatting.AQUA, text), true);
        }

        private void fail(String text, RuntimeException exception) {
            if (exception != null) {
                WarDayMod.LOGGER.error("War Day preparation failed during {}", phase.label, exception);
            } else {
                WarDayMod.LOGGER.warn("War Day preparation stopped during {}: {}", phase.label, text);
            }
            source.sendFailure(message(ChatFormatting.RED, text));
            finished = true;
        }

        private List<ChunkDimPos> sortedChunks(Set<ChunkDimPos> chunks) {
            return chunks.stream()
                    .sorted(Comparator.comparingInt(ChunkDimPos::x).thenComparingInt(ChunkDimPos::z))
                    .toList();
        }

    }

    private static final class CornerSpawnSearchCursor {
        private final ServerLevel sourceLevel;
        private final ServerLevel targetLevel;
        private final PlacementPlan arenaPlan;
        private final TargetFootprint claim;
        private final BlockPos preferred;
        private final int halfSize;
        private final int maximumDistance;
        private int distance;
        private int ringIndex;
        private BlockPos safeTarget;
        private boolean finished;

        private CornerSpawnSearchCursor(
                ServerLevel sourceLevel,
                ServerLevel targetLevel,
                PlacementPlan arenaPlan,
                PlacementPlan defenderPlan,
                BlockPos preferred
        ) {
            this.sourceLevel = sourceLevel;
            this.targetLevel = targetLevel;
            this.arenaPlan = arenaPlan;
            this.claim = defenderPlan.targetFootprint();
            this.preferred = preferred;
            this.halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
            this.maximumDistance = WarDayAttackerTerrainPlan.maximumArenaSearchRadius(
                    preferred.getX(), preferred.getZ(), halfSize);
        }

        private boolean advance() {
            if (finished) {
                return false;
            }
            if (distance > maximumDistance) {
                finished = true;
                return false;
            }

            int currentDistance = distance;
            WarDayAttackerTerrainPlan.ColumnOffset offset = WarDayAttackerTerrainPlan.nearestRingOffset(
                    currentDistance, ringIndex++);
            if (ringIndex >= WarDayAttackerTerrainPlan.nearestRingSize(currentDistance)) {
                distance++;
                ringIndex = 0;
            }

            int targetX = preferred.getX() + offset.x();
            int targetZ = preferred.getZ() + offset.z();
            if (!WarDayAttackerTerrainPlan.insideArena(targetX, targetZ, halfSize)
                    || (targetX >= claim.minX() && targetX <= claim.maxX()
                    && targetZ >= claim.minZ() && targetZ <= claim.maxZ())) {
                return true;
            }

            BlockPos sourceColumn = arenaPlan.sourcePos(new BlockPos(
                    targetX, arenaPlan.targetAnchorPos().getY(), targetZ));
            BlockPos sourceSurface = new BlockPos(
                    sourceColumn.getX(),
                    sourceLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            sourceColumn.getX(), sourceColumn.getZ()),
                    sourceColumn.getZ()
            );
            BlockPos candidateTarget = arenaPlan.targetPos(sourceSurface);
            if (arenaPlan.containsTargetColumn(candidateTarget)
                    && candidateTarget.getY() > targetLevel.getMinBuildHeight()
                    && candidateTarget.getY() + 1 < targetLevel.getMaxBuildHeight()
                    && isSafeSpawnPos(sourceLevel, sourceSurface)) {
                safeTarget = candidateTarget;
                finished = true;
                return false;
            }
            return true;
        }

        private Optional<BlockPos> safeTarget() {
            return Optional.ofNullable(safeTarget);
        }

        private String progressText() {
            return "search radius " + Math.min(distance, maximumDistance) + "/" + maximumDistance;
        }
    }

    private enum PreparationPhase {
        LOADING_SOURCE_CHUNKS("loading source chunks"),
        LOADING_TARGET_CHUNKS("loading target chunks"),
        VALIDATING_CORNER_SPAWNS("validating corner spawns"),
        CHECKING_DEFENDER_DESTINATION("checking defender destination"),
        CHECKING_TERRAIN_DESTINATION("checking terrain destination"),
        CLEARING_ARENA("clearing arena"),
        VERIFYING_CLEAR_ENTITIES("verifying entity cleanup"),
        COPYING_TERRAIN("copying terrain"),
        COPYING_DEFENDER("copying defender base"),
        APPLYING_BIOME("applying arena biome"),
        FINALIZING("finalizing");

        private final String label;

        PreparationPhase(String label) {
            this.label = label;
        }
    }

    private static final class MutableCopyResult {
        private int blocksCopied;
        private int blockEntitiesCopied;
        private int containersCleared;
    }

    private static final class PlanBlockCursor {
        private final ServerLevel level;
        private final List<ChunkDimPos> chunks;
        private final boolean skipAirSections;
        private final int minY;
        private final int maxY;
        private final long estimatedPositions;
        private int chunkIndex;
        private LevelChunk chunk;
        private int sectionY;
        private int localX;
        private int localZ;
        private int localY;
        private boolean sectionReady;
        private long visitedPositions;
        private BlockPos currentPos = BlockPos.ZERO;
        private BlockState currentState = Blocks.AIR.defaultBlockState();

        private PlanBlockCursor(ServerLevel level, PlacementPlan plan, boolean skipAirSections) {
            this.level = level;
            this.chunks = plan.cluster().stream()
                    .sorted(Comparator.comparingInt(ChunkDimPos::x).thenComparingInt(ChunkDimPos::z))
                    .toList();
            this.skipAirSections = skipAirSections;
            this.minY = level.getMinBuildHeight();
            this.maxY = level.getMaxBuildHeight();
            this.sectionY = minY;
            this.estimatedPositions = (long) chunks.size() * (maxY - minY) * 256L;
        }

        private boolean advance() {
            while (chunkIndex < chunks.size()) {
                if (chunk == null) {
                    ChunkDimPos pos = chunks.get(chunkIndex);
                    chunk = level.getChunk(pos.x(), pos.z());
                    sectionY = minY;
                    localX = 0;
                    localZ = 0;
                    localY = 0;
                    sectionReady = false;
                }
                if (sectionY >= maxY) {
                    chunk = null;
                    chunkIndex++;
                    continue;
                }
                if (!sectionReady) {
                    if (skipAirSections && chunk.getSection(level.getSectionIndex(sectionY)).hasOnlyAir()) {
                        visitedPositions += 4096L;
                        sectionY += 16;
                        continue;
                    }
                    sectionReady = true;
                }

                ChunkDimPos chunkPos = chunks.get(chunkIndex);
                int y = sectionY + localY;
                currentPos = new BlockPos(chunkPos.x() * 16 + localX, y, chunkPos.z() * 16 + localZ);
                currentState = chunk.getBlockState(currentPos);
                visitedPositions++;
                localY++;
                if (localY >= 16 || sectionY + localY >= maxY) {
                    localY = 0;
                    localZ++;
                    if (localZ >= 16) {
                        localZ = 0;
                        localX++;
                        if (localX >= 16) {
                            localX = 0;
                            sectionY += 16;
                            sectionReady = false;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        private BlockPos currentPos() {
            return currentPos;
        }

        private BlockState currentState() {
            return currentState;
        }

        private String progressText() {
            long percent = estimatedPositions == 0L ? 100L : Math.min(100L, visitedPositions * 100L / estimatedPositions);
            return percent + "%";
        }
    }

    private static final class ArenaBlockCursor {
        private final ServerLevel level;
        private final int halfSize;
        private final int minChunk;
        private final int maxChunk;
        private final int minY;
        private final int maxY;
        private final long estimatedPositions;
        private final boolean skipAirSections;
        private int chunkX;
        private int chunkZ;
        private LevelChunk chunk;
        private int sectionY;
        private int x;
        private int z;
        private int localY;
        private int minX;
        private int maxX;
        private int minZ;
        private int maxZ;
        private boolean sectionReady;
        private long visitedPositions;
        private BlockPos currentPos = BlockPos.ZERO;
        private BlockState currentState = Blocks.AIR.defaultBlockState();

        private ArenaBlockCursor(ServerLevel level, boolean skipAirSections) {
            this.level = level;
            this.skipAirSections = skipAirSections;
            this.halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
            this.minChunk = Math.floorDiv(-halfSize, 16);
            this.maxChunk = Math.floorDiv(halfSize - 1, 16);
            this.chunkX = minChunk;
            this.chunkZ = minChunk;
            this.minY = level.getMinBuildHeight();
            this.maxY = level.getMaxBuildHeight();
            this.sectionY = minY;
            this.estimatedPositions = (long) halfSize * 2L * halfSize * 2L * (maxY - minY);
        }

        private boolean advance() {
            while (chunkX <= maxChunk) {
                if (chunk == null) {
                    chunk = level.getChunk(chunkX, chunkZ);
                    minX = Math.max(-halfSize, chunkX * 16);
                    maxX = Math.min(halfSize, chunkX * 16 + 16);
                    minZ = Math.max(-halfSize, chunkZ * 16);
                    maxZ = Math.min(halfSize, chunkZ * 16 + 16);
                    x = minX;
                    z = minZ;
                    localY = 0;
                    sectionY = minY;
                    sectionReady = false;
                }
                if (sectionY >= maxY) {
                    chunk = null;
                    chunkZ++;
                    if (chunkZ > maxChunk) {
                        chunkZ = minChunk;
                        chunkX++;
                    }
                    continue;
                }
                if (!sectionReady) {
                    if (skipAirSections && chunk.getSection(level.getSectionIndex(sectionY)).hasOnlyAir()) {
                        visitedPositions += (long) (maxX - minX) * (maxZ - minZ) * 16L;
                        sectionY += 16;
                        continue;
                    }
                    sectionReady = true;
                }

                currentPos = new BlockPos(x, sectionY + localY, z);
                currentState = chunk.getBlockState(currentPos);
                visitedPositions++;
                localY++;
                if (localY >= 16 || sectionY + localY >= maxY) {
                    localY = 0;
                    z++;
                    if (z >= maxZ) {
                        z = minZ;
                        x++;
                        if (x >= maxX) {
                            x = minX;
                            sectionY += 16;
                            sectionReady = false;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        private BlockPos currentPos() {
            return currentPos;
        }

        private BlockState currentState() {
            return currentState;
        }

        private String progressText() {
            long percent = estimatedPositions == 0L ? 100L : Math.min(100L, visitedPositions * 100L / estimatedPositions);
            return percent + "%";
        }
    }

    private record PlacementPlan(
            String label,
            ResourceKey<Level> dimension,
            BlockPos anchorPos,
            Set<ChunkDimPos> cluster,
            ClusterBounds bounds,
            BlockPos targetAnchorPos,
            Set<ChunkDimPos> excludedSourceChunks,
            Optional<BaseArea> baseArea,
            Optional<BlockPos> automaticSpawnTarget
    ) {
        private static final Direction DEFENDER_TARGET_FACING = Direction.WEST;

        private static Optional<PlacementPlan> defenderCentered(BaseArea baseArea) {
            PlacementPlan centered = new PlacementPlan(
                    baseArea.team().getName().getString(),
                    baseArea.dimension(),
                    baseArea.nexus().pos(),
                    baseArea.cluster(),
                    baseArea.bounds(),
                    new BlockPos(0, WarDayConfig.WAR_DAY_BASE_Y.getAsInt(), 0),
                    Set.of(),
                    Optional.of(baseArea),
                    Optional.empty()
            );
            TargetFootprint footprint = centered.unclippedTargetFootprint();
            int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
            boolean fits = footprint.minX() >= -halfSize && footprint.maxX() < halfSize
                    && footprint.minZ() >= -halfSize && footprint.maxZ() < halfSize;
            return fits ? Optional.of(centered) : Optional.empty();
        }

        private static PlacementPlan from(BaseArea baseArea) {
            return defenderCentered(baseArea).orElseThrow();
        }

        private static PlacementPlan from(AttackerArea area) {
            return new PlacementPlan(
                    "Claim-surrounding battlefield terrain",
                    area.dimension(),
                    area.sourceAnchorPos(),
                    area.cluster(),
                    area.bounds(),
                    PlacementPlan.defenderCentered(area.baseArea()).orElseThrow().targetAnchorPos(),
                    area.baseArea().cluster(),
                    Optional.of(area.baseArea()),
                    Optional.of(area.spawnTargetPositions().getFirst())
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
            ChunkDimPos chunk = new ChunkDimPos(dimension, new ChunkPos(sourcePos));
            return cluster.contains(chunk) && !excludedSourceChunks.contains(chunk);
        }

        private boolean containsSourceBlock(BlockPos sourcePos) {
            return containsSourceChunk(sourcePos) && containsTargetColumn(targetPos(sourcePos));
        }

        private boolean containsTargetBlock(BlockPos targetPos) {
            return containsTargetColumn(targetPos) && containsSourceChunk(sourcePos(targetPos));
        }

        private boolean containsTargetColumn(BlockPos targetPos) {
            return WarDayAttackerTerrainPlan.insideArena(
                    targetPos.getX(), targetPos.getZ(), WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt());
        }

        private int copyableColumnCount() {
            int columns = 0;
            for (ChunkDimPos chunk : cluster) {
                int minX = chunk.x() * 16;
                int minZ = chunk.z() * 16;
                for (int x = minX; x < minX + 16; x++) {
                    for (int z = minZ; z < minZ + 16; z++) {
                        if (containsTargetColumn(targetPos(new BlockPos(x, anchorPos.getY(), z)))) {
                            BlockPos sourcePos = new BlockPos(x, anchorPos.getY(), z);
                            if (!containsSourceBlock(sourcePos)) {
                                continue;
                            }
                            columns++;
                        }
                    }
                }
            }
            return columns;
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

        private int rotationQuarterTurns() {
            return switch (rotation()) {
                case NONE -> 0;
                case CLOCKWISE_90 -> 1;
                case CLOCKWISE_180 -> 2;
                case COUNTERCLOCKWISE_90 -> 3;
            };
        }

        private TargetFootprint unclippedTargetFootprint() {
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

        private TargetFootprint targetFootprint() {
            TargetFootprint footprint = unclippedTargetFootprint();
            int halfSize = WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt();
            return new TargetFootprint(
                    Math.max(footprint.minX(), -halfSize),
                    Math.max(footprint.minZ(), -halfSize),
                    Math.min(footprint.maxX(), halfSize - 1),
                    Math.min(footprint.maxZ(), halfSize - 1)
            );
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

    private record RapidBreakPenalty(long startGameTime, long endGameTime, MobEffectInstance previousGlowing) {
    }

    private static final class CornerSelectionMenu extends AbstractContainerMenu {
        private static final int[] CORNER_SLOTS = {1, 3, 5, 7};
        private static final Item[] CORNER_ICONS = {
                Items.BLUE_WOOL, Items.GREEN_WOOL, Items.YELLOW_WOOL, Items.RED_WOOL
        };
        private final ServerPlayer selectingPlayer;
        private final SimpleContainer choices = new SimpleContainer(9);

        private CornerSelectionMenu(int containerId, Inventory inventory, ServerPlayer selectingPlayer) {
            super(MenuType.GENERIC_9x1, containerId);
            this.selectingPlayer = selectingPlayer;
            List<WarDayAttackerTerrainPlan.CornerSpawn> corners = WarDayAttackerTerrainPlan.cornerSpawnTargets(
                    WarDayConfig.MAP_HALF_SIZE_BLOCKS.getAsInt(), MATCH_BORDER_SPAWN_MARGIN);
            for (int index = 0; index < CORNER_SLOTS.length; index++) {
                ItemStack icon = new ItemStack(CORNER_ICONS[index]);
                icon.set(DataComponents.CUSTOM_NAME,
                        net.minecraft.network.chat.Component.literal(corners.get(index).name()));
                choices.setItem(CORNER_SLOTS[index], icon);
            }
            for (int slot = 0; slot < 9; slot++) {
                addSlot(new Slot(choices, slot, 8 + slot * 18, 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 50 + row * 18));
                }
            }
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column, 8 + column * 18, 108));
            }
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (player == selectingPlayer && slotId >= 0 && slotId < 9) {
                for (int index = 0; index < CORNER_SLOTS.length; index++) {
                    if (slotId == CORNER_SLOTS[index]) {
                        chooseAttackerRespawnCorner(selectingPlayer, index);
                        selectingPlayer.closeContainer();
                        return;
                    }
                }
                return;
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            WarDayState state = WarDayState.get(selectingPlayer.getServer());
            return player == selectingPlayer
                    && state.isCombatActive()
                    && state.pendingRespawnTicks(selectingPlayer.getUUID()).orElse(0) > 0;
        }
    }

    private record ParticipantRespawn(
            ServerLevel level,
            BlockPos spawnPos,
            Item matchBlock,
            String teamMarker,
            boolean attacker
    ) {
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
