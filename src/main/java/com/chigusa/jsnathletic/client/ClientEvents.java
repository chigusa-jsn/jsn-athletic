package com.chigusa.jsnathletic.client;

import com.chigusa.jsnathletic.JsnAthletic;
import com.chigusa.jsnathletic.Net;
import com.chigusa.jsnathletic.bar.SpinBarBlockEntity;
import com.chigusa.jsnathletic.mover.MoverBlockEntity;
import com.chigusa.jsnathletic.mover.PlatformEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 自分のキャラの動き（プレイヤーの動きは画面側で決まるので、ここで計算する）
 * ・動く足場に乗っていたら、一緒に運ぶ
 * ・回るバーに当たったら、はじき飛ばす
 * ・Rキー：チェックポイントにもどる（長押しでやめる）
 */
@EventBusSubscriber(modid = JsnAthletic.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private static int hitCooldown, retryHeld;

    @SubscribeEvent
    public static void pre(ClientTickEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) return;
        long t = mc.level.getGameTime();   // このあと1つ進んで t+1 になる
        carry(mc, p, t);
        bars(p, t + 1);
        pendulums(p, t + 1);
        keys(mc);
    }

    private static void carry(Minecraft mc, LocalPlayer p, long t) {
        if (p.isPassenger() || p.getAbilities().flying) return;
        AABB pb = p.getBoundingBox();
        for (PlatformEntity e : mc.level.getEntitiesOfClass(PlatformEntity.class, pb.inflate(8))) {
            var m = e.host();
            if (m == null) continue;
            AABB now = e.boxAt(t);
            // 足場の上に立っている？（足の高さが足場の上の面のすぐ近く）
            boolean onTop = pb.minY >= now.maxY - 0.15 && pb.minY <= now.maxY + 0.2
                    && pb.maxX > now.minX + 0.02 && pb.minX < now.maxX - 0.02 && pb.maxZ > now.minZ + 0.02 && pb.minZ < now.maxZ - 0.02;
            Vec3 d = e.posAt(t + 1).subtract(e.posAt(t)).add(m.surfaceMotion(e.index(), t));
            if (onTop && d.lengthSqr() > 0) {
                p.setPos(p.getX() + d.x, Math.max(p.getY() + d.y, e.posAt(t + 1).y + e.thick()), p.getZ() + d.z);
                break;
            }
        }
    }

    private static void bars(LocalPlayer p, long t) {
        if (hitCooldown > 0) {
            hitCooldown--;
            return;
        }
        if (p.isSpectator() || p.getAbilities().flying) return;
        AABB pb = p.getBoundingBox();
        Vec3 pc = new Vec3(p.getX(), 0, p.getZ());
        for (SpinBarBlockEntity b : SpinBarBlockEntity.CLIENT) {
            if (b.isRemoved() || b.getLevel() != p.level()) continue;
            double y = b.barY(), half = SpinBarBlockEntity.THICK / 2;
            if (pb.maxY < y - half || pb.minY > y + half) continue;
            Vec3 c = b.center();
            if (c.distanceToSqr(p.getX(), y, p.getZ()) > (b.length + 1) * (b.length + 1)) continue;
            double ang = b.angleAt(t);
            for (int i = 0; i < b.bars; i++) {
                double a = Math.toRadians(ang + i * 360.0 / b.bars);
                Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
                Vec3 rel = pc.subtract(c.x, 0, c.z);
                double along = rel.dot(dir);
                if (along < 0.2 || along > b.length + 0.3) continue;
                Vec3 side = rel.subtract(dir.scale(along));
                if (side.length() > 0.2 + p.getBbWidth() / 2) continue;
                // 当たった：回る向きへ はじく
                double sign = b.reverse ? -1 : 1;
                Vec3 tangent = new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(sign);
                double power = 0.6 + Math.min(1.2, Math.abs(b.speed) / 120.0) * along / Math.max(1, b.length);
                Vec3 push = tangent.scale(power).add(dir.scale(0.25)).add(0, 0.45, 0);
                p.setDeltaMovement(push);
                p.hurtMarked = true;
                hitCooldown = 12;
                p.playSound(SoundEvents.SLIME_BLOCK_HIT, 1F, 0.8F);
                return;
            }
        }
    }

    /** ふりこの鉄球に当たったら、ゆれている向きへ はじく */
    private static void pendulums(LocalPlayer p, long t) {
        if (hitCooldown > 0 || p.isSpectator() || p.getAbilities().flying) return;
        Vec3 mid = p.position().add(0, p.getBbHeight() / 2, 0);
        for (com.chigusa.jsnathletic.gimmick.PendulumBlockEntity b : com.chigusa.jsnathletic.gimmick.PendulumBlockEntity.CLIENT) {
            if (b.isRemoved() || b.getLevel() != p.level()) continue;
            Vec3 c = b.ballPos(t);
            double r = b.ball / 2;
            // 体（たての棒）と球の近さ
            double dy = Math.max(0, Math.max(p.getY() - c.y, c.y - (p.getY() + p.getBbHeight())));
            double dx = c.x - p.getX(), dz = c.z - p.getZ();
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz > r + p.getBbWidth() / 2 + 0.05 || dy > r) continue;
            double vel = b.angle(t + 1) - b.angle(t);
            Vec3 dir = b.swingDir().scale(Math.signum(vel == 0 ? 1 : vel));
            Vec3 away = new Vec3(-dx, 0, -dz).normalize();
            Vec3 push = dir.scale(1.0 + Math.min(1.0, Math.abs(vel) / 3)).add(away.scale(0.3)).add(0, 0.5, 0);
            p.setDeltaMovement(push);
            p.hurtMarked = true;
            hitCooldown = 12;
            p.playSound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, 0.5F, 1.4F);
            return;
        }
    }

    private static void keys(Minecraft mc) {
        if (ClientSetup.RETRY.isDown() && mc.screen == null) {
            retryHeld++;
            if (retryHeld == 30 && RunHud.running) PacketDistributor.sendToServer(new Net.Key(1));
        } else {
            if (retryHeld > 0 && retryHeld < 30 && RunHud.running) {
                PacketDistributor.sendToServer(new Net.Key(0));
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
            }
            retryHeld = 0;
        }
        while (ClientSetup.RETRY.consumeClick()) {
        }
    }

    private ClientEvents() {}
}
