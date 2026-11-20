package com.chigusa.jsnathletic.client;

import com.chigusa.jsnathletic.JsnAthletic;
import com.chigusa.jsnathletic.Net;
import com.chigusa.jsnathletic.bar.SpinBarBlock;
import com.chigusa.jsnathletic.mover.MoverBlock;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = JsnAthletic.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    public static final KeyMapping RETRY = new KeyMapping("key.jsn_athletic.retry", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.jsn_athletic");

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        MoverBlock.openScreen = pos -> Minecraft.getInstance().setScreen(new ConfigScreen(pos));
        SpinBarBlock.openScreen = pos -> Minecraft.getInstance().setScreen(new ConfigScreen(pos));
        com.chigusa.jsnathletic.gimmick.GimmickBlock.openScreen = pos -> Minecraft.getInstance().setScreen(new ConfigScreen(pos));
        Net.onHud = RunHud::onHud;
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(JsnAthletic.PLATFORM.get(), PlatformRenderer::new);
        e.registerBlockEntityRenderer(JsnAthletic.MOVER_BE.get(), MoverRenderer::new);
        e.registerBlockEntityRenderer(JsnAthletic.SPIN_BAR_BE.get(), SpinBarRenderer::new);
        e.registerBlockEntityRenderer(JsnAthletic.PENDULUM_BE.get(), PendulumRenderer::new);
        e.registerBlockEntityRenderer(JsnAthletic.ROLLING_BEAM_BE.get(), RollingBeamRenderer::new);
    }

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        e.register(RETRY);
    }

    private ClientSetup() {}
}
