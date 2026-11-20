package com.chigusa.jsnathletic.bar;

import com.chigusa.jsnathletic.JsnAthletic;
import com.chigusa.jsnathletic.mover.MoverBlockEntity;
import com.chigusa.jsnathletic.mover.Params;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** 回るバー：水平に回る棒。当たると はじき飛ばされる */
public class SpinBarBlockEntity extends BlockEntity {
    /** 画面側で、近くのバーを探すための一覧 */
    public static final Set<SpinBarBlockEntity> CLIENT = Collections.newSetFromMap(new WeakHashMap<>());

    public int bars = 2, style = 6, trigger = MoverBlockEntity.ALWAYS, seconds = 10;
    public double length = 5, speed = 60, height = 1;
    public boolean reverse;
    public boolean running = true, powered;
    public long startTick, stopAt;
    public double base;

    public static final double THICK = 0.4;

    public SpinBarBlockEntity(BlockPos pos, BlockState state) {
        super(JsnAthletic.SPIN_BAR_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) CLIENT.add(this);
    }

    @Override
    public void setRemoved() {
        CLIENT.remove(this);
        super.setRemoved();
    }

    public double rate() {
        return speed / 20.0 * (reverse ? -1 : 1);
    }

    public double angleAt(double t) {
        return running && t >= startTick ? base + rate() * (t - startTick) : base;
    }

    public void setRunning(boolean r, long now) {
        if (r == running) return;
        base = angleAt(now);
        startTick = now;
        running = r;
        sync();
    }

    /** バーの高さのまんなか */
    public double barY() {
        return worldPosition.getY() + 1 + height + THICK / 2;
    }

    public Vec3 center() {
        return new Vec3(worldPosition.getX() + 0.5, barY(), worldPosition.getZ() + 0.5);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpinBarBlockEntity be) {
        long now = level.getGameTime();
        if (be.trigger == MoverBlockEntity.ALWAYS && !be.running) be.setRunning(true, now);
        if (be.trigger == MoverBlockEntity.TIMED && be.running && be.stopAt > 0 && now >= be.stopAt) {
            be.stopAt = 0;
            be.setRunning(false, now);
        }
    }

    public void onPowered(boolean on) {
        if (level == null || level.isClientSide || on == powered) return;
        powered = on;
        long now = level.getGameTime();
        if (trigger == MoverBlockEntity.HOLD) setRunning(on, now);
        else if (trigger == MoverBlockEntity.TIMED && on) {
            stopAt = now + seconds * 20L;
            setRunning(true, now);
            sync();
        }
    }

    public void apply(String key, double v) {
        long now = level == null ? 0 : level.getGameTime();
        base = angleAt(now);
        startTick = now;
        switch (key) {
            case "count" -> bars = (int) Math.max(1, Math.min(4, v));
            case "style" -> style = Math.floorMod((int) v, Params.STYLES.length);
            case "trigger" -> {
                trigger = Math.floorMod((int) v, 3);
                running = trigger == MoverBlockEntity.ALWAYS;
            }
            case "seconds" -> seconds = (int) Math.max(1, Math.min(120, v));
            case "radius" -> length = Math.max(1, Math.min(16, v));
            case "speed" -> speed = Math.max(5, Math.min(360, v));
            case "height" -> height = Math.max(-1, Math.min(8, v));
            case "reverse" -> reverse = v != 0;
            default -> {}
        }
        sync();
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        t.putInt("bars", bars);
        t.putInt("style", style);
        t.putInt("trigger", trigger);
        t.putInt("seconds", seconds);
        t.putDouble("length", length);
        t.putDouble("speed", speed);
        t.putDouble("height", height);
        t.putBoolean("reverse", reverse);
        t.putBoolean("running", running);
        t.putBoolean("powered", powered);
        t.putLong("start", startTick);
        t.putLong("stopAt", stopAt);
        t.putDouble("base", base);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.loadAdditional(t, reg);
        if (!t.contains("bars")) return;
        bars = Math.max(1, t.getInt("bars"));
        style = t.getInt("style");
        trigger = t.getInt("trigger");
        seconds = Math.max(1, t.getInt("seconds"));
        length = t.getDouble("length");
        speed = t.getDouble("speed");
        height = t.getDouble("height");
        reverse = t.getBoolean("reverse");
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
}
