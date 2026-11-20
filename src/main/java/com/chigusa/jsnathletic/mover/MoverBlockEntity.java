package com.chigusa.jsnathletic.mover;

import com.chigusa.jsnathletic.JsnAthletic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 仕掛けの台：上に足場（PlatformEntity）を出して、回したり往復させたりする。
 * 足場の位置は「時刻の式」で決まるので、サーバーでも画面でも同じ場所になる。
 */
public class MoverBlockEntity extends BlockEntity implements PartHost, Configurable {
    public static final int ROTATE = 0, SLIDE = 1;
    public static final int ALWAYS = 0, HOLD = 1, TIMED = 2;

    // ---- 設定 ----
    public int mode = ROTATE, count = 4, style = 0, trigger = ALWAYS, seconds = 10, axis = 0;
    public double radius = 4, distance = 6, speed = 30, size = 2, height = 2;
    public boolean reverse, arms = true;
    // ---- 動き ----
    public boolean running = true, powered;
    public long startTick, stopAt;
    public double base;

    public static final double THICK = 0.5;

    public MoverBlockEntity(BlockPos pos, BlockState state) {
        super(JsnAthletic.MOVER_BE.get(), pos, state);
    }

    /** 1ティックで進む量（回転なら度、往復ならブロック） */
    public double rate() {
        return speed / 20.0 * (reverse ? -1 : 1);
    }

    public double phaseAt(double t) {
        return running && t >= startTick ? base + rate() * (t - startTick) : base;
    }

    public void setRunning(boolean r, long now) {
        if (r == running) return;
        base = phaseAt(now);
        startTick = now;
        running = r;
        sync();
    }

    /** 設定を変える前に、いまの位置を固定しておく */
    public void rebase(long now) {
        base = phaseAt(now);
        startTick = now;
    }

    @Override
    public BlockPos hostPos() {
        return worldPosition;
    }

    @Override
    public int partCount() {
        return count;
    }

    @Override
    public Vec3 partPos(int i, double t) {
        return partPosPhase(i, phaseAt(t));
    }

    @Override
    public double partWidth() {
        return size;
    }

    @Override
    public double partHeight() {
        return THICK;
    }

    @Override
    public double reach() {
        return mode == ROTATE ? radius + size : distance + count * (size + 1) + size;
    }

    /** i 番目の足場の、下の面のまんなか（phase の位置で） */
    public Vec3 partPosPhase(int i, double phase) {
        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 1 + height - THICK, cz = worldPosition.getZ() + 0.5;
        if (mode == ROTATE) {
            double a = Math.toRadians(phase + i * 360.0 / Math.max(1, count));
            return new Vec3(cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius);
        }
        double s = Params.pingpong(phase + (i % 2 == 1 ? distance : 0), distance);
        double gap = size + 0.5;
        return switch (axis) {
            case 1 -> new Vec3(cx + i * gap, cy, cz + s);
            case 2 -> new Vec3(cx + i * gap, cy + s, cz);
            default -> new Vec3(cx + s, cy, cz + i * gap);
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MoverBlockEntity be) {
        long now = level.getGameTime();
        if (be.trigger == ALWAYS && !be.running) be.setRunning(true, now);
        if (be.trigger == TIMED && be.running && be.stopAt > 0 && now >= be.stopAt) {
            be.stopAt = 0;
            be.setRunning(false, now);
        }
        if (now % 20 == 0) be.ensureParts((ServerLevel) level);
    }

    void ensureParts(ServerLevel level) {
        PartHost.ensure(level, this);
    }

    public void onPowered(boolean on) {
        if (level == null || level.isClientSide || on == powered) return;
        powered = on;
        long now = level.getGameTime();
        if (trigger == HOLD) setRunning(on, now);
        else if (trigger == TIMED && on) {
            stopAt = now + seconds * 20L;
            setRunning(true, now);
            sync();
        }
    }

    public void removeParts() {
        if (level instanceof ServerLevel sl) PartHost.removeAll(sl, this);
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        t.putInt("mode", mode);
        t.putInt("count", count);
        t.putInt("style", style);
        t.putInt("trigger", trigger);
        t.putInt("seconds", seconds);
        t.putInt("axis", axis);
        t.putDouble("radius", radius);
        t.putDouble("distance", distance);
        t.putDouble("speed", speed);
        t.putDouble("size", size);
        t.putDouble("height", height);
        t.putBoolean("reverse", reverse);
        t.putBoolean("arms", arms);
        t.putBoolean("running", running);
        t.putBoolean("powered", powered);
        t.putLong("start", startTick);
        t.putLong("stopAt", stopAt);
        t.putDouble("base", base);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.loadAdditional(t, reg);
        if (!t.contains("mode")) return;
        mode = t.getInt("mode");
        count = Math.max(1, t.getInt("count"));
        style = t.getInt("style");
        trigger = t.getInt("trigger");
        seconds = Math.max(1, t.getInt("seconds"));
        axis = t.getInt("axis");
        radius = t.getDouble("radius");
        distance = t.getDouble("distance");
        speed = t.getDouble("speed");
        size = t.getDouble("size");
        height = t.getDouble("height");
        reverse = t.getBoolean("reverse");
        arms = t.getBoolean("arms");
        running = t.getBoolean("running");
        powered = t.getBoolean("powered");
        startTick = t.getLong("start");
        stopAt = t.getLong("stopAt");
        base = t.getDouble("base");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        return saveWithoutMetadata(reg);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 設定の変更（画面から） */
    @Override
    public void apply(String key, double v) {
        long now = level == null ? 0 : level.getGameTime();
        rebase(now);
        switch (key) {
            case "mode" -> mode = (int) v == SLIDE ? SLIDE : ROTATE;
            case "count" -> count = (int) clamp(v, 1, 8);
            case "style" -> style = Math.floorMod((int) v, Params.STYLES.length);
            case "trigger" -> {
                trigger = Math.floorMod((int) v, 3);
                running = trigger == ALWAYS;
                base = phaseAt(now);
            }
            case "seconds" -> seconds = (int) clamp(v, 1, 120);
            case "axis" -> axis = Math.floorMod((int) v, 3);
            case "radius" -> radius = clamp(v, 1, 16);
            case "distance" -> distance = clamp(v, 1, 24);
            case "speed" -> speed = clamp(v, mode == ROTATE ? 5 : 0.5, mode == ROTATE ? 240 : 12);
            case "size" -> size = clamp(v, 1, 6);
            case "height" -> height = clamp(v, -2, 16);
            case "reverse" -> reverse = v != 0;
            case "arms" -> arms = v != 0;
            case "reset" -> {
                base = 0;
                startTick = now;
            }
            default -> {}
        }
        if (level instanceof ServerLevel sl) {
            removeParts();
            ensureParts(sl);
        }
        sync();
    }

    static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }
}
