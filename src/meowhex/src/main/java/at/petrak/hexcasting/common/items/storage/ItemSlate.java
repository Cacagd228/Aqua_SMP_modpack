package at.petrak.hexcasting.common.items.storage;

import at.petrak.hexcasting.annotations.SoftImplement;
import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.client.gui.PatternTooltipComponent;
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import at.petrak.hexcasting.common.misc.PatternTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class ItemSlate extends BlockItem implements IotaHolderItem {
    public static final ResourceLocation WRITTEN_PRED = modLoc("written");

    public ItemSlate(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public Component getName(ItemStack pStack) {
        var key = "block." + HexAPI.MOD_ID + ".slate." + (hasPattern(pStack) ? "written" : "blank");
        return Component.translatable(key);
    }

    private static CompoundTag getBETag(ItemStack stack) {
        var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data != null) {
            var tag = data.copyTag();
            // миграция старых 1.20 слэйтов где pattern был в CUSTOM_DATA.BlockEntityTag
            if (!tag.contains(BlockEntitySlate.TAG_PATTERN) || tag.getCompound(BlockEntitySlate.TAG_PATTERN).isEmpty()) {
                var legacy = NBTHelper.getCompound(stack, "BlockEntityTag");
                if (legacy != null && legacy.contains(BlockEntitySlate.TAG_PATTERN, Tag.TAG_COMPOUND)
                    && !legacy.getCompound(BlockEntitySlate.TAG_PATTERN).isEmpty()) {
                    tag.put(BlockEntitySlate.TAG_PATTERN, legacy.getCompound(BlockEntitySlate.TAG_PATTERN).copy());
                }
            }
            return tag;
        }
        // fallback для старых миров: читаем напрямую из CUSTOM_DATA.BlockEntityTag
        var legacy = NBTHelper.getCompound(stack, "BlockEntityTag");
        if (legacy != null && !legacy.isEmpty()) {
            return legacy.copy();
        }
        return null;
    }

    private static void setBETag(ItemStack stack, CompoundTag tag) {
        // чистим легаси BlockEntityTag из CUSTOM_DATA чтобы не дублировать
        var legacy = NBTHelper.getCompound(stack, "BlockEntityTag");
        if (legacy != null && !legacy.isEmpty()) {
            NBTHelper.remove(stack, "BlockEntityTag");
        }
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
            return;
        }
        // 1.21.1: BLOCK_ENTITY_DATA компоненты требуют обязательного поля id
        if (!tag.contains("id")) {
            tag.putString("id", "hexcasting:slate");
        }
        // если после удаления pattern остался только id — считаем слэйт пустым
        if (tag.size() == 1 && tag.contains("id")) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
            return;
        }
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
    }

    public static Optional<HexPattern> getPattern(ItemStack stack) {
        var bet = getBETag(stack);
        if (bet != null && bet.contains(BlockEntitySlate.TAG_PATTERN, Tag.TAG_COMPOUND)) {
            var patTag = bet.getCompound(BlockEntitySlate.TAG_PATTERN);
            if (!patTag.isEmpty() && HexPattern.isPattern(patTag)) {
                return Optional.of(HexPattern.fromNBT(patTag));
            }
        }
        return Optional.empty();
    }

    public static boolean hasPattern(ItemStack stack) {
        return getPattern(stack).isPresent();
    }

    @SoftImplement("IForgeItem")
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        // чиним старые стака с block_entity_data без id (краш Missing id) + мигрируем BlockEntityTag из CUSTOM_DATA
        var betFix = getBETag(stack);
        if (betFix != null && !betFix.contains("id") && betFix.contains(BlockEntitySlate.TAG_PATTERN)) {
            betFix.putString("id", "hexcasting:slate");
            setBETag(stack, betFix);
        } else if (betFix != null && NBTHelper.getCompound(stack, "BlockEntityTag") != null) {
            // мигрируем легаси даже если id уже есть
            var legacy = NBTHelper.getCompound(stack, "BlockEntityTag");
            if (legacy != null && legacy.contains(BlockEntitySlate.TAG_PATTERN) && !betFix.contains(BlockEntitySlate.TAG_PATTERN)) {
                betFix.put(BlockEntitySlate.TAG_PATTERN, legacy.getCompound(BlockEntitySlate.TAG_PATTERN).copy());
                setBETag(stack, betFix);
            }
        }
        if (!hasPattern(stack)) {
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
            NBTHelper.remove(stack, "BlockEntityTag");
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
        // авто-фикс для уже существующих слэйтов без id / легаси BlockEntityTag
        var betFix = getBETag(pStack);
        if (betFix != null && !betFix.contains("id") && betFix.contains(BlockEntitySlate.TAG_PATTERN)) {
            betFix.putString("id", "hexcasting:slate");
            setBETag(pStack, betFix);
        } else if (betFix != null) {
            var legacy = NBTHelper.getCompound(pStack, "BlockEntityTag");
            if (legacy != null && legacy.contains(BlockEntitySlate.TAG_PATTERN, Tag.TAG_COMPOUND)
                && !betFix.contains(BlockEntitySlate.TAG_PATTERN)) {
                betFix.put(BlockEntitySlate.TAG_PATTERN, legacy.getCompound(BlockEntitySlate.TAG_PATTERN).copy());
                setBETag(pStack, betFix);
            }
        }
        if (!hasPattern(pStack)) {
            pStack.remove(DataComponents.BLOCK_ENTITY_DATA);
            NBTHelper.remove(pStack, "BlockEntityTag");
        }
    }

    @Override
    public @Nullable
    CompoundTag readIotaTag(ItemStack stack) {
        var bet = getBETag(stack);

        if (bet == null || !bet.contains(BlockEntitySlate.TAG_PATTERN, Tag.TAG_COMPOUND)) {
            return null;
        }

        var patTag = bet.getCompound(BlockEntitySlate.TAG_PATTERN);
        if (patTag.isEmpty()) {
            return null;
        }
        var out = new CompoundTag();
        out.putString(HexIotaTypes.KEY_TYPE, "hexcasting:pattern");
        out.put(HexIotaTypes.KEY_DATA, patTag);
        return out;
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canWrite(ItemStack stack, Iota datum) {
        return datum instanceof PatternIota || datum == null;
    }

    @Override
    public void writeDatum(ItemStack stack, Iota datum) {
        if (this.canWrite(stack, datum)) {
            if (datum == null) {
                var beTag = getBETag(stack);
                if (beTag != null) {
                    beTag.remove(BlockEntitySlate.TAG_PATTERN);
                }
                setBETag(stack, beTag);
            } else if (datum instanceof PatternIota pat) {
                var beTag = getBETag(stack);
                if (beTag == null) {
                    beTag = new CompoundTag();
                }
                beTag.put(BlockEntitySlate.TAG_PATTERN, pat.getPattern().serializeToNBT());
                setBETag(stack, beTag);
            }
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return getPattern(stack).map(pat -> new PatternTooltip(pat, PatternTooltipComponent.SLATE_BG));
    }
}
