package com.chigusa.jsnathletic.client;

import com.chigusa.jsnathletic.mover.MoverBlockEntity;
import com.chigusa.jsnathletic.mover.Params;
import com.chigusa.jsnathletic.mover.PlatformEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/** 足場：えらんだブロックの絵で、うすい板を描く */
public class PlatformRenderer extends EntityRenderer<PlatformEntity> {
    public PlatformRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    @Override
    public void render(PlatformEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        MoverBlockEntity m = e.mover();
        if (m == null) return;
        double h = m.size / 2;
        Draw.box(ps, buf, Params.style(m.style), -h, 0, -h, h, MoverBlockEntity.THICK, h, light);
    }

    @Override
    public ResourceLocation getTextureLocation(PlatformEntity e) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
