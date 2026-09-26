package dev.hexsable.scroll;

import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.gui.PatternTooltipComponent;
import at.petrak.hexcasting.common.entities.EntityWallScroll;
import at.petrak.hexcasting.common.items.storage.ItemScroll;
import at.petrak.hexcasting.common.misc.PatternTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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
 * Свиток рун: трофей из сундуков, навсегда открывающий одну страницу
 * thehexbook (см. {@link SableScrollDefs}). Поднятие выдаёт парный
 * {@code hexsable:scrolls/*} advancement, который показывает страницу
 * и разрешает каст описанных на ней узоров ({@link SableScrollGate}).
 *
 * По образцу {@code me.nanorasmus.nanodev.hex_js.addon.scroll.ItemPatternScroll}.
 */
public class ItemSableScroll extends Item {
    private final String scrollId;

    public ItemSableScroll(Properties properties, String scrollId) {
        super(properties);
        this.scrollId = scrollId;
    }

    public String scrollId() {
        return scrollId;
    }

    /**
     * Свиток вешается на стену как ванильный: штрихи руны известны статически
     * ({@link SableScrollDefs#patternForScroll}), поэтому запекаем их в вешаемую
     * копию прямо на месте — таскать свиток из данжа необязательно. Ручной стак
     * при этом не меняется (остаётся ключом-страницей).
     */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack itemstack = ctx.getItemInHand();
        ItemStack toHang = itemstack.copy();
        toHang.setCount(1);
        if (!NBTHelper.hasCompound(toHang, ItemScroll.TAG_PATTERN)) {
            HexPattern known = SableScrollDefs.patternForScroll(scrollId);
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
        String nameKey = SableScrollDefs.SCROLL_TO_NAME_KEY.getOrDefault(scrollId, scrollId);
        String bookKey = SableScrollDefs.SCROLL_TO_BOOK_KEY.getOrDefault(scrollId, "");
        lines.add(Component.translatable("item.hexsable.pattern_scroll.unlocks",
                Component.translatable(nameKey)).withStyle(ChatFormatting.GRAY));
        if (!bookKey.isEmpty()) {
            lines.add(Component.translatable("item.hexsable.pattern_scroll.book",
                    Component.translatable(bookKey)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * Превью руны при наведении — как у ванильных свитков с штрихами, только
     * штрихи берём из статических сигнатур ({@link SableScrollDefs#patternForScroll}),
     * а не из NBT стака.
     */
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var compound = NBTHelper.getCompound(stack, ItemScroll.TAG_PATTERN);
        HexPattern pattern = compound != null ? HexPattern.fromNBT(compound)
                : SableScrollDefs.patternForScroll(scrollId);
        if (pattern != null) {
            return Optional.of(new PatternTooltip(pattern, PatternTooltipComponent.PRISTINE_BG));
        }
        return Optional.empty();
    }
}
