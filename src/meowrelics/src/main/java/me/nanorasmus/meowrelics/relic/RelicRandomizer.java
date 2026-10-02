package me.nanorasmus.meowrelics.relic;

import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Выбирает случайную реликвию для выдачи из мешочка.
 *
 * <p>Часть реликвий исключена из ротации списком {@link #EXCLUDED}. Их по-прежнему
 * можно получить иным способом (командами, вручную в сундук) — выпадать из мешочков
 * они просто перестают.
 *
 * <p>Редкость берётся не с потолка, а из настоящих лут-весов реликвий: сумма
 * весов всех записей её {@code LootTemplate}. Именно из этих шаблонов мод Relics
 * строит свой {@code RelicLootModifier}, поэтому обычный мешочек выдаёт реликвии
 * с той же относительной частотой, что и сундуки.
 *
 * <p>Для редкого мешочка есть второй слой — «мощь» реликвии. Это сумма
 * {@code initialMaxLevel * requiredPoints} по всем способностям, то есть ровно
 * то, во сколько уровней можно прокачать реликвию (та же формула, что у
 * {@code RelicData.calculateMaxLevel()}). Редкость не забывается: вес
 * реликвии — это её лут-вес, умноженный на мощь. Так реликвия с большим
 * потенциалом выпадает чаще, но не вытесняет остальные полностью.
 *
 * <p>Пул строится на каждый вызов, а не кэшируется: {@code /meowrelics reload}
 * пересобирает шаблоны реликвий, и мешочки сразу подхватывают новые веса.
 * Перебор тридцатка предметов дешевле, чем следить за инвалидацией кэша.
 */
public final class RelicRandomizer {

    private static final Logger LOG = LoggerFactory.getLogger("MeowRelics");

    /**
     * Реликвии, убранные из ротации мешочков.
     *
     * <p>Хранится в {@link #EXCLUDED}: список лежит в balance.json, чтобы менять
     * пул можно было без пересборки мода. Если файл не прочитан, ротация
     * останется полной.
     */
    private static volatile java.util.Set<String> EXCLUDED = java.util.Set.of();

    /** Заменяет список исключений. Вызывается загрузчиком баланса. */
    public static void setExcluded(java.util.Collection<String> ids) {
        EXCLUDED = ids == null ? java.util.Set.of()
                : java.util.Set.copyOf(ids);
        LOG.info("Исключено из пула мешочков: {}", EXCLUDED.size());
    }

    private RelicRandomizer() {
    }

    /** Обычный мешочек: реликвия по лут-весам. */
    public static ItemStack rollByLootWeight(RandomSource random) {
        return roll(random, false);
    }

    /** Редкий мешочек: тот же пул, но вес завышен в пользу мощных реликвий. */
    public static ItemStack rollByPower(RandomSource random) {
        return roll(random, true);
    }

    /**
     * Выкатывает новый стек реликвии.
     *
     * <p>Сам мод Relics так же поступает в луте: берёт
     * {@code item.getDefaultInstance()}, а качество статов раскатывается лениво
     * при первом обращении ({@code AbilityStatData.getComponent()}). Поэтому
     * отдельная «набивка» реликвии не нужна — стек и так готовый.
     *
     * @return стек реликвии либо {@link ItemStack#EMPTY}, если реликвий нет
     */
    private static ItemStack roll(RandomSource random, boolean favourPower) {
        List<Entry> pool = collectPool();
        if (pool.isEmpty()) {
            LOG.warn("В реестре нет ни одной реликвии — мешочек вскрыть нечем.");
            return ItemStack.EMPTY;
        }

        int total = 0;
        for (Entry entry : pool) {
            total += entry.weight(favourPower);
        }

        Item chosen;
        if (total <= 0) {
            // Все веса нулевые (например, loot-шаблоны выключили в балансе) —
            // откатываемся на равномерный выбор, чтобы мешочек не был бесполезным.
            chosen = pool.get(random.nextInt(pool.size())).item;
        } else {
            int roll = random.nextInt(total);
            chosen = pool.get(pool.size() - 1).item;
            for (Entry entry : pool) {
                roll -= entry.weight(favourPower);
                if (roll < 0) {
                    chosen = entry.item;
                    break;
                }
            }
        }

        LOG.debug("Мешочек выдал: {}", BuiltInRegistries.ITEM.getKey(chosen));
        return new ItemStack(chosen);
    }

    /** Собирает список реликвий с лут-весом и мощностью, кроме исключённых. */
    private static List<Entry> collectPool() {
        List<Entry> pool = new ArrayList<>();
        java.util.Set<String> excluded = EXCLUDED;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof IRelicItem relic) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null && excluded.contains(id.toString())) {
                    continue;
                }
                pool.add(new Entry(item, lootWeight(relic), power(relic)));
            }
        }
        return pool;
    }

    /** Сумма весов всех лут-записей реликвии. */
    private static int lootWeight(IRelicItem relic) {
        int weight = 0;
        for (LootEntry entry : relic.getDefaultLootTemplate().getEntries()) {
            weight += Math.max(0, entry.getWeight());
        }
        return weight;
    }

    /**
     * «Мощь» реликвии — суммарный максимальный уровень её способностей.
     *
     * <p>Считается по шаблону, а не по данным стака, поэтому результат зависит
     * только от баланса и одинаков для всех экземпляров одной реликвии.
     */
    private static int power(IRelicItem relic) {
        RelicTemplate template = relic.getDefaultRelicTemplate();
        int power = 0;
        for (AbilityTemplate ability : template.getAbilities().getAbilities().values()) {
            power += ability.getInitialMaxLevel() * ability.getRequiredPoints();
        }
        return power;
    }

    /** Одно ведение в пуле: предмет, его лут-вес и мощность. */
    private record Entry(Item item, int lootWeight, int power) {

        /**
         * Вес для конкретного мешочка. Нижняя граница 1, чтобы реликвия с
         * нулевой лут-таблицей не выпала из ротации совсем.
         */
        int weight(boolean favourPower) {
            if (!favourPower) {
                return Math.max(1, lootWeight);
            }
            return Math.max(1, lootWeight * Math.max(1, power));
        }
    }
}
