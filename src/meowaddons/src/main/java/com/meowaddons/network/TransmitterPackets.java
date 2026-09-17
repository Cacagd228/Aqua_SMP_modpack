package com.meowaddons.network;

import com.meowaddons.MeowAddons;
import com.meowaddons.transmitter.TransmitterBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class TransmitterPackets {

    /** C2S: игрок задал адрес передатчику через меню в стиле Frogport. */
    public record SetAddress(BlockPos pos, String address) implements CustomPacketPayload {
        public static final Type<SetAddress> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "transmitter_set_address"));
        public static final StreamCodec<ByteBuf, SetAddress> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, SetAddress::pos,
                ByteBufCodecs.STRING_UTF8, SetAddress::address,
                SetAddress::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @EventBusSubscriber(modid = MeowAddons.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToServer(SetAddress.TYPE, SetAddress.STREAM_CODEC, (payload, ctx) -> {
                ServerPlayer player = (ServerPlayer) ctx.player();
                if (player.distanceToSqr(payload.pos().getX() + 0.5,
                        payload.pos().getY() + 0.5,
                        payload.pos().getZ() + 0.5) > 8 * 8) {
                    return;
                }
                if (player.level().getBlockEntity(payload.pos()) instanceof TransmitterBlockEntity transmitter) {
                    transmitter.setOwnAddress(payload.address());
                    player.level().playSound(null, payload.pos(),
                            SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.7f, 1.2f);
                }
            });
        }
    }
}
