package com.chigusa.jsnathletic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/** ブロックの絵を、好きな大きさの箱にのばして描く */
public final class Draw {
    /** いまの原点から (x0,y0,z0) 〜 (x1,y1,z1) の箱 */
    public static void box(PoseStack ps, MultiBufferSource buf, BlockState state, double x0, double y0, double z0, double x1, double y1, double z1, int light) {
        ps.pushPose();
        ps.translate(x0, y0, z0);
        ps.scale((float) (x1 - x0), (float) (y1 - y0), (float) (z1 - z0));
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, ps, buf, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        ps.popPose();
    }

    private Draw() {}
}
