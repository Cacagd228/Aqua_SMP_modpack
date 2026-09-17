package at.petrak.hexcasting.forge.network;

import at.petrak.hexcasting.common.msgs.*;
import me.nanorasmus.nanodev.hex_js.network.MsgAnnouncementS2C;
import me.nanorasmus.nanodev.hex_js.network.MsgCopyPatternC2S;
import me.nanorasmus.nanodev.hex_js.network.MsgSilenceDenyS2C;
import me.nanorasmus.nanodev.hex_js.network.MsgUnworthyS2C;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.Consumer;
import java.util.function.Function;

public class ForgePacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static void init(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);

        server(registrar, MsgNewSpellPatternC2S.ID, MsgNewSpellPatternC2S::deserialize, MsgNewSpellPatternC2S::handle);
        server(registrar, MsgShiftScrollC2S.ID, MsgShiftScrollC2S::deserialize, MsgShiftScrollC2S::handle);
        server(registrar, MsgCopyPatternC2S.ID, MsgCopyPatternC2S::deserialize, MsgCopyPatternC2S::handle);

        client(registrar, MsgNewSpellPatternS2C.ID, MsgNewSpellPatternS2C::deserialize, MsgNewSpellPatternS2C::handle);
        client(registrar, MsgBlinkS2C.ID, MsgBlinkS2C::deserialize, MsgBlinkS2C::handle);
        client(registrar, MsgSentinelStatusUpdateAck.ID, MsgSentinelStatusUpdateAck::deserialize, MsgSentinelStatusUpdateAck::handle);
        client(registrar, MsgPigmentUpdateAck.ID, MsgPigmentUpdateAck::deserialize, MsgPigmentUpdateAck::handle);
        client(registrar, MsgAltioraUpdateAck.ID, MsgAltioraUpdateAck::deserialize, MsgAltioraUpdateAck::handle);
        client(registrar, MsgCastParticleS2C.ID, MsgCastParticleS2C::deserialize, MsgCastParticleS2C::handle);
        client(registrar, MsgOpenSpellGuiS2C.ID, MsgOpenSpellGuiS2C::deserialize, MsgOpenSpellGuiS2C::handle);
        client(registrar, MsgBeepS2C.ID, MsgBeepS2C::deserialize, MsgBeepS2C::handle);
        client(registrar, MsgBrainsweepAck.ID, MsgBrainsweepAck::deserialize, MsgBrainsweepAck::handle);
        client(registrar, MsgNewWallScrollS2C.ID, MsgNewWallScrollS2C::deserialize, MsgNewWallScrollS2C::handle);
        client(registrar, MsgRecalcWallScrollDisplayS2C.ID, MsgRecalcWallScrollDisplayS2C::deserialize, MsgRecalcWallScrollDisplayS2C::handle);
        client(registrar, MsgNewSpiralPatternsS2C.ID, MsgNewSpiralPatternsS2C::deserialize, MsgNewSpiralPatternsS2C::handle);
        client(registrar, MsgClearSpiralPatternsS2C.ID, MsgClearSpiralPatternsS2C::deserialize, MsgClearSpiralPatternsS2C::handle);
        client(registrar, MsgAnnouncementS2C.ID, MsgAnnouncementS2C::deserialize, MsgAnnouncementS2C::handle);
        client(registrar, MsgSilenceDenyS2C.ID, MsgSilenceDenyS2C::deserialize, MsgSilenceDenyS2C::handle);
        client(registrar, MsgUnworthyS2C.ID, MsgUnworthyS2C::deserialize, MsgUnworthyS2C::handle);
    }

    public static void sendToPlayer(ServerPlayer player, IMessage message) {
        PacketDistributor.sendToPlayer(player, payload(message));
    }

    public static void sendToServer(IMessage message) {
        PacketDistributor.sendToServer(payload(message));
    }

    public static void sendNear(ServerPlayer excluded, double x, double y, double z, double radius,
        net.minecraft.server.level.ServerLevel level, IMessage message) {
        PacketDistributor.sendToPlayersNear(level, excluded, x, y, z, radius, payload(message));
    }

    public static void sendTracking(net.minecraft.world.entity.Entity entity, IMessage message) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload(message));
    }

    @SuppressWarnings("unchecked")
    public static Packet<ClientGamePacketListener> toVanillaClientboundPacket(IMessage message) {
        return (Packet<ClientGamePacketListener>) (Packet<?>) new ClientboundCustomPayloadPacket(payload(message));
    }

    private static <T extends IMessage> void client(PayloadRegistrar registrar, ResourceLocation id,
        Function<FriendlyByteBuf, T> decoder, Consumer<T> handler) {
        var type = new CustomPacketPayload.Type<MessagePayload<T>>(id);
        registrar.playToClient(type, codec(type, decoder), (payload, context) ->
            context.enqueueWork(() -> handler.accept(payload.message())));
    }

    private static <T extends IMessage> void server(PayloadRegistrar registrar, ResourceLocation id,
        Function<FriendlyByteBuf, T> decoder, TriConsumer<T, MinecraftServer, ServerPlayer> handler) {
        var type = new CustomPacketPayload.Type<MessagePayload<T>>(id);
        registrar.playToServer(type, codec(type, decoder), (payload, context) ->
            context.enqueueWork(() -> {
                var player = (ServerPlayer) context.player();
                handler.accept(payload.message(), player.getServer(), player);
            }));
    }

    private static <T extends IMessage> StreamCodec<RegistryFriendlyByteBuf, MessagePayload<T>> codec(
        CustomPacketPayload.Type<MessagePayload<T>> type, Function<FriendlyByteBuf, T> decoder) {
        return StreamCodec.of(
            (buf, payload) -> payload.message().serialize(buf),
            buf -> new MessagePayload<>(type, decoder.apply(buf))
        );
    }

    private static CustomPacketPayload payload(IMessage message) {
        return new MessagePayload<>(new CustomPacketPayload.Type<>(message.id()), message);
    }

    private record MessagePayload<T extends IMessage>(CustomPacketPayload.Type<MessagePayload<T>> type,
                                                      T message) implements CustomPacketPayload {
    }
}
