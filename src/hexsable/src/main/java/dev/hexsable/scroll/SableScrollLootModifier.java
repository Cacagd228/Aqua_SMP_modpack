package dev.hexsable.scroll;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.common.casting.PatternRegistryManifest;
import at.petrak.hexcasting.common.items.storage.ItemScroll;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Глобальный лут-модификатор, подкидывающий свитки рун в сундуки.
 *
 * <p>Применяется к каждому лут-столу, чей путь начинается с {@code chests/}
 * (ванилла и моды). Базовый шанс — {@code commonChance} (по умолчанию 5%);
 * более богатые ванильные столы крутят {@code richChance} /
 * {@code treasureChance}. Шансы крутятся датапаком через
 * {@code data/hexsable/loot_modifiers/sable_scrolls.json}.
 *
 * <p>По образцу {@code me.nanorasmus.nanodev.hex_js.addon.scroll.ScrollPageLootModifier}.
 */
public class SableScrollLootModifier extends LootModifier {
    public static final Supplier<MapCodec<SableScrollLootModifier>> CODEC =
            Suppliers.memoize(() -> RecordCodecBuilder.mapCodec(
                    inst -> codecStart(inst).and(
                            Codec.FLOAT.fieldOf("common_chance").orElse(0.05f)
                                    .forGetter(it -> it.commonChance)
                    ).and(
                            Codec.FLOAT.fieldOf("rich_chance").orElse(0.12f)
                                    .forGetter(it -> it.richChance)
                    ).and(
                            Codec.FLOAT.fieldOf("treasure_chance").orElse(0.25f)
                                    .forGetter(it -> it.treasureChance)
                    ).apply(inst, SableScrollLootModifier::new)
            ));

    private final float commonChance;
    private final float richChance;
    private final float treasureChance;

    public SableScrollLootModifier(LootItemCondition[] conditionsIn, float commonChance,
            float richChance, float treasureChance) {
        super(conditionsIn);
        this.commonChance = commonChance;
        this.richChance = richChance;
        this.treasureChance = treasureChance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
            LootContext context) {
        ResourceLocation tableId = context.getQueriedLootTableId();
        if (tableId == null || !tableId.getPath().startsWith("chests/")) {
            return generatedLoot;
        }
        float chance = chanceFor(tableId.getPath());
        if (context.getRandom().nextFloat() < chance) {
            String scrollId = SableScrollItems.randomScrollId(context.getRandom());
            ItemStack stack = new ItemStack(SableScrollItems.BY_ID.get(scrollId).get());
            writePerWorldStrokes(stack, scrollId, context);
            generatedLoot.add(stack);
        }
        return generatedLoot;
    }

    /**
     * Свитки great spells несут настоящие построчные штрихи своего мира (как
     * ванильные данж-свитки): выбирается один случайный per-world op со
     * страницы свитка и его канонические штрихи запекаются в стак.
     * {@link ItemSableScroll} такой свиток умеет вешать на стену для изучения;
     * поднятие всё равно выдаёт advancement страницы.
     */
    private void writePerWorldStrokes(ItemStack stack, String scrollId, LootContext context) {
        List<String> ops = SableScrollDefs.SCROLL_TO_OPS.getOrDefault(scrollId, List.of());
        if (ops.isEmpty()) {
            return;
        }
        var server = context.getLevel().getServer();
        if (server == null) {
            return;
        }
        var registry = IXplatAbstractions.INSTANCE.getActionRegistry();
        var perWorldOps = new ArrayList<ResourceKey<ActionRegistryEntry>>();
        for (String op : ops) {
            var key = ResourceKey.create(registry.key(), ResourceLocation.parse(op));
            if (registry.containsKey(key)
                    && HexUtils.isOfTag(registry, key, HexTags.Actions.PER_WORLD_PATTERN)) {
                perWorldOps.add(key);
            }
        }
        if (perWorldOps.isEmpty()) {
            return;
        }
        var chosen = perWorldOps.get(context.getRandom().nextInt(perWorldOps.size()));
        var pattern = PatternRegistryManifest.getCanonicalStrokesPerWorld(chosen, server.overworld());
        if (pattern == null) {
            return;
        }
        NBTHelper.putString(stack, ItemScroll.TAG_OP_ID, chosen.location().toString());
        NBTHelper.putCompound(stack, ItemScroll.TAG_PATTERN, pattern.serializeToNBT());
    }

    private float chanceFor(String tablePath) {
        return SableChestTiers.chanceFor(tablePath, commonChance, richChance, treasureChance);
    }

    @Override
    public MapCodec<SableScrollLootModifier> codec() {
        return SableScrollItems.SABLE_SCROLLS.get();
    }
}
