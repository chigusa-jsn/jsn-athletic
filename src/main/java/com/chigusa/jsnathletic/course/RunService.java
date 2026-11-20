package com.chigusa.jsnathletic.course;

import com.chigusa.jsnathletic.JsnAthletic;
import com.chigusa.jsnathletic.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * タイム計測：スタートの床から出た瞬間から、ゴールの床に乗るまで。
 * チェックポイントに乗ると、落ちたとき（またはRキー）にそこへもどれる。
 */
@EventBusSubscriber(modid = JsnAthletic.MODID)
public final class RunService {
    static final class Run {
        BlockPos start;
        String dim;
        long startTick;
        Vec3 respawn;
        float yaw;
        double respawnY;
        int falls;
        long lastHud;
        final Set<BlockPos> checkpoints = new HashSet<>();
    }

    static final Map<UUID, Run> RUNS = new HashMap<>();
    public static final int FALL_DEPTH = 8;

    static String courseName(ServerLevel level, BlockPos start) {
        return level.getBlockEntity(start) instanceof StartBlockEntity s && !s.name.isEmpty() ? s.name : "コース（" + start.getX() + ", " + start.getY() + ", " + start.getZ() + "）";
    }

    public static void step(ServerPlayer p, BlockPos pos, CourseBlock.Kind kind) {
        long now = p.serverLevel().getGameTime();
        Run r = RUNS.get(p.getUUID());
        switch (kind) {
            case START -> {
                // 乗っているあいだは計りなおし（床から出た時から数える）
                boolean fresh = r == null || !r.start.equals(pos);
                if (fresh) {
                    r = new Run();
                    r.start = pos;
                    r.dim = p.level().dimension().location().toString();
                    RUNS.put(p.getUUID(), r);
                    p.displayClientMessage(Component.literal("§a" + courseName(p.serverLevel(), pos) + "：スタートの床から出るとタイム計測！"), true);
                }
                r.startTick = now;
                r.respawn = Vec3.atBottomCenterOf(pos).add(0, 0.2, 0);
                r.respawnY = pos.getY();
                r.yaw = p.getYRot();
                r.falls = 0;
                r.checkpoints.clear();
                if (fresh || now - r.lastHud > 10) {
                    r.lastHud = now;
                    hud(p, r, 0, false);
                }
            }
            case CHECKPOINT -> {
                if (r == null) return;
                if (r.checkpoints.add(pos)) {
                    p.displayClientMessage(Component.literal("§bチェックポイント！ §7" + time(now - r.startTick)), true);
                    p.level().playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1F, 1.6F);
                    r.respawn = Vec3.atBottomCenterOf(pos).add(0, 0.2, 0);
                    r.respawnY = pos.getY();
                    r.yaw = p.getYRot();
                    hud(p, r, (int) (now - r.startTick), false);
                }
            }
            case GOAL -> {
                if (r == null) return;
                finish(p, r, now);
            }
        }
    }

    private static void finish(ServerPlayer p, Run r, long now) {
        RUNS.remove(p.getUUID());
        int ticks = (int) (now - r.startTick);
        Records rec = Records.get(p.server);
        String key = r.dim + "|" + r.start.asLong();
        int before = rec.best(key, p.getUUID());
        boolean pb = before <= 0 || ticks < before;
        int rank = rec.add(key, p.getUUID(), p.getName().getString(), ticks);
        String course = courseName(p.serverLevel(), r.start);
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1F, 1F);
        Component msg = Component.literal("§6§l GOAL！ §r§f" + p.getName().getString() + " が「" + course + "」を " + time(ticks) + " でクリア"
                + (pb ? (before > 0 ? " §a（自己ベスト更新！）" : " §a（初クリア！）") : "") + (rank == 1 ? " §e♛ コース新記録！" : rank > 0 ? " §7" + rank + "位" : ""));
        for (ServerPlayer o : p.server.getPlayerList().getPlayers())
            if (o.level() == p.level() && o.distanceToSqr(p) < 128 * 128) o.sendSystemMessage(msg);
        PacketDistributor.sendToPlayer(p, new Net.Hud(false, ticks, Math.min(ticks, before > 0 ? before : ticks), course, r.falls, true));
    }

    static void hud(ServerPlayer p, Run r, int elapsed, boolean done) {
        Records rec = Records.get(p.server);
        int best = rec.best(r.dim + "|" + r.start.asLong(), p.getUUID());
        PacketDistributor.sendToPlayer(p, new Net.Hud(true, elapsed, best, courseName(p.serverLevel(), r.start), r.falls, done));
    }

    /** Rキー：チェックポイント（なければスタート）へもどる */
    public static void retry(ServerPlayer p) {
        Run r = RUNS.get(p.getUUID());
        if (r == null) return;
        back(p, r);
    }

    /** Rキーを長押し：やめる */
    public static void quit(ServerPlayer p) {
        if (RUNS.remove(p.getUUID()) != null) {
            p.displayClientMessage(Component.literal("§7タイム計測をやめたよ"), true);
            PacketDistributor.sendToPlayer(p, new Net.Hud(false, 0, 0, "", 0, false));
        }
    }

    private static void back(ServerPlayer p, Run r) {
        r.falls++;
        p.fallDistance = 0;
        p.setDeltaMovement(Vec3.ZERO);
        p.teleportTo(p.serverLevel(), r.respawn.x, r.respawn.y, r.respawn.z, r.yaw, p.getXRot());
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
        hud(p, r, (int) (p.serverLevel().getGameTime() - r.startTick), false);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        if (RUNS.isEmpty()) return;
        MinecraftServer s = e.getServer();
        for (UUID u : new ArrayList<>(RUNS.keySet())) {
            ServerPlayer p = s.getPlayerList().getPlayer(u);
            Run r = RUNS.get(u);
            if (p == null || !p.level().dimension().location().toString().equals(r.dim)) {
                RUNS.remove(u);
                continue;
            }
            // 落ちたら、チェックポイントへ
            if (p.getY() < r.respawnY - FALL_DEPTH && !p.isSpectator()) back(p, r);
            // 遠くへ行ったら、やめたことに
            if (p.blockPosition().distSqr(r.start) > 400 * 400) quit(p);
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) quit(p);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        RUNS.remove(e.getEntity().getUUID());
    }

    /** 床を右クリック：ランキング */
    public static void info(ServerPlayer p, BlockPos pos, CourseBlock.Kind kind) {
        if (kind != CourseBlock.Kind.START) {
            p.displayClientMessage(Component.literal(kind == CourseBlock.Kind.GOAL ? "§6ゴール：ここに乗るとタイムが止まる" : "§bチェックポイント：落ちたらここから（Rキーでももどれる）"), true);
            return;
        }
        String key = p.level().dimension().location().toString() + "|" + pos.asLong();
        List<Records.Entry> top = Records.get(p.server).top(key);
        p.sendSystemMessage(Component.literal("§6§l―― " + courseName(p.serverLevel(), pos) + " ランキング ――"));
        if (top.isEmpty()) p.sendSystemMessage(Component.literal("§7まだだれもクリアしていないよ"));
        for (int i = 0; i < top.size(); i++) {
            Records.Entry en = top.get(i);
            p.sendSystemMessage(Component.literal((i == 0 ? "§e♛ " : "§7" + (i + 1) + ". ") + "§f" + en.name + "  " + time(en.ticks)));
        }
        int best = Records.get(p.server).best(key, p.getUUID());
        if (best > 0) p.sendSystemMessage(Component.literal("§a自己ベスト：" + time(best)).withStyle(ChatFormatting.GREEN));
    }

    public static String time(long ticks) {
        long cs = ticks * 5;   // 1ティック＝0.05秒
        return String.format("%d:%02d.%02d", cs / 6000, cs / 100 % 60, cs % 100);
    }

    /** コースごとの記録（ワールドに保存） */
    public static final class Records extends SavedData {
        public static final class Entry {
            UUID uuid;
            String name;
            int ticks;
        }

        final Map<String, List<Entry>> courses = new HashMap<>();

        static Records get(MinecraftServer s) {
            return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Records::new, Records::load), "jsn_athletic_records");
        }

        int best(String key, UUID u) {
            for (Entry e : courses.getOrDefault(key, List.of())) if (e.uuid.equals(u)) return e.ticks;
            return 0;
        }

        /** 1人1つ（いちばん速いもの）。戻り値は順位（10位より下なら0） */
        int add(String key, UUID u, String name, int ticks) {
            List<Entry> l = courses.computeIfAbsent(key, k -> new ArrayList<>());
            Entry mine = null;
            for (Entry e : l) if (e.uuid.equals(u)) mine = e;
            if (mine == null) {
                mine = new Entry();
                mine.uuid = u;
                mine.ticks = Integer.MAX_VALUE;
                l.add(mine);
            }
            mine.name = name;
            if (ticks < mine.ticks) mine.ticks = ticks;
            l.sort((a, b) -> Integer.compare(a.ticks, b.ticks));
            while (l.size() > 50) l.remove(l.size() - 1);
            setDirty();
            if (mine.ticks != ticks) return 0;
            int rank = l.indexOf(mine) + 1;
            return rank <= 10 ? rank : 0;
        }

        List<Entry> top(String key) {
            List<Entry> l = courses.getOrDefault(key, List.of());
            return l.subList(0, Math.min(10, l.size()));
        }

        static Records load(CompoundTag t, HolderLookup.Provider reg) {
            Records r = new Records();
            for (String k : t.getAllKeys()) {
                ListTag lt = t.getList(k, Tag.TAG_COMPOUND);
                List<Entry> l = new ArrayList<>();
                for (int i = 0; i < lt.size(); i++) {
                    CompoundTag c = lt.getCompound(i);
                    Entry e = new Entry();
                    e.uuid = c.getUUID("u");
                    e.name = c.getString("n");
                    e.ticks = c.getInt("t");
                    l.add(e);
                }
                r.courses.put(k, l);
            }
            return r;
        }

        @Override
        public CompoundTag save(CompoundTag t, HolderLookup.Provider reg) {
            courses.forEach((k, l) -> {
                ListTag lt = new ListTag();
                for (Entry e : l) {
                    CompoundTag c = new CompoundTag();
                    c.putUUID("u", e.uuid);
                    c.putString("n", e.name);
                    c.putInt("t", e.ticks);
                    lt.add(c);
                }
                t.put(k, lt);
            });
            return t;
        }
    }

    private RunService() {}
}
