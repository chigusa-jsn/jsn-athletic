package com.chigusa.jsnathletic;

import com.chigusa.jsnathletic.bar.SpinBarBlock;
import com.chigusa.jsnathletic.bar.SpinBarBlockEntity;
import com.chigusa.jsnathletic.course.CourseBlock;
import com.chigusa.jsnathletic.course.CrumbleBlock;
import com.chigusa.jsnathletic.course.StartBlockEntity;
import com.chigusa.jsnathletic.mover.MoverBlock;
import com.chigusa.jsnathletic.mover.MoverBlockEntity;
import com.chigusa.jsnathletic.mover.PlatformEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.function.Supplier;

/** jsn-Athletic：アスレチックの仕掛け */
@Mod(JsnAthletic.MODID)
public final class JsnAthletic {
    public static final String MODID = "jsn_athletic";
    public static final Logger LOG = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // 仕掛けの台（回る足場・動く床）
    public static final DeferredBlock<MoverBlock> MOVER = BLOCKS.register("mover", () -> new MoverBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> MOVER_ITEM = ITEMS.registerSimpleBlockItem(MOVER);
    public static final Supplier<BlockEntityType<MoverBlockEntity>> MOVER_BE = BLOCK_ENTITIES.register("mover",
            () -> BlockEntityType.Builder.of(MoverBlockEntity::new, MOVER.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<PlatformEntity>> PLATFORM = ENTITIES.register("platform",
            () -> EntityType.Builder.<PlatformEntity>of(PlatformEntity::new, MobCategory.MISC).sized(1F, 0.5F).clientTrackingRange(10)
                    .updateInterval(100).noSummon().fireImmune().build("platform"));

    // 回るバー
    public static final DeferredBlock<SpinBarBlock> SPIN_BAR = BLOCKS.register("spin_bar", () -> new SpinBarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> SPIN_BAR_ITEM = ITEMS.registerSimpleBlockItem(SPIN_BAR);
    public static final Supplier<BlockEntityType<SpinBarBlockEntity>> SPIN_BAR_BE = BLOCK_ENTITIES.register("spin_bar",
            () -> BlockEntityType.Builder.of(SpinBarBlockEntity::new, SPIN_BAR.get()).build(null));

    // 落ちる床
    public static final DeferredBlock<CrumbleBlock> CRUMBLE = BLOCKS.register("crumble_block", () -> new CrumbleBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.8F).sound(SoundType.GRAVEL).noOcclusion()));
    public static final DeferredItem<BlockItem> CRUMBLE_ITEM = ITEMS.registerSimpleBlockItem(CRUMBLE);

    // コース：スタート・チェックポイント・ゴール
    public static final DeferredBlock<CourseBlock> START = BLOCKS.register("start_line", () -> new CourseBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(1F).sound(SoundType.STONE), CourseBlock.Kind.START));
    public static final DeferredBlock<CourseBlock> CHECKPOINT = BLOCKS.register("checkpoint", () -> new CourseBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1F).sound(SoundType.STONE), CourseBlock.Kind.CHECKPOINT));
    public static final DeferredBlock<CourseBlock> GOAL = BLOCKS.register("goal_line", () -> new CourseBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1F).sound(SoundType.STONE), CourseBlock.Kind.GOAL));
    public static final DeferredItem<BlockItem> START_ITEM = ITEMS.registerSimpleBlockItem(START);
    public static final DeferredItem<BlockItem> CHECKPOINT_ITEM = ITEMS.registerSimpleBlockItem(CHECKPOINT);
    public static final DeferredItem<BlockItem> GOAL_ITEM = ITEMS.registerSimpleBlockItem(GOAL);
    public static final Supplier<BlockEntityType<StartBlockEntity>> START_BE = BLOCK_ENTITIES.register("start_line",
            () -> BlockEntityType.Builder.of(StartBlockEntity::new, START.get()).build(null));

    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.literal("jsn-Athletic")).icon(() -> new ItemStack(MOVER_ITEM.get()))
            .displayItems((p, out) -> {
                out.accept(MOVER_ITEM.get());
                out.accept(SPIN_BAR_ITEM.get());
                out.accept(CRUMBLE_ITEM.get());
                out.accept(START_ITEM.get());
                out.accept(CHECKPOINT_ITEM.get());
                out.accept(GOAL_ITEM.get());
            }).build());

    public JsnAthletic(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
        bus.addListener(RegisterPayloadHandlersEvent.class, Net::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    /** 設定を変えていい人：クリエイティブか、OP */
    public static boolean canEdit(net.minecraft.world.entity.player.Player p) {
        return p.isCreative() || p.hasPermissions(2);
    }
}
