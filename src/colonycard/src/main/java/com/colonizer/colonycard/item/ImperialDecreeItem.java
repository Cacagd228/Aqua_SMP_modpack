package com.colonizer.colonycard.item;

import com.colonizer.colonycard.client.ClientHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

/**
 * Бумажная грамота. ПКМ — открыть экран декрета (только на клиенте).
 *
 * <p>Имя награждённого хранится в {@code CustomData{recipient:"..."}}.
 * Проставляет его команда {@code /colonycard gramota}. Если поле пустое —
 * экран показывает имя держателя.
 */
public class ImperialDecreeItem extends Item {
    public static final String TAG_RECIPIENT = "recipient";

    public ImperialDecreeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            ClientHooks.openDecreeScreen(readRecipient(stack, player));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static String readRecipient(ItemStack stack, Player fallbackHolder) {
        String fromTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(TAG_RECIPIENT);
        if (fromTag != null && !fromTag.isEmpty()) {
            return fromTag;
        }
        if (fallbackHolder != null) {
            return fallbackHolder.getGameProfile().getName();
        }
        return "";
    }

    /** Новый стак грамоты, выписанной на конкретное имя. */
    public static ItemStack createFor(String recipientName) {
        ItemStack stack = new ItemStack(ModItems.IMPERIAL_DECREE.get());
        if (recipientName != null && !recipientName.isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putString(TAG_RECIPIENT, recipientName));
        }
        return stack;
    }
}
