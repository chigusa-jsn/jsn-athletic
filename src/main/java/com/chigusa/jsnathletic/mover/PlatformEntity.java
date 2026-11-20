package com.chigusa.jsnathletic.mover;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 乗れる足場。ボートやシュルカーと同じように、上に立てる。
 * 場所は仕掛けの台（MoverBlockEntity）の式で毎ティック決める。
 */
public class PlatformEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> CONTROLLER = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Integer> INDEX = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.INT);
    private float lastSize = -1, lastThick = -1;

    public PlatformEntity(EntityType<? extends PlatformEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setup(BlockPos controller, int index) {
        entityData.set(CONTROLLER, controller);
        entityData.set(INDEX, index);
    }

    public BlockPos controller() {
        return entityData.get(CONTROLLER);
    }

    public int index() {
        return entityData.get(INDEX);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(CONTROLLER, BlockPos.ZERO);
        b.define(INDEX, 0);
    }

    public PartHost host() {
        return level().getBlockEntity(controller()) instanceof PartHost m ? m : null;
    }

    /** 回る足場・動く床なら、その台 */
    public MoverBlockEntity mover() {
        return host() instanceof MoverBlockEntity m ? m : null;
    }

    /** 時刻 t のときの場所 */
    public Vec3 posAt(double t) {
        PartHost m = host();
        return m == null ? position() : m.partPos(index(), t);
    }

    public double size() {
        PartHost m = host();
        return m == null ? 1 : m.partWidth();
    }

    public double thick() {
        PartHost m = host();
        return m == null ? 0.5 : m.partHeight();
    }

    /** 時刻 t のときの当たり判定の箱 */
    public AABB boxAt(double t) {
        Vec3 p = posAt(t);
        double h = size() / 2;
        return new AABB(p.x - h, p.y, p.z - h, p.x + h, p.y + thick(), p.z + h);
    }

    @Override
    public void tick() {
        PartHost m = host();
        if (m == null) {
            if (!level().isClientSide && level().isLoaded(controller())) discard();
            return;
        }
        if (!level().isClientSide && index() >= m.partCount()) {
            discard();
            return;
        }
        if (lastSize != (float) m.partWidth() || lastThick != (float) m.partHeight()) {
            lastSize = (float) m.partWidth();
            lastThick = (float) m.partHeight();
            refreshDimensions();
        }
        Vec3 p = posAt(level().getGameTime());
        setPos(p.x, p.y, p.z);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed((float) size(), (float) thick());
    }

    // サーバーからの場所の知らせは使わない（自分で式から計算する）
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;   // 台がまた出してくれる
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 128 * 128;
    }
}
