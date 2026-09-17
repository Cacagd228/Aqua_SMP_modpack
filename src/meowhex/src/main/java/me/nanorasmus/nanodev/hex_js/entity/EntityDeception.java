package me.nanorasmus.nanodev.hex_js.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hermes' Deception — пустышка, копирующая внешность LivingEntity/Mob.
 * Копирует модель/текстуру/размер/хитбокс/свечение/экипировку/позу/имя, без ИИ.
 * 1 HP, без лимита времени, гравитация+физика, hitbox, pushable, NoAI, без урона от падения.
 */
public class EntityDeception extends LivingEntity {

    private static final EntityDataAccessor<String> DATA_TARGET_ID = SynchedEntityData.defineId(EntityDeception.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_CUSTOM_NAME_JSON = SynchedEntityData.defineId(EntityDeception.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> DATA_GLOWING = SynchedEntityData.defineId(EntityDeception.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_POSE_ID = SynchedEntityData.defineId(EntityDeception.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> DATA_PLAYER_UUID = SynchedEntityData.defineId(EntityDeception.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_PLAYER_NAME = SynchedEntityData.defineId(EntityDeception.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_CASTER_UUID = SynchedEntityData.defineId(EntityDeception.class, EntityDataSerializers.STRING);

    // Lifetime removed — lives until killed/recalled/out of mana

    // Per-caster limit: 5 copies
    private static final ConcurrentHashMap<UUID, java.util.LinkedHashSet<UUID>> CASTER_TO_DECEPTIONS = new ConcurrentHashMap<>();
    private static final int MAX_PER_CASTER = 5;
    /** Upkeep rate per deception: mana per second, drained smoothly every tick. */
    private static final double UPKEEP_MANA_PER_SECOND = 20.0;
    private static final double TICKS_PER_SECOND = 20.0;

    private int ageTicks = 0;
    private UUID casterId;
    private Vec3 lookVec = new Vec3(0, 0, 1);
    private EntityDimensions targetDimensions = EntityDimensions.scalable(0.6f, 1.8f);
    private final java.util.Map<EquipmentSlot, ItemStack> equipment = new java.util.EnumMap<>(EquipmentSlot.class);

    public EntityDeception(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        // 1 HP
        try {
            var attr = this.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(1.0);
        } catch (Throwable ignored) {}
        this.setHealth(1.0f);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equipment.put(slot, ItemStack.EMPTY);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public static void trackCaster(UUID caster, UUID deceptionId) {
        CASTER_TO_DECEPTIONS.compute(caster, (k, set) -> {
            if (set == null) set = new java.util.LinkedHashSet<>();
            set.add(deceptionId);
            return set;
        });
    }

    public static java.util.Set<UUID> getDeceptionsForCaster(UUID caster) {
        var set = CASTER_TO_DECEPTIONS.get(caster);
        return set == null ? java.util.Collections.emptySet() : new java.util.LinkedHashSet<>(set);
    }

    /** Legacy single — returns first if any */
    public static UUID getDeceptionForCaster(UUID caster) {
        var set = CASTER_TO_DECEPTIONS.get(caster);
        if (set == null || set.isEmpty()) return null;
        return set.iterator().next();
    }

    public static void untrack(UUID caster) {
        CASTER_TO_DECEPTIONS.remove(caster);
    }

    public static void untrack(UUID caster, UUID deceptionId) {
        CASTER_TO_DECEPTIONS.computeIfPresent(caster, (k, set) -> {
            set.remove(deceptionId);
            return set.isEmpty() ? null : set;
        });
    }

    public static int countForCaster(UUID caster) {
        var set = CASTER_TO_DECEPTIONS.get(caster);
        return set == null ? 0 : set.size();
    }

    public void setCaster(UUID caster) {
        this.casterId = caster;
        if (caster != null) {
            this.entityData.set(DATA_CASTER_UUID, caster.toString());
            trackCaster(caster, this.getUUID());
        }
    }

    public void setLookFromVec(Vec3 look) {
        if (look != null && look.lengthSqr() > 1e-6) {
            this.lookVec = look.normalize();
            double yaw = Math.toDegrees(Math.atan2(-lookVec.x, lookVec.z));
            double pitch = Math.toDegrees(Math.atan2(-lookVec.y, Math.sqrt(lookVec.x*lookVec.x + lookVec.z*lookVec.z)));
            this.setYRot((float) yaw);
            this.setXRot((float) pitch);
            this.yHeadRot = (float) yaw;
            this.yBodyRot = (float) yaw;
        }
    }

    public Vec3 getLookVec() { return lookVec; }

    public String getPlayerUUID() { return this.entityData.get(DATA_PLAYER_UUID); }
    public String getPlayerName() { return this.entityData.get(DATA_PLAYER_NAME); }
    public String getCasterUuid() { return this.entityData.get(DATA_CASTER_UUID); }

    public UUID getCasterId() { return casterId; }

    public void setTargetFrom(LivingEntity target) {
        // If copying another deception, copy its stored appearance instead of hex_js:deception type
        if (target instanceof EntityDeception deception) {
            String tid = deception.entityData.get(DATA_TARGET_ID);
            if (tid != null && !tid.isEmpty()) {
                this.entityData.set(DATA_TARGET_ID, tid);
                this.targetDimensions = deception.targetDimensions;
                this.updateBoundingBoxFromTarget();
                this.entityData.set(DATA_GLOWING, deception.entityData.get(DATA_GLOWING));
                this.setGlowingTag(deception.hasGlowingTag());
                this.entityData.set(DATA_POSE_ID, deception.entityData.get(DATA_POSE_ID));
                try { this.setPose(Pose.values()[deception.entityData.get(DATA_POSE_ID)]); } catch (Throwable ignored) {}
                String nameJson = deception.entityData.get(DATA_CUSTOM_NAME_JSON);
                this.entityData.set(DATA_CUSTOM_NAME_JSON, nameJson);
                this.setCustomName(deception.getCustomName());
                this.setCustomNameVisible(deception.isCustomNameVisible());
                this.entityData.set(DATA_PLAYER_UUID, deception.entityData.get(DATA_PLAYER_UUID));
                this.entityData.set(DATA_PLAYER_NAME, deception.entityData.get(DATA_PLAYER_NAME));
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack st = deception.getItemBySlot(slot);
                    equipment.put(slot, st.copy());
                    this.setItemSlot(slot, st.copy());
                }
                this.updateBoundingBoxFromTarget();
                return;
            }
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (id != null) {
            this.entityData.set(DATA_TARGET_ID, id.toString());
            // Dimensions from target
            EntityDimensions dims = target.getDimensions(target.getPose());
            this.targetDimensions = dims;
            this.updateBoundingBoxFromTarget();
        }
        // Player profile for skin
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            try {
                this.entityData.set(DATA_PLAYER_UUID, player.getUUID().toString());
                this.entityData.set(DATA_PLAYER_NAME, player.getGameProfile().getName());
            } catch (Throwable ignored) {
                this.entityData.set(DATA_PLAYER_UUID, "");
                this.entityData.set(DATA_PLAYER_NAME, "");
            }
        } else {
            this.entityData.set(DATA_PLAYER_UUID, "");
            this.entityData.set(DATA_PLAYER_NAME, "");
        }
        // Glow
        this.entityData.set(DATA_GLOWING, (byte) (target.hasGlowingTag() ? 1 : 0));
        this.setGlowingTag(target.hasGlowingTag());
        // Pose
        this.entityData.set(DATA_POSE_ID, (byte) target.getPose().ordinal());
        this.setPose(target.getPose());
        // Name
        if (target.hasCustomName()) {
            this.setCustomName(target.getCustomName());
            this.setCustomNameVisible(target.isCustomNameVisible());
            try {
                this.entityData.set(DATA_CUSTOM_NAME_JSON, target.getCustomName().getString());
            } catch (Throwable ignored) {}
        } else {
            this.setCustomName(null);
            this.entityData.set(DATA_CUSTOM_NAME_JSON, "");
        }
        // Equipment
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                ItemStack stack = target.getItemBySlot(slot);
                equipment.put(slot, stack.copy());
                this.setItemSlot(slot, stack.copy());
            } catch (Throwable ignored) {}
        }
        this.updateBoundingBoxFromTarget();
    }

    public ResourceLocation getTargetId() {
        String s = this.entityData.get(DATA_TARGET_ID);
        if (s == null || s.isEmpty()) return null;
        return ResourceLocation.tryParse(s);
    }

    public EntityType<?> getTargetType() {
        ResourceLocation id = getTargetId();
        if (id == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TARGET_ID, "");
        builder.define(DATA_CUSTOM_NAME_JSON, "");
        builder.define(DATA_GLOWING, (byte)0);
        builder.define(DATA_POSE_ID, (byte)0);
        builder.define(DATA_PLAYER_UUID, "");
        builder.define(DATA_PLAYER_NAME, "");
        builder.define(DATA_CASTER_UUID, "");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(DATA_TARGET_ID)) {
            // Try to refresh dims when target changes (client)
            String s = this.entityData.get(DATA_TARGET_ID);
            if (s != null && !s.isEmpty()) {
                ResourceLocation id = ResourceLocation.tryParse(s);
                if (id != null) {
                    var typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
                    typeOpt.ifPresent(type -> {
                        try {
                            EntityDimensions dims = type.getDimensions();
                            this.targetDimensions = dims;
                            this.updateBoundingBoxFromTarget();
                        } catch (Throwable ignored) {}
                    });
                }
            }
        }
    }

    private void updateBoundingBoxFromTarget() {
        if (targetDimensions != null) {
            float w = targetDimensions.width();
            float h = targetDimensions.height();
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();
            this.setBoundingBox(new net.minecraft.world.phys.AABB(x - w/2.0, y, z - w/2.0, x + w/2.0, y + h, z + w/2.0));
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false; // no fall damage
    }

    @Override
    public void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
        // suppress fall damage check
    }

    @Override
    public void tick() {
        super.tick();
        // Keep custom hitbox after super tick (which may reset via refreshDimensions)
        updateBoundingBoxFromTarget();
        if (!this.level().isClientSide) {
            ageTicks++;
            // Upkeep: плавное списание каждый тик — 20 маны/с (1 мана за тик) с
            // мана-пула кастера. Пыль из инвентаря здесь не трогаем: посохи/артефакты — мана,
            // пыль остаётся только для магических кругов.
            if (casterId != null) {
                try {
                    var server = this.level().getServer();
                    if (server != null) {
                        var player = server.getPlayerList().getPlayer(casterId);
                        if (player != null) {
                            boolean free = player.isCreative() || player.isSpectator();
                            try { free = free || at.petrak.hexcasting.api.misc.ManaHelper.hasInfiniteMana(player); } catch (Throwable ignored) {}
                            if (free) {
                                // No upkeep in creative / infinite mana
                            } else {
                                double manaCost = UPKEEP_MANA_PER_SECOND / TICKS_PER_SECOND;
                                double mana = at.petrak.hexcasting.api.misc.ManaHelper.getMana(player);
                                if (mana < manaCost) {
                                    this.discard();
                                } else {
                                    at.petrak.hexcasting.api.misc.ManaHelper.setMana(player, mana - manaCost);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    // On error, keep alive to avoid accidental deletion
                }
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (casterId != null) {
            untrack(casterId, this.getUUID());
        }
    }

    @Override
    protected void tickDeath() {
        // default
        super.tickDeath();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 1 HP, any damage kills
        boolean res = super.hurt(source, amount);
        if (res) {
            // Ensure death on any hit (since max 1)
            if (this.getHealth() > 0) this.setHealth(0);
            this.kill();
        }
        return res;
    }

    // LivingEntity abstract impl
    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return java.util.List.of(
                equipment.getOrDefault(EquipmentSlot.HEAD, ItemStack.EMPTY),
                equipment.getOrDefault(EquipmentSlot.CHEST, ItemStack.EMPTY),
                equipment.getOrDefault(EquipmentSlot.LEGS, ItemStack.EMPTY),
                equipment.getOrDefault(EquipmentSlot.FEET, ItemStack.EMPTY)
        );
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return equipment.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        equipment.put(slot, stack.copy());
        // Do not call super (abstract) — just store
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isPushable() {
        return true; // can be pushed
    }

    @Override
    public void push(Entity entity) {
        // Do not push other entities (без толчка)
        // Intentionally no-op to prevent pushing, but still pushable by others via isPushable
    }

    @Override
    protected void pushEntities() {
        // Suppress pushing logic
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("TargetId", this.entityData.get(DATA_TARGET_ID));
        tag.putString("PlayerUUID", this.entityData.get(DATA_PLAYER_UUID));
        tag.putString("PlayerName", this.entityData.get(DATA_PLAYER_NAME));
        tag.putDouble("LookX", lookVec.x);
        tag.putDouble("LookY", lookVec.y);
        tag.putDouble("LookZ", lookVec.z);
        tag.putInt("AgeTicks", ageTicks);
        if (casterId != null) tag.putString("CasterId", casterId.toString());
        // Save pose, glowing, dims, equipment
        tag.putByte("TargetPose", this.entityData.get(DATA_POSE_ID));
        tag.putByte("TargetGlow", this.entityData.get(DATA_GLOWING));
        // Equipment
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack st = equipment.getOrDefault(slot, ItemStack.EMPTY);
            if (!st.isEmpty()) {
                tag.put("Equip_" + slot.getName(), st.save(this.registryAccess(), new CompoundTag()));
            }
        }
        if (targetDimensions != null) {
            tag.putFloat("Width", targetDimensions.width());
            tag.putFloat("Height", targetDimensions.height());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        String tid = tag.getString("TargetId");
        if (tid != null && !tid.isEmpty()) this.entityData.set(DATA_TARGET_ID, tid);
        if (tag.contains("PlayerUUID")) this.entityData.set(DATA_PLAYER_UUID, tag.getString("PlayerUUID"));
        if (tag.contains("PlayerName")) this.entityData.set(DATA_PLAYER_NAME, tag.getString("PlayerName"));
        if (tag.contains("LookX")) {
            double lx = tag.getDouble("LookX");
            double ly = tag.getDouble("LookY");
            double lz = tag.getDouble("LookZ");
            lookVec = new Vec3(lx, ly, lz);
            setLookFromVec(lookVec);
        }
        ageTicks = tag.getInt("AgeTicks");
        if (tag.contains("CasterId")) {
            try { casterId = UUID.fromString(tag.getString("CasterId")); } catch (Throwable ignored) {}
        }
        if (tag.contains("TargetPose")) this.entityData.set(DATA_POSE_ID, tag.getByte("TargetPose"));
        if (tag.contains("TargetGlow")) {
            byte g = tag.getByte("TargetGlow");
            this.entityData.set(DATA_GLOWING, g);
            this.setGlowingTag(g != 0);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String key = "Equip_" + slot.getName();
            if (tag.contains(key)) {
                try {
                    ItemStack st = ItemStack.parse(this.registryAccess(), tag.getCompound(key)).orElse(ItemStack.EMPTY);
                    equipment.put(slot, st);
                } catch (Throwable ignored) {}
            }
        }
        if (tag.contains("Width") && tag.contains("Height")) {
            float w = tag.getFloat("Width");
            float h = tag.getFloat("Height");
            targetDimensions = EntityDimensions.scalable(w, h);
            this.updateBoundingBoxFromTarget();
        }
    }


}
