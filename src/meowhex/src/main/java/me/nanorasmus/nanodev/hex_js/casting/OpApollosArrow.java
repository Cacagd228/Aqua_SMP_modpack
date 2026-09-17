package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.EntityIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.Vec3Iota;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Apollo's Arrow — Vec3/Entity (откуда/кто), Vec3 (куда), Double (сила 1-5)
 * Стрела от источника в цель, 10% сильнее лука, 5+10*ур (15-55 пыли), трейсер HEX 1/3т.
 */
public class OpApollosArrow implements SpellAction {

    public static final OpApollosArrow INSTANCE = new OpApollosArrow();

    private OpApollosArrow() {}

    @Override public int getArgc() { return 3; }
    @Override public boolean hasCastingSound(CastingEnvironment env) { return true; }
    @Override public boolean awardsCastingStat(CastingEnvironment env) { return true; }
    @Override public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) { return SpellAction.DefaultImpls.operate(this, env, image, cont); }
    @Override public Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) { return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata); }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T { throw (T) t; }

    @Override
    public Result execute(List<? extends Iota> args, CastingEnvironment env) {
        // args[0] = source Vec3/Entity, args[1] = target Vec3, args[2] = Double power
        Iota a0 = args.get(0);
        Iota a1 = args.get(1);
        Iota a2 = args.get(2);

        Vec3 sourceVec = null;
        Entity sourceEnt = null;
        boolean sourceIsEntity = false;

        if (a0 instanceof EntityIota) {
            try { sourceEnt = at.petrak.hexcasting.api.casting.OperatorUtils.getEntity(args, 0, getArgc()); sourceIsEntity = true; }
            catch (Throwable t) { sneakyThrow(t); }
        } else if (a0 instanceof Vec3Iota) {
            try { sourceVec = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 0, getArgc()); }
            catch (Throwable t) { sneakyThrow(t); }
        } else {
            sneakyThrow(new OvidMishap("Требуется Vec3 или Entity как источник"));
        }

        Vec3 dirVec;
        try { dirVec = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 1, getArgc()); }
        catch (Throwable t) { sneakyThrow(t); return null; }

        double power;
        try {
            power = at.petrak.hexcasting.api.casting.OperatorUtils.getDouble(args, 2, getArgc());
        } catch (Throwable t) { sneakyThrow(t); return null; }

        if (power < 1) power = 1;
        if (power > 5) power = 5;
        int level = (int) Math.round(power);
        if (level < 1) level = 1;
        if (level > 5) level = 5;

        long cost = (5L + 10L * level) * 10000L;

        ServerLevel world = env.getWorld();
        Vec3 sourcePos;
        Entity owner = null;

        if (sourceIsEntity) {
            if (!(sourceEnt instanceof LivingEntity)) {
                sneakyThrow(new OvidMishap("Источник-сущность должна быть LivingEntity"));
            }
            try { env.assertEntityInRange(sourceEnt); } catch (Throwable t) { sneakyThrow(t); }
            sourcePos = ((LivingEntity) sourceEnt).getEyePosition();
            owner = sourceEnt;
        } else {
            try {
                env.assertVecInRange(sourceVec);
                env.assertVecInWorld(sourceVec);
            } catch (Throwable t) { sneakyThrow(t); }
            if (!env.isVecInWorld(sourceVec)) sneakyThrow(new OvidMishap("Вектор вне мира"));
            sourcePos = sourceVec;
            try { owner = env.getCaster(); } catch (Throwable ignored) {}
        }

        // dirVec is direction, not world pos — do not check isVecInWorld / inRange
        if (dirVec.lengthSqr() < 1e-6) sneakyThrow(new OvidMishap("Нулевой вектор направления"));
        Vec3 dir = dirVec.normalize();

        if (sourceIsEntity) {
            sourcePos = sourcePos.add(dir.scale(0.5));
        }

        // If source is entity, offset a bit forward
        if (sourceIsEntity) {
            sourcePos = sourcePos.add(dir.scale(0.5));
        }

        // Speed 10% stronger than bow: bow 3.0 max, so 3.3 at power 5
        float speed = (float) (level * 0.66f); // 1->0.66, 5->3.3
        // Clamp
        if (speed < 0.66f) speed = 0.66f;
        if (speed > 3.5f) speed = 3.5f;

        // Pigment color for tracer
        int color = 0xFF_FF00FF;
        try {
            var pigment = env.getPigment();
            if (pigment != null) {
                var provider = at.petrak.hexcasting.xplat.IXplatAbstractions.INSTANCE.getColorProvider(pigment);
                if (provider != null) {
                    // Try to get color via pigment
                    // Fallback to default
                }
            }
        } catch (Throwable ignored) {}
        // Try to get hex color from pigment via FrozenPigment -> ColorProvider is complex, use default

        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(sourcePos, 0.8, 10),
                ParticleSpray.burst(sourcePos, 0.5, 10)
        );

        return new Result(new Spell(sourcePos, dir, speed, owner, color), cost, particles, 0);
    }

    public static class Spell implements RenderedSpell {
        private final Vec3 pos;
        private final Vec3 dir;
        private final float speed;
        private final Entity owner;
        private final int color;

        public Spell(Vec3 pos, Vec3 dir, float speed, Entity owner, int color) {
            this.pos = pos;
            this.dir = dir;
            this.speed = speed;
            this.owner = owner;
            this.color = color;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) { return RenderedSpell.DefaultImpls.cast(this, env, image); }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            var type = me.nanorasmus.nanodev.hex_js.entity.HexEntities.APOLLO_ARROW.get();
            var arrow = new me.nanorasmus.nanodev.hex_js.entity.EntityApolloArrow(world, pos.x, pos.y, pos.z, new ItemStack(Items.ARROW), color);
            // Set owner
            if (owner instanceof net.minecraft.world.entity.LivingEntity living) {
                arrow.setOwner(living);
            } else if (owner != null) {
                try { arrow.setOwner(owner); } catch (Throwable ignored) {}
            } else {
                try {
                    var caster = env.getCaster();
                    if (caster != null) arrow.setOwner(caster);
                } catch (Throwable ignored) {}
            }
            arrow.shoot(dir.x, dir.y, dir.z, speed, 0.2f);
            // No gravity tweak? Use default
            world.addFreshEntity(arrow);
        }
    }
}
