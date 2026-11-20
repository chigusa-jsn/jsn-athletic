package com.chigusa.jsnathletic.gimmick;

import com.chigusa.jsnathletic.JsnAthletic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 消えたり出たりする床：4色（グループ）で、1.5秒ずつ順番に出たり消えたりする。
 * 赤→黄→緑→青 の順に波のように出るので、色を並べると道になる。消える前に点滅する。
 */
public class BlinkBlock extends Block {
    public static final IntegerProperty GROUP = IntegerProperty.create("group", 0, 3);
    /** 0=出ている 1=もうすぐ消える 2=消えている */
    public static final IntegerProperty STATE = IntegerProperty.create("state", 0, 2);
    public static final int SLOT = 30, WARN = 10;
    public static final String[] COLORS = {"赤", "黄", "緑", "青"};

    public BlinkBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(GROUP, 0).setValue(STATE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(GROUP, STATE);
    }

    /** 時刻 t で、グループ g はどうなっているか */
    public static int stateAt(long t, int g) {
        long slot = t / SLOT;
        int k = (int) Math.floorMod(slot - g, 4);
        if (k >= 2) return 2;
        if (k == 1 && t % SLOT >= SLOT - WARN) return 1;
        return 0;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int s = stateAt(level.getGameTime(), state.getValue(GROUP));
        if (s != state.getValue(STATE)) level.setBlock(pos, state.setValue(STATE, s), 2);
        level.scheduleTick(pos, this, 5);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(STATE) == 2 ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(STATE) == 2 ? Block.box(0, 0, 0, 16, 1, 16) : Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!JsnAthletic.canEdit(player)) return InteractionResult.PASS;
        int g = (state.getValue(GROUP) + 1) % 4;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(GROUP, g), 3);
            player.displayClientMessage(Component.literal("色：" + COLORS[g] + "（赤→黄→緑→青 の順に出るよ）"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
