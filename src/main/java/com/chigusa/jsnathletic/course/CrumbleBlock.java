package com.chigusa.jsnathletic.course;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 落ちる床：乗ると ひびが入って、少しすると消える。3秒で元にもどる */
public class CrumbleBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public static final int CRACK_TICKS = 10, GONE_TICKS = 60;

    public CrumbleBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STAGE);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.setValue(STAGE, 1), 3);
            level.scheduleTick(pos, this, CRACK_TICKS);
            level.playSound(null, pos, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int s = state.getValue(STAGE);
        if (s == 1) {
            level.setBlock(pos, state.setValue(STAGE, 2), 3);
            level.scheduleTick(pos, this, CRACK_TICKS);
        } else if (s == 2) {
            level.setBlock(pos, state.setValue(STAGE, 3), 3);
            level.scheduleTick(pos, this, GONE_TICKS);
            level.playSound(null, pos, SoundEvents.SCAFFOLDING_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
        } else if (s == 3) {
            level.setBlock(pos, state.setValue(STAGE, 0), 3);
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.5F, 1.4F);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(STAGE) == 3 ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(STAGE) == 3 ? Block.box(0, 0, 0, 16, 1, 16) : Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(STAGE) == 3 ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }
}
