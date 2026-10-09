package me.nanorasmus.nanodev.hex_js.addon.scroll;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class SealedScrollItem extends Item {

    private static final List<String> HEXSABLE_SCROLL_IDS = List.of(
            "scroll_get_sable", "scroll_zone_sable", "scroll_pos_sable",
            "scroll_velocity_sable", "scroll_angular_velocity_sable",
            "scroll_mass_sable", "scroll_bounds_sable", "scroll_to_world_sable",
            "scroll_to_local_sable", "scroll_dir_to_world_sable",
            "scroll_dir_to_local_sable", "scroll_impulse_sable",
            "scroll_impulse_at_sable", "scroll_spin_sable", "scroll_blink_sable"
    );

    public SealedScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        List<String> pool = new ArrayList<>(ScrollDefs.SCROLLS);
        pool.addAll(HEXSABLE_SCROLL_IDS);
        String scrollId = pool.get(level.getRandom().nextInt(pool.size()));

        ItemStack loot = lookupScrollStack(scrollId);
        if (loot.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("meowhex.tooltip.sealed_scroll.empty").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResultHolder.fail(stack);
        }

        if (!player.getInventory().add(loot)) {
            player.drop(loot, false);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.4, 0.4, 0.4, 0.1);
        }

        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    private ItemStack lookupScrollStack(String scrollId) {
        // Try meowhex namespace first
        java.util.Optional<net.minecraft.world.item.Item> meowOpt = BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath("meowhex", scrollId));
        if (meowOpt.isPresent()) {
            return new ItemStack(meowOpt.get());
        }
        java.util.Optional<net.minecraft.world.item.Item> hexOpt = BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath("hexsable", scrollId));
        if (hexOpt.isPresent()) {
            return new ItemStack(hexOpt.get());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("meowhex.tooltip.sealed_scroll").withStyle(ChatFormatting.GRAY));
    }
}
