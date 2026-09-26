package me.nanorasmus.nanodev.hex_js.casting;

import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Морф-хекс: цель-игрок превращена в курицу.
 * <ul>
 *   <li>Входящий урон x0.2 (урон хекс НЕ развеивает).</li>
 *   <li>Локдаун: движение (обнуление горизонтальной скорости + отмена прыжка),
 *       атаки, ломание/установка/использование блоков, использование предметов,
 *       смена слотов, подбор/выброс, посадка на транспорт, жемчуг/хорус-телепорты.</li>
 *   <li>Магия режется {@code SILENCE} (вешается рядом в {@link OpMorphHex}).</li>
 *   <li>Масштаб 0.55 на время эффекта, возврат после (молоко/смерть/истечение).</li>
 *   <li>Снятие: молоко, смерть. Кудахтанье + пуф раз в ~2с.</li>
 * </ul>
 */
public final class MorphHexHandler {
    /** Доля входящего урона, проходящая по морфированному. */
    public static final float INCOMING_SHARE = 0.2f;
    /** Масштаб тела-курицы. */
    public static final double MORPH_SCALE = 0.55;

    /** Залоченный слот хотбара на момент морфа. */
    private static final ConcurrentHashMap<UUID, Integer> LOCKED_SLOTS = new ConcurrentHashMap<>();
    /** Исходный base scale на момент морфа. */
    private static final ConcurrentHashMap<UUID, Double> BASE_SCALES = new ConcurrentHashMap<>();
    /** Якорь позиции: движение клиентов авторитетно, поэтому держим телепортом. */
    private static final ConcurrentHashMap<UUID, double[]> ANCHORS = new ConcurrentHashMap<>();

    private MorphHexHandler() {
    }

