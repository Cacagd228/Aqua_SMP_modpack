package com.colonizer.colonycard.client;

import com.colonizer.colonycard.network.stage.SyncStagePacket;
import com.colonizer.colonycard.stage.StageTask;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Последний известный клиенту список задач этапа. Обновляется сервером. */
public final class ClientStageCache {
    private ClientStageCache() {
    }

    private static List<StageTask> tasks = List.of();

    public static void apply(SyncStagePacket msg) {
        tasks = List.copyOf(msg.tasks);
        Minecraft mc = Minecraft.getInstance();
        if (msg.openScreen && mc.player != null && mc.screen == null) {
            mc.setScreen(new StageRequestsScreen());
        } else if (mc.screen instanceof StageRequestsScreen screen) {
            screen.onDataUpdated();
        }
    }

    public static List<StageTask> tasks() {
        return tasks;
    }

    public static boolean isComplete() {
        return !tasks.isEmpty() && tasks.stream().allMatch(StageTask::isComplete);
    }

    public static int doneCount() {
        int n = 0;
        for (StageTask t : tasks) {
            if (t.isComplete()) {
                n++;
            }
        }
        return n;
    }
}
