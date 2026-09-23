package com.colonizer.colonycard;

import com.colonizer.colonycard.block.ModBlocks;
import com.colonizer.colonycard.config.ModCommonConfig;
import com.colonizer.colonycard.data.ModAttachments;
import com.colonizer.colonycard.item.ModItems;
import com.colonizer.colonycard.network.SyncColonistDataPacket;
import com.colonizer.colonycard.network.stage.AddTaskPacket;
import com.colonizer.colonycard.network.stage.DonatePacket;
import com.colonizer.colonycard.network.stage.RemoveTaskPacket;
import com.colonizer.colonycard.network.stage.SyncStagePacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(ColonyCardMod.MODID)
public class ColonyCardMod {
    public static final String MODID = "colonycard";

    public ColonyCardMod(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC);
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1").optional();
        r.playToClient(SyncColonistDataPacket.TYPE, SyncColonistDataPacket.STREAM_CODEC, SyncColonistDataPacket::handle);
        r.playToClient(SyncStagePacket.TYPE, SyncStagePacket.STREAM_CODEC, SyncStagePacket::handle);
        r.playToServer(DonatePacket.TYPE, DonatePacket.STREAM_CODEC, DonatePacket::handle);
        r.playToServer(AddTaskPacket.TYPE, AddTaskPacket.STREAM_CODEC, AddTaskPacket::handle);
        r.playToServer(RemoveTaskPacket.TYPE, RemoveTaskPacket.STREAM_CODEC, RemoveTaskPacket::handle);
    }
}
