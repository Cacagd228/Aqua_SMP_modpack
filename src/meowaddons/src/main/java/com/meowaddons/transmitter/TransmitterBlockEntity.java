package com.meowaddons.transmitter;

import com.meowaddons.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class TransmitterBlockEntity extends BlockEntity implements MenuProvider {
    private static final int ROUTE_INTERVAL_TICKS = 10;
    private static final int MIN_TRANSIT_TICKS = 20; // 1с
    private static final int DELIVERY_RETRY_TICKS = 20;
    /** Тиков на блок расстояния: 1с за каждые 100 блоков => 20/100 = 0.2 тика/блок. */
    private static final double TICKS_PER_BLOCK = 0.2;
    /** Длительность анимации языка/портала в тиках. */
    public static final int ANIM_DURATION_TICKS = 20;
    /** Пауза после приёма коробки перед маршрутизацией дальше. */
    private static final int ROUTE_REST_TICKS = 40;
    /** NBT-ключ коробки: позиция передатчика, из которого коробка была отправлена. */
    private static final String FROM_TAG = "meowaddons_transit_from";

    /** Фазы анимации: нет / отправка (язык с посылкой) / приём (язык вернётся с посылкой). */
    public static final int PHASE_NONE = 0;
    public static final int PHASE_SEND = 1;
    public static final int PHASE_RECEIVE = 2;

    private String ownAddress = "";
    private boolean inTransit = false;
    private int transitTicksLeft = 0;
    @Nullable
    private BlockPos transitTarget = null;
    private int routeCooldown = 0;
    /** Коробка, летящая к цели (изъята из инвентаря на время транзита). */
    private ItemStack transitBox = ItemStack.EMPTY;

    // Состояние анимации, синхронизируется на клиент
    private int animPhase = PHASE_NONE;
    private long animStartGameTime = 0;
    @Nullable
    private ItemStack animatedPackage = ItemStack.EMPTY;

    /** Поворот кваки в градусах (шаг 11.25°, как у Create), задаётся при установке. */
    private float passiveYaw = 0;

    /** Сколько игроков открыли интерфейс — квака открывает рот, как у Create. Не сохраняется на диск. */
    private int openCount = 0;

    public void onMenuOpened() {
        openCount++;
        if (level != null && !level.isClientSide) {
            if (openCount == 1) {
                com.simibubi.create.AllSoundEvents.FROGPORT_OPEN.playOnServer(level, worldPosition, 0.7f, 1.0f);
            }
            syncToClient();
        }
    }

    public void onMenuClosed() {
        openCount = Math.max(0, openCount - 1);
        if (level != null && !level.isClientSide) {
            if (openCount == 0 && animPhase == PHASE_NONE) {
                com.simibubi.create.AllSoundEvents.FROGPORT_CLOSE.playOnServer(level, worldPosition, 0.7f, 1.0f);
            }
            syncToClient();
        }
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getOpenCount() {
        return openCount;
    }

    private final ItemStackHandler inventory = new ItemStackHandler(18) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return PackageAccess.isPackage(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Обёртка для воронок/шлюзов Create: во время транзита коробку нельзя достать. */
    private final IItemHandlerModifiable exposedInventory = new IItemHandlerModifiable() {
        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            inventory.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (isSlotLocked()) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inventory.isItemValid(slot, stack);
        }
    };

    public TransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INTERDIMENSIONAL_TRANSMITTER.get(), pos, state);
    }

    public String getOwnAddress() {
        return ownAddress;
    }

    public void setOwnAddress(String address) {
        this.ownAddress = address == null ? "" : address.trim();
        if (level != null && !level.isClientSide) {
            TransmitterIndex.get(level).register(worldPosition, ownAddress);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isEmpty() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return !inTransit;
    }

    public IItemHandlerModifiable getExposedInventory() {
        return exposedInventory;
    }

    private boolean isSlotLocked() {
        return inTransit;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && !ownAddress.isBlank()) {
            TransmitterIndex.get(level).register(worldPosition, ownAddress);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            TransmitterIndex.get(level).unregister(worldPosition);
        }
    }

    public void onBlockRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    popResourcesFrom(serverLevel, worldPosition, stack);
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
            if (!transitBox.isEmpty()) {
                popResourcesFrom(serverLevel, worldPosition, transitBox);
                transitBox = ItemStack.EMPTY;
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, TransmitterBlockEntity be) {
        be.tickServer(level, pos);
    }

    private void tickServer(Level level, BlockPos pos) {
        if (animPhase != PHASE_NONE && level.getGameTime() - animStartGameTime >= ANIM_DURATION_TICKS) {
            animPhase = PHASE_NONE;
            animatedPackage = ItemStack.EMPTY;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (openCount == 0) {
                com.simibubi.create.AllSoundEvents.FROGPORT_CLOSE.playOnServer(level, pos, 0.7f, 1.0f);
            }
        }

        if (inTransit) {
            if (--transitTicksLeft > 0) {
                return;
            }
            tryDeliver(level);
            return;
        }

        ItemStack box = ItemStack.EMPTY;
        int boxSlot = -1;
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                box = inventory.getStackInSlot(i);
                boxSlot = i;
                break;
            }
        }
        if (box.isEmpty()) {
            routeCooldown = 0;
            return;
        }
        if (--routeCooldown > 0) {
            return;
        }
        routeCooldown = ROUTE_INTERVAL_TICKS;
        tryRoute(level, pos, box, boxSlot);
    }

    private void tryRoute(Level level, BlockPos from, ItemStack box, int boxSlot) {
        String boxAddress = PackageAccess.getAddress(box);
        if (boxAddress.isBlank()) {
            return;
        }
        // Не отправляем коробку обратно туда, откуда она прилетела (защита от пинг-понга
        // при одинаковых адресах / пересекающихся glob-паттернах)
        BlockPos lastSender = readLastSender(box);
        TransmitterIndex index = TransmitterIndex.get(level);
        var candidates = index.findMatches(filter -> PackageAccess.matchAddress(boxAddress, filter));

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos candidate : candidates) {
            if (candidate.equals(worldPosition)) continue;
            if (lastSender != null && candidate.equals(lastSender)) continue;
            if (!level.isLoaded(candidate)) continue;
            if (!(level.getBlockEntity(candidate) instanceof TransmitterBlockEntity target)) continue;
            if (!target.canAccept()) continue;
            double dist = candidate.distSqr(worldPosition);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        if (best == null) {
            return;
        }
        double blocks = Math.sqrt(bestDist);
        transitTicksLeft = Math.max(MIN_TRANSIT_TICKS, (int) Math.ceil(blocks * TICKS_PER_BLOCK));
        transitTarget = best.immutable();
        inTransit = true;
        writeLastSender(box, worldPosition);
        inventory.setStackInSlot(boxSlot, ItemStack.EMPTY);
        transitBox = box.copy();
        startAnimation(PHASE_SEND, box, level);
    }

    private void tryDeliver(Level level) {
        if (!(transitTarget instanceof BlockPos target)
                || !(level.getBlockEntity(target) instanceof TransmitterBlockEntity receiver)
                || !receiver.canAccept()) {
            // Цель пропала/занята — ждём и пробуем снова, не меняя маршрут
            transitTicksLeft = DELIVERY_RETRY_TICKS;
            return;
        }
        ItemStack box = transitBox;
        int insertSlot = -1;
        for (int i = 0; i < receiver.inventory.getSlots(); i++) {
            if (receiver.inventory.getStackInSlot(i).isEmpty()) {
                insertSlot = i;
                break;
            }
        }
        if (insertSlot >= 0) {
            receiver.inventory.setStackInSlot(insertSlot, box.copy());
        } else {
            // Все слоты заняли, пока летели — роняем коробку у приёмника
            if (level instanceof ServerLevel serverLevel) {
                popResourcesFrom(serverLevel, target, box);
            }
        }
        receiver.setChanged();
        // Даём анимации приёма доиграться до того, как приёмник попробует переслать коробку дальше
        receiver.routeCooldown = ROUTE_REST_TICKS;
        receiver.startAnimation(PHASE_RECEIVE, box.copy(), level);

        inTransit = false;
        transitTarget = null;
        transitTicksLeft = 0;
        transitBox = ItemStack.EMPTY;
        setChanged();
    }

    @Nullable
    private static BlockPos readLastSender(ItemStack box) {
        net.minecraft.world.item.component.CustomData data =
                box.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        int[] t = data.copyTag().getIntArray(FROM_TAG);
        return t.length == 3 ? new BlockPos(t[0], t[1], t[2]) : null;
    }

    private static void writeLastSender(ItemStack box, BlockPos sender) {
        net.minecraft.world.item.component.CustomData data =
                box.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.EMPTY);
        net.minecraft.nbt.CompoundTag tag = data.copyTag();
        tag.putIntArray(FROM_TAG, new int[]{sender.getX(), sender.getY(), sender.getZ()});
        box.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }

    /** Запуск анимации портала+языка с синхронизацией на клиент. */
    private void startAnimation(int phase, ItemStack packageStack, Level level) {
        animPhase = phase;
        animStartGameTime = level.getGameTime();
        animatedPackage = packageStack.copy();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        var sound = phase == PHASE_SEND
                ? com.simibubi.create.AllSoundEvents.FROGPORT_DEPOSIT
                : com.simibubi.create.AllSoundEvents.FROGPORT_CATCH;
        sound.playOnServer(level, worldPosition, 0.8f, 1.0f);
    }

    public int getAnimPhase() {
        return animPhase;
    }

    public long getAnimStartGameTime() {
        return animStartGameTime;
    }

    public ItemStack getAnimatedPackage() {
        return animatedPackage == null ? ItemStack.EMPTY : animatedPackage;
    }

    public float getPassiveYaw() {
        return passiveYaw;
    }

    public void setPassiveYaw(float yaw) {
        passiveYaw = yaw;
    }

    private boolean canAccept() {
        if (inTransit) {
            return false;
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void popResourcesFrom(ServerLevel level, BlockPos pos, ItemStack stack) {
        net.minecraft.world.entity.item.ItemEntity entity =
                new net.minecraft.world.entity.item.ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, stack.copy());
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("own_address", ownAddress);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putBoolean("in_transit", inTransit);
        tag.putInt("transit_ticks", transitTicksLeft);
        if (transitTarget != null) {
            tag.putIntArray("transit_target",
                    new int[]{transitTarget.getX(), transitTarget.getY(), transitTarget.getZ()});
        }
        tag.putInt("anim_phase", animPhase);
        tag.putLong("anim_start", animStartGameTime);
        tag.putFloat("PlacedYaw", passiveYaw);
        if (inTransit && transitBox != null && !transitBox.isEmpty()) {
            tag.put("transit_box",
                    net.minecraft.world.item.ItemStack.CODEC
                            .encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                                    transitBox)
                            .getOrThrow());
        }
        if (animatedPackage != null && !animatedPackage.isEmpty()) {
            tag.put("anim_package",
                    net.minecraft.world.item.ItemStack.CODEC
                            .encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                                    animatedPackage)
                            .getOrThrow());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownAddress = tag.getString("own_address");
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
            // Миграция со старых сейвов: инвентарь мог быть сохранён с Size=1
            if (inventory.getSlots() != 18) {
                ItemStack first = inventory.getSlots() > 0 ? inventory.getStackInSlot(0) : ItemStack.EMPTY;
                inventory.setSize(18);
                if (!first.isEmpty()) {
                    inventory.setStackInSlot(0, first);
                }
            }
        }
        inTransit = tag.getBoolean("in_transit");
        transitTicksLeft = tag.getInt("transit_ticks");
        transitTarget = null;
        int[] t = tag.getIntArray("transit_target");
        if (t.length == 3) {
            transitTarget = new BlockPos(t[0], t[1], t[2]);
        }
        animPhase = tag.contains("anim_phase") ? tag.getInt("anim_phase") : PHASE_NONE;
        animStartGameTime = tag.getLong("anim_start");
        passiveYaw = tag.getFloat("PlacedYaw");
        openCount = tag.getInt("open_count");
        transitBox = ItemStack.EMPTY;
        if (tag.contains("transit_box")) {
            transitBox = net.minecraft.world.item.ItemStack.CODEC
                    .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                            tag.get("transit_box"))
                    .result()
                    .orElse(ItemStack.EMPTY);
        }
        animatedPackage = ItemStack.EMPTY;
        if (tag.contains("anim_package")) {
            ItemStack parsed = net.minecraft.world.item.ItemStack.CODEC
                    .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                            tag.get("anim_package"))
                    .result()
                    .orElse(ItemStack.EMPTY);
            if (!parsed.isEmpty()) {
                animatedPackage = parsed;
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putInt("open_count", openCount);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.meowaddons.interdimensional_transmitter");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return TransmitterMenu.serverMenu(windowId, inventory, this);
    }

    /** Изолированный доступ к API пакетов Create, чтобы мод собирался и работал без него. */
    private static final class PackageAccess {
        static boolean isPackage(ItemStack stack) {
            return com.simibubi.create.content.logistics.box.PackageItem.isPackage(stack);
        }

        static String getAddress(ItemStack stack) {
            return com.simibubi.create.content.logistics.box.PackageItem.getAddress(stack);
        }

        /** Симметричный glob-матчинг Create: порядок аргументов не важен. */
        static boolean matchAddress(String a, String b) {
            return com.simibubi.create.content.logistics.box.PackageItem.matchAddress(a, b);
        }
    }
}
