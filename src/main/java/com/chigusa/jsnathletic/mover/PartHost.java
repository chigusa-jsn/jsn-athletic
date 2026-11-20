package com.chigusa.jsnathletic.mover;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 乗れる箱（PlatformEntity）を出す仕掛け：回る足場・動く床・回転する角材 */
public interface PartHost {
    BlockPos hostPos();

    int partCount();

    /** i 番目の箱の、下の面のまんなか（時刻 t） */
    Vec3 partPos(int i, double t);

    double partWidth();

    double partHeight();

    /** 上に乗っている人を、1ティックでどれだけ流すか（足場の動きとは別。角材の回転など） */
    default Vec3 surfaceMotion(int i, double t) {
        return Vec3.ZERO;
    }

    /** 箱をエンティティの絵で描くか（角材はブロックのほうで描く） */
    default boolean renderParts() {
        return true;
    }

    double reach();

    /** 箱の数をそろえる（足りなければ出す、多ければ消す） */
    static void ensure(ServerLevel level, PartHost h) {
        BlockPos pos = h.hostPos();
        AABB box = new AABB(pos).inflate(h.reach() + 3);
        List<PlatformEntity> list = level.getEntitiesOfClass(PlatformEntity.class, box, e -> pos.equals(e.controller()));
        int n = h.partCount();
        boolean[] have = new boolean[n];
        for (PlatformEntity e : list) {
            int i = e.index();
            if (i >= n || have[i]) e.discard();
            else have[i] = true;
        }
        for (int i = 0; i < n; i++) if (!have[i]) {
            PlatformEntity e = new PlatformEntity(com.chigusa.jsnathletic.JsnAthletic.PLATFORM.get(), level);
            e.setup(pos, i);
            Vec3 p = h.partPos(i, level.getGameTime());
            e.setPos(p.x, p.y, p.z);
            level.addFreshEntity(e);
        }
    }

    static void removeAll(ServerLevel level, PartHost h) {
        BlockPos pos = h.hostPos();
        for (PlatformEntity e : level.getEntitiesOfClass(PlatformEntity.class, new AABB(pos).inflate(h.reach() + 4), e -> pos.equals(e.controller())))
            e.discard();
    }
}
