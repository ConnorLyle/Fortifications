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
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class WarDayCommands {
    private static final int VALIDATE_PERMISSION_LEVEL = 2;
    private static final SuggestionProvider<CommandSourceStack> TEAM_NAME_SUGGESTIONS = WarDayCommands::suggestTeamNames;

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("warday")
                .requires(source -> source.hasPermission(VALIDATE_PERMISSION_LEVEL))
                .then(Commands.literal("validate")
                        .executes(context -> validate(context.getSource())))
                .then(Commands.literal("scan")
                        .executes(context -> scan(context.getSource())))
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
                "Base spacing target: " + WarDayConfig.BASE_SPACING_BLOCKS.getAsInt() + " blocks"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "Scanning " + context.radius() + " blocks around " + context.level().dimension().location() + " " + formatPos(context.center())), false);

        reportBlocks(source, "nexus", context.nexuses());
        reportBlocks(source, "forward marker", context.forwardMarkers());

        TeamValidation teamAValidation = validateTeamMarkers(context.teamA(), context.nexuses(), context.forwardMarkers(), context.chunkManager());
        TeamValidation teamBValidation = validateTeamMarkers(context.teamB(), context.nexuses(), context.forwardMarkers(), context.chunkManager());
        reportTeamValidation(source, teamAValidation);
        reportTeamValidation(source, teamBValidation);

        boolean passed = teamAValidation.passed() && teamBValidation.passed();
        if (passed) {
            source.sendSuccess(() -> message(ChatFormatting.GREEN,
                    "Validation passed: each configured team has one nexus and one forward marker in the same connected claimed chunk cluster."), true);
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
                "War Day scan for " + context.teamA().getName().getString() + " vs " + context.teamB().getName().getString()), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "Scanning " + context.radius() + " blocks around " + context.level().dimension().location() + " " + formatPos(context.center())), false);

        TeamValidation teamAValidation = validateTeamMarkers(context.teamA(), context.nexuses(), context.forwardMarkers(), context.chunkManager());
        TeamValidation teamBValidation = validateTeamMarkers(context.teamB(), context.nexuses(), context.forwardMarkers(), context.chunkManager());
        reportTeamScan(source, teamAValidation, context.chunkManager());
        reportTeamScan(source, teamBValidation, context.chunkManager());
        reportGuardrails(source, teamAValidation, context.chunkManager());
        reportGuardrails(source, teamBValidation, context.chunkManager());

        boolean passed = teamAValidation.passed() && teamBValidation.passed();
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
        source.sendSuccess(() -> message(ChatFormatting.GRAY, "Base spacing: " + WarDayConfig.BASE_SPACING_BLOCKS.getAsInt() + " blocks"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY, "Target Y: " + WarDayConfig.WAR_DAY_BASE_Y.getAsInt()), false);

        int halfSpacing = WarDayConfig.BASE_SPACING_BLOCKS.getAsInt() / 2;
        PlacementPlan teamAPlan = PlacementPlan.from(bases.teamA(), new BlockPos(-halfSpacing, WarDayConfig.WAR_DAY_BASE_Y.getAsInt(), 0));
        PlacementPlan teamBPlan = PlacementPlan.from(bases.teamB(), new BlockPos(halfSpacing, WarDayConfig.WAR_DAY_BASE_Y.getAsInt(), 0));
        reportPlacementPlan(source, teamAPlan);
        reportPlacementPlan(source, teamBPlan);

        source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                "Next implementation step will copy these source chunk clusters into the target placements."), false);
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
        int halfSpacing = WarDayConfig.BASE_SPACING_BLOCKS.getAsInt() / 2;
        PlacementPlan teamAPlan = PlacementPlan.from(bases.teamA(), new BlockPos(-halfSpacing, WarDayConfig.WAR_DAY_BASE_Y.getAsInt(), 0));
        PlacementPlan teamBPlan = PlacementPlan.from(bases.teamB(), new BlockPos(halfSpacing, WarDayConfig.WAR_DAY_BASE_Y.getAsInt(), 0));

        CopyCheck teamACheck = checkDestinationEmpty(source.getServer().getLevel(bases.teamA().dimension()), targetLevel, teamAPlan);
        CopyCheck teamBCheck = checkDestinationEmpty(source.getServer().getLevel(bases.teamB().dimension()), targetLevel, teamBPlan);
        reportCopyCheck(source, bases.teamA().team().getName().getString(), teamACheck);
        reportCopyCheck(source, bases.teamB().team().getName().getString(), teamBCheck);

        if (!teamACheck.passed() || !teamBCheck.passed()) {
            source.sendFailure(message(ChatFormatting.RED, "Prepare confirm refused to overwrite destination blocks."));
            return 0;
        }

        CopyResult teamAResult = copyBase(source.getServer().getLevel(bases.teamA().dimension()), targetLevel, teamAPlan);
        CopyResult teamBResult = copyBase(source.getServer().getLevel(bases.teamB().dimension()), targetLevel, teamBPlan);
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "Copied " + bases.teamA().team().getName().getString() + ": " + teamAResult.blocksCopied()
                        + " blocks, " + teamAResult.blockEntitiesCopied() + " block entities, "
                        + teamAResult.containersCleared() + " containers cleared."), true);
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                "Copied " + bases.teamB().team().getName().getString() + ": " + teamBResult.blocksCopied()
                        + " blocks, " + teamBResult.blockEntitiesCopied() + " block entities, "
                        + teamBResult.containersCleared() + " containers cleared."), true);
        source.sendSuccess(() -> message(ChatFormatting.YELLOW,
                "Copied bases are not rotated yet. Nexus win tracking will be added in a later pass."), false);
        return 1;
    }

    private Optional<ResolvedBases> resolveBases(CommandSourceStack source) {
        Optional<ScanContext> scanContext = createScanContext(source);
        if (scanContext.isEmpty()) {
            return Optional.empty();
        }

        ScanContext context = scanContext.get();
        TeamValidation teamAValidation = validateTeamMarkers(context.teamA(), context.nexuses(), context.forwardMarkers(), context.chunkManager());
        TeamValidation teamBValidation = validateTeamMarkers(context.teamB(), context.nexuses(), context.forwardMarkers(), context.chunkManager());

        if (!teamAValidation.passed() || !teamBValidation.passed()) {
            reportTeamValidation(source, teamAValidation);
            reportTeamValidation(source, teamBValidation);
            source.sendFailure(message(ChatFormatting.RED, "Prepare preview requires both teams to pass marker validation."));
            return Optional.empty();
        }

        BaseArea teamA = BaseArea.from(teamAValidation, context.chunkManager());
        BaseArea teamB = BaseArea.from(teamBValidation, context.chunkManager());
        boolean sameDimension = teamA.dimension().equals(teamB.dimension());
        boolean teamAWithinLimits = reportAndCheckGuardrails(source, teamA);
        boolean teamBWithinLimits = reportAndCheckGuardrails(source, teamB);

        if (!sameDimension) {
            source.sendFailure(message(ChatFormatting.RED, "Both source bases must be in the same dimension for the first prepare implementation."));
            source.sendFailure(message(ChatFormatting.RED,
                    "Team 1 dimension=" + teamA.dimension().location() + ", Team 2 dimension=" + teamB.dimension().location()));
        }

        if (!sameDimension || !teamAWithinLimits || !teamBWithinLimits) {
            return Optional.empty();
        }

        return Optional.of(new ResolvedBases(teamA, teamB));
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

        if (teamA.isEmpty() || teamB.isEmpty()) {
            source.sendFailure(message(ChatFormatting.RED, "Could not find both configured FTB teams."));
            source.sendFailure(message(ChatFormatting.RED, "Team A: " + WarDayConfig.TEAM_A_NAME.get() + " found=" + teamA.isPresent()));
            source.sendFailure(message(ChatFormatting.RED, "Team B: " + WarDayConfig.TEAM_B_NAME.get() + " found=" + teamB.isPresent()));
            return Optional.empty();
        }

        List<LocatedBlock> nexuses = new ArrayList<>();
        List<LocatedBlock> forwardMarkers = new ArrayList<>();
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        int radius = WarDayConfig.VALIDATION_RADIUS_BLOCKS.getAsInt();
        scanArea(level, center, radius, chunkManager, nexuses, forwardMarkers);

        return Optional.of(new ScanContext(level, center, radius, chunkManager, teamA.get(), teamB.get(), nexuses, forwardMarkers));
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
            List<LocatedBlock> forwardMarkers
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
                String facing = state.getValue(ForwardMarkerBlock.FACING).getName();
                forwardMarkers.add(new LocatedBlock(level.dimension(), pos.immutable(), facing, getClaimOwner(chunkManager, level, pos)));
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

    private static void reportPlacementPlan(CommandSourceStack source, PlacementPlan plan) {
        source.sendSuccess(() -> message(ChatFormatting.GREEN,
                plan.baseArea().team().getName().getString() + " placement"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- source: " + plan.baseArea().dimension().location()
                        + " chunks [" + plan.baseArea().bounds().minX() + ", " + plan.baseArea().bounds().minZ() + "] to ["
                        + plan.baseArea().bounds().maxX() + ", " + plan.baseArea().bounds().maxZ() + "]"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- target origin: " + formatPos(plan.targetOrigin())), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- target footprint: [" + plan.targetMinX() + ", " + plan.targetMinZ() + "] to ["
                        + plan.targetMaxX() + ", " + plan.targetMaxZ() + "]"), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- nexus offset from source min: " + formatPos(plan.nexusOffset())), false);
        source.sendSuccess(() -> message(ChatFormatting.GRAY,
                "- forward marker offset from source min: " + formatPos(plan.forwardMarkerOffset())
                        + " facing " + plan.baseArea().forwardMarker().facing()), false);
    }

    private static CopyCheck checkDestinationEmpty(ServerLevel sourceLevel, ServerLevel targetLevel, PlacementPlan plan) {
        if (sourceLevel == null) {
            return new CopyCheck(false, 0, 0, BlockPos.ZERO);
        }

        int checked = 0;
        for (ChunkDimPos chunk : plan.baseArea().cluster()) {
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
                        BlockPos targetPos = plan.targetPos(sourcePos, sourceLevel.getMinBuildHeight());
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

        for (ChunkDimPos chunk : plan.baseArea().cluster()) {
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

                        BlockPos targetPos = plan.targetPos(sourcePos, sourceLevel.getMinBuildHeight());
                        targetLevel.setBlock(targetPos, sourceState, 3);
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

    private static net.minecraft.network.chat.MutableComponent message(ChatFormatting color, String text) {
        return net.minecraft.network.chat.Component.literal(text).withStyle(color);
    }

    private static String formatPos(BlockPos pos) {
        return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    private record LocatedBlock(ResourceKey<Level> dimension, BlockPos pos, String facing, Optional<Team> owner) {
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
            Team teamB,
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

    private record ResolvedBases(BaseArea teamA, BaseArea teamB) {
    }

    private record PlacementPlan(BaseArea baseArea, BlockPos targetOrigin) {
        private static PlacementPlan from(BaseArea baseArea, BlockPos targetOrigin) {
            return new PlacementPlan(baseArea, targetOrigin);
        }

        private int targetMinX() {
            return targetOrigin.getX();
        }

        private int targetMinZ() {
            return targetOrigin.getZ();
        }

        private int targetMaxX() {
            return targetOrigin.getX() + baseArea.bounds().blockWidth() - 1;
        }

        private int targetMaxZ() {
            return targetOrigin.getZ() + baseArea.bounds().blockDepth() - 1;
        }

        private BlockPos nexusOffset() {
            return offsetFromSourceMin(baseArea.nexus().pos());
        }

        private BlockPos forwardMarkerOffset() {
            return offsetFromSourceMin(baseArea.forwardMarker().pos());
        }

        private BlockPos offsetFromSourceMin(BlockPos sourcePos) {
            return new BlockPos(
                    sourcePos.getX() - baseArea.bounds().minBlockX(),
                    sourcePos.getY(),
                    sourcePos.getZ() - baseArea.bounds().minBlockZ()
            );
        }

        private BlockPos targetPos(BlockPos sourcePos, int sourceMinY) {
            return new BlockPos(
                    targetOrigin.getX() + sourcePos.getX() - baseArea.bounds().minBlockX(),
                    targetOrigin.getY() + sourcePos.getY() - sourceMinY,
                    targetOrigin.getZ() + sourcePos.getZ() - baseArea.bounds().minBlockZ()
            );
        }
    }

    private record CopyCheck(boolean passed, int sourceBlocksChecked, int conflicts, BlockPos firstConflict) {
    }

    private record CopyResult(int blocksCopied, int blockEntitiesCopied, int containersCleared) {
    }
}
