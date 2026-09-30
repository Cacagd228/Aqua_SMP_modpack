package dev.aerofix.mixin;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.aerofix.AeroFixServerConfig;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Санация состояния блока руля (Steering Wheel).
 *
 * <p>Три точки, где в стоке может появиться NaN/выброс за пределы:
 * <ol>
 *   <li>{@code angleInput.value} — берётся из NBT. Значение 0 проходит и в стоковый
 *       {@code Mth.clamp(v, -0, 0)}, и в {@code SteeringWheelHandler#angleLimit},
 *       где потом идёт деление. Лечится клампом в [1, 360] — ровно тот диапазон,
 *       который Create сам задаёт в {@code ScrollValueBehaviour.between(1, 360)}.</li>
 *   <li>{@code angle} / {@code targetAngle} / {@code targetAngleToUpdate} — углы.
 *       Неfinite-значение из пакета или из битого сейва здесь превращается в NaN
 *       в Sable. {@code tick()} чинит это каждый тик, поэтому битый сейв
 *       самовосстанавливается, а не требует ручного пересоздания блока.</li>
 *   <li>{@code sequencedAngleLimit} — тот же класс проблем; кап 720°.</li>
 * </ol>
 *
 * <p>{@code updateTargetAngle} пропускает неfinite-вход, чтобы он не дошёл до
 * {@code Mth.clamp}: в Sтоке {@code Math.max(NaN, min)} == NaN, то есть кламп
 * NaN не спасает.
 */
@Mixin(SteeringWheelBlockEntity.class)
public abstract class SteeringWheelBlockEntityMixin {
    private static final float MIN_ANGLE_INPUT = 1f;
    private static final float MAX_ANGLE_INPUT = 360f;
    private static final double MAX_SEQUENCED_LIMIT = 720.0;

    @Shadow
    public ScrollValueBehaviour angleInput;

    @Shadow
    public float targetAngle;

    @Shadow
    public float targetAngleToUpdate;

    @Shadow
    private float angle;

    @Shadow
    private double sequencedAngleLimit;

    /**
     * Единственная точка, куда угол из сети попадает в блок. Отсекаем неfinite-вход,
     * попутно подтягивая предел угла в допустимый диапазон — дальше стоковый код
     * сам клампит значение по {@code angleInput.getValue()}.
     */
    @Inject(method = "updateTargetAngle", at = @At("HEAD"), cancellable = true)
    private void aerofix$rejectNonFiniteAngle(float value, CallbackInfo ci) {
        if (!AeroFixServerConfig.fixSteeringWheel()) {
            return;
        }
        if (angleInput != null) {
            angleInput.value = Mth.clamp(angleInput.value, 1, 360);
        }
        if (!Float.isFinite(value)) {
            ci.cancel();
        }
    }

    /**
     * Самовосстановление состояния каждый тик. Здесь же дожимается кламп
     * {@code targetAngleToUpdate}, записанного из сети пакетом.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void aerofix$healState(CallbackInfo ci) {
        if (!AeroFixServerConfig.fixSteeringWheel()) {
            return;
        }

        float limit = MAX_ANGLE_INPUT;
        if (angleInput != null) {
            angleInput.value = Mth.clamp(angleInput.value, 1, 360);
            limit = angleInput.value;
        }

        if (!Float.isFinite(this.angle)) {
            this.angle = 0f;
        }
        if (!Float.isFinite(this.targetAngle)) {
            this.targetAngle = 0f;
        }
        if (!Float.isFinite(this.targetAngleToUpdate)) {
            this.targetAngleToUpdate = 0f;
        }
        this.targetAngleToUpdate = Mth.clamp(this.targetAngleToUpdate, -limit, limit);

        if (!Double.isFinite(this.sequencedAngleLimit)) {
            this.sequencedAngleLimit = 0.0;
        }
        this.sequencedAngleLimit = Mth.clamp(this.sequencedAngleLimit, 0.0, MAX_SEQUENCED_LIMIT);
    }

    /**
     * Чинит битый сейв на месте, до того как Create его прочитает. Ключи те же,
     * что использует {@code SteeringWheelBlockEntity#read}.
     */
    @Inject(method = "read(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Z)V",
            at = @At("HEAD"))
    private void aerofix$sanitizeNbt(CompoundTag tag, HolderLookup.Provider registries,
                                     boolean clientPacket, CallbackInfo ci) {
        if (!AeroFixServerConfig.fixSteeringWheel()) {
            return;
        }

        aerofix$clampTagFloat(tag, "Angle", -MAX_ANGLE_INPUT, MAX_ANGLE_INPUT);
        aerofix$clampTagFloat(tag, "TargetAngle", -MAX_ANGLE_INPUT, MAX_ANGLE_INPUT);
        aerofix$clampTagFloat(tag, "TargetAngleToUpdate", -MAX_ANGLE_INPUT, MAX_ANGLE_INPUT);

        if (tag.contains("SequencedAngleLimit")) {
            double seq = tag.getDouble("SequencedAngleLimit");
            if (!Double.isFinite(seq)) {
                seq = 0.0;
            }
            tag.putDouble("SequencedAngleLimit", Mth.clamp(seq, 0.0, MAX_SEQUENCED_LIMIT));
        }
    }

    private static void aerofix$clampTagFloat(CompoundTag tag, String key, float min, float max) {
        if (!tag.contains(key)) {
            return;
        }
        float value = tag.getFloat(key);
        if (!Float.isFinite(value)) {
            value = 0f;
        }
        tag.putFloat(key, Mth.clamp(value, min, max));
    }
}
