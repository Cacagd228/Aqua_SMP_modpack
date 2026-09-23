package com.colonizer.colonycard.event;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.data.ColonistData;
import com.colonizer.colonycard.data.ColonistPools;
import com.colonizer.colonycard.data.ModAttachments;
import com.colonizer.colonycard.network.SyncColonistDataPacket;
import com.colonizer.colonycard.stage.CrownStageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Server-side lifecycle: creates a colonist profile on first join and keeps the owning client synced. */
@EventBusSubscriber(modid = ColonyCardMod.MODID)
public final class ServerEvents {
    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ColonistData data = player.getData(ModAttachments.COLONIST_DATA);
        if (!data.initialized()) {
            RandomSource random = RandomSource.create();
            data = new ColonistData(
                    player.getGameProfile().getName(),
                    System.currentTimeMillis(),
                    ColonistPools.randomGoal(random),
                    ColonistPools.randomTraits(random),
                    0,
                    0,
                    0,
                    ColonistPools.randomArchipelago(random)
            );
            player.setData(ModAttachments.COLONIST_DATA, data);
        } else if (data.archipelago().isEmpty()) {
            // Миграция старых досье (до паспортного разворота): добрасываем архипелаг.
            data = data.withArchipelago(ColonistPools.randomArchipelago(RandomSource.create()));
            player.setData(ModAttachments.COLONIST_DATA, data);
        }
        if (data.initialized() && data.traits().size() > ColonistPools.TRAITS_PER_COLONIST) {
            // Примет теперь две: подрезаем старые тройки до первых двух.
            data = data.withTraits(List.copyOf(data.traits().subList(0, ColonistPools.TRAITS_PER_COLONIST)));
            player.setData(ModAttachments.COLONIST_DATA, data);
        }

        sync(player);
        CrownStageManager.syncTo(player, false);
    }

    public static void sync(ServerPlayer player) {
        ColonistData data = player.getData(ModAttachments.COLONIST_DATA);
        PacketDistributor.sendToPlayer(player, new SyncColonistDataPacket(player.getGameProfile().getName(), data));
    }
}
