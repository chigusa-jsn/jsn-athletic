package com.chigusa.jsnathletic.mover;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 足場の見た目（ブロックの絵を借りる）と、動かし方の名前 */
public final class Params {
    public static final Block[] STYLES = {Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.LIME_CONCRETE, Blocks.GOLD_BLOCK, Blocks.QUARTZ_BLOCK,
            Blocks.BLUE_ICE, Blocks.RED_CONCRETE, Blocks.SMOOTH_STONE, Blocks.PURPUR_BLOCK, Blocks.MOSS_BLOCK};
    public static final String[] STYLE_NAMES = {"石レンガ", "木", "黄緑", "金", "白", "氷", "赤", "石", "紫", "コケ"};
    public static final String[] TRIGGERS = {"いつも動く", "レッドストーンの間だけ", "ボタンで○秒"};
    public static final String[] AXES = {"東西（X）", "南北（Z）", "上下（Y）"};

    public static BlockState style(int i) {
        return STYLES[Math.floorMod(i, STYLES.length)].defaultBlockState();
    }

    /** 0〜d を行ったり来たり */
    public static double pingpong(double x, double d) {
        if (d <= 0) return 0;
        double m = ((x % (2 * d)) + 2 * d) % (2 * d);
        return m <= d ? m : 2 * d - m;
    }

    private Params() {}
}
