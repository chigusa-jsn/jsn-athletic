package com.chigusa.jsnathletic.client;

import com.chigusa.jsnathletic.gimmick.RollingBeamBlockEntity;
import com.chigusa.jsnathletic.mover.Params;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 回転する角材：長い軸のまわりを回る。目印に、縞もようの帯をつける */
public class RollingBeamRenderer implements BlockEntityRenderer<RollingBeamBlockEntity> {
    private static final BlockState POST = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState STRIPE = Blocks.YELLOW_CONCRETE.defaultBlockState();

    public RollingBeamRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RollingBeamBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        double ang = be.phaseAt(be.getLevel().getGameTime() + partial);
        double w = be.width, L = be.length, h = w / 2;
        double cy = 1 + be.height + h;
        // 両はしの支柱
        for (double end : new double[]{0, L}) {
            Vec3 o = be.axisDir().scale(end);
            Draw.box(ps, buf, POST, 0.35 + o.x, 1, 0.35 + o.z, 0.65 + o.x, cy + 0.1, 0.65 + o.z, light);
        }
        ps.pushPose();
        ps.translate(0.5, cy, 0.5);
        if (be.axis == 0) ps.mulPose(Axis.XP.rotationDegrees((float) ang));
        else ps.mulPose(Axis.ZP.rotationDegrees((float) ang));
        BlockState st = Params.style(be.style);
        if (be.axis == 0) {
            Draw.box(ps, buf, st, 0, -h, -h, L, h, h, light);
            for (double x = 1; x < L; x += 2) Draw.box(ps, buf, STRIPE, x, -h - 0.01, -h - 0.01, x + 0.25, h + 0.01, h + 0.01, light);
        } else {
            Draw.box(ps, buf, st, -h, -h, 0, h, h, L, light);
            for (double z = 1; z < L; z += 2) Draw.box(ps, buf, STRIPE, -h - 0.01, -h - 0.01, z, h + 0.01, h + 0.01, z + 0.25, light);
        }
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(RollingBeamBlockEntity be) {
        return true;
    }

    public AABB getRenderBoundingBox(RollingBeamBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(be.length + 2, be.height + be.width + 2, be.length + 2);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
