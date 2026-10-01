package com.colonizer.colonycard;

import com.colonizer.colonycard.block.ModBlocks;
import com.colonizer.colonycard.config.ModCommonConfig;
import com.colonizer.colonycard.data.ModAttachments;
import com.colonizer.colonycard.data.ModEffects;
import com.colonizer.colonycard.item.ModItems;
import com.colonizer.colonycard.network.SyncColonistDataPacket;
import com.colonizer.colonycard.network.SyncDebugIdentityPacket;
import com.colonizer.colonycard.network.SyncNameplatePacket;
import com.colonizer.colonycard.network.stage.AddTaskPacket;
import com.colonizer.colonycard.network.stage.DonatePacket;
import com.colonizer.colonycard.network.stage.RemoveTaskPacket;
import com.colonizer.colonycard.network.stage.SyncStagePacket;
import com.colonizer.colonycard.network.trade.CreateLotPacket;
import com.colonizer.colonycard.network.trade.RemoveLotPacket;
import com.colonizer.colonycard.network.trade.SyncTradePacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

@Mod(ColonyCardMod.MODID)
public class ColonyCardMod {
    public static final String MODID = "colonycard";

    public ColonyCardMod(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (ModList.get().isLoaded("curios")) {
                CuriosApi.registerCurio(ModItems.BALACLAVA.get(), (ICurioItem) ModItems.BALACLAVA.get());
            }
        });
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1").optional();
        r.playToClient(SyncColonistDataPacket.TYPE, SyncColonistDataPacket.STREAM_CODEC, SyncColonistDataPacket::handle);
        r.playToClient(SyncNameplatePacket.TYPE, SyncNameplatePacket.STREAM_CODEC, SyncNameplatePacket::handle);
        r.playToClient(SyncDebugIdentityPacket.TYPE, SyncDebugIdentityPacket.STREAM_CODEC, SyncDebugIdentityPacket::handle);
        r.playToClient(SyncStagePacket.TYPE, SyncStagePacket.STREAM_CODEC, SyncStagePacket::handle);
        r.playToServer(DonatePacket.TYPE, DonatePacket.STREAM_CODEC, DonatePacket::handle);
        r.playToServer(AddTaskPacket.TYPE, AddTaskPacket.STREAM_CODEC, AddTaskPacket::handle);
        r.playToServer(RemoveTaskPacket.TYPE, RemoveTaskPacket.STREAM_CODEC, RemoveTaskPacket::handle);
        r.playToClient(SyncTradePacket.TYPE, SyncTradePacket.STREAM_CODEC, SyncTradePacket::handle);
        r.playToServer(CreateLotPacket.TYPE, CreateLotPacket.STREAM_CODEC, CreateLotPacket::handle);
        r.playToServer(RemoveLotPacket.TYPE, RemoveLotPacket.STREAM_CODEC, RemoveLotPacket::handle);
    }
}