    public static boolean isMorphed(LivingEntity e) {
        try {
            return e.hasEffect(HexEffects.MORPH_HEX);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Вызывается из руны: запомнить слот/масштаб/якорь, уменьшить, снять с транспорта. */
    public static void morph(ServerPlayer player) {
        UUID id = player.getUUID();
        LOCKED_SLOTS.putIfAbsent(id, player.getInventory().selected);
        ANCHORS.putIfAbsent(id, new double[]{player.getX(), player.getY(), player.getZ()});
        try {
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr != null) {
                BASE_SCALES.putIfAbsent(id, attr.getBaseValue());
                attr.setBaseValue(MORPH_SCALE);
            }
        } catch (Throwable ignored) {
        }
        try {
            player.stopRiding();
        } catch (Throwable ignored) {
        }
        try {
            player.stopFallFlying();
        } catch (Throwable ignored) {
        }
        try {
            player.stopUsingItem();
        } catch (Throwable ignored) {
        }
    }

    private static void restore(ServerPlayer player) {
        UUID id = player.getUUID();
        ANCHORS.remove(id);
        Integer slot = LOCKED_SLOTS.remove(id);
        if (slot != null) {
            try {
                int n = player.getInventory().items.size();
                if (slot >= 0 && slot < n) {
                    player.getInventory().selected = slot;
                }
            } catch (Throwable ignored) {
            }
        }
        Double base = BASE_SCALES.remove(id);
        if (base != null) {
            try {
                var attr = player.getAttribute(Attributes.SCALE);
                if (attr != null) {
                    attr.setBaseValue(base);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    // ---------- урон ----------

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        // Исходящий от морфированного (долетевшее до каста, напр. стрела): в ноль.
        try {
            var src = event.getSource().getEntity();
            if (src instanceof LivingEntity attacker && isMorphed(attacker)) {
                event.setNewDamage(0);
                return;
            }
        } catch (Throwable ignored) {
        }
        // Входящий по морфированному: только 20%.
        if (!isMorphed(victim)) {
            return;
        }
        float orig = event.getNewDamage();
        if (orig <= 0) {
            return;
        }
        event.setNewDamage(orig * INCOMING_SHARE);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isMorphed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // ---------- блоки ----------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof Player p && isMorphed(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p && isMorphed(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isMorphed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isMorphed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isMorphed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isMorphed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isMorphed(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player p && isMorphed(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof Player p && isMorphed(p)) {
            event.setCanceled(true);
        }
    }

    // ---------- движение/транспорт/телепорты/предметы ----------

    // Прыжок отдельно не отменяется (LivingJumpEvent не cancellable в 1.21.1) —
    // его давит якорь телепорта в onPlayerTick: подпрыгнул — вернулся.

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof Player p && isMorphed(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        if (event.getEntity() instanceof Player p && isMorphed(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ChorusFruit event) {
        if (event.getEntity() instanceof Player p && isMorphed(p)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof Player p && isMorphed(p)) {
            event.setCanceled(true);
        }
    }

    // ---------- тик: слоты, скорость, масштаб, кудахтанье ----------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UUID id = player.getUUID();
        if (!isMorphed(player)) {
            // Эффект сошёл (молоко/время/смерть) — вернуть слот и рост.
            if (LOCKED_SLOTS.containsKey(id) || BASE_SCALES.containsKey(id) || ANCHORS.containsKey(id)) {
                restore(player);
            }
            return;
        }
        double[] anchor = ANCHORS.get(id);
        if (anchor == null) {
            anchor = new double[]{player.getX(), player.getY(), player.getZ()};
            ANCHORS.put(id, anchor);
        }
        // Якорь: движение игроков клиент-авторитетно, поэтому любое смещение
        // дальше 0.15 блока возвращаем телепортом (прыжки, бег, нокбэк — всё).
        try {
            double dx = player.getX() - anchor[0];
            double dy = player.getY() - anchor[1];
            double dz = player.getZ() - anchor[2];
            if (dx * dx + dy * dy + dz * dz > 0.0225) {
                player.connection.teleport(anchor[0], anchor[1], anchor[2],
                        player.getYRot(), player.getXRot(), java.util.Set.of());
            }
        } catch (Throwable ignored) {
        }
        try {
            if (player.isPassenger()) {
                player.stopRiding();
            }
        } catch (Throwable ignored) {
        }
        // Замок хотбара.
        Integer locked = LOCKED_SLOTS.get(id);
        if (locked == null) {
            LOCKED_SLOTS.put(id, player.getInventory().selected);
        } else if (player.getInventory().selected != locked) {
            player.getInventory().selected = locked;
        }
        // Остановить использование (еда/лук/щит).
        try {
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
        } catch (Throwable ignored) {
        }
        // Полный стоп горизонтали (ходьба, спринт, полёт, плавание, нокбэк).
        try {
            var m = player.getDeltaMovement();
            if (m.x != 0 || m.z != 0) {
                player.setDeltaMovement(0, m.y, 0);
            }
            player.setJumping(false);
        } catch (Throwable ignored) {
        }
        try {
            if (player.isFallFlying()) {
                player.stopFallFlying();
            }
        } catch (Throwable ignored) {
        }
        // Кудахтанье + пушок.
        if (player.tickCount % 40 == 0) {
            try {
                BlockPos pos = player.blockPosition();
                player.level().playSound(null, pos, SoundEvents.CHICKEN_AMBIENT,
                        SoundSource.PLAYERS, 1.0f, 0.9f + player.getRandom().nextFloat() * 0.3f);
            } catch (Throwable ignored) {
            }
            try {
                if (player.serverLevel() != null) {
                    player.serverLevel().sendParticles(
                            net.minecraft.core.particles.ParticleTypes.POOF,
                            player.getX(), player.getY() + 0.5, player.getZ(),
                            3, 0.25, 0.25, 0.25, 0.02);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Чистим кэш без реставрации (игрока уже нет); при входе масштаб вернёт тик.
            LOCKED_SLOTS.remove(player.getUUID());
            // scale чинить некому — при следующем входе тик восстановит по BASE_SCALES.
        }
    }
}
