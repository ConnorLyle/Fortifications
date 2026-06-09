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
    public static final ModConfigSpec.IntValue RESPAWN_DELAY_SECONDS;
    public static final ModConfigSpec.IntValue MATCH_DURATION_SECONDS;
    public static final ModConfigSpec.IntValue MAP_HALF_SIZE_BLOCKS;
    public static final ModConfigSpec.IntValue DIG_LIMIT_BLOCKS;
    public static final ModConfigSpec.IntValue DIG_LIMIT_WINDOW_SECONDS;
    public static final ModConfigSpec.IntValue DIG_PENALTY_SECONDS;
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
                .comment("Target distance between copied base centers during future prepare/copy phases.")
                .defineInRange("baseSpacingBlocks", 1000, 256, 10000);
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
                .comment("Preview target Y level for copied base origins during /warday prepare.")
                .defineInRange("warDayBaseY", 80, -64, 320);
        RESPAWN_DELAY_SECONDS = builder
                .comment("Legacy fallback respawn delay in seconds. Scaling death penalties are used during active matches.")
                .defineInRange("respawnDelaySeconds", 10, 0, 300);
        builder.pop();

        builder.push("match");
        MATCH_DURATION_SECONDS = builder
                .comment("Maximum active War Day attack length in seconds.")
                .defineInRange("matchDurationSeconds", 900, 60, 7200);
        MAP_HALF_SIZE_BLOCKS = builder
                .comment("Half-size of the square match bounds centered on the defending nexus. 125 gives a 250x250 map.")
                .defineInRange("mapHalfSizeBlocks", 125, 16, 2048);
        DEFENDER_MATCH_BLOCK = builder
                .comment("Only placeable block for defender-team participants during War Day.")
                .define("defenderMatchBlock", "minecraft:blue_wool");
        ATTACKER_MATCH_BLOCK = builder
                .comment("Only placeable block for attacker-team participants during War Day.")
                .define("attackerMatchBlock", "minecraft:red_wool");
        DIG_LIMIT_BLOCKS = builder
                .comment("Blocks a player may dig inside the rolling dig-limit window before receiving the digging penalty.")
                .defineInRange("digLimitBlocks", 10, 1, 512);
        DIG_LIMIT_WINDOW_SECONDS = builder
                .comment("Rolling window in seconds used for the digging penalty.")
                .defineInRange("digLimitWindowSeconds", 30, 1, 300);
        DIG_PENALTY_SECONDS = builder
                .comment("Seconds of glowing and mining slowdown applied when a player exceeds the dig limit.")
                .defineInRange("digPenaltySeconds", 60, 1, 600);
        builder.pop();

        SPEC = builder.build();
    }

    private WarDayConfig() {
    }
}
