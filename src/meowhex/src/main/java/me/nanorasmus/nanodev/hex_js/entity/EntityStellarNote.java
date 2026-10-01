package me.nanorasmus.nanodev.hex_js.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Звёздочка «Стеллар Тюна»: летит к намеченной цели по волнообразной траектории.
 *
 * <p>В Terraria снаряд летит «звёздами по волнообразной траектории» к курсору.
 * Здесь перенесена именно волна: {@link #WAVE_AMPLITUDE} — амплитуда бокового
 * отклонения, {@link #WAVE_PERIOD} — период в тиках. Скорость складывается из
 * движения к цели и касательной компоненты, поэтому звезда идёт дугой, а не
 * по прямой; знак касательной чередуется, так что дуга «дышит» вокруг цели.
 *
 * <p>Отличия от оригинала (осознанные):
 * <ul>
 *   <li>бьёт <b>только</b> ту цель, на которую игрок навёлся: иначе предмет
 *       просто играет ноту и не стреляет;</li>
 *   <li>блоки непроходимы — при ударе звезда гаснет;</li>
 *   <li>само-наведения и пробивания блоков нет.</li>
 * </ul>
 *
 * <p>Урон магический ({@value #DAMAGE} HP), поэтому броня и защита от магии
 * работают как обычно. Звуков не играет: ноту выдаёт сам предмет при выстреле.
 *
 * <p>Внешний вид — только лента-след, спрайта нет: спрайт звезды, вырезанный из
 * демо-ролика Terraria, выглядел хуже нарисованной фигуры (GIF с 4-цветной
 * палитрой), поэтому оставлен один след. Рисует {@code StellarNoteRenderer}.
 */
public class EntityStellarNote extends Projectile {

    /** Амплитуда бокового отклонения, блоков. */
    public static final double WAVE_AMPLITUDE = 0.55;
    /** Период колебания в тиках: меньше — плотнее волна. */
    public static final int WAVE_PERIOD = 14;
    /** Скорость по направлению к цели, блоков за тик. */
    public static final double SPEED = 1.5;
    /** Урон попадания, HP. */
    public static final float DAMAGE = 4.0f;
    /** Страховка от «вечно летящей» звезды, тиков. */
    public static final int MAX_AGE = 100;

    /** Сколько спрайтов-искр есть на выбор — по одному на выстрел. */
    public static final int SPRITE_VARIANTS = 3;

    /**
     * Номер спрайта этой звезды. Синхронизируется, чтобы клиент показал ту же
     * искру, что выбрал сервер, а не свою случайную.
     */
    private static final EntityDataAccessor<Integer> DATA_SPRITE =
            SynchedEntityData.defineId(EntityStellarNote.class, EntityDataSerializers.INT);

    private UUID targetId;
    private int tickOffset;

    public EntityStellarNote(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // super не зовём: Entity.defineSynchedData абстрактный, звать нечего.
        builder.define(DATA_SPRITE, 0);
    }

    /** Какой спрайт-искру рисовать, 0..{@link #SPRITE_VARIANTS}-1. */
    public int getSprite() {
        return Math.floorMod(this.entityData.get(DATA_SPRITE), SPRITE_VARIANTS);
    }

    /** Выбирает спрайт случайно — каждая звезда в залпе выглядит иначе. */
    public void randomizeSprite(RandomSource rnd) {
        this.entityData.set(DATA_SPRITE, rnd.nextInt(SPRITE_VARIANTS));
    }

    public EntityStellarNote(Level level, LivingEntity owner, LivingEntity target, int tickOffset) {
        super(HexEntities.STELLAR_NOTE.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setDeltaMovement(Vec3.ZERO);
        if (target != null) {
            this.targetId = target.getUUID();
        }
        this.tickOffset = tickOffset;
    }

    /** Смещение фазы волны, чтобы звезды одного залпа не шли в унисон. */
    public void setTickOffset(int tickOffset) {
        this.tickOffset = tickOffset;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount > MAX_AGE) {
            this.fizzle();
            return;
        }
        LivingEntity target = this.resolveTarget();
        if (target == null) {
            this.fizzle();
            return;
        }
        this.flyTowards(target);
    }

    private LivingEntity resolveTarget() {
        if (this.targetId == null) {
            return null;
        }
        if (this.level() instanceof ServerLevel sl && sl.getEntity(this.targetId) instanceof LivingEntity le
                && le.isAlive() && le.level() == sl) {
            return le;
        }
        return null;
    }

    private void flyTowards(LivingEntity target) {
        Vec3 pos = this.position();
        Vec3 toTarget = target.getEyePosition().subtract(pos);
        double dist = toTarget.length();
        if (dist < 1.1) {
            this.impact(target);
            return;
        }
        Vec3 dir = toTarget.scale(1.0 / dist);

        // Касательная вокруг цели даёт боковое отклонение волны.
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-6) {
            // Цель строго над или под нами — берём другую базу для касательной.
            side = dir.cross(new Vec3(1, 0, 0));
        }
        if (side.lengthSqr() < 1e-6) {
            side = dir.cross(new Vec3(0, 0, 1));
        }
        side = side.normalize();

        double phase = (this.tickCount + this.tickOffset) * (2.0 * Math.PI / WAVE_PERIOD);
        Vec3 step = dir.scale(SPEED)
                .add(side.scale(Math.sin(phase) * WAVE_AMPLITUDE * SPEED));

        this.setDeltaMovement(step);
        this.faceTowards(step);
        Vec3 before = this.position();
        this.move(MoverType.SELF, step);
        this.spawnTrail(before);
    }

    private void impact(LivingEntity target) {
        if (this.level() instanceof ServerLevel sl) {
            if (target.hurt(damageSources().magic(), DAMAGE)) {
                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyePosition().y, target.getZ(),
                        12, 0.3, 0.3, 0.3, 0.1);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.4f);
            }
        }
        this.fizzle();
    }

    /**
     * След теперь рисует рендерер лентой (см. {@code StellarNoteRenderer}), поэтому
     * здесь частицы не спавнятся: раз в два тика они давали рваные разрывы
     * вместо непрерывного следа. Осталось только подсветить точку попадания.
     */
    private void spawnTrail(Vec3 pos) {
        // намеренно пусто — след рисуется на клиенте
    }

    private void faceTowards(Vec3 d) {
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        this.setYRot((float) Math.toDegrees(Math.atan2(d.x, d.z)));
        this.setXRot((float) Math.toDegrees(Math.atan2(d.y, horiz)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private void fizzle() {
        if (this.level() instanceof ServerLevel sl && !this.isRemoved()) {
            sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 6,
                    0.08, 0.08, 0.08, 0.02);
        }
        this.discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.targetId != null) {
            tag.putUUID("Target", this.targetId);
        }
        tag.putInt("WaveOffset", this.tickOffset);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Target")) {
            this.targetId = tag.getUUID("Target");
        }
        this.tickOffset = tag.getInt("WaveOffset");
    }
}
