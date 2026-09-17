package at.petrak.hexcasting.api.casting.eval.env;

import at.petrak.hexcasting.api.casting.eval.CastResult;
import at.petrak.hexcasting.api.casting.eval.sideeffects.EvalSound;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds;
import at.petrak.hexcasting.common.msgs.MsgNewSpiralPatternsS2C;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.util.List;

public class PackagedItemCastEnv extends PlayerBasedCastEnv {

    protected EvalSound sound = HexEvalSounds.NOTHING;

    /** Медиа, прокачанная через предмет за этот каст (для износа, см. ItemPackagedHex). */
    private long channeledMedia = 0;

    public PackagedItemCastEnv(ServerPlayer caster, InteractionHand castingHand) {
        super(caster, castingHand);
    }

    public long getChanneledMedia() {
        return channeledMedia;
    }

    @Override
    protected long extractMana(long costLeft) {
        long before = costLeft;
        long leftover = super.extractMana(costLeft);
        long paid = before - Math.max(leftover, 0);
        if (paid > 0) {
            channeledMedia += paid;
        }
        return leftover;
    }

    @Override
    public long extractMediaEnvironment(long costLeft) {
        if (this.caster.isCreative() || at.petrak.hexcasting.api.misc.ManaHelper.hasInfiniteMana(this.caster))
            return 0;

        // meowhex: своего пула аметиста у предмета нет — всё платится маной кастера.
        return this.extractMana(costLeft);
    }

    @Override
    public void postExecution(CastResult result) {
        super.postExecution(result);

        if (result.component1() instanceof PatternIota patternIota) {
            var packet = new MsgNewSpiralPatternsS2C(
                    this.caster.getUUID(), List.of(patternIota.getPattern()), 140
            );
            IXplatAbstractions.INSTANCE.sendPacketToPlayer(this.caster, packet);
            IXplatAbstractions.INSTANCE.sendPacketTracking(this.caster, packet);
        }

        // TODO: how do we know when to actually play this sound?
        this.sound = this.sound.greaterOf(result.getSound());
    }

    @Override
    public InteractionHand getCastingHand() {
        return this.castingHand;
    }

    @Override
    public FrozenPigment getPigment() {
        var casterStack = this.caster.getItemInHand(this.castingHand);
        var casterHexHolder = IXplatAbstractions.INSTANCE.findHexHolder(casterStack);
        if (casterHexHolder == null)
            return IXplatAbstractions.INSTANCE.getPigment(this.caster);
        var hexHolderPigment = casterHexHolder.getPigment();
        if (hexHolderPigment != null)
            return hexHolderPigment;
        return IXplatAbstractions.INSTANCE.getPigment(this.caster);
    }

    public EvalSound getSound() {
        return sound;
    }
}
