package com.colonizer.colonycard.identity;

import com.colonizer.colonycard.data.ModEnchantments;
import com.colonizer.colonycard.item.PassportItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Вся v1-логика личности в одном месте. Отдельного хранилища личности нет:
 * сервер вычисляет результат напрямую из рук и лица каждый раз заново.
 *
 * <p>Режимы (пакет несет только их + паспортное имя; истинный ник клиент
 * берет из стандартного nameplate сам):
 * <ul>
 *   <li>{@link #MODE_DEFAULT} — лицо открыто, паспорта нет: ничего не менять;</li>
 *   <li>{@link #MODE_HIDDEN} — лицо закрыто, паспорта нет: ник скрыть;</li>
 *   <li>{@link #MODE_PASSPORT_ONLY} — лицо закрыто, паспорт есть: имя из паспорта;</li>
 *   <li>{@link #MODE_TRUE_PLUS} — лицо открыто, паспорт есть: ванильное имя + имя из паспорта.</li>
 * </ul>
 */
public final class FaceCover {
    private FaceCover() {
    }

    public static final String TAG_HIDES = "hides_identity";

    public static final byte MODE_DEFAULT = 0;
    public static final byte MODE_HIDDEN = 1;
    public static final byte MODE_PASSPORT_ONLY = 2;
    public static final byte MODE_TRUE_PLUS = 3;

    public record Resolved(byte mode, String passportName) {
        public String key() {
            return mode + "\u0000" + passportName;
        }
    }

    /** Универсальный признак. Никаких проверок по ID предмета. */
    public static boolean hidesIdentity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean(TAG_HIDES);
    }

    /**
     * Закрывает ли предмет лицо: флаг {@code hides_identity} ИЛИ зачарование
     * {@code colonycard:stealth} любого уровня. Обе проверки универсальные.
     */
    public static boolean coversFace(ItemStack stack) {
        if (hidesIdentity(stack)) {
            return true;
        }
        return ModEnchantments.hasStealth(stack);
    }

    /** Зачарование stealth на шлеме любого уровня. */
    public static boolean hasStealth(ItemStack stack) {
        return ModEnchantments.hasStealth(stack);
    }

    public static void setHidesIdentity(ItemStack stack, boolean hides) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (hides) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(TAG_HIDES, true));
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(TAG_HIDES));
        }
    }

    /** Паспорт в руках: сначала MainHand, затем OffHand. Пустой стак = нет паспорта. */
    public static ItemStack heldPassportStack(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!PassportItem.readPassportName(main).isEmpty()) {
            return main;
        }
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (!PassportItem.readPassportName(off).isEmpty()) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    public static String heldPassportName(Player player) {
        return PassportItem.readPassportName(heldPassportStack(player));
    }

    /** Лицо закрыто: эффект дебага, HEAD-слот или Curios-слоты face/head с флагом. */
    public static boolean isFaceCovered(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.hasEffect(com.colonizer.colonycard.data.ModEffects.COVERED_FACE)) {
            return true;
        }
        if (hidesIdentity(entity.getItemBySlot(EquipmentSlot.HEAD))
                || hasStealth(entity.getItemBySlot(EquipmentSlot.HEAD))) {
            return true;
        }
        if (!ModList.get().isLoaded("curios")) {
            return false;
        }
        var optional = CuriosApi.getCuriosInventory(entity);
        if (optional.isEmpty()) {
            return false;
        }
        var found = optional.get().findCurios(FaceCover::coversFace);
        for (var result : found) {
            String id = result.slotContext().identifier();
            if ("face".equals(id) || "head".equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static Resolved resolve(Player player) {
        String passport = heldPassportName(player);
        boolean covered = isFaceCovered(player);
        if (!covered) {
            if (passport.isEmpty()) {
                return new Resolved(MODE_DEFAULT, "");
            }
            return new Resolved(MODE_TRUE_PLUS, passport);
        }
        if (passport.isEmpty()) {
            return new Resolved(MODE_HIDDEN, "");
        }
        return new Resolved(MODE_PASSPORT_ONLY, passport);
    }

}
