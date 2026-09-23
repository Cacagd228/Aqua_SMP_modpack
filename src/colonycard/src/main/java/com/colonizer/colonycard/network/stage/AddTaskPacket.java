package com.colonizer.colonycard.network.stage;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.stage.CrownStageManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server. Добавить задачу (только админы, уровень 2). */
public class AddTaskPacket implements CustomPacketPayload {
    public static final Type<AddTaskPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "stage_add_task"));

    public static final StreamCodec<FriendlyByteBuf, AddTaskPacket> STREAM_CODEC =
            StreamCodec.ofMember(AddTaskPacket::encode, AddTaskPacket::decode);

    public final String itemId;
    public final int count;

    public AddTaskPacket(String itemId, int count) {
        this.itemId = itemId;
        this.count = count;
    }

    @Override
    public Type<AddTaskPacket> type() {
        return TYPE;
    }

    public static void encode(AddTaskPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.itemId, 128);
        buf.writeVarInt(msg.count);
    }

    public static AddTaskPacket decode(FriendlyByteBuf buf) {
        return new AddTaskPacket(buf.readUtf(128), buf.readVarInt());
    }

    public static void handle(AddTaskPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && player.hasPermissions(2)) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(net.minecraft.resources.ResourceLocation.parse(msg.itemId));
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    CrownStageManager.addTask(player.getServer(), msg.itemId, Math.max(1, msg.count));
                }
            }
        });
    }
}
