package xyz.lineage.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent.Init.Post;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import xyz.lineage.stats.HeroStat;

/** Hushed client omens: the unwise cannot read runes aright. */
@OnlyIn(Dist.CLIENT)
public class WhisperClientEvents {
    private static final ResourceLocation RUNE_FONT = ResourceLocation.withDefaultNamespace("alt");

    @SubscribeEvent
    public void tidySatchel(Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        for (GuiEventListener guest : new ArrayList<>(event.getListenersList())) {
            String name = guest.getClass().getName();
            if (name.contains("apothic_attributes") || name.contains("AttributesGui") || name.contains("AttributesLibClient")) {
                event.removeListener(guest);
            }
        }
    }

    @SubscribeEvent
    public void cloudRunes(ItemTooltipEvent event) {
        LocalPlayer dreamer = Minecraft.getInstance().player;
        if (dreamer == null) {
            return;
        }
        if ((int) Math.round(HeroStat.WISDOM.read(dreamer)) > 7) {
            return;
        }
        ItemEnchantments worn = event.getItemStack().getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments kept = event.getItemStack().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (worn.isEmpty() && kept.isEmpty()) {
            return;
        }
        List<Component> lines = event.getToolTip();
        for (int i = 1; i < lines.size(); i++) {
            String plain = lines.get(i).getString();
            if (!runeLine(plain, worn, kept)) {
                continue;
            }
            char[] veiled = plain.toCharArray();
            for (int j = 0; j < veiled.length; j++) {
                if (Character.isLetterOrDigit(veiled[j])) {
                    veiled[j] = (char) ('a' + Math.abs(veiled[j] * 7 + 13) % 26);
                }
            }
            lines.set(i, Component.literal(new String(veiled)).withStyle(Style.EMPTY.withFont(RUNE_FONT).withColor(ChatFormatting.GRAY)));
        }
    }

    private static boolean runeLine(String plain, ItemEnchantments worn, ItemEnchantments kept) {
        for (Holder<Enchantment> holder : worn.keySet()) {
            if (plain.contains(holder.value().description().getString())) {
                return true;
            }
        }
        for (Holder<Enchantment> holder : kept.keySet()) {
            if (plain.contains(holder.value().description().getString())) {
                return true;
            }
        }
        return false;
    }
}
