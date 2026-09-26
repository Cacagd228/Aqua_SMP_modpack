package com.fmm.teams.client;

import java.util.List;

import com.fmm.teams.net.TeamNet;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class TeamClient {
    private TeamClient() {}

    public static final KeyMapping OPEN_TEAMS = new KeyMapping(
            "key.fmm_teams.open",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.fmm_teams");

    private static volatile TeamNet.Snapshot snapshot =
            TeamNet.Snapshot.empty(List.of());
    private static final Logger LOGGER = LogUtils.getLogger();

    public static TeamNet.Snapshot snapshot() {
        return snapshot;
    }

    public static void applySync(TeamNet.ClientboundTeamSync sync) {
        snapshot = sync.toSnapshot();
    }

    public static void requestSync() {
        PacketDistributor.sendToServer(TeamNet.ServerboundTeamAction.of("sync", "", null));
    }

    public static void openTeams() {
        Minecraft mc = Minecraft.getInstance();
        requestSync();
        mc.execute(() -> {
            if (mc.screen == null) mc.setScreen(new TeamsScreen());
        });
    }

    public static void init(IEventBus modBus) {
        LOGGER.info("[fmm_teams] client init");
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            e.register(OPEN_TEAMS);
            LOGGER.info("[fmm_teams] keybind registered: {}", OPEN_TEAMS.getName());
        });
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> e.getDispatcher().register(
                Commands.literal("fteams").executes(ctx -> {
                    openTeams();
                    return 1;
                })));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_TEAMS.consumeClick()) {
                if (mc.screen == null && mc.player != null) {
                    openTeams();
                }
            }
        });
    }
}
