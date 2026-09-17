package xyz.lineage.trait;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import xyz.lineage.LineageCore;

/** Lucky hands: the earth and the fallen yield a little more. */
public final class FortuneFavor implements Trait {
    private final ResourceLocation sigil;

    public FortuneFavor(String name) {
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
    public void harvest(ServerPlayer player, BlockDropsEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (wieldsSilk(player, event.getTool())) {
            return;
        }
        boolean harvestBlock = event.getState().is(net.neoforged.neoforge.common.Tags.Blocks.ORES)
            || event.getState().is(BlockTags.CROPS);
        for (ItemEntity mote : event.getDrops()) {
            ItemStack stack = mote.getItem();
            if (stack.isEmpty() || !blessed(stack, harvestBlock)) {
                continue;
            }
            if (heaped(stack)) {
                stack.grow(player.getRandom().nextFloat() < 0.5F ? 2 : 1);
            } else if (player.getRandom().nextFloat() < 0.4F) {
                stack.grow(1);
            }
            mote.setItem(stack);
        }
    }

    @Override
    public void spoil(ServerPlayer player, LivingDropsEvent event) {
        if (event.isCanceled()) {
            return;
        }
        LivingEntity fallen = event.getEntity();
        ServerLevel nest = player.serverLevel();
        for (ItemEntity mote : event.getDrops()) {
            ItemStack stack = mote.getItem();
            if (!stack.isEmpty() && stack.isStackable() && !stack.isDamageableItem()
                && player.getRandom().nextFloat() < 0.5F) {
                stack.grow(1);
                mote.setItem(stack);
            }
        }
        if (fallen instanceof WitherSkeleton) {
            boolean crowned = event.getDrops().stream().anyMatch(m -> m.getItem().is(Items.WITHER_SKELETON_SKULL));
            if (!crowned && player.getRandom().nextFloat() < 0.015F) {
                event.getDrops().add(new ItemEntity(nest, fallen.getX(), fallen.getY(), fallen.getZ(),
                    new ItemStack(Items.WITHER_SKELETON_SKULL)));
            }
        } else if (fallen instanceof Zombie && !(fallen instanceof Drowned)) {
            boolean gifted = event.getDrops().stream().anyMatch(m ->
                m.getItem().is(Items.IRON_INGOT) || m.getItem().is(Items.CARROT) || m.getItem().is(Items.POTATO));
            if (!gifted && player.getRandom().nextFloat() < 0.015F) {
                ItemStack[] keepsakes = {new ItemStack(Items.IRON_INGOT), new ItemStack(Items.CARROT), new ItemStack(Items.POTATO)};
                event.getDrops().add(new ItemEntity(nest, fallen.getX(), fallen.getY(), fallen.getZ(),
                    keepsakes[player.getRandom().nextInt(keepsakes.length)]));
            }
        } else if (fallen instanceof Drowned && player.getRandom().nextFloat() < 0.01F) {
            event.getDrops().add(new ItemEntity(nest, fallen.getX(), fallen.getY(), fallen.getZ(),
                new ItemStack(Items.NAUTILUS_SHELL)));
        }
    }

    private static boolean wieldsSilk(ServerPlayer player, ItemStack tool) {
        HolderLookup<Enchantment> lookup = player.registryAccess().lookup(Registries.ENCHANTMENT).orElse(null);
        if (lookup == null) {
            return false;
        }
        Holder<Enchantment> silk = lookup.get(Enchantments.SILK_TOUCH).orElse(null);
        return silk != null && tool.getEnchantmentLevel(silk) > 0;
    }

    private static boolean blessed(ItemStack stack, boolean harvestBlock) {
        return harvestBlock
            || stack.is(net.neoforged.neoforge.common.Tags.Items.RAW_MATERIALS)
            || stack.is(net.neoforged.neoforge.common.Tags.Items.GEMS)
            || stack.is(net.neoforged.neoforge.common.Tags.Items.DUSTS)
            || stack.is(net.neoforged.neoforge.common.Tags.Items.CROPS)
            || stack.is(ItemTags.COALS)
            || stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)
            || stack.is(Items.LAPIS_LAZULI)
            || stack.is(Items.REDSTONE)
            || stack.is(Items.GLOWSTONE_DUST)
            || stack.is(Items.MELON_SLICE)
            || stack.is(Items.FLINT)
            || stack.is(Items.PRISMARINE_CRYSTALS);
    }

    private static boolean heaped(ItemStack stack) {
        return stack.is(Items.LAPIS_LAZULI)
            || stack.is(Items.REDSTONE)
            || stack.is(Items.GLOWSTONE_DUST)
            || stack.is(Items.MELON_SLICE)
            || stack.is(Items.PRISMARINE_CRYSTALS)
            || stack.is(Items.RAW_COPPER)
            || stack.is(net.neoforged.neoforge.common.Tags.Items.CROPS)
            || stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS);
    }
}
