package com.colonizer.colonycard.network;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.client.ClientColonistCache;
import com.colonizer.colonycard.data.ColonistData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Server -> client. Pushes the receiving player's own colonist data. */
public class SyncColonistDataPacket implements CustomPacketPayload {
    public static final Type<SyncColonistDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "sync_colonist_data"));

    public static final StreamCodec<FriendlyByteBuf, SyncColonistDataPacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncColonistDataPacket::encode, SyncColonistDataPacket::decode);

    public final String playerName;
    public final ColonistData data;

    public SyncColonistDataPacket(String playerName, ColonistData data) {
        this.playerName = playerName;
        this.data = data;
    }

    @Override
    public Type<SyncColonistDataPacket> type() {
        return TYPE;
    }

    public static void encode(SyncColonistDataPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName, 64);
        buf.writeUtf(msg.data.colonizerName(), 64);
        buf.writeVarLong(msg.data.arrivalDate());
        buf.writeUtf(msg.data.arrivalGoal(), 256);
        List<String> traits = msg.data.traits();
        buf.writeVarInt(traits.size());
        for (String t : traits) {
            buf.writeUtf(t, 64);
        }
        buf.writeVarInt(msg.data.loyalty());
        buf.writeVarInt(msg.data.contribution());
        buf.writeVarInt(msg.data.rewardsMask());
        buf.writeUtf(msg.data.archipelago(), 64);
    }

    public static SyncColonistDataPacket decode(FriendlyByteBuf buf) {
        String playerName = buf.readUtf(64);
        String colonizerName = buf.readUtf(64);
        long arrivalDate = buf.readVarLong();
        String arrivalGoal = buf.readUtf(256);
        int traitCount = Math.min(buf.readVarInt(), 32);
        List<String> traits = new ArrayList<>();
        for (int i = 0; i < traitCount; i++) {
            traits.add(buf.readUtf(64));
        }
        int loyalty = buf.readVarInt();
        int contribution = buf.readVarInt();
        int rewardsMask = buf.readVarInt();
        String archipelago = buf.readUtf(64);
        ColonistData data = new ColonistData(colonizerName, arrivalDate, arrivalGoal, traits, loyalty, contribution, rewardsMask, archipelago);
        return new SyncColonistDataPacket(playerName, data);
    }

    public static void handle(SyncColonistDataPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientColonistCache.apply(msg.playerName, msg.data));
    }
}
