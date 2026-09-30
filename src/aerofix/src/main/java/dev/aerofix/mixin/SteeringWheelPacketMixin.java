package dev.aerofix.mixin;

import dev.aerofix.AeroFixServerConfig;
import dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity;
import dev.simulated_team.simulated.network.packets.SteeringWheelPacket;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Серверная валидация пакета руля.
 *
 * <p>Стоковый {@code SteeringWheelPacket#handle} берёт {@code targetAngle} из сети и
 * пишет его в блок руля <em>как есть</em>. Дальше угол уходит в
 * {@code updateTargetAngle} → в Sable-физику, где становится
 * {@code generatedSpeed} / {@code logicalSpeed}.
 *
 * <p>Что это даёт:
 * <ul>
 *   <li>Неfinite-угол в Sable — это NaN во всех производных величинах, а от NaN
 *       contraption уже не «разъезжается», а застревает и тянет за собой симуляцию.</li>
 *   <li>Проверка дистанции закрывает управление рулём, находящимся в другом конце
 *       карты: раньше пакет с корректным BlockPos применялся к любому рулю на сервере.</li>
 * </ul>
 *
 * <p>Остальное тело метода намеренно не трогаем: у нас нет доступа к BE из
 * {@code @Redirect} без хака с полем на record-классе, поэтому кламп значения
 * делает {@link SteeringWheelBlockEntityMixin} в {@code tick()}. Поле
 * {@code targetAngleToUpdate} читается только в {@code tick()} и {@code write()},
 * то есть до следующего тика никто не успеет увидеть невалидное значение.
 */
@Mixin(SteeringWheelPacket.class)
public abstract class SteeringWheelPacketMixin {
    @Shadow
    public abstract boolean shouldStop();

    @Shadow
    public abstract float targetAngle();

    @Shadow
    public abstract BlockPos pos();

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void aerofix$rejectBadSteeringPacket(ServerPacketContext ctx, CallbackInfo ci) {
        if (!AeroFixServerConfig.fixSteeringWheel()) {
            return;
        }

        ServerPlayer player = ctx.player();
        if (player == null) {
            ci.cancel();
            return;
        }

        if (!Float.isFinite(this.targetAngle())) {
            ci.cancel();
            return;
        }

        BlockPos pos = this.pos();
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SteeringWheelBlockEntity)) {
            // Не руль — пакету тут делать нечего. Сток и так отфильтровал бы это
            // через instanceof, но только ПОСЛЕ getBlockEntity на чужом BlockPos.
            ci.cancel();
            return;
        }

        Vec3 centre = Vec3.atCenterOf(pos);
        double range = AeroFixServerConfig.steeringMaxRange();
        if (player.distanceToSqr(centre.x, centre.y, centre.z) > range * range) {
            ci.cancel();
        }
    }
}
