package me.nanorasmus.nanodev.hex_js.addon.scroll;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.client.gui.PatternTooltipComponent;
import at.petrak.hexcasting.common.casting.PatternRegistryManifest;
import at.petrak.hexcasting.common.entities.EntityWallScroll;
import at.petrak.hexcasting.common.items.storage.ItemScroll;
import at.petrak.hexcasting.common.misc.PatternTooltip;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;
import java.util.Optional;

/**
 * A pattern scroll: a chest-loot trophy permanently unlocking one Patchouli
 * book page (see {@link ScrollDefs}). Picking it up grants the matching
 * {@code meowhex:scrolls/*} advancement, which both reveals the page and
 * allows casting the patterns described on it ({@link ScrollGate}).
 */
public class ItemPatternScroll extends Item {
    private final String scrollId;

    public ItemPatternScroll(Properties properties, String scrollId) {
        super(properties);
        this.scrollId = scrollId;
    }

    public String scrollId() {
        return scrollId;
    }

    /**
     * Any scroll can be hung on a wall like a vanilla scroll, so its rune can
     * be studied and copied by hand. Scrolls carrying real per-world strokes
     * (great-spell loot) hang as-is; plain unlock scrolls get the rune's
     * strokes baked into the hung copy on the spot (canonically for normal
     * patterns, world-specifically for great spells on the server). Breaking
     * the wall scroll returns the item. The hand stack itself is never
     * modified and stays a page key.
     */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack itemstack = ctx.getItemInHand();
        ItemStack toHang = itemstack.copy();
        toHang.setCount(1);
        if (!NBTHelper.hasCompound(toHang, ItemScroll.TAG_PATTERN)) {
            HexPattern known = resolveHangPattern(scrollId, ctx.getLevel());
            if (known == null) {
                return InteractionResult.PASS;
            }
            NBTHelper.putCompound(toHang, ItemScroll.TAG_PATTERN, known.serializeToNBT());
        }
        var posClicked = ctx.getClickedPos();
        var direction = ctx.getClickedFace();
        var posInFront = posClicked.relative(direction);
        Player player = ctx.getPlayer();
        if (player != null && !this.mayPlace(player, direction, itemstack, posInFront)) {
            return InteractionResult.FAIL;
        }
        Level level = ctx.getLevel();
        var scrollEntity = new EntityWallScroll(level, posInFront, direction, toHang, false, 3);
        var stackTag = itemstack.get(DataComponents.CUSTOM_DATA);
        if (stackTag != null) {
            EntityType.updateCustomEntityTag(level, player, scrollEntity, stackTag);
        }

        if (scrollEntity.survives()) {
            if (!level.isClientSide) {
                scrollEntity.playPlacementSound();
                level.gameEvent(player, GameEvent.ENTITY_PLACE, posClicked);
                level.addFreshEntity(scrollEntity);
            }

            itemstack.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.CONSUME;
        }
    }

    // [VanillaCopy] of HangingEntityItem
    protected boolean mayPlace(Player player, Direction direction, ItemStack stack, BlockPos pos) {
        return !direction.getAxis().isVertical() && player.mayUseItemAt(pos, direction, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
            TooltipFlag flag) {
        String nameKey = ScrollDefs.SCROLL_TO_NAME_KEY.getOrDefault(scrollId, scrollId);
        String bookKey = ScrollDefs.SCROLL_TO_BOOK_KEY.getOrDefault(scrollId, "");
        lines.add(Component.translatable("item.meowhex.pattern_scroll.unlocks",
                Component.translatable(nameKey)).withStyle(ChatFormatting.GRAY));
        if (!bookKey.isEmpty()) {
            lines.add(Component.translatable("item.meowhex.pattern_scroll.book",
                    Component.translatable(bookKey)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * Rune preview on hover, like vanilla scrolls carrying strokes — resolved
     * from the scroll's first op via the action registry (the same prototype
     * the book renders for {@code op_id} pages). Scrolls covering no ops
     * (pure lore pages) show no preview.
     */
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var compound = NBTHelper.getCompound(stack, ItemScroll.TAG_PATTERN);
        if (compound != null) {
            var pattern = HexPattern.fromNBT(compound);
            return Optional.of(new PatternTooltip(pattern,
                    NBTHelper.hasString(stack, ItemScroll.TAG_OP_ID)
                            ? PatternTooltipComponent.ANCIENT_BG
                            : PatternTooltipComponent.PRISTINE_BG));
        }
        HexPattern known = prototypeForScroll(scrollId);
        if (known != null) {
            return Optional.of(new PatternTooltip(known, PatternTooltipComponent.PRISTINE_BG));
        }
        return Optional.empty();
    }

    /** Canonical display strokes of the scroll's first op, if resolvable. */
    static HexPattern prototypeForScroll(String scrollId) {
        List<String> ops = ScrollDefs.SCROLL_TO_OPS.getOrDefault(scrollId, List.of());
        var registry = IXplatAbstractions.INSTANCE.getActionRegistry();
        for (String op : ops) {
            try {
                var key = ResourceKey.create(registry.key(), ResourceLocation.parse(op));
                if (!registry.containsKey(key)) {
                    continue;
                }
                ActionRegistryEntry entry = registry.get(key);
                if (entry != null && entry.prototype() != null) {
                    return entry.prototype();
                }
            } catch (RuntimeException ignored) {
                // registry not ready or op unknown — try the next op
            }
        }
        return null;
    }

    /**
     * Strokes to bake into the hung wall-scroll copy: the registry prototype
     * for normal patterns; real world-specific strokes for great spells when
     * a server level is available.
     */
    private static HexPattern resolveHangPattern(String scrollId, Level level) {
        List<String> ops = ScrollDefs.SCROLL_TO_OPS.getOrDefault(scrollId, List.of());
        var registry = IXplatAbstractions.INSTANCE.getActionRegistry();
        ServerLevel overworld = level.getServer() != null ? level.getServer().overworld() : null;
        HexPattern fallback = null;
        for (String op : ops) {
            ResourceKey<ActionRegistryEntry> key;
            try {
                key = ResourceKey.create(registry.key(), ResourceLocation.parse(op));
                if (!registry.containsKey(key)) {
                    continue;
                }
            } catch (RuntimeException ignored) {
                continue;
            }
            if (overworld != null && HexUtils.isOfTag(registry, key, HexTags.Actions.PER_WORLD_PATTERN)) {
                var real = PatternRegistryManifest.getCanonicalStrokesPerWorld(key, overworld);
                if (real != null) {
                    return real;
                }
            }
            if (fallback == null) {
                try {
                    ActionRegistryEntry entry = registry.get(key);
                    if (entry != null && entry.prototype() != null) {
                        fallback = entry.prototype();
                    }
                } catch (RuntimeException ignored) {
                    // try the next op
                }
            }
        }
        return fallback;
    }
}
