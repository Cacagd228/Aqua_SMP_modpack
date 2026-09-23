package com.colonizer.colonycard.network.stage;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.client.ClientStageCache;
import com.colonizer.colonycard.stage.StageTask;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Server -> client. Состояние этапа «Запросы короны». */
public class SyncStagePacket implements CustomPacketPayload {
    public static final Type<SyncStagePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "sync_stage"));

    public static final StreamCodec<FriendlyByteBuf, SyncStagePacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncStagePacket::encode, SyncStagePacket::decode);

    public final List<StageTask> tasks;
    public final boolean openScreen;

    public SyncStagePacket(List<StageTask> tasks, boolean openScreen) {
        this.tasks = List.copyOf(tasks);
        this.openScreen = openScreen;
    }

    @Override
    public Type<SyncStagePacket> type() {
        return TYPE;
    }

    public static void encode(SyncStagePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.tasks.size());
        for (StageTask t : msg.tasks) {
            buf.writeUtf(t.itemId(), 128);
            buf.writeVarInt(t.needed());
            buf.writeVarInt(t.donated());
        }
        buf.writeBoolean(msg.openScreen);
    }

    public static SyncStagePacket decode(FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), 1024);
        List<StageTask> tasks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            tasks.add(new StageTask(buf.readUtf(128), buf.readVarInt(), buf.readVarInt()));
        }
        return new SyncStagePacket(tasks, buf.readBoolean());
    }

    public static void handle(SyncStagePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientStageCache.apply(msg));
    }
}
