package com.chigusa.jsnathletic.gimmick;

import com.chigusa.jsnathletic.JsnAthletic;
import com.chigusa.jsnathletic.mover.PartHost;
import com.chigusa.jsnathletic.mover.TimedBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 設定画面とレッドストーンで動く仕掛けの台（ふりこ・角材） */
public class GimmickBlock extends BaseEntityBlock {
    public static Consumer<BlockPos> openScreen;
    private final BiFunction<BlockPos, BlockState, ? extends TimedBlockEntity> factory;
    private final Supplier<? extends BlockEntityType<? extends TimedBlockEntity>> type;

    public GimmickBlock(Properties p, BiFunction<BlockPos, BlockState, ? extends TimedBlockEntity> factory,
                        Supplier<? extends BlockEntityType<? extends TimedBlockEntity>> type) {
        super(p);
        this.factory = factory;
        this.type = type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new GimmickBlock(p, factory, type));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if (level.isClientSide || t != type.get()) return null;
        return (lv, pos, st, be) -> {
            TimedBlockEntity tb = (TimedBlockEntity) be;
            tb.timedTick();
            if (be instanceof PartHost h && lv.getGameTime() % 20 == 0) PartHost.ensure((ServerLevel) lv, h);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!JsnAthletic.canEdit(player)) {
            if (!level.isClientSide) player.displayClientMessage(Component.literal("設定を変えられるのはクリエイティブかOPの人だけだよ"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide && openScreen != null) openScreen.accept(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.getBlockEntity(pos) instanceof TimedBlockEntity be) be.onPowered(level.hasNeighborSignal(pos));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof PartHost h) PartHost.removeAll(sl, h);
        super.onRemove(state, level, pos, newState, moving);
    }
}
