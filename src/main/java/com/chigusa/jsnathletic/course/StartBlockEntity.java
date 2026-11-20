package com.chigusa.jsnathletic.course;

import com.chigusa.jsnathletic.JsnAthletic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** スタート：コースの名前 */
public class StartBlockEntity extends BlockEntity {
    public String name = "";

    public StartBlockEntity(BlockPos pos, BlockState state) {
        super(JsnAthletic.START_BE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.saveAdditional(t, reg);
        t.putString("name", name);
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider reg) {
        super.loadAdditional(t, reg);
        name = t.getString("name");
    }
}
