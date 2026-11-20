package com.chigusa.jsnathletic.gimmick;

import com.chigusa.jsnathletic.JsnAthletic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** ジャンプ台：乗ると高く飛ばされる。右クリックで強さ（1〜5）。スニークで乗るとただの床 */
public class JumpPadBlock extends Block {
    public static final IntegerProperty POWER = IntegerProperty.create("power", 1, 5);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public JumpPadBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(POWER, 3));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(POWER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    public static double velocity(int power) {
        return 0.65 + power * 0.25;   // 1：約4マス 〜 5：約18マス
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity e) {
        launch(level, pos, state, e);
        super.stepOn(level, pos, state, e);
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity e) {
        if (e.isSuppressingBounce()) super.updateEntityAfterFallOn(level, e);
        else if (level instanceof Level lv) launch(lv, e.blockPosition(), lv.getBlockState(e.getOnPos()), e);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity e, float dist) {
        // 落ちてきてもダメージなし
        e.resetFallDistance();
    }

    private static void launch(Level level, BlockPos pos, BlockState state, Entity e) {
        if (e.isSuppressingBounce() || !state.hasProperty(POWER)) return;
        Vec3 d = e.getDeltaMovement();
        double v = velocity(state.getValue(POWER));
        if (d.y >= v * 0.9) return;
        e.setDeltaMovement(d.x * 1.1, v, d.z * 1.1);
        e.resetFallDistance();
        e.hurtMarked = true;
        if (level.isClientSide) {
            if (e instanceof Player) level.playLocalSound(e.getX(), e.getY(), e.getZ(), SoundEvents.SLIME_JUMP, SoundSource.BLOCKS, 1F, 1.3F, false);
        } else if (!(e instanceof Player)) {
            level.playSound(null, pos, SoundEvents.SLIME_JUMP, SoundSource.BLOCKS, 1F, 1.3F);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!JsnAthletic.canEdit(player)) return InteractionResult.PASS;
        int p = state.getValue(POWER) % 5 + 1;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(POWER, p), 3);
            player.displayClientMessage(Component.literal("ジャンプの強さ：" + "▮".repeat(p) + "▯".repeat(5 - p) + "（" + p + "）"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
