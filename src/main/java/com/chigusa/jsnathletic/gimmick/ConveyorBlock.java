package com.chigusa.jsnathletic.gimmick;

import com.chigusa.jsnathletic.JsnAthletic;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** ベルトコンベア：乗ると矢印の向きへ流される。右クリックで速さ（1〜3） */
public class ConveyorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ConveyorBlock> CODEC = simpleCodec(ConveyorBlock::new);
    public static final IntegerProperty SPEED = IntegerProperty.create("speed", 1, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public ConveyorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SPEED, 2));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, SPEED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 置いた人が向いている向きへ流れる
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity e) {
        if (!e.isSteppingCarefully()) {
            Direction d = state.getValue(FACING);
            double a = 0.035 * state.getValue(SPEED);
            Vec3 m = e.getDeltaMovement();
            Vec3 dir = new Vec3(d.getStepX(), 0, d.getStepZ());
            // その向きの速さが、上限より遅いときだけ押す
            double along = m.dot(dir), max = 0.09 * state.getValue(SPEED) + 0.05;
            if (along < max) e.setDeltaMovement(m.add(dir.scale(Math.min(a, max - along))));
        }
        super.stepOn(level, pos, state, e);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!JsnAthletic.canEdit(player)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            BlockState ns = player.isShiftKeyDown() ? state.setValue(FACING, state.getValue(FACING).getClockWise())
                    : state.setValue(SPEED, state.getValue(SPEED) % 3 + 1);
            level.setBlock(pos, ns, 3);
            player.displayClientMessage(Component.literal(player.isShiftKeyDown() ? "向きを変えたよ" : "速さ：" + ns.getValue(SPEED) + "（スニーク＋右クリックで向き）"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
