package com.chigusa.jsnathletic.client;

import com.chigusa.jsnathletic.gimmick.PendulumBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** ふりこ：くさりと鉄球 */
public class PendulumRenderer implements BlockEntityRenderer<PendulumBlockEntity> {
    private static final BlockState CHAIN = Blocks.IRON_BLOCK.defaultBlockState();
    private static final BlockState[] BALLS = {Blocks.NETHERITE_BLOCK.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState()};

    public PendulumRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PendulumBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        double a = be.angle(be.getLevel().getGameTime() + partial);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        // X方向にゆれるなら Z軸まわり、Z方向なら X軸まわり
        if (be.axis == 0) ps.mulPose(Axis.ZP.rotationDegrees((float) a));
        else ps.mulPose(Axis.XP.rotationDegrees((float) -a));
        double L = be.length, r = be.ball / 2;
        // くさり（細い棒）
        Draw.box(ps, buf, CHAIN, -0.06, -L + r * 0.8, -0.06, 0.06, 0, 0.06, light);
        // 鉄球（3つの箱を重ねて、丸っぽく）
        BlockState b = BALLS[Math.floorMod(be.style, BALLS.length)];
        double k = r * 0.72;
        Draw.box(ps, buf, b, -k, -L - r, -k, k, -L + r, k, light);
        Draw.box(ps, buf, b, -r, -L - k, -k, r, -L + k, k, light);
        Draw.box(ps, buf, b, -k, -L - k, -r, k, -L + k, r, light);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(PendulumBlockEntity be) {
        return true;
    }

    public AABB getRenderBoundingBox(PendulumBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(be.length + 2);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
