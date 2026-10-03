package xyz.lineage.game;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.lineage.Lineage;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.registry.SoulAttachments;

/**
 * The drowned north: cold water bites mortal flesh and swallows hulls alike.
 * Mermaid blood alone swims the chill unburned.
 */
public final class FrozenWaters {
    /**
     * Biome path fragments whose water bites. A biome is cold when its path
     * contains one of these, so the deep variants (deep_frozen_ocean,
     * deep_cold_ocean) are covered alongside their shallow counterparts
     * without listing every variant, as are modded oceans in another
     * namespace. Plain deep_ocean counts too - the pressure alone is enough
     * down there.
     */
    private static final Set<String> COLD_BIOMES =
        Set.of("frozen_ocean", "cold_ocean", "deep_ocean");
    /**
     * Create's diving suit and lava diving suit, both of which shrug off the cold.
     * Only the complete set counts: a helmet without boots leaves the diver cold.
     */
    private static final Set<ResourceLocation> DIVING_HELMETS = Set.of(
        ResourceLocation.fromNamespaceAndPath("create", "copper_diving_helmet"),
        ResourceLocation.fromNamespaceAndPath("create", "netherite_diving_helmet"));
    private static final Set<ResourceLocation> DIVING_BOOTS = Set.of(
        ResourceLocation.fromNamespaceAndPath("create", "copper_diving_boots"),
        ResourceLocation.fromNamespaceAndPath("create", "netherite_diving_boots"));
    private static final Set<ResourceLocation> BACKTANKS = Set.of(
        ResourceLocation.fromNamespaceAndPath("create", "copper_backtank"),
        ResourceLocation.fromNamespaceAndPath("create", "netherite_backtank"));
    /** Create names its air component with the typo "banktank"; match it as-is. */
    private static final ResourceLocation BACKTANK_AIR =
        ResourceLocation.fromNamespaceAndPath("create", "banktank_air");
    private static final EquipmentSlot[] BALLOON_SLOTS =
        {EquipmentSlot.CHEST, EquipmentSlot.OFFHAND, EquipmentSlot.MAINHAND};
    /** One heartbeat between bites. */
    private static final int CHILL_RHYTHM = 20;
    /** A single heart, timed like vanilla freeze. */
    private static final float CHILL_ACHES = 2.0F;
    /**
     * Boats shed one damage per tick, so a slow drip never breaks a hull.
     * VehicleEntity adds amount * 10 and destroys past 40; at 3.0 every half
     * second the net gain beats the decay and the hull gives out in ~2s.
     */
    private static final int BOAT_RHYTHM = 10;
    private static final float BOAT_ACHES = 3.0F;

    /**
     * Runs every tick so the frost sheen never flickers off (frozen ticks decay
     * by two per tick), but only bites once per second.
     */
    @SubscribeEvent
    public void chill(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean cold = swimmingInColdWater(player);
        if (!cold || isMermaid(player) || suitedUp(player)) {
            return;
        }
        if (player.canFreeze()) {
            // One tick short of the vanilla kill threshold: the overlay stays full,
            // but vanilla never adds its own freeze damage on top of ours.
            player.setTicksFrozen(Math.max(player.getTicksFrozen(), player.getTicksRequiredToFreeze() - 1));
        }
        if (player.tickCount % CHILL_RHYTHM != 0) {
            return;
        }
        player.hurt(player.damageSources().freeze(), CHILL_ACHES);
    }

    @SubscribeEvent
    public void hulls(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof VehicleEntity hull) || hull.level().isClientSide()
            || hull.tickCount % BOAT_RHYTHM != 0) {
            return;
        }
        if (!drowned(hull.level(), hull.getX(), hull.getY(), hull.getZ())) {
            return;
        }
        hull.hurt(hull.damageSources().freeze(), BOAT_ACHES);
    }

    /** True while the player sits in cold water. */
    public static boolean swimmingInColdWater(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || player.isInvulnerable() || !player.isAlive()) {
            return false;
        }
        if (!player.isInWaterOrBubble() && !player.isSwimming()) {
            return false;
        }
        return drowned(player.level(), player.getX(), player.getY(), player.getZ());
    }

    /** True where water meets a cold biome. */
    public static boolean drowned(LevelReader level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (!cold(level, pos)) {
            return false;
        }
        return level.getFluidState(pos).is(FluidTags.WATER) || level.getFluidState(pos.below()).is(FluidTags.WATER);
    }

    private static boolean cold(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).unwrapKey()
            .map(key -> {
                String path = key.location().getPath();
                return COLD_BIOMES.stream().anyMatch(path::contains);
            })
            .orElse(Boolean.FALSE);
    }

    /** True with a full Create diving suit and a breathing backtank on an empty tank. */
    private static boolean suitedUp(ServerPlayer player) {
        return wears(player, EquipmentSlot.HEAD, DIVING_HELMETS)
            && wears(player, EquipmentSlot.FEET, DIVING_BOOTS)
            && hasAir(player);
    }

    private static boolean wears(ServerPlayer player, EquipmentSlot slot, Set<ResourceLocation> ids) {
        ItemStack stack = player.getItemBySlot(slot);
        return !stack.isEmpty() && ids.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** A backtank is worn in the chest slot or carried in either hand. */
    private static boolean hasAir(ServerPlayer player) {
        for (EquipmentSlot slot : BALLOON_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !BACKTANKS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                continue;
            }
            DataComponentType<?> air = BuiltInRegistries.DATA_COMPONENT_TYPE.get(BACKTANK_AIR);
            Object left = air == null ? null : stack.get(air);
            if (left instanceof Integer bubbles && bubbles > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMermaid(ServerPlayer player) {
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        if (ledger == null) {
            return false;
        }
        Lineage lineage = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
        return lineage != null && LineageCatalog.MERMAID.equals(lineage.id());
    }
}
