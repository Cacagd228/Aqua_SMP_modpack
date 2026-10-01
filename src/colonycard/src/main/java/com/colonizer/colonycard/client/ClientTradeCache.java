package com.colonizer.colonycard.client;

import com.colonizer.colonycard.network.trade.SyncTradePacket;
import com.colonizer.colonycard.trade.TradeLot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Последний известный клиенту список лотов + контекст открытия меню.
 * Обновляется сервером (см. {@link SyncTradePacket}).
 */
public final class ClientTradeCache {
    private ClientTradeCache() {
    }

    private static List<TradeLot> lots = List.of();
    private static int feePercent = 10;

    // Контекст формы выставления лота.
    private static String formItemId = "";
    private static int formMaxCount;
    private static String formDim = "";
    private static BlockPos formPos = BlockPos.ZERO;

    public static void apply(SyncTradePacket msg) {
        lots = List.copyOf(msg.lots);
        feePercent = msg.feePercent;
        formItemId = msg.formItemId;
        formMaxCount = msg.formMaxCount;
        formDim = msg.formDim;
        formPos = msg.formPos();

        Minecraft mc = Minecraft.getInstance();
        if (msg.mode != SyncTradePacket.Mode.NONE && mc.player != null && mc.screen == null) {
            mc.setScreen(new TradeTerminalScreen(msg.mode == SyncTradePacket.Mode.CREATE));
        } else if (mc.screen instanceof TradeTerminalScreen screen) {
            screen.onDataUpdated();
        }
    }

    public static List<TradeLot> lots() {
        return lots;
    }

    public static int feePercent() {
        return feePercent;
    }

    public static String formItemId() {
        return formItemId;
    }

    public static int formMaxCount() {
        return formMaxCount;
    }

    public static String formDim() {
        return formDim;
    }

    public static BlockPos formPos() {
        return formPos;
    }

    // ---- выборки для UI ----

    /** Уникальные itemId, отсортированные по отображаемому названию. */
    public static List<String> distinctItemIds() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TradeLot lot : lots) {
            counts.merge(lot.itemId(), 1, Integer::sum);
        }
        List<String> ids = new ArrayList<>(counts.keySet());
        ids.sort(Comparator.comparing(ClientTradeCache::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(id -> id));
        return ids;
    }

    public static int countFor(String itemId) {
        int n = 0;
        for (TradeLot lot : lots) {
            if (lot.itemId().equals(itemId)) {
                n++;
            }
        }
        return n;
    }

    /** Лоты по предмету, новые сверху (по цене — только в UI, порядок тут как от сервера). */
    public static List<TradeLot> lotsFor(String itemId) {
        List<TradeLot> out = new ArrayList<>();
        for (TradeLot lot : lots) {
            if (lot.itemId().equals(itemId)) {
                out.add(lot);
            }
        }
        return out;
    }

    /** Отображаемое название предмета (или его id, если предмета нет в реестре). */
    public static String displayName(String itemId) {
        Item item = resolveItem(itemId);
        if (item == null) {
            return itemId;
        }
        try {
            return new ItemStack(item).getHoverName().getString();
        } catch (Exception e) {
            return itemId;
        }
    }

    public static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return item == null || item == Items.AIR ? null : item;
        } catch (Exception e) {
            return null;
        }
    }

    /** Ключ для поиска: название + id, в нижнем регистре. */
    public static String searchKey(String itemId) {
        return (displayName(itemId) + " " + itemId).toLowerCase(Locale.ROOT);
    }
}
