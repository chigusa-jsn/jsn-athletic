package com.chigusa.jsnathletic.client;

import com.chigusa.jsnathletic.bar.SpinBarBlockEntity;
import com.chigusa.jsnathletic.mover.Params;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 回るバー */
public class SpinBarRenderer implements BlockEntityRenderer<SpinBarBlockEntity> {
    private static final BlockState HUB = Blocks.POLISHED_BLACKSTONE.defaultBlockState();

    public SpinBarRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(SpinBarBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        double a0 = be.angleAt(be.getLevel().getGameTime() + partial);
        double y = 1 + be.height;
        if (be.height > 0) Draw.box(ps, buf, HUB, 0.3, 1, 0.3, 0.7, y, 0.7, light);
        Draw.box(ps, buf, HUB, 0.2, y - 0.05, 0.2, 0.8, y + SpinBarBlockEntity.THICK + 0.05, 0.8, light);
        BlockState st = Params.style(be.style);
        for (int i = 0; i < be.bars; i++) {
            double a = a0 + i * 360.0 / be.bars;
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees((float) -a));
            Draw.box(ps, buf, st, 0.3, y, -0.2, be.length, y + SpinBarBlockEntity.THICK, 0.2, light);
            ps.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(SpinBarBlockEntity be) {
        return true;
    }

    public AABB getRenderBoundingBox(SpinBarBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(be.length + 1, be.height + 2, be.length + 1);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
