package com.colonizer.colonycard.network;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.client.ClientDebugIdentity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -&gt; all clients. Вкл/выкл отладочного режима личности. */
public class SyncDebugIdentityPacket implements CustomPacketPayload {
    public static final Type<SyncDebugIdentityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "sync_debug_identity"));

    public static final StreamCodec<FriendlyByteBuf, SyncDebugIdentityPacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncDebugIdentityPacket::encode, SyncDebugIdentityPacket::decode);

    public final boolean enabled;

    public SyncDebugIdentityPacket(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Type<SyncDebugIdentityPacket> type() {
        return TYPE;
    }

    public static void encode(SyncDebugIdentityPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static SyncDebugIdentityPacket decode(FriendlyByteBuf buf) {
        return new SyncDebugIdentityPacket(buf.readBoolean());
    }

    public static void handle(SyncDebugIdentityPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientDebugIdentity.setEnabled(msg.enabled));
    }
}
