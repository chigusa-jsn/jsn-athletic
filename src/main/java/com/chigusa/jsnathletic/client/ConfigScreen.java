package com.chigusa.jsnathletic.client;

import com.chigusa.jsnathletic.Net;
import com.chigusa.jsnathletic.bar.SpinBarBlockEntity;
import com.chigusa.jsnathletic.mover.MoverBlockEntity;
import com.chigusa.jsnathletic.mover.Params;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 仕掛けの設定画面（仕掛けの台・回るバー） */
public class ConfigScreen extends Screen {
    private final BlockPos pos;
    private int left, top;

    record Row(String label, Supplier<String> value, String key, Supplier<Double> current, double step, boolean cycle) {}

    private final List<Row> rows = new ArrayList<>();

    public ConfigScreen(BlockPos pos) {
        super(Component.literal("仕掛けの設定"));
        this.pos = pos;
    }

    private Object be() {
        return Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getBlockEntity(pos);
    }

    private void send(String key, double v) {
        PacketDistributor.sendToServer(new Net.Config(pos, key, v));
    }

    @Override
    protected void init() {
        rows.clear();
        clearWidgets();
        Object o = be();
        if (o instanceof MoverBlockEntity m) {
            rows.add(new Row("動き方", () -> m.mode == MoverBlockEntity.ROTATE ? "回る" : "往復する", "mode", () -> (double) (1 - m.mode), 0, true));
            rows.add(new Row("足場の数", () -> String.valueOf(m.count), "count", () -> (double) m.count, 1, false));
            if (m.mode == MoverBlockEntity.ROTATE) rows.add(new Row("回る半径", () -> fmt(m.radius) + "マス", "radius", () -> m.radius, 0.5, false));
            else {
                rows.add(new Row("動く向き", () -> Params.AXES[m.axis], "axis", () -> (double) (m.axis + 1), 0, true));
                rows.add(new Row("動く距離", () -> fmt(m.distance) + "マス", "distance", () -> m.distance, 0.5, false));
            }
            rows.add(new Row("速さ", () -> m.mode == MoverBlockEntity.ROTATE ? fmt(m.speed) + "度/秒" : fmt(m.speed) + "マス/秒", "speed", () -> m.speed,
                    m.mode == MoverBlockEntity.ROTATE ? 5 : 0.5, false));
            rows.add(new Row("足場の大きさ", () -> fmt(m.size) + "マス", "size", () -> m.size, 0.5, false));
            rows.add(new Row("高さ", () -> fmt(m.height) + "マス上", "height", () -> m.height, 0.5, false));
            rows.add(new Row("見た目", () -> Params.STYLE_NAMES[Math.floorMod(m.style, Params.STYLES.length)], "style", () -> (double) (m.style + 1), 0, true));
            rows.add(new Row("向き", () -> m.reverse ? "逆まわり" : "ふつう", "reverse", () -> m.reverse ? 0.0 : 1.0, 0, true));
            if (m.mode == MoverBlockEntity.ROTATE) rows.add(new Row("柱とアーム", () -> m.arms ? "見せる" : "かくす", "arms", () -> m.arms ? 0.0 : 1.0, 0, true));
            rows.add(new Row("動かし方", () -> Params.TRIGGERS[m.trigger], "trigger", () -> (double) (m.trigger + 1), 0, true));
            if (m.trigger == MoverBlockEntity.TIMED) rows.add(new Row("動く時間", () -> m.seconds + "秒", "seconds", () -> (double) m.seconds, 1, false));
        } else if (o instanceof SpinBarBlockEntity b) {
            rows.add(new Row("バーの数", () -> String.valueOf(b.bars), "count", () -> (double) b.bars, 1, false));
            rows.add(new Row("バーの長さ", () -> fmt(b.length) + "マス", "radius", () -> b.length, 0.5, false));
            rows.add(new Row("速さ", () -> fmt(b.speed) + "度/秒", "speed", () -> b.speed, 10, false));
            rows.add(new Row("高さ", () -> fmt(b.height) + "マス上", "height", () -> b.height, 0.5, false));
            rows.add(new Row("見た目", () -> Params.STYLE_NAMES[Math.floorMod(b.style, Params.STYLES.length)], "style", () -> (double) (b.style + 1), 0, true));
            rows.add(new Row("向き", () -> b.reverse ? "逆まわり" : "ふつう", "reverse", () -> b.reverse ? 0.0 : 1.0, 0, true));
            rows.add(new Row("動かし方", () -> Params.TRIGGERS[b.trigger], "trigger", () -> (double) (b.trigger + 1), 0, true));
            if (b.trigger == MoverBlockEntity.TIMED) rows.add(new Row("動く時間", () -> b.seconds + "秒", "seconds", () -> (double) b.seconds, 1, false));
        }
        if (o instanceof com.chigusa.jsnathletic.gimmick.PendulumBlockEntity b) {
            rows.add(new Row("くさりの長さ", () -> fmt(b.length) + "マス", "radius", () -> b.length, 0.5, false));
            rows.add(new Row("ゆれの大きさ", () -> fmt(b.amplitude) + "度", "amplitude", () -> b.amplitude, 5, false));
            rows.add(new Row("速さ", () -> fmt(b.speed), "speed", () -> b.speed, 10, false));
            rows.add(new Row("鉄球の大きさ", () -> fmt(b.ball) + "マス", "size", () -> b.ball, 0.5, false));
            rows.add(new Row("ゆれる向き", () -> b.axis == 0 ? "東西（X）" : "南北（Z）", "axis", () -> (double) (b.axis + 1), 0, true));
            rows.add(new Row("タイミングずらし", () -> b.offset + "度", "offset", () -> (double) b.offset, 45, false));
            rows.add(new Row("鉄球の色", () -> new String[]{"黒", "銀", "金"}[Math.floorMod(b.style, 3)], "style", () -> (double) (b.style + 1), 0, true));
            rows.add(new Row("動かし方", () -> Params.TRIGGERS[b.trigger], "trigger", () -> (double) (b.trigger + 1), 0, true));
            if (b.trigger == MoverBlockEntity.TIMED) rows.add(new Row("動く時間", () -> b.seconds + "秒", "seconds", () -> (double) b.seconds, 1, false));
        } else if (o instanceof com.chigusa.jsnathletic.gimmick.RollingBeamBlockEntity b) {
            rows.add(new Row("角材の長さ", () -> fmt(b.length) + "マス", "radius", () -> b.length, 1, false));
            rows.add(new Row("角材の太さ", () -> fmt(b.width) + "マス", "size", () -> b.width, 0.5, false));
            rows.add(new Row("回る速さ", () -> fmt(b.speed) + "度/秒", "speed", () -> b.speed, 5, false));
            rows.add(new Row("高さ", () -> fmt(b.height) + "マス上", "height", () -> b.height, 0.5, false));
            rows.add(new Row("のびる向き", () -> b.axis == 0 ? "東（X）" : "南（Z）", "axis", () -> (double) (b.axis + 1), 0, true));
            rows.add(new Row("見た目", () -> Params.STYLE_NAMES[Math.floorMod(b.style, Params.STYLES.length)], "style", () -> (double) (b.style + 1), 0, true));
            rows.add(new Row("向き", () -> b.reverse ? "逆まわり" : "ふつう", "reverse", () -> b.reverse ? 0.0 : 1.0, 0, true));
            rows.add(new Row("動かし方", () -> Params.TRIGGERS[b.trigger], "trigger", () -> (double) (b.trigger + 1), 0, true));
            if (b.trigger == MoverBlockEntity.TIMED) rows.add(new Row("動く時間", () -> b.seconds + "秒", "seconds", () -> (double) b.seconds, 1, false));
        }
        int w = 260, h = 30 + rows.size() * 22 + 30;
        left = (width - w) / 2;
        top = (height - h) / 2;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int y = top + 26 + i * 22;
            if (r.cycle()) {
                addRenderableWidget(Button.builder(Component.literal("かえる"), btn -> {
                    send(r.key(), r.current().get());
                    later();
                }).bounds(left + w - 60, y, 50, 18).build());
            } else {
                addRenderableWidget(Button.builder(Component.literal("－"), btn -> {
                    send(r.key(), r.current().get() - r.step() * (hasShiftDown() ? 5 : 1));
                    later();
                }).bounds(left + w - 90, y, 30, 18).build());
                addRenderableWidget(Button.builder(Component.literal("＋"), btn -> {
                    send(r.key(), r.current().get() + r.step() * (hasShiftDown() ? 5 : 1));
                    later();
                }).bounds(left + w - 40, y, 30, 18).build());
            }
        }
        if (o instanceof MoverBlockEntity) addRenderableWidget(Button.builder(Component.literal("位置をもどす"), btn -> send("reset", 0))
                .bounds(left + 10, top + 26 + rows.size() * 22 + 4, 90, 18).build());
        addRenderableWidget(Button.builder(Component.literal("とじる"), btn -> onClose()).bounds(left + w - 70, top + 26 + rows.size() * 22 + 4, 60, 18).build());
    }

    private int rebuildIn;

    /** サーバーから新しい設定が届いたら、画面を作りなおす（行の数が変わることがある） */
    private void later() {
        rebuildIn = 4;
    }

    @Override
    public void tick() {
        if (rebuildIn > 0 && --rebuildIn == 0) init();
        if (be() == null) onClose();
    }

    static String fmt(double v) {
        return v == (long) v ? String.valueOf((long) v) : String.valueOf(Math.round(v * 10) / 10.0);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int w = 260, h = 30 + rows.size() * 22 + 30;
        g.fill(left, top, left + w, top + h, 0xE0101820);
        g.renderOutline(left, top, w, h, 0xFF60A0F0);
        super.render(g, mx, my, pt);
        Object o = be();
        String title = o instanceof SpinBarBlockEntity ? "回るバーの設定" : o instanceof com.chigusa.jsnathletic.gimmick.PendulumBlockEntity ? "ふりこの鉄球の設定"
                : o instanceof com.chigusa.jsnathletic.gimmick.RollingBeamBlockEntity ? "回転する角材の設定" : "仕掛けの台の設定";
        g.drawString(font, "§l" + title, left + 10, top + 8, 0xFFFFFF);
        g.drawString(font, "§8Shift＋クリックで5つずつ", left + w - 10 - font.width("Shift＋クリックで5つずつ"), top + 8, 0xFFFFFF);
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int y = top + 26 + i * 22;
            g.drawString(font, "§7" + r.label(), left + 10, y + 5, 0xFFFFFF);
            String v = r.value().get();
            if (r.cycle()) g.drawString(font, v, left + 100, y + 5, 0xFFFFFF);
            else g.drawCenteredString(font, v, left + 260 - 125 - 0, y + 5, 0xFFFFFF);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
