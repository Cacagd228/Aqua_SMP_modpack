package com.colonizer.colonycard.network.stage;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.stage.CrownStageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server. Пожертвовать предметы в задачу этапа. */
public class DonatePacket implements CustomPacketPayload {
    public static final Type<DonatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "stage_donate"));

    public static final StreamCodec<FriendlyByteBuf, DonatePacket> STREAM_CODEC =
            StreamCodec.ofMember(DonatePacket::encode, DonatePacket::decode);

    public final int index;
    public final int amount;

    public DonatePacket(int index, int amount) {
        this.index = index;
        this.amount = amount;
    }

    @Override
    public Type<DonatePacket> type() {
        return TYPE;
    }

    public static void encode(DonatePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.index);
        buf.writeVarInt(msg.amount);
    }

    public static DonatePacket decode(FriendlyByteBuf buf) {
        return new DonatePacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(DonatePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                CrownStageManager.donate(player, msg.index, msg.amount);
            }
        });
    }
}
