package me.nanorasmus.nanodev.hex_js.addon.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;

/**
 * Аметистовая удочка: работает как ванильная, но держит на воде сразу
 * {@value #MAX_BOBBERS} поплавка и поднимает их разом.
 *
 * <p>Прямая копия {@link net.minecraft.world.item.FishingRodItem} с двумя
 * изменениями: при забросе создаются два {@link FishingHook} веером, при подъёме
 * подтягиваются все поплавки игрока. Своя сущность не нужна — работаем поверх
 * ванильного поплавка, он и так синхронизируется на клиент сам.
 *
 * <p>Поле {@code Player.fishing} вмещает только один поплавок, поэтому при двух
 * поплавках оно указывает лишь на последний созданный и источником правды не
 * годится. Все поплавки игрока ищем обычным запросом к миру — см. {@link #bobbersOf}.
 */
public class ItemAmethystFishingRod extends Item {

    /** Сколько поплавков держит аметистовая удочка одновременно. */
    private static final int MAX_BOBBERS = 2;

    /** Прочность: по единице на поднятый поплавок, как у ванильной удочки. */
    private static final int DURABILITY = 64;

    /** Насколько далеко ищутся поплавки игрока, в блоках. */
    private static final double BOBBER_RANGE = 64.0;

    /**
     * Насколько поплавки разводятся в стороны при забросе, в блоках. Уменьшено
     * с 0.6 — два поплавка впритык к удочке читались как один.
     */
    private static final double SPREAD = 0.3;

    public ItemAmethystFishingRod(Properties properties) {
        super(properties.durability(DURABILITY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return bobbersOf(player).isEmpty() ? cast(level, player, hand, stack) : retrieve(level, player, hand, stack);
    }

    private InteractionResultHolder<ItemStack> cast(Level level, Player player, InteractionHand hand, ItemStack stack) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (level instanceof ServerLevel serverLevel) {
            int lureTicks = (int) (EnchantmentHelper.getFishingTimeReduction(serverLevel, stack, player) * 20.0F);
            int luck = EnchantmentHelper.getFishingLuckBonus(serverLevel, stack, player);

            for (int i = 0; i < MAX_BOBBERS; i++) {
                FishingHook hook = new FishingHook(player, level, luck, lureTicks);
                spread(hook, i);
                level.addFreshEntity(hook);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.gameEvent(GameEvent.ITEM_INTERACT_START);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private InteractionResultHolder<ItemStack> retrieve(Level level, Player player, InteractionHand hand, ItemStack stack) {
        // Подъём и износ считает сервер: клиент может не знать о поплавке, который
        // ещё не отслеживается (например, только что перекинуло через реку).
        if (!level.isClientSide) {
            int damage = 0;
            for (FishingHook hook : bobbersOf(player)) {
                damage += hook.retrieve(stack);
            }
            if (damage > 0) {
                ItemStack original = stack.copy();
                stack.hurtAndBreak(damage, player, LivingEntity.getSlotForHand(hand));
                if (stack.isEmpty()) {
                    EventHooks.onPlayerDestroyItem(player, original, hand);
                }
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Все живые поплавки игрока — свои и чужие для него не差别яются. */
    private static List<FishingHook> bobbersOf(Player player) {
        AABB box = player.getBoundingBox().inflate(BOBBER_RANGE);
        return player.level().getEntitiesOfClass(FishingHook.class, box,
                hook -> !hook.isRemoved() && hook.getPlayerOwner() == player);
    }

    /**
     * Расводит поплавки веером, чтобы они не ложились в одну точку: чётный индекс
     * уходит влево от направления полёта, нечётный — вправо. Смещение добавляется
     * к скорости до нормализации, поэтому работает и при взгляде вверх-вниз.
     */
    private static void spread(FishingHook hook, int index) {
        Vec3 movement = hook.getDeltaMovement();
        // Горизонтальный вектор, перпендикулярный направлению полёта (поворот на 90° по Y).
        double sideX = -movement.z;
        double sideZ = movement.x;
        double length = Math.sqrt(sideX * sideX + sideZ * sideZ);
        if (length < 1.0E-4) {
            return;
        }
        double sign = (index % 2 == 0) ? -1.0 : 1.0;
        double k = sign * SPREAD / length;
        hook.setDeltaMovement(movement.add(sideX * k, 0.0, sideZ * k));
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_FISHING_ROD_ACTIONS.contains(ability);
    }
}
