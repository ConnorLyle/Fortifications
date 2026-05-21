package com.trove.warday;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(WarDayMod.MODID)
public class WarDayMod {
    public static final String MODID = "warday";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> NEXUS = BLOCKS.registerSimpleBlock("nexus", warDayBlockProperties());
    public static final DeferredBlock<Block> FORWARD_MARKER = BLOCKS.registerSimpleBlock("forward_marker", warDayBlockProperties());

    public static final DeferredItem<BlockItem> NEXUS_ITEM = ITEMS.registerSimpleBlockItem("nexus", NEXUS);
    public static final DeferredItem<BlockItem> FORWARD_MARKER_ITEM = ITEMS.registerSimpleBlockItem("forward_marker", FORWARD_MARKER);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WAR_DAY_TAB =
            CREATIVE_MODE_TABS.register("war_day", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.warday"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> NEXUS_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(NEXUS_ITEM.get());
                        output.accept(FORWARD_MARKER_ITEM.get());
                    })
                    .build());

    public WarDayMod(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private static BlockBehaviour.Properties warDayBlockProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(50.0F, 1200.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.GLASS);
    }
}
