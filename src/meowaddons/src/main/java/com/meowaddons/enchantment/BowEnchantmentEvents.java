package com.meowaddons.enchantment;

import com.meowaddons.MeowAddons;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// Один @EventBusSubscriber без ручной регистрации в MeowAddons
// (ручной NeoForge.EVENT_BUS.register дублировал события: было 6 стрел вместо 3).
@EventBusSubscriber(modid = MeowAddons.MODID)
public class BowEnchantmentEvents {
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> VANILLA_INFINITY =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("infinity"));

    // ---------- Мульти выстрел: залп из 3 стрел сразу ----------
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        ItemStack bow = event.getBow();

        if (!bow.is(Items.BOW)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (ModEnchantments.getTripleShotLevel(bow, level) == 0) return;

        // Слабый натяг — не перехватываем, пусть ванилла отработает как обычно
        float power = BowItem.getPowerForTime(event.getCharge());
        if (power < 0.1F) return;

        ItemStack projectile = player.getProjectile(bow);
        boolean infinite = player.hasInfiniteMaterials() || hasInfinity(bow, projectile, level);
        if (!infinite && projectile.isEmpty()) return;
        // Залп стоит 3 стрелы: иначе 1 потраченная стрела превращается в 3 подобранные (дюп).
        // Не хватает стрел — не перехватываем, пусть ванилла стреляет как обычно.
        if (!infinite && projectile.getCount() < 3) return;

        event.setCanceled(true);

        spawnArrow(serverLevel, player, bow, projectile, 0.0F, power);
        spawnArrow(serverLevel, player, bow, projectile, -10.0F, power);
        spawnArrow(serverLevel, player, bow, projectile, 10.0F, power);

        if (!infinite) {
            projectile.shrink(3);
        }
        bow.hurtAndBreak(1, serverLevel, player, (Item item) -> {});
    }

    private static void spawnArrow(ServerLevel level, Player player, ItemStack bow,
                                   ItemStack projectile, float yawOffset, float power) {
        ArrowItem arrowItem = projectile.getItem() instanceof ArrowItem ai
                ? ai
                : (ArrowItem) Items.ARROW;
        // Передаём реальный стак стрелы (tipped/spectral сохраняются)
        // и лук как weapon (Power/Punch/Flame применяются ваниллой сами).
        AbstractArrow arrow = arrowItem.createArrow(level, projectile, player, bow);
        if (arrow == null) return;

        arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, 0.0F, power * 3.0F, 1.0F);
        if (power >= 1.0F) {
            arrow.setCritArrow(true);
        }
        arrow.pickup = player.hasInfiniteMaterials()
                ? AbstractArrow.Pickup.CREATIVE_ONLY
                : AbstractArrow.Pickup.ALLOWED;
        level.addFreshEntity(arrow);
    }

    private static boolean hasInfinity(ItemStack bow, ItemStack projectile, Level level) {
        // Ванильная Infinity работает только с обычными стрелами
        if (!projectile.is(Items.ARROW)) return false;
        var holder = level.holderLookup(Registries.ENCHANTMENT).get(VANILLA_INFINITY).orElse(null);
        return holder != null && EnchantmentHelper.getItemEnchantmentLevel(holder, bow) > 0;
    }

    // ---------- Auto Shot: серверный тик вместо клиентского ----------
    // Старый клиентский вариант (ClientTick + startUsing/releaseUsing на клиенте)
    // на дедике рассинхронизировался с сервером. Сервер видит натяжение лука
    // (player.isUsingItem) и сам отпускает тетиву на полном натяге.
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isUsingItem()) return;

        ItemStack useItem = player.getUseItem();
        if (!useItem.is(Items.BOW)) return;
        if (ModEnchantments.getAutoShotLevel(useItem, player.level()) == 0) return;

        int usedTicks = useItem.getUseDuration(player) - player.getUseItemRemainingTicks();
        if (BowItem.getPowerForTime(usedTicks) >= 1.0F) {
            player.releaseUsingItem();
        }
    }
}
