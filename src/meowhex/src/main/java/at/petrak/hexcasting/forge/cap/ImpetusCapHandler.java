package at.petrak.hexcasting.forge.cap;

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import at.petrak.hexcasting.common.lib.HexBlockEntities;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;

public class ImpetusCapHandler {
    public static void register(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent evt) {
        evt.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            HexBlockEntities.IMPETUS_LOOK_TILE,
            new ICapabilityProvider<BlockEntityAbstractImpetus, Direction, net.neoforged.neoforge.items.IItemHandler>() {
                @Override
                public net.neoforged.neoforge.items.IItemHandler getCapability(BlockEntityAbstractImpetus impetus, Direction dir) {
                    return new ForgeImpetusCapability(impetus);
                }
            }
        );
        evt.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            HexBlockEntities.IMPETUS_REDSTONE_TILE,
            new ICapabilityProvider<BlockEntityAbstractImpetus, Direction, net.neoforged.neoforge.items.IItemHandler>() {
                @Override
                public net.neoforged.neoforge.items.IItemHandler getCapability(BlockEntityAbstractImpetus impetus, Direction dir) {
                    return new ForgeImpetusCapability(impetus);
                }
            }
        );
        evt.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            HexBlockEntities.IMPETUS_RIGHTCLICK_TILE,
            new ICapabilityProvider<BlockEntityAbstractImpetus, Direction, net.neoforged.neoforge.items.IItemHandler>() {
                @Override
                public net.neoforged.neoforge.items.IItemHandler getCapability(BlockEntityAbstractImpetus impetus, Direction dir) {
                    return new ForgeImpetusCapability(impetus);
                }
            }
        );
    }
}
