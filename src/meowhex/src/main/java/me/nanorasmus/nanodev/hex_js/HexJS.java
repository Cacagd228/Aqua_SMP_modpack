package me.nanorasmus.nanodev.hex_js;

import me.nanorasmus.nanodev.hex_js.addon.HextendedAddon;
import me.nanorasmus.nanodev.hex_js.helpers.PatternTextUtils;
import me.nanorasmus.nanodev.hex_js.storage.ChatPatternStore;
import me.nanorasmus.nanodev.hex_js.storage.HexJsData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for the HexJS NeoForged addon.
 *
 * <p>Owns the mod lifecycle and the active {@link MinecraftServer} reference.
 * All per-world configuration (pattern black/white lists, redirects, Bookkeeper
 * limits) lives in the {@link HexJsData} SavedData entry, which is loaded on
 * server start and flushed on server stop.
 */
@Mod(HexJS.MOD_ID)
public class HexJS {
    public static final String MOD_ID = "meowhex";
    public static final Logger LOGGER = LogManager.getLogger("MeowHex");

    private static MinecraftServer server;

    public HexJS(IEventBus modBus) {
        HexJSInitializer.init(modBus);
        HextendedAddon.init(modBus);
        me.nanorasmus.nanodev.hex_js.entity.HexEntities.init(modBus);
        me.nanorasmus.nanodev.hex_js.effect.HexEffects.init(modBus);
        me.nanorasmus.nanodev.hex_js.sound.HexSounds.init(modBus);
        if (FMLEnvironment.dist.isClient()) {
            HexJSClient.init(modBus);
        }
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            // Same ordering as hexcasting itself (ForgeXplatImpl.initPlatformSpecific
            // runs inside enqueueWork): register curios after registries freeze.
            modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) ->
                    e.enqueueWork(me.nanorasmus.nanodev.hex_js.addon.interop.HexJsCuriosInterop::init));
            if (FMLEnvironment.dist.isClient()) {
                modBus.addListener(me.nanorasmus.nanodev.hex_js.addon.interop.HexJsCuriosInteropClient::onClientSetup);
                modBus.addListener(me.nanorasmus.nanodev.hex_js.addon.interop.HexJsCuriosInteropClient::onRegisterLayers);
            }
        }
        if (net.neoforged.fml.ModList.get().isLoaded(
                me.nanorasmus.nanodev.hex_js.addon.interop.ApothicInterop.APOTHIC_ID)) {
            me.nanorasmus.nanodev.hex_js.addon.interop.ApothicInterop.init();
        }
        if (net.neoforged.fml.ModList.get().isLoaded(
                me.nanorasmus.nanodev.hex_js.addon.interop.ApofixInterop.APOFIX_ID)) {
            me.nanorasmus.nanodev.hex_js.addon.interop.ApofixInterop.init();
            NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.addon.interop.ApofixInterop.class);
        }
        // Magic resist hook is fully soft (registry lookups only): the enchant
        // layer works standalone, the apofix attribute layer when apofix is present.
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.addon.interop.MagicResistHandler.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.addon.interop.ManaBreakHandler.class);
        NeoForge.EVENT_BUS.addListener(HexJS::onServerStarting);
        NeoForge.EVENT_BUS.addListener(HexJS::onServerStopping);
        NeoForge.EVENT_BUS.addListener(HexJS::onServerChat);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.effect.SilenceMagicBlockHandler.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.command.SilenceCommand.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.command.AttrsDebugCommand.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.addon.item.ItemManaBerry.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.entity.SummonAgroHandler.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.casting.ErebusChainHandler.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.casting.AnnouncerHandler.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.casting.UtgardHandler.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.casting.PingPongHandler.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.casting.ManaPairingHandler.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.casting.ChronosHandler.class);
        NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.casting.RuneVisageHandler.class);
    }

    private static void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        HexJsData.load(server);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        HexJsData.save(event.getServer());
        server = null;
    }

    /**
     * Records the last set of patterns players wrote in chat (as {@code <dir,sig>}
     * markers) so the "Постижение" spell can read them. A message without patterns
     * does not wipe the previous set.
     */
    private static void onServerChat(ServerChatEvent event) {
        var patterns = PatternTextUtils.parsePatterns(event.getRawText());
        if (!patterns.isEmpty()) {
            ChatPatternStore.record(patterns);
        }
    }

    /**
     * The active integrated/dedicated server, or {@code null} while not on a server
     * thread (for example during startup script load).
     */
    public static MinecraftServer server() {
        return server;
    }

    public static ResourceLocation modLoc(String key) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, key);
    }
}