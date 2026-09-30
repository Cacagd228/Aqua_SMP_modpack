package me.nanorasmus.nanodev.hex_js.assembly;

import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.api.utils.NBTHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The step list and aether accumulated on a workpiece, stored in the stack's
 * {@code DataComponents.CUSTOM_DATA} through the fork's {@link NBTHelper}.
 *
 * <p>Living in the item rather than in the depot block entity is what lets a
 * half-finished workpiece be moved between depots — or picked up and put back —
 * without losing its place in the sequence.
 *
 * <p>Two API traps this class exists to route around:
 * <ul>
 *   <li>{@code NBTHelper.getOrCreateList} / {@code getOrCreateCompound} return
 *       <em>detached copies</em> on an {@link ItemStack} — {@code customDataTag()}
 *       goes through {@code CustomData.copyTag()} — so mutating what they hand
 *       back is silently discarded. The list is therefore built explicitly and
 *       written back with {@link NBTHelper#putList}.</li>
 *   <li>The getters read through a copied tag too, so they are cheap and safe but
 *       never mutate; all writes go through the put/update helpers.</li>
 * </ul>
 */
public final class AssemblyNbt {
    /** Ordered list of applied step ids, a ListTag of StringTag. */
    public static final String KEY_STEPS = "meowhex:steps";
    /** Accumulated aether, in media (NOT mana). */
    public static final String KEY_MANA = "meowhex:mana";

    /** 1 mana = 1000 media, matching {@code OpChargeVessel}. */
    public static final long MANA_TO_MEDIA = MediaConstants.DUST_UNIT / 10L;

    private AssemblyNbt() {
    }

    /** The steps already applied to this workpiece, in order. Empty if none. */
    public static List<String> steps(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        ListTag list = NBTHelper.getList(stack, KEY_STEPS, Tag.TAG_STRING);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            // A malformed entry of another type is skipped rather than fatal: the
            // tag is player-visible data and should not be able to hard-crash a cast.
            Tag t = list.get(i);
            if (t instanceof StringTag s) {
                out.add(s.getAsString());
            }
        }
        return out;
    }

    /**
     * Write a fresh, explicitly built step list.
     *
     * <p>Must be handed a list the caller already owns — this copies it into a
     * fresh ListTag rather than retaining the caller's.
     */
    public static void setSteps(ItemStack stack, List<String> steps) {
        ListTag list = new ListTag();
        for (String step : steps) {
            list.add(StringTag.valueOf(step));
        }
        NBTHelper.putList(stack, KEY_STEPS, list);
    }

    /** Aether on this workpiece, in media. */
    public static long mana(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0L;
        }
        return NBTHelper.getLong(stack, KEY_MANA);
    }

    public static void setMana(ItemStack stack, long media) {
        if (media <= 0L) {
            NBTHelper.remove(stack, KEY_MANA);
        } else {
            NBTHelper.putLong(stack, KEY_MANA, media);
        }
    }

    /** Aether in mana, the unit recipes and the mana rune both speak. */
    public static long manaToManaUnits(long media) {
        return media / MANA_TO_MEDIA;
    }

    public static long manaUnitsToMedia(long mana) {
        return mana * MANA_TO_MEDIA;
    }

    /**
     * Strip the assembly state from a finished product, so a result item that
     * happens to be the same item as some other recipe's input does not arrive
     * pre-loaded with someone else's steps.
     *
     * <p>Both keys are removed rather than zeroed: leaving an empty
     * {@code meowhex:steps} behind would still make the stack fail to stack with
     * a clean one of the same item.
     */
    public static void clear(ItemStack stack) {
        NBTHelper.remove(stack, KEY_STEPS);
        NBTHelper.remove(stack, KEY_MANA);
    }

    /** True if this stack carries any assembly state at all. */
    public static boolean hasState(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (NBTHelper.hasList(stack, KEY_STEPS, Tag.TAG_STRING) || NBTHelper.hasLong(stack, KEY_MANA));
    }

    /** Human-readable step list for a mishap message. */
    public static String describe(List<String> steps) {
        return steps.isEmpty() ? "[]" : String.join(" -> ", steps);
    }

    /** Copy of a step list with one more step appended, leaving the input alone. */
    public static List<String> append(List<String> existing, String step) {
        List<String> out = new ArrayList<>(existing.size() + 1);
        out.addAll(existing);
        out.add(step);
        return out;
    }
}
