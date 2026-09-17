package com.meowaddons.transmitter;

import com.meowaddons.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public class TransmitterMenu extends AbstractContainerMenu {
    public final int transmitterSlots;

    @Nullable
    private final TransmitterBlockEntity transmitter;

    /** Серверная сторона: создаётся из createMenu с доступом к BlockEntity. */
    public TransmitterMenu(int windowId, Inventory playerInventory, @Nullable TransmitterBlockEntity transmitter) {
        super(ModMenus.TRANSMITTER.get(), windowId);
        this.transmitter = transmitter;
        int slots = 18;
        if (transmitter != null) {
            var handler = transmitter.getExposedInventory();
            slots = handler.getSlots();
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new SlotItemHandler(handler, row * 9 + col, 27 + col * 18, 9 + row * 18));
                }
            }
        } else {
            var empty = new net.neoforged.neoforge.items.ItemStackHandler(18);
            for (int i = 0; i < 18; i++) {
                addSlot(new SlotItemHandler(empty, i, 0, 0));
            }
        }
        this.transmitterSlots = slots;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 38 + col * 18, 108 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 38 + col * 18, 166));
        }
    }

    /** Серверное открытие: фиксируем, что квака «открыта» (рот разинут). */
    public static TransmitterMenu serverMenu(int windowId, Inventory playerInventory,
                                             TransmitterBlockEntity transmitter) {
        TransmitterMenu menu = new TransmitterMenu(windowId, playerInventory, transmitter);
        transmitter.onMenuOpened();
        return menu;
    }

    public BlockPos getBlockPos() {
        return transmitter != null ? transmitter.getBlockPos() : BlockPos.ZERO;
    }

    @Nullable
    public TransmitterBlockEntity getTransmitter() {
        return transmitter;
    }

    /** Клиентская сторона: читаем BlockEntity из клиентского мира, как у Create. */
    public static TransmitterMenu newClientMenu(int windowId, Inventory playerInventory,
                                                RegistryFriendlyByteBuf buf) {
        BlockPos readBlockPos = buf.readBlockPos();
        ClientLevel world = Minecraft.getInstance().level;
        if (world != null && world.getBlockEntity(readBlockPos) instanceof TransmitterBlockEntity be) {
            return new TransmitterMenu(windowId, playerInventory, be);
        }
        return new TransmitterMenu(windowId, playerInventory, (TransmitterBlockEntity) null);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Реализация как у сундуков, с копированием стека (слоты на IItemHandler)
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem().copy();
        ItemStack moved = stack.copy();

        int size = transmitterSlots;
        if (index < size) {
            if (!this.moveItemStackTo(stack, size, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, size, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setByPlayer(stack.copy());
        }
        return moved;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && transmitter != null) {
            transmitter.onMenuClosed();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (transmitter == null || transmitter.isRemoved()) {
            return false;
        }
        BlockPos pos = transmitter.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 8 * 8;
    }
}
