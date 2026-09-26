package me.nanorasmus.nanodev.hex_js.addon.scroll;

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
import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.common.casting.PatternRegistryManifest;
import at.petrak.hexcasting.common.items.storage.ItemScroll;
import at.petrak.hexcasting.xplat.IXplatAbstractions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Global loot modifier dropping pattern scrolls into chests.
 *
 * <p>Applies to every loot table whose path starts with {@code chests/} (vanilla
 * and modded alike). Base chance is {@code commonChance} (default 5%); richer
 * vanilla tables roll {@code richChance} / {@code treasureChance}. Chances are
 * datapack-tunable via {@code data/meowhex/loot_modifiers/scroll_pages.json}.
 */
public class ScrollPageLootModifier extends LootModifier {
    public static final Supplier<MapCodec<ScrollPageLootModifier>> CODEC =
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
                    ).apply(inst, ScrollPageLootModifier::new)
            ));

    private final float commonChance;
    private final float richChance;
    private final float treasureChance;

    public ScrollPageLootModifier(LootItemCondition[] conditionsIn, float commonChance,
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
            String scrollId = ScrollItems.randomScrollId(context.getRandom());
            ItemStack stack = new ItemStack(ScrollItems.BY_ID.get(scrollId).get());
            writePerWorldStrokes(stack, scrollId, context);
            generatedLoot.add(stack);
        }
        return generatedLoot;
    }

    /**
     * Great-spell scrolls carry the real world-specific strokes (like vanilla
     * dungeon scrolls did): picks one random per-world op from the scroll's page
     * and bakes its canonical strokes into the stack. {@link ItemPatternScroll}
     * can then hang the scroll on a wall for studying; pickup still grants the
     * page advancement as usual.
     */
    private void writePerWorldStrokes(ItemStack stack, String scrollId, LootContext context) {
        List<String> ops = ScrollDefs.SCROLL_TO_OPS.getOrDefault(scrollId, List.of());
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
        return ChestTiers.chanceFor(tablePath, commonChance, richChance, treasureChance);
    }

    @Override
    public MapCodec<ScrollPageLootModifier> codec() {
        return ScrollItems.SCROLL_PAGES.get();
    }
}
