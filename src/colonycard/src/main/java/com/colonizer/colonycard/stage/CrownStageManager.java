package com.colonizer.colonycard.stage;

import com.colonizer.colonycard.network.stage.SyncStagePacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Серверная логика этапа: пожертвования, задачи админов, рассылка, уведомление о закрытии. */
public final class CrownStageManager {
    private CrownStageManager() {
    }

    public static StageSavedData data(MinecraftServer server) {
        return StageSavedData.get(server.overworld());
    }

    /** Отправить состояние этапа одному игроку; openScreen=true — сразу открыть меню. */
    public static void syncTo(ServerPlayer player, boolean openScreen) {
        StageSavedData d = data(player.getServer());
        player.connection.send(new SyncStagePacket(d.tasks(), openScreen));
    }

    /** Разослать состояние этапа всем игрокам. */
    public static void broadcast(MinecraftServer server) {
        StageSavedData d = data(server);
        PacketDistributor.sendToAllPlayers(new SyncStagePacket(d.tasks(), false));
    }

    /**
     * Принять пожертвование: списать предметы из инвентаря, зачесть в задачу.
     * @return фактически зачтённое количество.
     */
    public static int donate(ServerPlayer player, int index, int amount) {
        MinecraftServer server = player.getServer();
        StageSavedData d = data(server);
        if (index < 0 || index >= d.tasks().size() || amount <= 0) {
            return 0;
        }
        StageTask task = d.tasks().get(index);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(task.itemId()));
        if (item == null || item == Items.AIR) {
            return 0;
        }
        int have = player.getInventory().countItem(item);
        int accepted = Math.min(Math.min(amount, have), task.remaining());
        if (accepted <= 0) {
            return 0;
        }
        int removed = player.getInventory().clearOrCountMatchingItems(
                stack -> stack.is(item), accepted, player.getInventory());
        if (removed <= 0) {
            return 0;
        }
        int credited = d.donate(index, removed);
        checkComplete(server);
        broadcast(server);
        return credited;
    }

    public static void addTask(MinecraftServer server, String itemId, int needed) {
        data(server).addTask(itemId, needed);
        broadcast(server);
    }

    public static boolean removeTask(MinecraftServer server, int index) {
        boolean ok = data(server).removeTask(index);
        if (ok) {
            broadcast(server);
        }
        return ok;
    }

    public static void clear(MinecraftServer server) {
        data(server).clear();
        broadcast(server);
    }

    /** Один раз на закрытие набора задач: чат + Discord webhook (заглушка). */
    private static void checkComplete(MinecraftServer server) {
        StageSavedData d = data(server);
        if (!d.isComplete() || d.notified()) {
            return;
        }
        d.setNotified(true);
        StringBuilder summary = new StringBuilder();
        for (StageTask t : d.tasks()) {
            if (summary.length() > 0) {
                summary.append('\n');
            }
            summary.append(t.itemId()).append(' ').append(t.donated()).append('/').append(t.needed());
        }
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("commands.crownrequest.stage_complete"), false);
        DiscordWebhook.sendStageComplete(summary.toString());
    }
}
