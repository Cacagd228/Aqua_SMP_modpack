package me.nanorasmus.nanodev.hex_js.addon.interop;

import at.petrak.hexcasting.common.lib.HexAttributes;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Optional Apofix bridge: when apofix is installed, the apofix mana attributes
 * become the source of truth for the hexcasting mana pool — the Hex <b>base</b>
 * values are mirrored from apofix totals:
 * <ul>
 *   <li>{@code hexcasting:mana_max} base {@code := apofix:max_mana}</li>
 *   <li>{@code hexcasting:mana_regen} base {@code := apofix:mana_regen * 6.0}
 *   (apofix is mana/second, hexcasting is mana/6s)</li>
 *   <li>{@code hexcasting:mana_discount} base {@code := apofix:mana_discount}</li>
 * </ul>
 *
 * <p>Hex baubles keep working as before: they are {@code ADD_VALUE} modifiers
 * stacking <b>on top of</b> the apofix-driven base (and the diadem multiplier
 * still doubles the whole pool). Without apofix, stock hexcasting behaviour
 * (base 200 / 1.0 / 0.0) is untouched.
 *
 * <p>Uses only vanilla registry lookups — no direct references to apofix
 * classes — so it is safe to load with or without the mod. Sync runs on login,
 * respawn and throttled (every 20 ticks) server-side, and only writes when the
 * apofix totals actually changed since the last sync (per-player cache, dropped
 * on logout), so there is no attribute churn or sync spam. Legacy bridge
 * transient modifiers from older builds are removed on sync.
 */
public final class ApofixInterop {
    public static final String APOFIX_ID = "apofix";

    public static final ResourceLocation APOFIX_MAX_MANA_ID = apofixLoc("max_mana");
    public static final ResourceLocation APOFIX_REGEN_ID = apofixLoc("mana_regen");
    public static final ResourceLocation APOFIX_DISCOUNT_ID = apofixLoc("mana_discount");
    public static final ResourceLocation APOFIX_SILENCE_STATUS_ID = apofixLoc("silence_status");

    /** apofix regen is mana/second, hexcasting regen is mana/6s. */
    public static final double REGEN_TO_HEX_UNIT = 6.0;

    /** Legacy transient-bridge ids (pre-mirror builds); removed on sync. */
    public static final ResourceLocation BRIDGE_MANA_MAX_ID = HexJS.modLoc("apofix_mana_max");
    public static final ResourceLocation BRIDGE_REGEN_ID = HexJS.modLoc("apofix_mana_regen");
    public static final ResourceLocation BRIDGE_DISCOUNT_ID = HexJS.modLoc("apofix_mana_discount");

    private static final double EPS = 1e-9;

    private static Holder<Attribute> apofixMaxMana;
    private static Holder<Attribute> apofixRegen;
    private static Holder<Attribute> apofixDiscount;
    private static Holder<Attribute> apofixSilenceStatus;
    private static boolean resolved;

    /** Last mirrored apofix totals per player: [maxMana, regen, discount]. */
    private static final Map<UUID, double[]> lastMirror = new HashMap<>();

    private ApofixInterop() {
    }

    public static boolean isLoaded() {
        return net.neoforged.fml.ModList.get().isLoaded(APOFIX_ID);
    }

    /** Logs the integration state. Holders resolve lazily on first sync. */
    public static void init() {
        HexJS.LOGGER.info("[MeowHex] Apofix detected: hexcasting mana follows apofix attributes.");
    }

    private static ResourceLocation apofixLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(APOFIX_ID, path);
    }

    private static void ensureResolved() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!isLoaded()) {
            return;
        }
        apofixMaxMana = BuiltInRegistries.ATTRIBUTE.getHolder(APOFIX_MAX_MANA_ID).orElse(null);
        apofixRegen = BuiltInRegistries.ATTRIBUTE.getHolder(APOFIX_REGEN_ID).orElse(null);
        apofixDiscount = BuiltInRegistries.ATTRIBUTE.getHolder(APOFIX_DISCOUNT_ID).orElse(null);
        apofixSilenceStatus = BuiltInRegistries.ATTRIBUTE.getHolder(APOFIX_SILENCE_STATUS_ID).orElse(null);
        if (apofixMaxMana == null || apofixRegen == null || apofixDiscount == null) {
            HexJS.LOGGER.warn("[MeowHex] Apofix is present but its mana attributes are missing from the registry.");
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            lastMirror.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !player.level().isClientSide
                && player.tickCount % 20 == 0) {
            syncPlayer(player);
            tickSilenceStatus(player);
        }
    }

    /**
     * Silence status: while {@code apofix:silence_status >= 1} and the player
     * holds any {@code meowhex:*} or {@code hexcasting:*} item in either hand,
     * the Hex silence is (re)applied for 5 seconds — effectively constant while
     * held, expiring naturally ~5s after release or status loss.
     */
    public static void tickSilenceStatus(ServerPlayer player) {
        if (player == null || !isLoaded()) {
            return;
        }
        ensureResolved();
        if (apofixSilenceStatus == null
                || player.getAttributeValue(apofixSilenceStatus) < 1.0) {
            return;
        }
        if (!holdsHexItem(player)) {
            return;
        }
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                me.nanorasmus.nanodev.hex_js.effect.HexEffects.SILENCE, 100, 0, false, true, true));
    }

    private static boolean holdsHexItem(ServerPlayer player) {
        return isHexItem(player.getMainHandItem()) || isHexItem(player.getOffhandItem());
    }

    private static boolean isHexItem(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String ns = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        return ns.equals("meowhex") || ns.equals("hexcasting");
    }

    /** Mirrors apofix totals into Hex base values; no-ops when apofix is absent. */
    public static void syncPlayer(ServerPlayer player) {
        if (player == null || !isLoaded()) {
            return;
        }
        ensureResolved();
        if (apofixMaxMana == null || apofixRegen == null || apofixDiscount == null) {
            return;
        }
        double maxMana = player.getAttributeValue(apofixMaxMana);
        double regen = player.getAttributeValue(apofixRegen);
        double discount = player.getAttributeValue(apofixDiscount);

        double[] last = lastMirror.get(player.getUUID());
        if (last != null
                && Math.abs(last[0] - maxMana) < EPS
                && Math.abs(last[1] - regen) < EPS
                && Math.abs(last[2] - discount) < EPS) {
            return;
        }

        mirrorBase(player, HexAttributes.MANA_MAX, BRIDGE_MANA_MAX_ID, maxMana);
        mirrorBase(player, HexAttributes.MANA_REGEN, BRIDGE_REGEN_ID, regen * REGEN_TO_HEX_UNIT);
        mirrorBase(player, HexAttributes.MANA_DISCOUNT, BRIDGE_DISCOUNT_ID, discount);
        lastMirror.put(player.getUUID(), new double[]{maxMana, regen, discount});
    }

    private static void mirrorBase(ServerPlayer player, Attribute hexAttribute,
            ResourceLocation legacyBridgeId, double base) {
        AttributeInstance hexInst = player.getAttribute(
                BuiltInRegistries.ATTRIBUTE.wrapAsHolder(hexAttribute));
        if (hexInst == null) {
            return;
        }
        hexInst.removeModifier(legacyBridgeId);
        if (Math.abs(hexInst.getBaseValue() - base) >= EPS) {
            hexInst.setBaseValue(base);
        }
    }
}
