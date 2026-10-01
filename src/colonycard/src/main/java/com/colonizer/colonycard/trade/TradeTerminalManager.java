package com.colonizer.colonycard.trade;

import com.colonizer.colonycard.network.trade.CreateLotPacket;
import com.colonizer.colonycard.network.trade.SyncTradePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Серверная логика торгового терминала: создание и снятие лотов, сбор
 * комиссии монетами Numismatics, рассылка списка всем клиентам.
 *
 * <p>Товар у продавца не забирается: лот — это объявление, сделка
 * совершается вживую по координатам терминала.
 */
public final class TradeTerminalManager {

    private TradeTerminalManager() {
    }

    private static long lastStamp;

    /** Монотонный таймстемп, чтобы новые лоты всегда сортировались первыми. */
    private static synchronized long nextStamp() {
        long now = System.currentTimeMillis();
        if (now <= lastStamp) {
            now = lastStamp + 1L;
        }
        lastStamp = now;
        return now;
    }

    public static TradeSavedData data(MinecraftServer server) {
        return TradeSavedData.get(server.overworld());
    }

    public static void syncTo(ServerPlayer player) {
        TradeSavedData d = data(player.getServer());
        player.connection.send(SyncTradePacket.update(d.lots(), d.feePercent()));
    }

    /** Открыть меню терминала на вкладке товаров. */
    public static void openBrowse(ServerPlayer player) {
        TradeSavedData d = data(player.getServer());
        player.connection.send(SyncTradePacket.browse(d.lots(), d.feePercent()));
    }

    /** Открыть меню терминала с формой выставления лота по предмету из руки. */
    public static void openCreate(ServerPlayer player, ItemStack stack, Level level, BlockPos pos) {
        TradeSavedData d = data(player.getServer());
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int have = player.getInventory().countItem(stack.getItem());
        if (itemId == null || have <= 0) {
            player.displayClientMessage(Component.translatable("colonycard.trade.msg.nothing_to_sell"), true);
            openBrowse(player);
            return;
        }
        String dim = level.dimension().location().toString();
        player.connection.send(SyncTradePacket.create(d.lots(), itemId.toString(), have, dim, pos, d.feePercent()));
    }

    /** Рассылка всем игрокам (без открытия меню). */
    public static void broadcast(MinecraftServer server) {
        TradeSavedData d = data(server);
        PacketDistributor.sendToAllPlayers(SyncTradePacket.update(d.lots(), d.feePercent()));
    }

    /** Создать лот по подтверждению из формы. */
    public static void createLot(Player player, CreateLotPacket packet) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.getServer() == null) {
            return;
        }
        MinecraftServer server = serverPlayer.getServer();
        TradeSavedData d = data(server);

        Item item = resolveItem(packet.itemId);
        if (item == null) {
            fail(serverPlayer, "colonycard.trade.msg.bad_item");
            return;
        }
        if (packet.count <= 0 || packet.price <= 0) {
            fail(serverPlayer, "colonycard.trade.msg.bad_number");
            return;
        }
        if (packet.dim == null || packet.pos == null || !isValidDimension(packet.dim)) {
            fail(serverPlayer, "colonycard.trade.msg.bad_place");
            return;
        }
        if (d.lotCount() >= SyncTradePacket.MAX_LOTS) {
            fail(serverPlayer, "colonycard.trade.msg.board_full");
            return;
        }
        if (d.countBy(serverPlayer.getUUID()) >= SyncTradePacket.MAX_LOTS_PER_PLAYER) {
            fail(serverPlayer, "colonycard.trade.msg.too_many_lots", SyncTradePacket.MAX_LOTS_PER_PLAYER);
            return;
        }
        // Лот — это объявление, но заведомо несуществующий товар вешать не даём:
        // количество сверяем с тем, что реально лежит в инвентаре продавца.
        if (packet.count > serverPlayer.getInventory().countItem(item)) {
            fail(serverPlayer, "colonycard.trade.msg.not_have", packet.count);
            return;
        }

        int fee = NumismaticsMoney.isAvailable() ? d.feeFor(packet.price) : 0;
        if (fee > 0 && !NumismaticsMoney.withdraw(serverPlayer, fee)) {
            fail(serverPlayer, "colonycard.trade.msg.no_money", fee, NumismaticsMoney.format(fee));
            return;
        }

        UUID id = UUID.randomUUID();
        TradeLot lot = new TradeLot(
                id,
                packet.itemId,
                packet.count,
                packet.price,
                serverPlayer.getUUID(),
                serverPlayer.getGameProfile().getName(),
                packet.dim,
                packet.pos.getX(), packet.pos.getY(), packet.pos.getZ(),
                nextStamp()
        );
        d.add(lot);
        broadcast(server);
        serverPlayer.displayClientMessage(
                Component.translatable("colonycard.trade.msg.lot_created", packet.count,
                        new ItemStack(item).getHoverName()), false);
    }

    /** Снять свой лот. */
    public static void removeLot(Player player, UUID lotId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null || lotId == null) {
            return;
        }
        TradeSavedData d = data(server);
        UUID owner = null;
        for (TradeLot lot : d.lots()) {
            if (lot.id().equals(lotId)) {
                owner = lot.sellerId();
                break;
            }
        }
        if (owner == null) {
            return;
        }
        if (!owner.equals(player.getUUID())) {
            fail(serverPlayer, "colonycard.trade.msg.not_yours");
            return;
        }
        d.remove(lotId);
        broadcast(server);
    }

    public static void clear(MinecraftServer server) {
        data(server).clear();
        broadcast(server);
    }

    public static int removeBySeller(MinecraftServer server, UUID seller) {
        int removed = data(server).removeBySeller(seller);
        if (removed > 0) {
            broadcast(server);
        }
        return removed;
    }

    public static void setFeePercent(MinecraftServer server, int percent) {
        data(server).setFeePercent(percent);
        broadcast(server);
    }

    private static Item resolveItem(String itemId) {
        if (itemId == null) {
            return null;
        }
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            return item == null || item == Items.AIR ? null : item;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isValidDimension(String dim) {
        try {
            ResourceLocation.parse(dim);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void fail(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), false);
    }
}
