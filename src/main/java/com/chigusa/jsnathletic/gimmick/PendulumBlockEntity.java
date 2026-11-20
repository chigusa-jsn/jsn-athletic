package com.chigusa.jsnathletic.gimmick;

import com.chigusa.jsnathletic.JsnAthletic;
import com.chigusa.jsnathletic.mover.TimedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** ふりこの鉄球：台の下にぶら下がって、左右にゆれる。当たると はじき飛ばされる */
public class PendulumBlockEntity extends TimedBlockEntity {
    public static final Set<PendulumBlockEntity> CLIENT = Collections.newSetFromMap(new WeakHashMap<>());
    public double length = 5, amplitude = 60, speed = 90, ball = 1.5;
    public int axis = 0, offset = 0, style = 0;

    public PendulumBlockEntity(BlockPos pos, BlockState state) {
        super(JsnAthletic.PENDULUM_BE.get(), pos, state);
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

    @Override
    public double speedPerTick() {
        return speed / 20.0;
    }

    /** ゆれの角度（度） */
    public double angle(double t) {
        return amplitude * Math.sin(Math.toRadians(phaseAt(t) + offset));
    }

    /** ゆれる向き */
    public Vec3 swingDir() {
        return axis == 0 ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
    }

    /** 鉄球のまんなか */
    public Vec3 ballPos(double t) {
        double a = Math.toRadians(angle(t));
        Vec3 pivot = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
        return pivot.add(swingDir().scale(Math.sin(a) * length)).add(0, -Math.cos(a) * length, 0);
    }

    @Override
    public void apply(String key, double v) {
        rebase();
        if (!applyCommon(key, v)) switch (key) {
            case "radius" -> length = Math.max(2, Math.min(16, v));
            case "amplitude" -> amplitude = Math.max(10, Math.min(85, v));
            case "speed" -> speed = Math.max(20, Math.min(360, v));
            case "size" -> ball = Math.max(1, Math.min(3, v));
            case "axis" -> axis = Math.floorMod((int) v, 2);
            case "offset" -> offset = Math.floorMod((int) v, 360);
            case "style" -> style = Math.floorMod((int) v, 3);
            default -> {}
        }
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        t.putDouble("length", length);
        t.putDouble("amplitude", amplitude);
        t.putDouble("speed", speed);
        t.putDouble("ball", ball);
        t.putInt("axis", axis);
        t.putInt("offset", offset);
        t.putInt("style", style);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.loadAdditional(t, reg);
        if (!t.contains("length")) return;
        length = t.getDouble("length");
        amplitude = t.getDouble("amplitude");
        speed = t.getDouble("speed");
        ball = t.getDouble("ball");
        axis = t.getInt("axis");
        offset = t.getInt("offset");
        style = t.getInt("style");
    }
}
