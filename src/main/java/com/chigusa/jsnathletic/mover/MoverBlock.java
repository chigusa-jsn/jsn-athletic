package com.chigusa.jsnathletic.mover;

import com.chigusa.jsnathletic.JsnAthletic;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

/** 仕掛けの台：右クリックで設定（クリエイティブかOP）。レッドストーンで動かせる */
public class MoverBlock extends BaseEntityBlock {
    public static final MapCodec<MoverBlock> CODEC = simpleCodec(MoverBlock::new);
    /** 画面を開く（クライアントでセット） */
    public static java.util.function.Consumer<BlockPos> openScreen;

    public MoverBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MoverBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, JsnAthletic.MOVER_BE.get(), MoverBlockEntity::serverTick);
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
        if (level.getBlockEntity(pos) instanceof MoverBlockEntity be) be.onPowered(level.hasNeighborSignal(pos));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MoverBlockEntity be) be.removeParts();
        super.onRemove(state, level, pos, newState, moving);
    }
}
