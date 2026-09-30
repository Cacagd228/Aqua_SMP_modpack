package dev.aerofix.mixin;

import dev.aerofix.AeroFixServerConfig;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintHandle;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Null-guard для поворотного подшипника (Swivel Bearing).
 *
 * <p>Стоковый {@code getAttachedSubLevel()} делает так:
 * <pre>{@code
 * SubLevel sl = SubLevelContainer.getContainer(level).getSubLevel(id);   // может вернуть null
 * validateConstraintHandle();                                           // handle может стать null
 * if (handle != null) reattachConstraint((ServerSubLevel) sl, true);    // sl — без проверки!
 * }</pre>
 *
 * <p>С двумя независимыми источниками null здесь это отложенный NPE: саблевел успевают
 * удалить между {@code getSubLevel} и вызовом, либо constraint handle протухает в
 * {@code validateConstraintHandle}. Апстрим чинит это перестановкой проверок, но
 * чтобы фикс не зависел от порядка байткода, мы просто не пускаем метод входить с
 * некорректными аргументами — guard стоит на самой точке вызова и покрывает все
 * вызывающие места сразу.
 *
 * <p>Проверять {@code handle.isValid()} здесь нельзя: повторное attach'ение к
 * протухшему handle — это ровно тот случай, ради которого метод существует.
 */
@Mixin(SwivelBearingBlockEntity.class)
public abstract class SwivelBearingBlockEntityMixin {
    @Shadow
    private RotaryConstraintHandle handle;

    @Inject(method = "reattachConstraint", at = @At("HEAD"), cancellable = true)
    private void aerofix$guardReattach(ServerSubLevel subLevel, boolean create, CallbackInfo ci) {
        if (!AeroFixServerConfig.fixSwivelBearing()) {
            return;
        }
        if (subLevel == null || this.handle == null) {
            ci.cancel();
        }
    }
}
