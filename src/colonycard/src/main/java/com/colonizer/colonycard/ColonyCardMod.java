package com.colonizer.colonycard;

import com.colonizer.colonycard.data.ModAttachments;
import com.colonizer.colonycard.network.SyncColonistDataPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(ColonyCardMod.MODID)
public class ColonyCardMod {
    public static final String MODID = "colonycard";

    public ColonyCardMod(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1").optional();
        r.playToClient(SyncColonistDataPacket.TYPE, SyncColonistDataPacket.STREAM_CODEC, SyncColonistDataPacket::handle);
    }
}
