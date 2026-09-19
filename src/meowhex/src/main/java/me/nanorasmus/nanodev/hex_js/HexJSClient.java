package me.nanorasmus.nanodev.hex_js;

import me.nanorasmus.nanodev.hex_js.addon.HextendedItems;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemDrawingOrb;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.client.RegisterClientStuff;
import at.petrak.hexcasting.client.render.GaslightingTracker;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.function.ToIntFunction;
import java.util.function.Predicate;

/**
 * Client-side registration for the hextended item set (loaded only on the client via
 * the {@link FMLClientSetupEvent}/{@link RegisterColorHandlersEvent.Item} listeners
 * registered from {@link HexJSClient#init}).
 *
 * <ul>
 *   <li>{@code hexcasting:overlay_layer} model predicate on the {@link ItemDrawingOrb}
 *       so the orb's model switches empty/filled/sealed.</li>
 *   <li>{@code hexcasting:variant} model predicate (gaslighting index) on the extended
 *       quenched staff, swapping {@code quenched_0..3} frames.</li>
 *   <li>The iota-storage colour tint for the drawing orb.</li>
 * </ul>
 *
 * <p>Item-property registration goes through Hex Casting's own
 * {@link IClientXplatAbstractions} abstraction (the vanilla {@code ItemProperties}
 * register hooks are private in 1.21.1), mirroring how hexcasting registers its own
 * storage items.
 */
@OnlyIn(Dist.CLIENT)
public final class HexJSClient {
    private HexJSClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(HexJSClient::onClientSetup);
        modBus.addListener(HexJSClient::onRegisterColorHandlers);
        modBus.addListener(HexJSClient::onRegisterRenderers);
        // Silence purple border
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.client.SilenceOverlay.class);
        // Red kill-flash vignette (worldborder-like, no visible walls)
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.client.KillFlashOverlay.class);
        // Kill announcement text at the bossbar spot (top center, no bar)
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(me.nanorasmus.nanodev.hex_js.client.AnnounceTextOverlay.class);
        // Strip hexcasting "Can be worn in:" duplicates and backfill the Curios "Slot:" line
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(me.nanorasmus.nanodev.hex_js.client.CurioTooltipHandler::onTooltip);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (HextendedItems.DRAWING_ORB.isBound()) {
                ItemDrawingOrb orb = (ItemDrawingOrb) HextendedItems.DRAWING_ORB.get();
                IClientXplatAbstractions.INSTANCE.registerItemProperty(
                        orb, ItemDrawingOrb.OVERLAY_PRED,
                        (stack, level, entity, seed) -> {
                            if (!hasIota(stack, orb) && !NBTHelper.hasString(stack, IotaHolderItem.TAG_OVERRIDE_VISUALLY)) {
                                return 0;
                            }
                            if (!ItemDrawingOrb.isSealed(stack)) {
                                return 1;
                            }
                            return 2;
                        });
            }

            if (HextendedItems.EXTENDED_QUENCHED_STAFF.isBound()) {
                IClientXplatAbstractions.INSTANCE.registerItemProperty(
                        HextendedItems.EXTENDED_QUENCHED_STAFF.get(), GaslightingTracker.GASLIGHTING_PRED,
                        (stack, level, entity, seed) -> Math.abs(GaslightingTracker.getGaslightingAmount() % 4));
            }
        });
    }

    private static void onRegisterColorHandlers(RegisterColorHandlersEvent.Item event) {
        if (HextendedItems.DRAWING_ORB.isBound()) {
            ItemDrawingOrb orb = (ItemDrawingOrb) HextendedItems.DRAWING_ORB.get();
            ToIntFunction<ItemStack> tint = stack -> orb.getColor(stack);
            event.register(RegisterClientStuff.makeIotaStorageColorizer(tint), orb);
        }
    }

    private static void onRegisterRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(me.nanorasmus.nanodev.hex_js.entity.HexEntities.DECEPTION.get(), me.nanorasmus.nanodev.hex_js.client.DeceptionRenderer::new);
        event.registerEntityRenderer(me.nanorasmus.nanodev.hex_js.entity.HexEntities.APOLLO_ARROW.get(), me.nanorasmus.nanodev.hex_js.client.ApolloArrowRenderer::new);
        event.registerEntityRenderer(me.nanorasmus.nanodev.hex_js.entity.HexEntities.SNIPER_SHOT.get(), me.nanorasmus.nanodev.hex_js.client.SniperShotRenderer::new);
        event.registerEntityRenderer(me.nanorasmus.nanodev.hex_js.entity.HexEntities.SUN_BEAM.get(), me.nanorasmus.nanodev.hex_js.client.SunBeamRenderer::new);
        // EntityHadesSummon extends Zombie — без рендера EntityRenderDispatcher.getRenderer
        // возвращает null и игра падает с NPE в shouldRender при первом кадре с саммоном.
        event.registerEntityRenderer(me.nanorasmus.nanodev.hex_js.entity.HexEntities.HADES_SUMMON.get(), net.minecraft.client.renderer.entity.ZombieRenderer::new);
        event.registerEntityRenderer(me.nanorasmus.nanodev.hex_js.entity.HexEntities.STYX_SHADE.get(), net.minecraft.client.renderer.entity.VexRenderer::new);
    }

    private static boolean hasIota(ItemStack stack, ItemDrawingOrb orb) {
        return orb.readIotaTag(stack) != null;
    }
}