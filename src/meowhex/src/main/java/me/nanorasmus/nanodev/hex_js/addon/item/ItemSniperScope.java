package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.common.items.HexBaubleItem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Sniper's Scope — a Curios bauble worn in the {@code head} slot. Wearing it
 * unlocks the {@code Deadeye} ("Снайперский выстрел") pattern that fires a
 * homing bullet through any blocks, stripping 40% of the target's max HP.
 *
 * <p>After each shot the scope goes on a cooldown (see {@code OpDeadeye});
 * the "ready at" game time is persisted on the worn stack so the cooldown
 * survives logging out.
 */
public class ItemSniperScope extends Item implements HexBaubleItem {

    /** NBT tag on the scope stack: game time (ticks) at which Deadeye may be fired again. */
    public static final String TAG_DEADEYE_READY_AT = "MeowhexDeadeyeReadyAt";

    public ItemSniperScope(Properties properties) {
        super(properties);
    }

    /** Game time (server ticks) at which the scope is ready again; 0 = never set. */
    public static long getDeadeyeReadyAt(ItemStack stack) {
        return NBTHelper.getLong(stack, TAG_DEADEYE_READY_AT);
    }

    /** Marks the scope on cooldown until the given server game time. */
    public static void setDeadeyeReadyAt(ItemStack stack, long readyAt) {
        NBTHelper.putLong(stack, TAG_DEADEYE_READY_AT, readyAt);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getHexBaubleAttrs(ItemStack stack) {
        return HashMultimap.create();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("meowhex.tooltip.sniper_scope")
                .withStyle(ChatFormatting.GRAY));
    }
}
