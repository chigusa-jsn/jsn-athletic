package com.chigusa.jsnathletic.gimmick;

import com.chigusa.jsnathletic.JsnAthletic;
import com.chigusa.jsnathletic.mover.PartHost;
import com.chigusa.jsnathletic.mover.Params;
import com.chigusa.jsnathletic.mover.TimedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 回転する角材：横に長い角材が、自分の長い軸のまわりを回る。
 * 上に乗ると回転で横に流されるので、逆に歩き続けないと落ちる。
 * 当たり判定は、角材の太さの四角い箱を、長さの分だけ並べる。
 */
public class RollingBeamBlockEntity extends TimedBlockEntity implements PartHost {
    public double length = 8, width = 1.5, speed = 45, height = 2;
    public int axis = 0, style = 1;

    public RollingBeamBlockEntity(BlockPos pos, BlockState state) {
        super(JsnAthletic.ROLLING_BEAM_BE.get(), pos, state);
    }

    @Override
    public double speedPerTick() {
        return speed / 20.0;
    }

    /** 長さの向き */
    public Vec3 axisDir() {
        return axis == 0 ? new Vec3(1, 0, 0) : new Vec3(0, 0, 1);
    }

    /** 角材のまんなか（長さの真ん中・太さの真ん中） */
    public Vec3 center() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1 + height + width / 2, worldPosition.getZ() + 0.5)
                .add(axisDir().scale(length / 2));
    }

    @Override
    public BlockPos hostPos() {
        return worldPosition;
    }

    @Override
    public int partCount() {
        return Math.max(1, (int) Math.ceil(length / width));
    }

    @Override
    public Vec3 partPos(int i, double t) {
        int n = partCount();
        double along = (i + 0.5) * length / n;
        Vec3 start = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1 + height, worldPosition.getZ() + 0.5);
        return start.add(axisDir().scale(along));
    }

    @Override
    public double partWidth() {
        return width;
    }

    @Override
    public double partHeight() {
        return width;
    }

    /** 上の面が流れる速さ：回転の速さ × 半径 */
    @Override
    public Vec3 surfaceMotion(int i, double t) {
        if (!running) return Vec3.ZERO;
        double v = Math.toRadians(rate()) * width * 0.6;
        Vec3 side = axis == 0 ? new Vec3(0, 0, 1) : new Vec3(-1, 0, 0);
        return side.scale(v);
    }

    @Override
    public boolean renderParts() {
        return false;
    }

    @Override
    public double reach() {
        return length + 2;
    }

    @Override
    public void apply(String key, double v) {
        rebase();
        if (!applyCommon(key, v)) switch (key) {
            case "radius" -> length = Math.max(2, Math.min(24, v));
            case "size" -> width = Math.max(1, Math.min(4, v));
            case "speed" -> speed = Math.max(5, Math.min(240, v));
            case "height" -> height = Math.max(-1, Math.min(16, v));
            case "axis" -> axis = Math.floorMod((int) v, 2);
            case "style" -> style = Math.floorMod((int) v, Params.STYLES.length);
            default -> {}
        }
        if (level instanceof ServerLevel sl) {
            PartHost.removeAll(sl, this);
            PartHost.ensure(sl, this);
        }
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        t.putDouble("length", length);
        t.putDouble("width", width);
        t.putDouble("speed", speed);
        t.putDouble("height", height);
        t.putInt("axis", axis);
        t.putInt("style", style);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.loadAdditional(t, reg);
        if (!t.contains("length")) return;
        length = t.getDouble("length");
        width = t.getDouble("width");
        speed = t.getDouble("speed");
        height = t.getDouble("height");
        axis = t.getInt("axis");
        style = t.getInt("style");
    }
}
