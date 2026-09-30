package com.fmm.teams;

import com.fmm.teams.client.TeamClient;
import com.fmm.teams.net.TeamNet;
import com.fmm.teams.server.AdminCommands;
import com.fmm.teams.server.TeamCommands;
import com.fmm.teams.server.TeamServer;
import com.fmm.teams.server.TerritoryTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(FmmTeams.MODID)
public class FmmTeams {
    public static final String MODID = "fmm_teams";

    public FmmTeams(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.register(TeamCommands.class);
        NeoForge.EVENT_BUS.register(AdminCommands.class);
        NeoForge.EVENT_BUS.register(TerritoryTracker.class);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                TeamServer.pushTo(sp, com.fmm.teams.team.TeamManager.get(sp.server));
            }
        });
        if (FMLEnvironment.dist.isClient()) {
            TeamClient.init(modEventBus);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TeamNet.PROTOCOL);
        registrar.playToServer(TeamNet.ServerboundTeamAction.TYPE,
                TeamNet.ServerboundTeamAction.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        TeamServer.handleAction(sp, payload);
                    }
                }));
        registrar.playToClient(TeamNet.ClientboundTeamSync.TYPE,
                TeamNet.ClientboundTeamSync.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (FMLEnvironment.dist.isClient()) {
                        clientSync(payload);
                    }
                }));
    }

    private static void clientSync(TeamNet.ClientboundTeamSync payload) {
        if (FMLEnvironment.dist.isClient()) {
            Minecraft.getInstance().execute(() -> TeamClient.applySync(payload));
        }
    }
}
