package me.nanorasmus.nanodev.hex_js.storage;

import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.misc.ManaHelper;
import at.petrak.hexcasting.common.items.ItemStaff;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import me.nanorasmus.nanodev.hex_js.addon.HexArtifactsItems;
import me.nanorasmus.nanodev.hex_js.helpers.CurioHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server logic for the Lens of Comprehension: copying a pattern set clicked in chat
 * onto the player's staff cast stack. The patterns are pushed as a VALUE (never
 * executed/cast), so they sit on the stack for later actions to use. Requires the
 * lens in a Curios slot and a staff in hand.
 */
public final class ArtifactHandler {
    /** 1 мана = 1000 media. */
    private static final long MEDIA_PER_MANA = 1000L;
    private static final long BASE_MANA = 100L;
    private static final long MANA_PER_IOTA = 200L;

    private ArtifactHandler() {
    }

    public static void copyPatternToStack(ServerPlayer player, List<HexPattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return;
        }
        if (!hasArtifactEquipped(player)) {
            player.sendSystemMessage(Component.translatable("meowhex.message.pattern_reader_missing"));
            return;
        }

        InteractionHand hand = staffHand(player);
        if (hand == null) {
            player.sendSystemMessage(Component.translatable("meowhex.message.need_staff"));
            return;
        }

        // 100 маны + 200 за каждую иоту в наборе.
        long mediaCost = (BASE_MANA + MANA_PER_IOTA * patterns.size()) * MEDIA_PER_MANA;
        if (!(player.isCreative() || ManaHelper.hasInfiniteMana(player))) {
            double manaCost = ManaHelper.manaCostOfMedia(player, mediaCost);
            double mana = ManaHelper.getMana(player);
            if (mana < manaCost) {
                player.sendSystemMessage(Component.translatable("hexcasting.message.cant_overcast"));
                return;
            }
            ManaHelper.setMana(player, mana - manaCost);
        }

        // Кладём ВЕСЬ набор на стек как значение (не исполняем).
        Iota toPush;
        if (patterns.size() == 1) {
            toPush = new PatternIota(patterns.get(0));
        } else {
            List<Iota> items = new ArrayList<>(patterns.size());
            for (HexPattern pattern : patterns) {
                items.add(new PatternIota(pattern));
            }
            toPush = new ListIota(items);
        }

        var vm = IXplatAbstractions.INSTANCE.getStaffcastVM(player, hand);
        CastingImage img = vm.getImage();
        List<Iota> newStack = new ArrayList<>(img.getStack());
        newStack.add(toPush);
        CastingImage newImg = img.copy(newStack, img.getParenCount(), img.getParenthesized(),
            img.getEscapeNext(), img.getOpsConsumed(), img.getUserData());
        vm.setImage(newImg);
        IXplatAbstractions.INSTANCE.setStaffcastImage(player, newImg);
    }

    private static boolean hasArtifactEquipped(ServerPlayer player) {
        return CurioHelper.hasCurio(player, HexArtifactsItems.PATTERN_READER.get());
    }

    private static InteractionHand staffHand(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof ItemStaff) {
                return hand;
            }
        }
        return null;
    }
}