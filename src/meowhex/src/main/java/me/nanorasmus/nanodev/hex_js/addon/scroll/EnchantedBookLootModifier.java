package me.nanorasmus.nanodev.hex_js.addon.scroll;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * Global loot modifier dropping enchanted books with the mod's treasure-only
 * enchantments ({@code meowhex:mana_break}, {@code meowhex:magic_protection}).
 *
 * <p>Chests ({@code chests/*}, vanilla and modded) roll tiered chances via
 * {@link ChestTiers}; the vanilla fishing treasure table rolls
 * {@code fishingChance}. The book carries a random listed enchantment at a
 * random level 1..max. Everything is datapack-tunable via
 * {@code data/meowhex/loot_modifiers/enchanted_books.json}.
 */
public class EnchantedBookLootModifier extends LootModifier {
    public static final Supplier<MapCodec<EnchantedBookLootModifier>> CODEC =
            Suppliers.memoize(() -> RecordCodecBuilder.mapCodec(
                    inst -> codecStart(inst).and(
                            Codec.STRING.listOf().fieldOf("books")
                                    .forGetter(it -> it.books)
                    ).and(
                            Codec.FLOAT.fieldOf("chest_common_chance").orElse(0.02f)
                                    .forGetter(it -> it.chestCommonChance)
                    ).and(
                            Codec.FLOAT.fieldOf("chest_rich_chance").orElse(0.05f)
                                    .forGetter(it -> it.chestRichChance)
                    ).and(
                            Codec.FLOAT.fieldOf("chest_treasure_chance").orElse(0.10f)
                                    .forGetter(it -> it.chestTreasureChance)
                    ).and(
                            Codec.FLOAT.fieldOf("fishing_chance").orElse(0.10f)
                                    .forGetter(it -> it.fishingChance)
                    ).apply(inst, EnchantedBookLootModifier::new)
            ));

    private static final ResourceLocation FISHING_TREASURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "gameplay/fishing/treasure");

    private final List<String> books;
    private final float chestCommonChance;
    private final float chestRichChance;
    private final float chestTreasureChance;
    private final float fishingChance;

    public EnchantedBookLootModifier(LootItemCondition[] conditionsIn, List<String> books,
            float chestCommonChance, float chestRichChance, float chestTreasureChance,
            float fishingChance) {
        super(conditionsIn);
        this.books = List.copyOf(books);
        this.chestCommonChance = chestCommonChance;
        this.chestRichChance = chestRichChance;
        this.chestTreasureChance = chestTreasureChance;
        this.fishingChance = fishingChance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
            LootContext context) {
        ResourceLocation tableId = context.getQueriedLootTableId();
        if (tableId == null || books.isEmpty()) {
            return generatedLoot;
        }
        float chance;
        if (FISHING_TREASURE.equals(tableId)) {
            chance = fishingChance;
        } else if (tableId.getPath().startsWith("chests/")) {
            chance = ChestTiers.chanceFor(tableId.getPath(),
                    chestCommonChance, chestRichChance, chestTreasureChance);
        } else {
            return generatedLoot;
        }
        if (context.getRandom().nextFloat() < chance) {
            ItemStack book = makeRandomBook(context);
            if (!book.isEmpty()) {
                generatedLoot.add(book);
            }
        }
        return generatedLoot;
    }

    private ItemStack makeRandomBook(LootContext context) {
        var lookup = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        String bookId = books.get(context.getRandom().nextInt(books.size()));
        Holder<Enchantment> holder = lookup
                .get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(bookId)))
                .orElse(null);
        if (holder == null) {
            return ItemStack.EMPTY;
        }
        int maxLevel = Math.max(1, holder.value().getMaxLevel());
        int level = 1 + context.getRandom().nextInt(maxLevel);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        var stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(holder, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return book;
    }

    @Override
    public MapCodec<EnchantedBookLootModifier> codec() {
        return ScrollItems.ENCHANTED_BOOKS.get();
    }
}
