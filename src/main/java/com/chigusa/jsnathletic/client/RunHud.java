package com.chigusa.jsnathletic.client;

import com.chigusa.jsnathletic.JsnAthletic;
import com.chigusa.jsnathletic.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** 画面の上に、タイムと自己ベスト（♛）を出す */
@EventBusSubscriber(modid = JsnAthletic.MODID, value = Dist.CLIENT)
public final class RunHud {
    public static boolean running;
    private static int elapsedAt, best, falls, finalTicks;
    private static long receivedAt, doneAt;
    private static String course = "";
    private static boolean newBest;

    public static void onHud(Net.Hud h) {
        receivedAt = System.currentTimeMillis();
        if (h.done()) {
            running = false;
            finalTicks = h.elapsed();
            doneAt = receivedAt;
            newBest = h.best() == h.elapsed();
            best = h.best();
            falls = h.falls();
            course = h.course();
            return;
        }
        running = h.running();
        elapsedAt = h.elapsed();
        best = h.best();
        falls = h.falls();
        course = h.course();
        if (!running) doneAt = 0;
    }

    static String time(long ms) {
        long cs = ms / 10;
        return String.format("%d:%02d.%02d", cs / 6000, cs / 100 % 60, cs % 100);
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        GuiGraphics g = e.getGuiGraphics();
        var f = mc.font;
        int cx = g.guiWidth() / 2;
        long now = System.currentTimeMillis();
        if (running) {
            long ms = elapsedAt * 50L + (now - receivedAt);
            String t = time(ms);
            g.pose().pushPose();
            g.pose().translate(cx, 6, 0);
            g.pose().scale(2F, 2F, 1);
            g.drawCenteredString(f, t, 0, 0, 0xFFFFFFFF);
            g.pose().popPose();
            String sub = (best > 0 ? "§e♛ " + time(best * 50L) + "  " : "") + "§7" + course + (falls > 0 ? "  §c落ちた " + falls : "");
            g.drawCenteredString(f, sub, cx, 26, 0xFFFFFF);
            g.drawCenteredString(f, "§8Rキー：チェックポイントへ（長押しでやめる）", cx, 37, 0xFFFFFF);
        } else if (doneAt > 0 && now - doneAt < 6000) {
            g.pose().pushPose();
            g.pose().translate(cx, 10, 0);
            g.pose().scale(2.5F, 2.5F, 1);
            g.drawCenteredString(f, "§6§lGOAL!", 0, 0, 0xFFFFFFFF);
            g.pose().popPose();
            g.pose().pushPose();
            g.pose().translate(cx, 36, 0);
            g.pose().scale(1.8F, 1.8F, 1);
            g.drawCenteredString(f, time(finalTicks * 50L), 0, 0, 0xFFFFFFFF);
            g.pose().popPose();
            g.drawCenteredString(f, newBest ? "§a自己ベスト！" : "§e♛ " + time(best * 50L), cx, 56, 0xFFFFFF);
        }
    }

    private RunHud() {}
}
