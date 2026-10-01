package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.common.lib.HexAttributes;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.addon.HexArtifactsItems;
import me.nanorasmus.nanodev.hex_js.helpers.CurioHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Песочные часы: реген маны работает только на месте.
 *
 * <p>Полностью переопределяет {@code MANA_REGEN}, а не просто добавляет бонус:
 * <ul>
 *   <li>носитель стоит (горизонтальная скорость ниже
 *       {@link #STANDING_EPSILON}) — реген {@value #REGEN_MULTIPLIER}× от того,
 *       что дали бы остальные источники суммарно;</li>
 *   <li>носитель двигается — реген <b>ноль</b>, базовый реген hexcasting
 *       тоже отключается ({@code ManaHelper} не тикает реген при rate &lt;= 0).</li>
 * </ul>
 *
 * <p>Поворот на месте движением не считается — проверяется только горизонтальная
 * скорость.
 *
 * <p>Как считается: модификатор сначала снимается, чтобы узнать «естественное»
 * значение атрибута (база + амулеты + стаки громоотвода), затем ставится разница
 * между ним и целью. Поэтому бонусы громоотвода и амулетов не теряются, а
 * просто множатся/обнуляются вместе с ними.
 *
 * <p>Значение разницы кэшируется, чтобы не дёргать пересчёт атрибутов каждый
 * тик: пересчёт применяется только когда цель реально изменилась.
 */
public final class HourglassHandler {
    /** Реген при неподвижности: 1.10 = +10% к тому, что дали бы остальные. */
    public static final double REGEN_MULTIPLIER = 1.10;
    /** Порог «стоит на месте», блоков за тик (только горизонталь). */
    public static final double STANDING_EPSILON = 0.02;

    /** Модификатор часов; величина считается динамически. */
    public static final ResourceLocation HOURGLASS_ID = HexJS.modLoc("hourglass_mana_regen");

    /** UUID -> последняя выставленная разница (кэш, чтобы не пересчитывать зря). */
    private static final ConcurrentHashMap<UUID, Double> APPLIED = new ConcurrentHashMap<>();

    private HourglassHandler() {
    }

    /** Надеты ли песочные часы (любой слот Curios). */
    public static boolean wearsHourglass(LivingEntity entity) {
        return entity instanceof ServerPlayer player
                && CurioHelper.hasCurio(player, HexArtifactsItems.HOURGLASS.get());
    }

    /** Горизонтальная скорость ниже порога — носитель стоит. */
    private static boolean isStanding(ServerPlayer player) {
        double sq = player.getDeltaMovement().horizontalDistanceSqr();
        return sq < STANDING_EPSILON * STANDING_EPSILON;
    }

    private static AttributeInstance regenAttr(ServerPlayer player) {
        return player.getAttribute(
                BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_REGEN));
    }

    /**
     * Пересчитывает бонус часов для одного игрока.
     *
     * @param removeOnly снять бонус и забыть игрока (например, часы сняли)
     */
    private static void refresh(ServerPlayer player, boolean removeOnly) {
        AttributeInstance attr = regenAttr(player);
        if (attr == null) {
            return;
        }
        if (removeOnly) {
            APPLIED.remove(player.getUUID());
            attr.removeModifier(HOURGLASS_ID);
            return;
        }
        // Убираем свой вклад, чтобы узнать «естественное» значение.
        attr.removeModifier(HOURGLASS_ID);
        double natural = attr.getValue();

        // MANA_REGEN — RangedAttribute с нижней границей 0: и множитель, и
        // обнуление лежат внутри допустимого диапазона, схлопывания не будет.
        double target = isStanding(player) ? natural * REGEN_MULTIPLIER : 0.0;
        double delta = target - natural;

        Double cached = APPLIED.get(player.getUUID());
        if (cached != null && Math.abs(cached - delta) < 1e-9) {
            return; // цель не изменилась — не трогаем атрибут зря
        }
        if (Math.abs(delta) < 1e-9) {
            APPLIED.remove(player.getUUID());
            return;
        }
        attr.addTransientModifier(new AttributeModifier(
                HOURGLASS_ID, delta, AttributeModifier.Operation.ADD_VALUE));
        APPLIED.put(player.getUUID(), delta);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return; // один прогон в тик
        }
        if (APPLIED.isEmpty() && level.players().isEmpty()) {
            return;
        }
        for (ServerPlayer player : new ArrayList<>(level.players())) {
            boolean worn = wearsHourglass(player);
            boolean tracked = APPLIED.containsKey(player.getUUID());
            if (!worn && !tracked) {
                continue; // ни часов, ни активного бонуса — не трогаем
            }
            refresh(player, !worn);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            dropBonus(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            dropBonus(player);
        }
    }

    /** Снимает бонус и чистит кэш. */
    private static void dropBonus(ServerPlayer player) {
        APPLIED.remove(player.getUUID());
        AttributeInstance attr = regenAttr(player);
        if (attr != null) {
            attr.removeModifier(HOURGLASS_ID);
        }
    }
}
