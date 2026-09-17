package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.common.lib.HexAttributes;
import at.petrak.hexcasting.forge.ForgeHexBerry;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Mana berry: eating one permanently raises the eater's {@code MANA_MAX} by
 * {@link #MANA_PER_BERRY}, up to {@link #MAX_BONUS} total bonus.
 *
 * <p>The eaten count lives in the {@code meowhex:berry_count} attachment
 * (survives death); the bonus itself is a transient modifier reapplied on eat,
 * login and respawn.
 */
public class ItemManaBerry extends Item {
    public static final double MANA_PER_BERRY = 100.0;
    public static final double MAX_BONUS = 500.0;
    public static final ResourceLocation BONUS_ID = HexJS.modLoc("berry_mana_max");

    public ItemManaBerry(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(4).saturationModifier(0.4f).alwaysEdible().build()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("meowhex.tooltip.mana_berry").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        if (!level.isClientSide && consumer instanceof ServerPlayer player) {
            int count = player.getData(ForgeHexBerry.BERRIES) + 1;
            player.setData(ForgeHexBerry.BERRIES, count);
            applyBonus(player);
        }
        return result;
    }

    /** (Re)applies the berry bonus from the stored count. */
    public static void applyBonus(ServerPlayer player) {
        double bonus = Math.min(player.getData(ForgeHexBerry.BERRIES) * MANA_PER_BERRY, MAX_BONUS);
        AttributeInstance inst = player.getAttribute(
                BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_MAX));
        if (inst == null) {
            return;
        }
        var existing = inst.getModifier(BONUS_ID);
        if (existing != null
                && existing.operation() == AttributeModifier.Operation.ADD_VALUE
                && Math.abs(existing.amount() - bonus) < 1e-9) {
            return;
        }
        inst.removeModifier(BONUS_ID);
        if (bonus > 1e-9) {
            inst.addTransientModifier(new AttributeModifier(
                    BONUS_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static int berryCount(ServerPlayer player) {
        return player.getData(ForgeHexBerry.BERRIES);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyBonus(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyBonus(player);
        }
    }
}
