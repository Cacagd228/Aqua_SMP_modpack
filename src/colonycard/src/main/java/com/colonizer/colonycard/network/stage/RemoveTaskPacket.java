package com.colonizer.colonycard.network.stage;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.stage.CrownStageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server. Удалить задачу (только админы, уровень 2). */
public class RemoveTaskPacket implements CustomPacketPayload {
    public static final Type<RemoveTaskPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "stage_remove_task"));

    public static final StreamCodec<FriendlyByteBuf, RemoveTaskPacket> STREAM_CODEC =
            StreamCodec.ofMember(RemoveTaskPacket::encode, RemoveTaskPacket::decode);

    public final int index;

    public RemoveTaskPacket(int index) {
        this.index = index;
    }

    @Override
    public Type<RemoveTaskPacket> type() {
        return TYPE;
    }

    public static void encode(RemoveTaskPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.index);
    }

    public static RemoveTaskPacket decode(FriendlyByteBuf buf) {
        return new RemoveTaskPacket(buf.readVarInt());
    }

    public static void handle(RemoveTaskPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.hasPermissions(2)) {
                CrownStageManager.removeTask(player.getServer(), msg.index);
            }
        });
    }
}
