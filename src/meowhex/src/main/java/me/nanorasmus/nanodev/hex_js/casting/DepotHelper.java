package me.nanorasmus.nanodev.hex_js.casting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

/**
 * Helper for Create depot interaction. Optional dependency on Create.
 * Works via reflection for DepotBlockEntity and via ItemHandler capability as fallback.
 * Block is expected to be "create:depot".
 */
public final class DepotHelper {
    private DepotHelper() {}

    public static boolean isDepot(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key != null && "create".equals(key.getNamespace()) && "depot".equals(key.getPath())) {
            return true;
        }
        // Fallback: check block entity class name
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            String cn = be.getClass().getName();
            if (cn.equals("com.simibubi.create.content.logistics.depot.DepotBlockEntity")) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static ItemStack getStack(ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;

        // Try reflection: DepotBlockEntity.getHeldItem()
        try {
            var m = be.getClass().getMethod("getHeldItem");
            Object res = m.invoke(be);
            if (res instanceof ItemStack s) {
                return s.copy();
            }
        } catch (Throwable ignored) {}

        // Fallback via capability
        try {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, level.getBlockState(pos), be, (Direction) null);
            if (handler == null) {
                // try with direction
                handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
            }
            if (handler != null && handler.getSlots() > 0) {
                return handler.getStackInSlot(0).copy();
            }
        } catch (Throwable ignored) {}

        return null;
    }

    public static boolean setStack(ServerLevel level, BlockPos pos, ItemStack stack) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return false;

        // Try DepotBlockEntity.setHeldItem(ItemStack)
        try {
            var m = be.getClass().getMethod("setHeldItem", ItemStack.class);
            m.invoke(be, stack.copy());
            be.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            level.invalidateCapabilities(pos);
            return true;
        } catch (Throwable ignored) {}

        // Try Clearable + capability approach
        try {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, level.getBlockState(pos), be, (Direction) null);
            if (handler != null) {
                // Extract existing
                if (handler.getSlots() > 0) {
                    // Use extract/insert if mutable
                    // Try to clear slot 0
                    // Some handlers are not directly mutable, but we can attempt extract then insert
                    // Fallback: use IItemHandlerModifiable if available
                    try {
                        var modifiableClass = Class.forName("net.neoforged.neoforge.items.IItemHandlerModifiable");
                        if (modifiableClass.isInstance(handler)) {
                            var setMethod = modifiableClass.getMethod("setStackInSlot", int.class, ItemStack.class);
                            setMethod.invoke(handler, 0, stack.copy());
                            be.setChanged();
                            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                            level.invalidateCapabilities(pos);
                            return true;
                        }
                    } catch (Throwable ignored2) {}

                    // Generic extract+insert for non-modifiable (may not allow set empty to larger)
                    // Extract all
                    handler.extractItem(0, Integer.MAX_VALUE, false);
                    if (!stack.isEmpty()) {
                        ItemStack remaining = stack.copy();
                        // handler.insertItem may split
                        ItemStack notInserted = handler.insertItem(0, remaining, false);
                        if (!notInserted.isEmpty()) {
                            // Could not insert fully -> fail
                            return false;
                        }
                    }
                    be.setChanged();
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    level.invalidateCapabilities(pos);
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        // Last resort: try Clearable
        if (be instanceof Clearable clearable) {
            clearable.clearContent();
            // No direct way to set stack without capability -> fail if stack not empty
            if (stack.isEmpty()) {
                be.setChanged();
                return true;
            }
        }
        return false;
    }

    public static boolean clearStack(ServerLevel level, BlockPos pos) {
        return setStack(level, pos, ItemStack.EMPTY);
    }
}
