package me.nanorasmus.nanodev.hex_js.entity;

import at.petrak.hexcasting.common.particles.ConjureParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityHadesSummon extends Zombie {

    private static final ConcurrentHashMap<UUID, java.util.LinkedHashSet<UUID>> CASTER_TO_SUMMONS = new ConcurrentHashMap<>();
    private static final int MAX_PER_CASTER = 5;
    private static final double UPKEEP_MANA_PER_SECOND = 20.0;
    private static final double TICKS_PER_SECOND = 20.0;

    private UUID casterId;
    private boolean small = false;
    private boolean husk = false;
    private Vec3 lookVec = new Vec3(0, 0, 1);
    private int ageTicks = 0;
    /** Принудительная цель от Гамбита Морриган — бьёт даже кастера. */
    private UUID forcedTargetId;

    public EntityHadesSummon(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public static void trackCaster(UUID caster, UUID summonId) {
        CASTER_TO_SUMMONS.compute(caster, (k, set) -> {
            if (set == null) set = new java.util.LinkedHashSet<>();
            set.add(summonId);
            return set;
        });
    }

    public static int countForCaster(UUID caster) {
        var set = CASTER_TO_SUMMONS.get(caster);
        return set == null ? 0 : set.size();
    }

    public static java.util.Set<UUID> getSummonsForCaster(UUID caster) {
        var set = CASTER_TO_SUMMONS.get(caster);
        return set == null ? java.util.Collections.emptySet() : new java.util.LinkedHashSet<>(set);
    }

    public static void untrack(UUID caster, UUID summonId) {
        CASTER_TO_SUMMONS.computeIfPresent(caster, (k, set) -> {
            set.remove(summonId);
            return set.isEmpty() ? null : set;
        });
    }

    public static void untrackCaster(UUID caster) {
        CASTER_TO_SUMMONS.remove(caster);
    }

    public UUID getCasterId() { return casterId; }

    /** Приказ Гамбита Морриган: атаковать эту сущность, кем бы она ни была. */
    public void setForcedTarget(@Nullable UUID target) { this.forcedTargetId = target; }

    @Nullable
    public UUID getForcedTargetId() { return forcedTargetId; }

    @Nullable
    public ServerPlayer getCasterPlayer() {
        if (casterId == null) return null;
        var server = this.level().getServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayer(casterId);
    }

    private boolean isFellowSummon(LivingEntity e) {
        if (casterId == null) return false;
        if (e instanceof EntityHadesSummon other) return casterId.equals(other.casterId);
        if (e instanceof EntityStyxShade other) return casterId.equals(other.getCasterId());
        return false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Убираем ванильный выбор жертв (ближайший игрок/житель/голем) — иначе бьёт владельца.
        this.targetSelector.removeAllGoals(g -> g instanceof NearestAttackableTargetGoal);
        this.targetSelector.addGoal(0, new ForcedTargetGoal(this));
        this.targetSelector.addGoal(1, new CasterHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new CasterAttackTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> e instanceof Enemy && !isFellowSummon(e)));
        this.goalSelector.addGoal(6, new FollowCasterGoal(this, 1.0));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // Приказ гамбита — выше запретов: бьёт даже кастера.
        if (target != null && forcedTargetId != null && target.getUUID().equals(forcedTargetId)) {
            super.setTarget(target);
            return;
        }
        // Страховка: кастер и собратья-саммоны того же кастера — никогда не цели.
        if (target != null && casterId != null) {
            if (target.getUUID().equals(casterId)) return;
            if (isFellowSummon(target)) return;
        }
        super.setTarget(target);
    }

    public void setCaster(UUID caster) {
        this.casterId = caster;
        if (caster != null) {
            trackCaster(caster, this.getUUID());
        }
    }

    public boolean isSmall() { return small; }
    public void setSmall(boolean small) { this.small = small; }

    public boolean isHusk() { return husk; }
    public void setHusk(boolean husk) { this.husk = husk; }

    public void setLookVec(Vec3 look) {
        if (look != null && look.lengthSqr() > 1e-6) {
            this.lookVec = look.normalize();
        }
    }

    public Vec3 getLookVec() { return lookVec; }



    @Override
    public boolean isBaby() {
        return small || super.isBaby();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            ageTicks++;
            // Метка призыва: фиолетовые hex-частицы над головой, как у кастующих.
            if (this.tickCount % 4 == 0) {
                try {
                    ServerLevel sl = (ServerLevel) this.level();
                    sl.sendParticles(new ConjureParticleOptions(0xb38ef3),
                            this.getX(), this.getY() + this.getBbHeight() + 0.15, this.getZ(),
                            2, 0.35, 0.4, 0.35, 0.02);
                } catch (Throwable ignored) {
                }
            }
            if (casterId != null) {
                try {
                    var server = this.level().getServer();
                    if (server != null) {
                        var player = server.getPlayerList().getPlayer(casterId);
                        if (player != null) {
                            boolean free = player.isCreative() || player.isSpectator();
                            try { free = free || at.petrak.hexcasting.api.misc.ManaHelper.hasInfiniteMana(player); } catch (Throwable ignored) {} 
                            if (!free) {
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
                    // ignore
                }
            }
        }
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        super.remove(reason);
        if (casterId != null) {
            untrack(casterId, this.getUUID());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (casterId != null) tag.putString("CasterId", casterId.toString());
        if (forcedTargetId != null) tag.putString("ForcedTarget", forcedTargetId.toString());
        tag.putBoolean("Small", small);
        tag.putBoolean("Husk", husk);
        tag.putInt("AgeTicks", ageTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CasterId")) {
            try { casterId = UUID.fromString(tag.getString("CasterId")); } catch (Throwable ignored) {}
        }
        if (tag.contains("ForcedTarget")) {
            try { forcedTargetId = UUID.fromString(tag.getString("ForcedTarget")); } catch (Throwable ignored) {}
        }
        small = tag.getBoolean("Small");
        husk = tag.getBoolean("Husk");
        ageTicks = tag.getInt("AgeTicks");
        if (casterId != null) {
            trackCaster(casterId, this.getUUID());
        }
    }

    /** Приказ Гамбита Морриган: гнать метку до смерти, невзирая на запреты. */
    private static class ForcedTargetGoal extends TargetGoal {
        private final EntityHadesSummon summon;

        ForcedTargetGoal(EntityHadesSummon summon) {
            super(summon, false);
            this.summon = summon;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Nullable
        private LivingEntity resolve() {
            UUID id = summon.forcedTargetId;
            if (id == null) return null;
            var server = summon.level().getServer();
            if (server == null) return null;
            for (ServerLevel sl : server.getAllLevels()) {
                var e = sl.getEntity(id);
                if (e instanceof LivingEntity le) return le;
            }
            return null; // чанк выгружен — приказ ждёт, не сбрасывается
        }

        @Override
        public boolean canUse() {
            if (summon.forcedTargetId == null) return false;
            LivingEntity mark = resolve();
            if (mark == null) return false;
            if (!mark.isAlive() || mark.isRemoved()) {
                summon.forcedTargetId = null; // метка мертва — приказ снят
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            LivingEntity mark = resolve();
            if (mark != null) summon.setTarget(mark);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = summon.getTarget();
            return target != null && target.isAlive() && !target.isRemoved()
                    && summon.forcedTargetId != null
                    && target.getUUID().equals(summon.forcedTargetId);
        }
    }

    /** Мстить за кастера: бить того, кто ударил кастера. */
    private static class CasterHurtByTargetGoal extends TargetGoal {
        private final EntityHadesSummon summon;
        private LivingEntity attacker;
        private int timestamp;

        CasterHurtByTargetGoal(EntityHadesSummon summon) {
            super(summon, false);
            this.summon = summon;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            ServerPlayer caster = summon.getCasterPlayer();
            if (caster == null) return false;
            LivingEntity revenge = caster.getLastHurtByMob();
            int ts = caster.getLastHurtByMobTimestamp();
            if (revenge == null || ts == this.timestamp) return false;
            if (!this.canAttack(revenge, TargetingConditions.DEFAULT)) return false;
            this.attacker = revenge;
            return true;
        }

        @Override
        public void start() {
            this.summon.setTarget(this.attacker);
            ServerPlayer caster = this.summon.getCasterPlayer();
            if (caster != null) this.timestamp = caster.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    /** Атаковать цель, которую бьёт сам кастер. */
    private static class CasterAttackTargetGoal extends TargetGoal {
        private final EntityHadesSummon summon;
        private LivingEntity victim;
        private int timestamp;

        CasterAttackTargetGoal(EntityHadesSummon summon) {
            super(summon, false);
            this.summon = summon;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            ServerPlayer caster = summon.getCasterPlayer();
            if (caster == null) return false;
            LivingEntity target = caster.getLastHurtMob();
            int ts = caster.getLastHurtMobTimestamp();
            if (target == null || ts == this.timestamp) return false;
            if (!this.canAttack(target, TargetingConditions.DEFAULT)) return false;
            this.victim = target;
            return true;
        }

        @Override
        public void start() {
            this.summon.setTarget(this.victim);
            ServerPlayer caster = this.summon.getCasterPlayer();
            if (caster != null) this.timestamp = caster.getLastHurtMobTimestamp();
            super.start();
        }
    }

    /** Держаться рядом с кастером, когда некого бить. */
    private static class FollowCasterGoal extends Goal {
        private final EntityHadesSummon summon;
        private final double speed;
        private int timeToRecalcPath;

        FollowCasterGoal(EntityHadesSummon summon, double speed) {
            this.summon = summon;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (summon.getTarget() != null) return false;
            ServerPlayer caster = summon.getCasterPlayer();
            if (caster == null || caster.isSpectator()) return false;
            return summon.distanceToSqr(caster) > 100.0;
        }

        @Override
        public boolean canContinueToUse() {
            if (summon.getNavigation().isDone()) return false;
            if (summon.getTarget() != null) return false;
            ServerPlayer caster = summon.getCasterPlayer();
            return caster != null && summon.distanceToSqr(caster) >= 64.0;
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
        }

        @Override
        public void stop() {
            summon.getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayer caster = summon.getCasterPlayer();
            if (caster == null) return;
            summon.getLookControl().setLookAt(caster, 10.0F, (float) summon.getMaxHeadXRot());
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                summon.getNavigation().moveTo(caster, this.speed);
            }
        }
    }

}
