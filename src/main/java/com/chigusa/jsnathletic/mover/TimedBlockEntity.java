package com.chigusa.jsnathletic.mover;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 「いつも動く／レッドストーンの間だけ／ボタンで○秒」と、時刻で進む量（phase）をまとめたもの */
public abstract class TimedBlockEntity extends BlockEntity implements Configurable {
    public int trigger = MoverBlockEntity.ALWAYS, seconds = 10;
    public boolean reverse, running = true, powered;
    public long startTick, stopAt;
    public double base;

    protected TimedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 1ティックで進む量 */
    public abstract double speedPerTick();

    public double rate() {
        return speedPerTick() * (reverse ? -1 : 1);
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

    public void rebase() {
        long now = level == null ? 0 : level.getGameTime();
        base = phaseAt(now);
        startTick = now;
    }

    public void timedTick() {
        long now = level.getGameTime();
        if (trigger == MoverBlockEntity.ALWAYS && !running) setRunning(true, now);
        if (trigger == MoverBlockEntity.TIMED && running && stopAt > 0 && now >= stopAt) {
            stopAt = 0;
            setRunning(false, now);
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

    /** 共通の設定。扱ったら true */
    protected boolean applyCommon(String key, double v) {
        switch (key) {
            case "trigger" -> {
                trigger = Math.floorMod((int) v, 3);
                running = trigger == MoverBlockEntity.ALWAYS;
            }
            case "seconds" -> seconds = (int) Math.max(1, Math.min(120, v));
            case "reverse" -> reverse = v != 0;
            default -> {
                return false;
            }
        }
        return true;
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        t.putInt("trigger", trigger);
        t.putInt("seconds", seconds);
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
        if (!t.contains("trigger")) return;
        trigger = t.getInt("trigger");
        seconds = Math.max(1, t.getInt("seconds"));
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
