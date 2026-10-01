package me.nanorasmus.meowrelics.relic;

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
 * Выбирает предмет meowhex (форк hexcasting) для мешочка hexcasting.
 *
 * <p>Предметы ищутся по id через реестр, а не через {@code import} классов
 * meowhex. Причина практическая: у нас нет общей сборки, а жёсткая
 * зависимость уронила бы мод, если в окружении окажется ванильный hexcasting
 * или его форк под другим modId. Список проверяется на каждом вскрытии, так что
 * состав начинки можно менять в одном месте без пересборки.
 *
 * <p>Веса заданы так, чтобы ожерелья выстраивались в убывающую лестницу: чем
 * дороже ожерелье, тем реже оно падает. Мана-ягода — расходник, поэтому она
 * самая частая и задаёт тон всему остальному.
 *
 * <p>Комментаторы, Pattern Reader и прицел в начинку не входят: первые —
 * чисто PvP-голос и дублирование гайда по паттернам, третий выведен из ротации.
 * Грозовой стержень и песочные часы добавлены позже — они из новых артефактов
 * meowhex и стоят в середине таблицы.
 */
public final class ArtifactRandomizer {

    private static final Logger LOG = LoggerFactory.getLogger("MeowRelics");

    private static final List<Entry> POOL = List.of(
            // id, вес (условные единицы, не проценты)
            new Entry("mana_berry", 40),
            new Entry("amethyst_necklace", 25),
            new Entry("charged_amethyst_necklace", 15),
            new Entry("lightning_rod", 10),
            new Entry("hourglass", 10),
            new Entry("self_torture_ring", 12),
            new Entry("overloaded_necklace", 5));

    private ArtifactRandomizer() {
    }

    /**
     * @return стек предмета либо {@link ItemStack#EMPTY}, если meowhex не установлен
     */
    public static ItemStack roll(RandomSource random) {
        List<Weighted> found = new ArrayList<>(POOL.size());
        for (Entry entry : POOL) {
            // getOptional возвращает пустой Optional, если предмета нет —
            // это и есть проверка наличия мода.
            BuiltInRegistries.ITEM.getOptional(entry.id())
                    .ifPresent(item -> found.add(new Weighted(item, entry.weight())));
        }
        if (found.isEmpty()) {
            LOG.warn("Предметы meowhex не найдены в реестре. Мешочек hexcasting вскрыть нечем.");
            return ItemStack.EMPTY;
        }

        int total = 0;
        for (Weighted weighted : found) {
            total += weighted.weight();
        }

        // Все веса нулевые — откатываемся на равномерный выбор, чтобы мешочек
        // не стал бесполезным из-за неудачной правки конфига.
        if (total <= 0) {
            return new ItemStack(found.get(random.nextInt(found.size())).item());
        }

        int roll = random.nextInt(total);
        Item chosen = found.get(found.size() - 1).item();
        for (Weighted weighted : found) {
            roll -= weighted.weight();
            if (roll < 0) {
                chosen = weighted.item();
                break;
            }
        }

        LOG.debug("Мешочек hexcasting выдал: {}", chosen);
        return new ItemStack(chosen);
    }

    /** Запись пула: id предмета и его вес. */
    private record Entry(String path, int weight) {
        ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath("meowhex", path);
        }
    }

    /** Найденный в реестре предмет вместе с весом. */
    private record Weighted(Item item, int weight) {
    }
}
