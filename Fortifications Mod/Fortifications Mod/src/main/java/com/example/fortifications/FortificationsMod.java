package com.example.fortifications;

import com.trove.warday.WarDayMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FortificationsMod.MOD_ID)
public class FortificationsMod {

    public static final String MOD_ID = "fortifications";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, MOD_ID);

    public static final DeferredBlock<FortChestBlock> FORT_CHEST = BLOCKS.register(
            "fort_chest",
            () -> new FortChestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST))
    );
    public static final DeferredItem<BlockItem> FORT_CHEST_ITEM = ITEMS.registerSimpleBlockItem("fort_chest", FORT_CHEST);
    public static final DeferredItem<ClassResetTokenItem> CLASS_RESET_TOKEN = ITEMS.register(
            "class_reset_token",
            () -> new ClassResetTokenItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE))
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FortChestBlockEntity>> FORT_CHEST_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "fort_chest",
                    () -> BlockEntityType.Builder.of(FortChestBlockEntity::new, FORT_CHEST.get()).build(null)
            );

    public static final DeferredHolder<Attribute, Attribute> UNARMED_DAMAGE = ATTRIBUTES.register(
            "unarmed_damage",
            () -> new RangedAttribute("attribute.name.fortifications.unarmed_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true)
    );

    /**
     * When true, Fortifications mode is active for ALL players.
     * Flipped by the /fortifications all command (OP level 2).
     */
    public static volatile boolean GLOBAL_ACTIVE = false;

    public FortificationsMod(IEventBus modEventBus, ModContainer container) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ATTRIBUTES.register(modEventBus);
        modEventBus.register(this);
        ClassResetService.initialize();
        WarDayMod.initialize(modEventBus, container);
    }

    @SubscribeEvent
    public void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(FORT_CHEST_ITEM.get());
        }
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(CLASS_RESET_TOKEN.get());
        }
    }

    @SubscribeEvent
    public void onEntityAttributeModification(net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent event) {
        event.add(net.minecraft.world.entity.EntityType.PLAYER, UNARMED_DAMAGE, 0.0D);
    }
}
