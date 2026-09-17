package me.nanorasmus.nanodev.hex_js.addon.item;

import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Slice of mana pie: eating one applies {@link HexEffects#MANA_REGEN} (level I,
 * 1% of max mana per second) for {@link #EFFECT_DURATION} ticks instead of an
 * instant restore.
 */
public class ItemManaPieSlice extends Item {
    /** Effect duration in ticks (10 seconds). */
    public static final int EFFECT_DURATION = 200;
    /** Effect level applied by the pie (0 = level I, 1%/s). */
    public static final int EFFECT_LEVEL = 0;

    public ItemManaPieSlice(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(4).saturationModifier(0.3f).alwaysEdible().build()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("meowhex.tooltip.mana_pie_slice").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        if (!level.isClientSide && consumer instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(HexEffects.MANA_REGEN, EFFECT_DURATION, EFFECT_LEVEL,
                    false, true, true));
        }
        return result;
    }
}
