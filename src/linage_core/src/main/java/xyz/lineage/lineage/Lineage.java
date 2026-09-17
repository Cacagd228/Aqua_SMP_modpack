package xyz.lineage.lineage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import xyz.lineage.stats.HeroStat;
import xyz.lineage.trait.Trait;

/**
 * Immutable lineage definition. Field layout mirrors the gameplay
 * needs (starting facets, gear, vigor/mana/stature curves) without
 * reusing the original record's shape or names.
 */
public record Lineage(
    ResourceLocation id,
    Component title,
    Component lore,
    ItemStack sigil,
    Map<HeroStat, Integer> facets,
    List<Trait> traits,
    ItemStack handMain,
    ItemStack handOff,
    ItemStack helm,
    ItemStack cuirass,
    ItemStack greaves,
    ItemStack sabatons,
    List<ItemStack> satchel,
    double health,
    double force,
    double ward,
    double spell,
    double pace,
    double mana,
    double stature,
    double regen,
    double evasion,
    double crit,
    double rend,
    double bargain,
    int contracts,
    double wisdom,
    double bulwark,
    double aegis
) {
    public Lineage {
        facets = Map.copyOf(facets);
        traits = List.copyOf(traits);
        satchel = List.copyOf(satchel);
        handMain = handMain == null ? ItemStack.EMPTY : handMain;
        handOff = handOff == null ? ItemStack.EMPTY : handOff;
        helm = helm == null ? ItemStack.EMPTY : helm;
        cuirass = cuirass == null ? ItemStack.EMPTY : cuirass;
        greaves = greaves == null ? ItemStack.EMPTY : greaves;
        sabatons = sabatons == null ? ItemStack.EMPTY : sabatons;
        if (stature <= 0.0) {
            stature = 1.8;
        }
    }

    public int facet(HeroStat stat) {
        return facets.getOrDefault(stat, 0);
    }

    public ItemStack plateFor(Player player, EquipmentSlot slot) {
        ItemStack base = switch (slot) {
            case HEAD -> helm();
            case CHEST -> cuirass();
            case LEGS -> greaves();
            case FEET -> sabatons();
            default -> ItemStack.EMPTY;
        };
        if (player != null && LineageCatalog.isTannedHide(base)) {
            return LineageCatalog.dyeForTraveler(base.getItem(), player.getUUID(), slot);
        }
        return base;
    }

    public List<ItemStack> provisions() {
        List<ItemStack> out = new ArrayList<>();
        if (!handMain.isEmpty()) {
            out.add(handMain.copy());
        }
        if (!handOff.isEmpty()) {
            out.add(handOff.copy());
        }
        if (!helm.isEmpty()) {
            out.add(helm.copy());
        }
        if (!cuirass.isEmpty()) {
            out.add(cuirass.copy());
        }
        if (!greaves.isEmpty()) {
            out.add(greaves.copy());
        }
        if (!sabatons.isEmpty()) {
            out.add(sabatons.copy());
        }
        for (ItemStack extra : satchel) {
            if (!extra.isEmpty()) {
                out.add(extra.copy());
            }
        }
        return out;
    }

    /** Short form: classic vigor curve, standard secondaries. */
    public static Lineage classic(ResourceLocation id, Component title, Component lore, ItemStack sigil,
        Map<HeroStat, Integer> facets, List<Trait> traits,
        ItemStack handMain, ItemStack handOff, ItemStack helm, ItemStack cuirass,
        ItemStack greaves, ItemStack sabatons, List<ItemStack> satchel,
        double mana, double stature) {
        return new Lineage(id, title, lore, sigil, facets, traits,
            handMain, handOff, helm, cuirass, greaves, sabatons, satchel,
            20.0, 1.0, 0.0, 1.0, 0.1, mana, stature,
            1.0, 0.05, 0.05, 1.5, 0.0, 0, 0.0, 0.0, 0.0);
    }
}
