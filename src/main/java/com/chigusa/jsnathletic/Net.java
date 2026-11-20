package com.chigusa.jsnathletic;

import com.chigusa.jsnathletic.bar.SpinBarBlockEntity;
import com.chigusa.jsnathletic.course.RunService;
import com.chigusa.jsnathletic.mover.MoverBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 通信：設定の変更・Rキー・タイムの表示 */
public final class Net {
    public record Config(BlockPos pos, String key, double value) implements CustomPacketPayload {
        public static final Type<Config> TYPE = new Type<>(JsnAthletic.id("config"));
        public static final StreamCodec<ByteBuf, Config> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, Config::pos,
                ByteBufCodecs.stringUtf8(32), Config::key, ByteBufCodecs.DOUBLE, Config::value, Config::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 0=もどる 1=やめる */
    public record Key(int action) implements CustomPacketPayload {
        public static final Type<Key> TYPE = new Type<>(JsnAthletic.id("key"));
        public static final StreamCodec<ByteBuf, Key> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Key::action, Key::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** タイムの表示：running=計測中、elapsed=いまの経過（ティック）、done=ゴールした直後 */
    public record Hud(boolean running, int elapsed, int best, String course, int falls, boolean done) implements CustomPacketPayload {
        public static final Type<Hud> TYPE = new Type<>(JsnAthletic.id("hud"));
        public static final StreamCodec<ByteBuf, Hud> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Hud::running, ByteBufCodecs.VAR_INT, Hud::elapsed,
                ByteBufCodecs.VAR_INT, Hud::best, ByteBufCodecs.stringUtf8(128), Hud::course, ByteBufCodecs.VAR_INT, Hud::falls, ByteBufCodecs.BOOL, Hud::done, Hud::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 画面で受けとる（クライアントでセット） */
    public static java.util.function.Consumer<Hud> onHud = h -> {};

    static void register(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToServer(Config.TYPE, Config.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> {
            if (!(c.player() instanceof ServerPlayer sp) || !JsnAthletic.canEdit(sp) || sp.blockPosition().distSqr(p.pos()) > 64 * 64) return;
            if (sp.level().getBlockEntity(p.pos()) instanceof com.chigusa.jsnathletic.mover.Configurable c) c.apply(p.key(), p.value());
        }));
        r.playToServer(Key.TYPE, Key.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> {
            if (c.player() instanceof ServerPlayer sp) {
                if (p.action() == 1) RunService.quit(sp);
                else RunService.retry(sp);
            }
        }));
        r.playToClient(Hud.TYPE, Hud.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> onHud.accept(p)));
    }

    private Net() {}
}
