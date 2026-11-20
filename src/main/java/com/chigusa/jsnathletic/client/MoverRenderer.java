package com.chigusa.jsnathletic.client;

import com.chigusa.jsnathletic.mover.MoverBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 回る足場の「柱」と「アーム」 */
public class MoverRenderer implements BlockEntityRenderer<MoverBlockEntity> {
    private static final BlockState ARM = Blocks.IRON_BLOCK.defaultBlockState();
    private static final BlockState HUB = Blocks.POLISHED_BLACKSTONE.defaultBlockState();

    public MoverRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(MoverBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.mode != MoverBlockEntity.ROTATE || !be.arms || be.getLevel() == null) return;
        double t = be.getLevel().getGameTime() + partial;
        double phase = be.phaseAt(t);
        double armY = 1 + be.height - MoverBlockEntity.THICK - 0.05;
        // まんなかの柱
        if (be.height > 0) Draw.box(ps, buf, HUB, 0.3, 1, 0.3, 0.7, 1 + be.height, 0.7, light);
        Draw.box(ps, buf, HUB, 0.15, armY - 0.2, 0.15, 0.85, armY + 0.25, 0.85, light);
        for (int i = 0; i < be.count; i++) {
            double a = phase + i * 360.0 / Math.max(1, be.count);
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees((float) -a));
            Draw.box(ps, buf, ARM, 0, armY - 0.1, -0.12, be.radius - be.size / 2 + 0.1, armY + 0.1, 0.12, light);
            ps.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(MoverBlockEntity be) {
        return true;
    }

    public AABB getRenderBoundingBox(MoverBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(be.reach() + 2, Math.max(3, be.height + be.distance + 2), be.reach() + 2);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
