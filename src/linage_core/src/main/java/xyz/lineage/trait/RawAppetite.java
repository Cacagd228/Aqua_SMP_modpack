package xyz.lineage.trait;

import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xyz.lineage.LineageCore;

/** Red fare only: cooked bread and greens turn to ash in a pale gut. */
public final class RawAppetite implements Trait {
    private static final Set<Item> RED_FARE = Set.of(
        Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT,
        Items.ROTTEN_FLESH, Items.COD, Items.SALMON, Items.TROPICAL_FISH);
    private final ResourceLocation sigil;

    public RawAppetite(String name) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
    }

    @Override
    public ResourceLocation sigil() {
        return sigil;
    }

    @Override
    public Component title() {
        return Component.translatable("trait." + LineageCore.MOD_ID + "." + sigil.getPath());
    }

    @Override
    public Component lore() {
        return Component.translatable("trait." + LineageCore.MOD_ID + "." + sigil.getPath() + ".desc");
    }

    @Override
    public void savor(ServerPlayer player, ItemStack food) {
        if (food.isEmpty() || RED_FARE.contains(food.getItem())) {
            return;
        }
        FoodProperties props = food.get(DataComponents.FOOD);
        if (props == null) {
            return;
        }
        FoodData gut = player.getFoodData();
        gut.setFoodLevel(Math.max(0, gut.getFoodLevel() - props.nutrition()));
        gut.setSaturation(Math.max(0.0F, gut.getSaturationLevel() - props.saturation()));
    }
}
