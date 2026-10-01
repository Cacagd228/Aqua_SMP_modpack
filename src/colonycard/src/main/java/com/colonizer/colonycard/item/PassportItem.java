package com.colonizer.colonycard.item;

import com.colonizer.colonycard.client.ClientHooks;
import com.colonizer.colonycard.data.ColonistData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Паспорт колониста (он же карта колониста как предмет).
 *
 * <p>Один класс на всех людей: личность хранится в данных конкретного стака
 * ({@code CUSTOM_DATA{passport_name, pp_*}}), отдельных классов на человека нет.
 * Открытие по ПКМ и по забинженной клавише показывает содержимое <b>held</b>-стака.
 */
public class PassportItem extends Item {
    public static final String TAG_NAME = "passport_name";
    public static final String TAG_OWNER = "pp_owner";
    public static final String TAG_DATE = "pp_date";
    public static final String TAG_GOAL = "pp_goal";
    public static final String TAG_TRAITS = "pp_traits";
    public static final String TAG_LOYALTY = "pp_loyalty";
    public static final String TAG_CONTRIB = "pp_contrib";
    public static final String TAG_REWARDS = "pp_rewards";
    public static final String TAG_ARCH = "pp_arch";

    public static final int MAX_NAME = 32;
    private static final String TRAIT_SEP = "\u001F";

    public PassportItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            ClientHooks.openCardWithHeld(stack.copy());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Чистит имя: trim, без форматирования/переносов, максимум {@link #MAX_NAME}. */
    public static String sanitizeName(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().replace("\u00A7", "").replace("\n", " ").replace("\r", " ");
        if (s.length() > MAX_NAME) {
            s = s.substring(0, MAX_NAME).trim();
        }
        return s;
    }

    /** Имя из конкретного стака. Пусто = паспорта (для nameplate-логики) нет. */
    public static String readPassportName(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof PassportItem)) {
            return "";
        }
        String name = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(TAG_NAME);
        return sanitizeName(name == null ? "" : name);
    }

    /** Снапшот досье владельца из стака. Null, если стак — не именной паспорт. */
    public static ColonistData readSnapshot(ItemStack stack) {
        String name = readPassportName(stack);
        if (name.isEmpty()) {
            return null;
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String owner = tag.getString(TAG_OWNER);
        long date = tag.getLong(TAG_DATE);
        String goal = tag.getString(TAG_GOAL);
        String traitsRaw = tag.getString(TAG_TRAITS);
        List<String> traits = new ArrayList<>();
        if (traitsRaw != null && !traitsRaw.isEmpty()) {
            for (String t : traitsRaw.split(TRAIT_SEP, -1)) {
                if (!t.isEmpty()) {
                    traits.add(t);
                }
            }
        }
        int loyalty = tag.getInt(TAG_LOYALTY);
        int contrib = tag.getInt(TAG_CONTRIB);
        int rewards = tag.getInt(TAG_REWARDS);
        String arch = tag.getString(TAG_ARCH);
        return new ColonistData(
                owner == null || owner.isEmpty() ? name : owner,
                date,
                goal == null ? "" : goal,
                traits,
                loyalty,
                Math.max(0, contrib),
                rewards,
                arch == null ? ColonistData.DEFAULT_ARCHIPELAGO : arch);
    }

    /** Новый паспорт: имя + снапшот досье на момент выдачи. */
    public static ItemStack createFor(ColonistData data, String displayName) {
        String name = sanitizeName(displayName != null && !displayName.isEmpty()
                ? displayName : data.colonizerName());
        ItemStack stack = new ItemStack(ModItems.PASSPORT.get());
        if (name.isEmpty()) {
            return stack;
        }
        String traitsJoined = String.join(TRAIT_SEP, data.traits());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_NAME, name);
            tag.putString(TAG_OWNER, data.colonizerName());
            tag.putLong(TAG_DATE, data.arrivalDate());
            tag.putString(TAG_GOAL, data.arrivalGoal());
            tag.putString(TAG_TRAITS, traitsJoined);
            tag.putInt(TAG_LOYALTY, data.loyalty());
            tag.putInt(TAG_CONTRIB, data.contribution());
            tag.putInt(TAG_REWARDS, data.rewardsMask());
            tag.putString(TAG_ARCH, data.archipelago());
        });
        return stack;
    }
}
