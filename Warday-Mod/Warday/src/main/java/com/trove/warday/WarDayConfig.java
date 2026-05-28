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
                .comment("Seconds a War Day participant must wait in spectator mode after respawning before rejoining the fight.")
                .defineInRange("respawnDelaySeconds", 10, 0, 300);
        builder.pop();

        SPEC = builder.build();
    }

    private WarDayConfig() {
    }
}
