package xyz.lineage;

import xyz.lineage.client.ChronicleKeys;
import xyz.lineage.client.WhisperClientEvents;
import xyz.lineage.command.HeritageCommands;
import xyz.lineage.game.SeasonedPlayerWatcher;
import xyz.lineage.net.ChronicleNetwork;
import xyz.lineage.registry.SoulAttachments;
import xyz.lineage.registry.VirtueAttributes;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(LineageCore.MOD_ID)
public final class LineageCore {
    public static final String MOD_ID = "lineage_core";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public LineageCore(IEventBus bus) {
        LOG.info("Lineage Core booting");
        SoulAttachments.TYPES.register(bus);
        VirtueAttributes.ATTRIBUTES.register(bus);
        bus.addListener(this::attachVirtuesToPlayer);
        bus.addListener(ChronicleNetwork::register);
        NeoForge.EVENT_BUS.register(new SeasonedPlayerWatcher());
        NeoForge.EVENT_BUS.addListener(HeritageCommands::register);
        if (FMLEnvironment.dist.isClient()) {
            ChronicleKeys.bind(bus);
            NeoForge.EVENT_BUS.register(new WhisperClientEvents());
        }
    }

    private void attachVirtuesToPlayer(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, VirtueAttributes.MIGHT);
        event.add(EntityType.PLAYER, VirtueAttributes.CELERITY);
        event.add(EntityType.PLAYER, VirtueAttributes.HEART);
        event.add(EntityType.PLAYER, VirtueAttributes.MIND);
        event.add(EntityType.PLAYER, VirtueAttributes.SPIRIT);
        event.add(EntityType.PLAYER, VirtueAttributes.GRACE);
    }
}
