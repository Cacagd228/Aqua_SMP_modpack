package me.nanorasmus.meowrelics.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Кладёт мешочки в сундуки.
 *
 * <p>Сам мод Relics больше не добавляет реликвии в лут напрямую — их лут-шаблоны
 * очищаются при старте сервера (см. {@code BalanceLoader}). Этот модификатор
 * закрывает освободившееся место: шанс на мешочек примерно тот же, что раньше
 * был на реликвию.
 *
 * <p>Каждый тип мешочка проверяется независимо, поэтому за один сундук может
 * выпасть и обычный, и редкий мешочек сразу.
 */
public class BagLootModifier extends LootModifier {

    private static final Logger LOG = LoggerFactory.getLogger("MeowRelics");

    /**
     * Шансы задаются в data-файле модификатора, поэтому в коде только умолчания.
     *
     * <p>{@code codecStart} добавляет служебное поле {@code conditions}, поэтому
     * параметры конструктора идут начиная с массива условий.
     */
    public static final MapCodec<BagLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance)
                    .and(ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("common_chance", 0.04F)
                            .forGetter(m -> m.commonChance))
                    .and(ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("rare_chance", 0.01F)
                            .forGetter(m -> m.rareChance))
                    .and(ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("hex_chance", 0.04F)
                            .forGetter(m -> m.hexChance))
                    .apply(instance, BagLootModifier::new));

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    /**
     * Тот же фильтр, что и у Relics: только сундуки. Иначе мешочки сыпались бы
     * в рыболовные приманки, руды и прочие таблицы, где их никто не ждёт.
     */
    private static final Pattern CHEST_TABLE = Pattern.compile("[\\w]+:chests\\/[\\w_\\/]*");

    private final float commonChance;
    private final float rareChance;
    private final float hexChance;

    public BagLootModifier(LootItemCondition[] conditions,
                           float commonChance, float rareChance, float hexChance) {
        super(conditions);
        this.commonChance = commonChance;
        this.rareChance = rareChance;
        this.hexChance = hexChance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        String table = context.getQueriedLootTableId().toString();
        if (!CHEST_TABLE.matcher(table).matches()) {
            return generatedLoot;
        }

        RandomSource random = context.getRandom();
        int before = generatedLoot.size();
        tryBag(generatedLoot, random, me.nanorasmus.meowrelics.registry.ModItems.RELIC_BAG, commonChance);
        tryBag(generatedLoot, random, me.nanorasmus.meowrelics.registry.ModItems.RELIC_BAG_RARE, rareChance);
        tryBag(generatedLoot, random, me.nanorasmus.meowrelics.registry.ModItems.RELIC_BAG_HEXCASTING, hexChance);

        int added = generatedLoot.size() - before;
        if (added > 0) {
            LOG.debug("В сундук {} добавлено мешочков: {}", table, added);
        }
        return generatedLoot;
    }

    private void tryBag(ObjectArrayList<ItemStack> loot, RandomSource random,
                        DeferredHolder<net.minecraft.world.item.Item, ?> bag, float chance) {
        // Шанс проверяем один раз на тип: иначе три независимые проверки на
        // сундук давали бы до трёх мешочков там, где задумано максимум один-два.
        if (random.nextFloat() < chance) {
            loot.add(new ItemStack(bag.get()));
        }
    }
}
