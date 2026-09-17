package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.MishapEnvironment;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * CastingEnvironment for deception entity, delegating most to original caster's env
 * but with ambit/position based on deception and free media (cost already covered by whisper).
 */
public class DeceptionCastEnv extends CastingEnvironment {

    private final CastingEnvironment parent;
    private final Entity deception;
    private final ServerLevel deceptionLevel;

    public DeceptionCastEnv(CastingEnvironment parent, Entity deception) {
        super(deception.level() instanceof ServerLevel sl ? sl : parent.getWorld());
        this.parent = parent;
        this.deception = deception;
        this.deceptionLevel = (ServerLevel) deception.level();
    }

    private ServerPlayer fakeCaster;

    /** Real owning player for silence / media attribution when deception mishaps. */
    public ServerPlayer getRealCaster() {
        try { return parent.getCaster(); } catch (Throwable t) { return null; }
    }

    public ServerPlayer getFakeCasterRaw() { return fakeCaster; }

    @Override
    public ServerPlayer getCaster() {
        if (fakeCaster != null) return fakeCaster;
        try {
            ServerPlayer orig = parent.getCaster();
            if (orig == null) return null;
            // Create FakePlayer at deception position to make ambit from deception
            ServerLevel sl = deceptionLevel;
            com.mojang.authlib.GameProfile profile = orig.getGameProfile();
            // Use FakePlayerFactory if available
            try {
                fakeCaster = net.neoforged.neoforge.common.util.FakePlayerFactory.get(sl, profile);
                fakeCaster.setPos(deception.getX(), deception.getY(), deception.getZ());
                fakeCaster.setYRot(deception.getYRot());
                fakeCaster.setXRot(deception.getXRot());
                // Copy creative/spectator
                if (orig.isCreative()) fakeCaster.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                else if (orig.isSpectator()) fakeCaster.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                else fakeCaster.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                return fakeCaster;
            } catch (Throwable t) {
                // Fallback to original
                return orig;
            }
        } catch (Throwable t) { return null; }
    }

    @Override
    public MishapEnvironment getMishapEnvironment() {
        return parent.getMishapEnvironment();
    }

    @Override
    public Vec3 mishapSprayPos() {
        return deception.position().add(0, deception.getBbHeight()/2, 0);
    }

    @Override
    protected boolean isVecInRangeEnvironment(Vec3 vec) {
        // 32 block ambit around deception (same as staff)
        return deception.position().distanceToSqr(vec) <= 32*32;
    }

    @Override
    protected boolean hasEditPermissionsAtEnvironment(BlockPos pos) {
        return parent.hasEditPermissionsAt(pos);
    }

    @Override
    public InteractionHand getCastingHand() {
        return parent.getCastingHand();
    }

    @Override
    protected List<ItemStack> getUsableStacks(StackDiscoveryMode mode) {
        // Delegate via reflection or return empty to avoid protected access issues
        try {
            var m = CastingEnvironment.class.getDeclaredMethod("getUsableStacks", StackDiscoveryMode.class);
            m.setAccessible(true);
            //noinspection unchecked
            return (List<ItemStack>) m.invoke(parent, mode);
        } catch (Throwable t) {
            return List.of();
        }
    }

    @Override
    protected List<HeldItemInfo> getPrimaryStacks() {
        try {
            var m = CastingEnvironment.class.getDeclaredMethod("getPrimaryStacks");
            m.setAccessible(true);
            //noinspection unchecked
            return (List<HeldItemInfo>) m.invoke(parent);
        } catch (Throwable t) {
            return List.of();
        }
    }

    @Override
    public boolean replaceItem(java.util.function.Predicate<ItemStack> predicate, ItemStack stack, InteractionHand hand) {
        return parent.replaceItem(predicate, stack, hand);
    }

    @Override
    public FrozenPigment getPigment() {
        return parent.getPigment();
    }

    @Override
    public FrozenPigment setPigment(FrozenPigment pigment) {
        return parent.setPigment(pigment);
    }

    @Override
    public void produceParticles(ParticleSpray spray, FrozenPigment pigment) {
        parent.produceParticles(spray, pigment);
    }

    @Override
    public void printMessage(net.minecraft.network.chat.Component message) {
        parent.printMessage(message);
    }

    @Override
    protected long extractMediaEnvironment(long cost) {
        // meowhex: внутренний каст через обманку бесплатен — предоплата уже снята
        // с мана-пула в OpWhisperHermes (ManaHelper). Пыль для кругов не трогаем.
        return 0;
    }

    @Override
    public boolean isEnlightened() {
        return parent.isEnlightened();
    }

    @Override
    protected boolean isCreativeMode() {
        try {
            var m = CastingEnvironment.class.getDeclaredMethod("isCreativeMode");
            m.setAccessible(true);
            return (boolean) m.invoke(parent);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void precheckAction(at.petrak.hexcasting.api.casting.PatternShapeMatch match) throws at.petrak.hexcasting.api.casting.mishaps.Mishap {
        parent.precheckAction(match);
    }
}
