package com.colonizer.colonycard.network;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.client.ClientNameplateCache;
import com.colonizer.colonycard.identity.FaceCover;
import com.colonizer.colonycard.item.PassportItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Server -&gt; client. Текущий Display цели.
 *
 * <p>Истинный ник НЕ передается: для {@code DEFAULT/HIDDEN} имя вообще не нужно,
 * для {@code PASSPORT_ONLY/TRUE_PLUS} едет только паспортное имя, а ванильную
 * часть клиент берет из стандартного nameplate ({@code event.getContent()}).
 */
public class SyncNameplatePacket implements CustomPacketPayload {
    public static final Type<SyncNameplatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "sync_nameplate"));

    public static final StreamCodec<FriendlyByteBuf, SyncNameplatePacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncNameplatePacket::encode, SyncNameplatePacket::decode);

    public final UUID target;
    public final byte mode;
    public final String passportName;

    public SyncNameplatePacket(UUID target, byte mode, String passportName) {
        this.target = target;
        this.mode = mode;
        this.passportName = passportName == null ? "" : passportName;
    }

    public SyncNameplatePacket(UUID target, FaceCover.Resolved resolved) {
        this(target, resolved.mode(), resolved.passportName());
    }

    @Override
    public Type<SyncNameplatePacket> type() {
        return TYPE;
    }

    public static void encode(SyncNameplatePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.target);
        buf.writeByte(msg.mode);
        buf.writeUtf(msg.passportName, PassportItem.MAX_NAME);
    }

    public static SyncNameplatePacket decode(FriendlyByteBuf buf) {
        UUID target = buf.readUUID();
        byte mode = buf.readByte();
        String passportName = buf.readUtf(PassportItem.MAX_NAME);
        return new SyncNameplatePacket(target, mode, passportName);
    }

    public static void handle(SyncNameplatePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientNameplateCache.apply(msg.target, msg.mode, msg.passportName));
    }
}
