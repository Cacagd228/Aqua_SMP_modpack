package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.common.items.ItemStaff;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

/**
 * Hextended Drawing Orb: a staff that can hold a single iota (like a scroll). While
 * held, {@link DrawingOrbAmbit} extends the caster's ambit to the entity stored in it.
 */
public class ItemDrawingOrb extends ItemStaff implements IotaHolderItem {
    public ItemDrawingOrb(Properties properties) {
        super(properties);
    }

    public static final ResourceLocation OVERLAY_PRED = modLoc("overlay_layer");
    public static final String TAG_DATA = "data";
    public static final String TAG_SEALED = "sealed";

    @Override
    public @Nullable CompoundTag readIotaTag(ItemStack stack) {
        return NBTHelper.getCompound(stack, TAG_DATA);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        IotaHolderItem.appendHoverText(this, stack, tooltip, flag);
    }

    public static boolean isSealed(ItemStack stack) {
        return NBTHelper.getBoolean(stack, TAG_SEALED);
    }

    public static void seal(ItemStack stack) {
        NBTHelper.putBoolean(stack, TAG_SEALED, true);
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return !isSealed(stack);
    }

    @Override
    public boolean canWrite(ItemStack stack, @Nullable Iota iota) {
        return iota == null || !isSealed(stack);
    }

    @Override
    public void writeDatum(ItemStack stack, @Nullable Iota iota) {
        if (iota == null) {
            CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            stack.set(DataComponents.CUSTOM_DATA, custom.update(tag -> {
                tag.remove(TAG_DATA);
                tag.remove(TAG_SEALED);
            }));
        } else if (!isSealed(stack)) {
            NBTHelper.put(stack, TAG_DATA, IotaType.serialize(iota));
        }
    }
}