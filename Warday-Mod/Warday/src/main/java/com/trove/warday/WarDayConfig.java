package com.trove.warday;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WarDayConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> TEAM_A_NAME;
    public static final ModConfigSpec.ConfigValue<String> TEAM_B_NAME;
    public static final ModConfigSpec.IntValue BASE_SPACING_BLOCKS;
    public static final ModConfigSpec.IntValue VALIDATION_RADIUS_BLOCKS;
    public static final ModConfigSpec.IntValue MAX_BASE_CHUNKS;
    public static final ModConfigSpec.IntValue MAX_BASE_FOOTPRINT_BLOCKS;
    public static final ModConfigSpec.IntValue WAR_DAY_BASE_Y;
    public static final ModConfigSpec.IntValue ATTACKER_TERRAIN_RADIUS_CHUNKS;
    public static final ModConfigSpec.IntValue MAX_ATTACKER_TERRAIN_CHUNKS;
    public static final ModConfigSpec.IntValue BIOME_TERRAIN_SEARCH_RADIUS_BLOCKS;
    public static final ModConfigSpec.IntValue BIOME_TERRAIN_SEARCH_ATTEMPTS;
    public static final ModConfigSpec.IntValue MAX_PREPARED_ENTITIES;
    public static final ModConfigSpec.IntValue RESPAWN_DELAY_SECONDS;
    public static final ModConfigSpec.IntValue MATCH_DURATION_SECONDS;
    public static final ModConfigSpec.IntValue VICTORY_FANFARE_SECONDS;
    public static final ModConfigSpec.IntValue MAP_HALF_SIZE_BLOCKS;
    public static final ModConfigSpec.IntValue DIG_LIMIT_BLOCKS;
    public static final ModConfigSpec.IntValue DIG_LIMIT_WINDOW_SECONDS;
    public static final ModConfigSpec.IntValue DIG_PENALTY_SECONDS;
    public static final ModConfigSpec.IntValue DIG_PENALTY_PERCENT;
    public static final ModConfigSpec.ConfigValue<String> DEFENDER_MATCH_BLOCK;
    public static final ModConfigSpec.ConfigValue<String> ATTACKER_MATCH_BLOCK;
    public static final ModConfigSpec.ConfigValue<String> WAR_DAY_DIMENSION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("validation");
        TEAM_A_NAME = builder
                .comment("Display/config name for the first War Day team.")
                .define("teamAName", "Team A");
        TEAM_B_NAME = builder
                .comment("Display/config name for the second War Day team.")
                .define("teamBName", "Team B");
        BASE_SPACING_BLOCKS = builder
                .comment("Legacy attacker-spacing setting retained for config compatibility. Attacker placement now uses the corner opposite the defender.")
                .defineInRange("baseSpacingBlocks", 100, 16, 10000);
        VALIDATION_RADIUS_BLOCKS = builder
                .comment("Temporary scan radius around the admin running /warday validate until FTB claim scanning is wired.")
                .defineInRange("validationRadiusBlocks", 512, 32, 4096);
        MAX_BASE_CHUNKS = builder
                .comment("Maximum connected claimed chunks allowed for one team's copied base area.")
                .defineInRange("maxBaseChunks", 64, 1, 1024);
        MAX_BASE_FOOTPRINT_BLOCKS = builder
                .comment("Maximum width or depth in blocks allowed for one team's copied base footprint.")
                .defineInRange("maxBaseFootprintBlocks", 256, 16, 4096);
        builder.pop();

        builder.push("prepare");
        WAR_DAY_DIMENSION = builder
                .comment("Target dimension id for the future War Day instance dimension.")
                .define("warDayDimension", "warday:war_day");
        WAR_DAY_BASE_Y = builder
                .comment("Target Y level for the copied defender nexus and generated-terrain source anchor during /warday prepare.")
                .defineInRange("warDayBaseY", 80, -64, 320);
        ATTACKER_TERRAIN_RADIUS_CHUNKS = builder
                .comment("Legacy terrain-radius setting retained for config compatibility. Prepare now copies the fixed nexus-centered arena window.")
                .defineInRange("attackerTerrainRadiusChunks", 8, 0, 12);
        MAX_ATTACKER_TERRAIN_CHUNKS = builder
                .comment("Safety limit for chunks intersecting the nexus-centered 256x256 source window. An unaligned nexus needs at most 289 chunks.")
                .defineInRange("maxAttackerTerrainChunks", 289, 1, 625);
        BIOME_TERRAIN_SEARCH_RADIUS_BLOCKS = builder
                .comment("Legacy remote-biome search setting retained for config compatibility; no longer used by preparation.")
                .defineInRange("biomeTerrainSearchRadiusBlocks", 8192, 512, 65536);
        BIOME_TERRAIN_SEARCH_ATTEMPTS = builder
                .comment("Legacy remote-biome search setting retained for config compatibility; no longer used by preparation.")
                .defineInRange("biomeTerrainSearchAttempts", 24, 1, 128);
        MAX_PREPARED_ENTITIES = builder
                .comment("Maximum persistent non-player entities captured from both copied areas and recreated for each match. Set to 0 to disable entity templates.")
                .defineInRange("maxPreparedEntities", 256, 0, 4096);
        RESPAWN_DELAY_SECONDS = builder
                .comment("Legacy fallback respawn delay in seconds. Scaling death penalties are used during active matches.")
                .defineInRange("respawnDelaySeconds", 10, 0, 300);
        builder.pop();

        builder.push("match");
        MATCH_DURATION_SECONDS = builder
                .comment("Maximum active War Day attack length in seconds.")
                .defineInRange("matchDurationSeconds", 900, 60, 7200);
        VICTORY_FANFARE_SECONDS = builder
                .comment("Seconds to celebrate the winning team after combat ends before restoring players. Set to 0 to skip the delay.")
                .defineInRange("victoryFanfareSeconds", 30, 0, 300);
        MAP_HALF_SIZE_BLOCKS = builder
                .comment("Fixed half-size of the nexus-centered War Day arena. 128 gives the required 256x256 map.")
                .defineInRange("mapHalfSizeBlocks", 128, 128, 128);
        DEFENDER_MATCH_BLOCK = builder
                .comment("Only placeable block for defender-team participants during War Day.")
                .define("defenderMatchBlock", "minecraft:blue_wool");
        ATTACKER_MATCH_BLOCK = builder
                .comment("Only placeable block for attacker-team participants during War Day.")
                .define("attackerMatchBlock", "minecraft:red_wool");
        DIG_LIMIT_BLOCKS = builder
                .comment("Successful block breaks inside the rolling window that trigger the rapid-breaking penalty.")
                .defineInRange("digLimitBlocks", 15, 1, 512);
        DIG_LIMIT_WINDOW_SECONDS = builder
                .comment("Rolling window in seconds used for the digging penalty.")
                .defineInRange("digLimitWindowSeconds", 30, 1, 300);
        DIG_PENALTY_SECONDS = builder
                .comment("Seconds of glowing and mining slowdown applied when a player exceeds the dig limit.")
                .defineInRange("digPenaltySeconds", 60, 1, 600);
        DIG_PENALTY_PERCENT = builder
                .comment("First rapid-breaking strike percentage. Later strikes add 20, 10, then 5 percentage points and cap at that fourth tier.")
                .defineInRange("digPenaltyPercent", 25, 1, 100);
        builder.pop();

        SPEC = builder.build();
    }

    private WarDayConfig() {
    }
}
