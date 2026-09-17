package at.petrak.hexcasting.api.casting.eval.env;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.eval.CastResult;
import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexCoord;
import at.petrak.hexcasting.api.misc.ManaHelper;
import at.petrak.hexcasting.api.mod.HexStatistics;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import at.petrak.hexcasting.common.msgs.*;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;

public class StaffCastEnv extends PlayerBasedCastEnv {
    private final InteractionHand castingHand;


    public StaffCastEnv(ServerPlayer caster, InteractionHand castingHand) {
        super(caster, castingHand);

        this.castingHand = castingHand;
    }

    @Override
    public void postExecution(CastResult result) {
        super.postExecution(result);

        if (result.component1() instanceof PatternIota patternIota) {
            var packet = new MsgNewSpiralPatternsS2C(
                this.caster.getUUID(), List.of(patternIota.getPattern()), Integer.MAX_VALUE
            );
            IXplatAbstractions.INSTANCE.sendPacketToPlayer(this.caster, packet);
            IXplatAbstractions.INSTANCE.sendPacketTracking(this.caster, packet);
        }

        // we always want to play this sound one at a time
        var sound = result.getSound().sound();
        if (sound != null) {
            var soundPos = this.caster.position();
            this.world.playSound(null, soundPos.x, soundPos.y, soundPos.z,
                sound, SoundSource.PLAYERS, 1f, 1f);
        }
    }

    @Override
    public long extractMediaEnvironment(long cost) {
        if (this.caster.isCreative() || ManaHelper.hasInfiniteMana(this.caster))
            return 0;

        return this.extractMana(cost);
    }

    @Override
    public InteractionHand getCastingHand() {
        return castingHand;
    }

    @Override
    public FrozenPigment getPigment() {
        return HexAPI.instance().getColorizer(this.caster);
    }

    public static void handleNewPatternOnServer(ServerPlayer sender, MsgNewSpellPatternC2S msg) {
        boolean cheatedPatternOverlap = false;

        List<ResolvedPattern> resolvedPatterns = msg.resolvedPatterns();
        if (!resolvedPatterns.isEmpty()) {
            var allPoints = new HashSet<HexCoord>();
            for (int i = 0; i < resolvedPatterns.size() - 1; i++) {
                ResolvedPattern pat = resolvedPatterns.get(i);
                allPoints.addAll(pat.getPattern().positions(pat.getOrigin()));
            }
            var currentResolvedPattern = resolvedPatterns.get(resolvedPatterns.size() - 1);
            var currentSpellPoints = currentResolvedPattern.getPattern()
                .positions(currentResolvedPattern.getOrigin());
            if (currentSpellPoints.stream().anyMatch(allPoints::contains)) {
                cheatedPatternOverlap = true;
            }
        }

        if (cheatedPatternOverlap) {
            return;
        }

        sender.awardStat(HexStatistics.PATTERNS_DRAWN);

        var vm = IXplatAbstractions.INSTANCE.getStaffcastVM(sender, msg.handUsed());

        // TODO: do we reset the number of evals run via the staff? because each new pat is a new tick.

        ExecutionClientView clientInfo = vm.queueExecuteAndWrapIota(new PatternIota(msg.pattern()), sender.serverLevel());

        // meowhex: ошибка каста выкидывает из меню рисования, НО стак не стирает.
        // Харнесс храним как обычно (ванилла), а клиенту шлём пустой вью —
        // GuiSpellcasting закрывается по isStackClear. При переоткрытии меню
        // (ItemStaff.use) клиент пересинкается с серверного харнесса — всё
        // написанное на месте, включая ошибочный штрих (красным).
        // Ловим любой неуспех (ERRORED — мишапы, INVALID — неверный паттерн/
        // запрещённый спелл): «неправильная йота» это как раз INVALID.
        // UNRESOLVED с сервера не приходит (это клиентское состояние штриха),
        // так что обычные штрихи меню не закрывают.
        boolean failed = !clientInfo.getResolutionType().getSuccess();

        if (clientInfo.isStackClear()) {
            IXplatAbstractions.INSTANCE.setStaffcastImage(sender, null);
            IXplatAbstractions.INSTANCE.setPatterns(sender, List.of());
        } else {
            IXplatAbstractions.INSTANCE.setStaffcastImage(sender, vm.getImage().withOverriddenUsedOps(0));
            if (!resolvedPatterns.isEmpty()) {
                resolvedPatterns.get(resolvedPatterns.size() - 1).setType(clientInfo.getResolutionType());
            }
            IXplatAbstractions.INSTANCE.setPatterns(sender, resolvedPatterns);
        }

        ExecutionClientView toSend = clientInfo;
        if (failed && !clientInfo.isStackClear()) {
            toSend = new ExecutionClientView(true, clientInfo.getResolutionType(), List.of(), null);
        }

        IXplatAbstractions.INSTANCE.sendPacketToPlayer(sender,
            new MsgNewSpellPatternS2C(toSend, resolvedPatterns.size() - 1));

        IMessage packet;
        if (toSend.isStackClear()) {
            packet = new MsgClearSpiralPatternsS2C(sender.getUUID());
        } else {
            packet = new MsgNewSpiralPatternsS2C(sender.getUUID(), List.of(msg.pattern()), Integer.MAX_VALUE);
        }
        IXplatAbstractions.INSTANCE.sendPacketToPlayer(sender, packet);
        IXplatAbstractions.INSTANCE.sendPacketTracking(sender, packet);

        if (clientInfo.getResolutionType().getSuccess()) {
            // Somehow we lost spraying particles on each new pattern, so do it here
            // this also nicely prevents particle spam on trinkets
            new ParticleSpray(sender.position(), new Vec3(0.0, 1.5, 0.0), 0.4, Math.PI / 3, 30)
                .sprayParticles(sender.serverLevel(), IXplatAbstractions.INSTANCE.getPigment(sender));
        }
    }
}
