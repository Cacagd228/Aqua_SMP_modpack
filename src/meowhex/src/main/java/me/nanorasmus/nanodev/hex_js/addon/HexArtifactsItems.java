package me.nanorasmus.nanodev.hex_js.addon;

import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemCommentator;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemHexAmulet;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemPatternReader;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemSniperScope;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * The "HEX Artifacts" item set: mana amulets worn in the Curios {@code necklace}
 * slot, plus the charged amethyst diadem (moved out of the Hextended tab).
 *
 * <p>Regeneration values are in <b>mana per second</b> (1.0 = 1 mana/s);
 * {@code ItemHexAmulet} converts to hexcasting's "mana per 6 seconds" internally.
 * Discounts are cost fractions (0.01 = 1%).
 */
public final class HexArtifactsItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HexJS.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HexJS.MOD_ID);

    /** All registered artifacts, in insertion order — used to fill the creative tab. */
    public static final List<DeferredHolder<Item, ? extends Item>> ALL = new ArrayList<>();

    // ---- amulets (necklace curio) ----
    public static final DeferredHolder<Item, ? extends Item> AMETHYST_NECKLACE = amulet(
            "amethyst_necklace", 100, 0.01, 0);
    public static final DeferredHolder<Item, ? extends Item> CHARGED_AMETHYST_NECKLACE = amulet(
            "charged_amethyst_necklace", 200, 0.02, 1.0);
    public static final DeferredHolder<Item, ? extends Item> OVERLOADED_NECKLACE = amulet(
            "overloaded_necklace", 400, 0.03, 3.0);

    // ---- ring of self-torture (ring curio) ----
    public static final DeferredHolder<Item, ? extends Item> SELF_TORTURE_RING =
            ITEMS.register("self_torture_ring", () -> new me.nanorasmus.nanodev.hex_js.addon.item.ItemSelfTortureRing(
                    new Item.Properties().stacksTo(1)));

    // ---- lens of comprehension (necklace curio) ----
    public static final DeferredHolder<Item, ? extends Item> PATTERN_READER =
            ITEMS.register("pattern_reader", () -> new ItemPatternReader(
                    new Item.Properties().stacksTo(1)));

    // ---- commentator (necklace curio, dota-style PvP announcer) ----
    public static final DeferredHolder<Item, ? extends Item> COMMENTATOR =
            ITEMS.register("commentator", () -> new ItemCommentator(
                    new Item.Properties().stacksTo(1)));

    // ---- meepo commentator (necklace curio, second announcer voice) ----
    public static final DeferredHolder<Item, ? extends Item> COMMENTATOR_MEEPO =
            ITEMS.register("commentator_meepo", () -> new ItemCommentator(
                    new Item.Properties().stacksTo(1), "item.meowhex.commentator_meepo.tooltip"));

    // ---- sniper's scope (dedicated scope curio) ----
    public static final DeferredHolder<Item, ? extends Item> SNIPER_SCOPE =
            ITEMS.register("sniper_scope", () -> new ItemSniperScope(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("hexartifacts", () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(AMETHYST_NECKLACE.get()))
                    .title(Component.translatable("itemGroup.meowhex_artifacts"))
                    .displayItems((params, out) -> {
                        ALL.forEach(h -> out.accept(h.get()));
                        out.accept(PATTERN_READER.get());
                        out.accept(SNIPER_SCOPE.get());
                        out.accept(COMMENTATOR.get());
                        out.accept(COMMENTATOR_MEEPO.get());
                        out.accept(HextendedItems.CHARGED_AMETHYST_DIADEM.get());
                        out.accept(HexFoodItems.MANA_BERRY.get());
                        out.accept(HexFoodItems.MANA_PIE_SLICE.get());
                        out.accept(SELF_TORTURE_RING.get());
                        out.accept(me.nanorasmus.nanodev.hex_js.addon.armor.HolyValkyrieArmorItems.HELMET.get());
                        out.accept(me.nanorasmus.nanodev.hex_js.addon.armor.HolyValkyrieArmorItems.CUIRASS.get());
                        out.accept(me.nanorasmus.nanodev.hex_js.addon.armor.HolyValkyrieArmorItems.LEGGINGS.get());
                        out.accept(me.nanorasmus.nanodev.hex_js.addon.armor.HolyValkyrieArmorItems.BOOTS.get());
                        out.accept(me.nanorasmus.nanodev.hex_js.addon.armor.ScarletKnightArmorItems.HOOD.get());
                        out.accept(me.nanorasmus.nanodev.hex_js.addon.armor.ScarletKnightArmorItems.CUIRASS.get());
                        out.accept(me.nanorasmus.nanodev.hex_js.addon.armor.ScarletKnightArmorItems.LEGGINGS.get());
                        out.accept(me.nanorasmus.nanodev.hex_js.addon.armor.ScarletKnightArmorItems.BOOTS.get());
                    })
                    .build());

    private HexArtifactsItems() {
    }

    public static void init(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private static DeferredHolder<Item, ? extends Item> amulet(String id, double maxMana, double discount, double manaRegen) {
        DeferredHolder<Item, ? extends Item> holder =
                ITEMS.register(id, () -> new ItemHexAmulet(new Item.Properties().stacksTo(1), id, maxMana, discount, manaRegen));
        ALL.add(holder);
        return holder;
    }
}
