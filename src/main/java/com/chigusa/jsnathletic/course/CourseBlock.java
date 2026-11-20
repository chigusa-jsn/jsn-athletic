package com.chigusa.jsnathletic.course;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** コースの床：スタート・チェックポイント・ゴール（乗ると反応する、うすい床） */
public class CourseBlock extends BaseEntityBlock {
    public enum Kind { START, CHECKPOINT, GOAL }

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);
    public final Kind kind;

    public CourseBlock(Properties p, Kind kind) {
        super(p);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new CourseBlock(p, kind));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == Kind.START ? new StartBlockEntity(pos, state) : null;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof ServerPlayer p) RunService.step(p, pos, kind);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p) RunService.info(p, pos, kind);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        // 金床で名前をつけたスタートは、それがコースの名前になる
        if (stack.has(DataComponents.CUSTOM_NAME) && level.getBlockEntity(pos) instanceof StartBlockEntity be) {
            be.name = stack.getHoverName().getString();
            be.setChanged();
        }
    }
}
