package me.nanorasmus.nanodev.hex_js.effect;

import me.nanorasmus.nanodev.hex_js.sound.HexSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class SilenceMagicBlockHandler {

    private static boolean isMagicItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // API interfaces are most reliable — check first
        try {
            if (stack.getItem() instanceof at.petrak.hexcasting.api.item.IotaHolderItem) return true;
            if (stack.getItem() instanceof at.petrak.hexcasting.api.item.MediaHolderItem) return true;
            if (stack.getItem() instanceof at.petrak.hexcasting.common.items.ItemStaff) return true;
        } catch (Throwable ignored) {}
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) return false;
        String path = key.getPath();
        String ns = key.getNamespace();
        if (ns.equals("hexcasting")) return true;
        if (ns.equals("meowhex")) return true;
        if (ns.equals("hex_js")) return true; // legacy, до breaking-ренейма
        if (path.contains("staff") || path.contains("wand") || path.contains("scroll") || path.contains("focus")
                || path.contains("slate") || path.contains("spellbook") || path.contains("cypher")
                || path.contains("trinket") || path.contains("artifact") || path.contains("spell")) {
            return true;
        }
        return false;
    }

    private static void deny(ServerPlayer sp) {
        try {
            sp.level().playSound(null, sp.blockPosition(), HexSounds.SILENCE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        } catch (Throwable ignored) {}
        try {
            Component msg = Component.literal("Безмолвие").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
            sp.displayClientMessage(msg, true);
        } catch (Throwable ignored) {}
        try {
            at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                    sp, new me.nanorasmus.nanodev.hex_js.network.MsgSilenceDenyS2C());
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer sp)) return;
        if (!sp.hasEffect(HexEffects.SILENCE)) return;
        if (!isMagicItem(event.getItemStack())) return;
        event.setCanceled(true);
        deny(sp);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer sp)) return;
        if (!sp.hasEffect(HexEffects.SILENCE)) return;
        if (!isMagicItem(event.getItemStack())) return;
        event.setCanceled(true);
        deny(sp);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer sp)) return;
        if (!sp.hasEffect(HexEffects.SILENCE)) return;
        if (!isMagicItem(event.getItemStack())) return;
        event.setCanceled(true);
        deny(sp);
    }

    // Also block use of item (like scroll)
    @SubscribeEvent
    public static void onUseItem(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!sp.hasEffect(HexEffects.SILENCE)) return;
        if (!isMagicItem(event.getItem())) return;
        event.setCanceled(true);
        deny(sp);
    }
}
