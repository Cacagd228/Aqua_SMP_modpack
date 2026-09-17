package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.EntityIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.Vec3Iota;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;
import static at.petrak.hexcasting.api.casting.OperatorUtils.getVec3;

/**
 * Ovid's Distillation — трансмутация предметов.
 * Стек: [A, B] где A = верх (преобразуется), B = низ (жертва).
 * <p>
 * Поддерживает два режима:
 * <ul>
 *   <li>Оба — EntityIota на ItemEntity в ambit (оригинал). Если counts не равны — mishap.</li>
 *   <li>Оба — Vec3Iota на create:depot (или любой блок с ItemHandler). Предмет А лежит в депо, предмет Б в другом депо. После каста А превращается в результат, Б пропадает.</li>
 *   <li>Смешанный: Vec3 (депо) + EntityIota и наоборот — тоже поддерживается, A остаётся целевым.</li>
 * </ul>
 * Рецепт: (itemA, itemB) -> result, регистрируется через KubeJS.
 * Результат масштабируется по количеству входа. Стоимость 20 пыли.
 */
public class OpOvidsDistillation implements SpellAction {

    public static final OpOvidsDistillation INSTANCE = new OpOvidsDistillation();

    private static final long COST = 20L * 10000L; // 20 dust

    private OpOvidsDistillation() {
    }

    @Override
    public int getArgc() {
        return 2;
    }

    @Override
    public boolean hasCastingSound(CastingEnvironment env) {
        return true;
    }

    @Override
    public boolean awardsCastingStat(CastingEnvironment env) {
        return true;
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) {
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        if (args.size() < 2) {
            sneakyThrow(new OvidMishap("Требуются два элемента на стеке"));
        }
        Iota aIota = args.get(0);
        Iota bIota = args.get(1);

        boolean aIsEnt = aIota instanceof EntityIota;
        boolean bIsEnt = bIota instanceof EntityIota;
        boolean aIsVec = aIota instanceof Vec3Iota;
        boolean bIsVec = bIota instanceof Vec3Iota;

        if (aIsEnt && bIsEnt) {
            return executeEntityEntity(args, env);
        } else if (aIsVec && bIsVec) {
            return executeDepotDepot(args, env);
        } else if ((aIsVec && bIsEnt) || (aIsEnt && bIsVec)) {
            return executeMixed(args, env, aIsVec);
        } else {
            sneakyThrow(new OvidMishap("Требуются две сущности предметов или два депо (Vec3). Смешанный: Vec3 + Entity тоже допустим"));
            return null;
        }
    }

    // ---- Entity + Entity (original) ----
    private SpellAction.Result executeEntityEntity(List<? extends Iota> args, CastingEnvironment env) {
        Entity aEnt;
        Entity bEnt;
        try {
            aEnt = getEntity(args, 0, getArgc());
            bEnt = getEntity(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        if (!(aEnt instanceof ItemEntity) || !(bEnt instanceof ItemEntity)) {
            sneakyThrow(new OvidMishap("Требуются сущности предметов"));
            return null;
        }
        ItemEntity aItem = (ItemEntity) aEnt;
        ItemEntity bItem = (ItemEntity) bEnt;

        try {
            env.assertEntityInRange(aItem);
            env.assertEntityInRange(bItem);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        ItemStack stackA = aItem.getItem();
        ItemStack stackB = bItem.getItem();

        if (stackA.isEmpty() || stackB.isEmpty()) {
            sneakyThrow(new OvidMishap("Пустой предмет"));
        }

        if (stackA.getCount() != stackB.getCount()) {
            sneakyThrow(new OvidMishap("Неравное количество"));
        }

        ItemStack resultTemplate = OvidRecipeRegistry.get(stackA, stackB);
        if (resultTemplate == null || resultTemplate.isEmpty()) {
            sneakyThrow(new OvidMishap("Нет рецепта для " + stackA.getItem() + " + " + stackB.getItem()));
        }

        int inputCount = stackA.getCount();
        ItemStack result = resultTemplate.copy();
        int templateCount = resultTemplate.getCount();
        int outCount = templateCount * inputCount;
        result.setCount(outCount);

        Vec3 posA = aItem.position();
        Vec3 bPos = bItem.position();
        try {
            env.assertVecInRange(posA);
            env.assertVecInRange(bPos);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        Vec3 aEye = posA.add(0, aItem.getEyeHeight() / 2.0, 0);
        Vec3 bEye = bPos.add(0, bItem.getEyeHeight() / 2.0, 0);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(aEye, 1.5, 20),
                ParticleSpray.cloud(bEye, 1.5, 20),
                ParticleSpray.burst(aEye, 1.5, 40)
        );

        return new SpellAction.Result(new EntitySpell(aItem, bItem, result), COST, particles, 0);
    }

    // ---- Depot + Depot (Vec3 + Vec3) ----
    private SpellAction.Result executeDepotDepot(List<? extends Iota> args, CastingEnvironment env) {
        Vec3 vecA;
        Vec3 vecB;
        try {
            vecA = getVec3(args, 0, getArgc());
            vecB = getVec3(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        try {
            env.assertVecInRange(vecA);
            env.assertVecInRange(vecB);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        if (!env.isVecInWorld(vecA) || !env.isVecInWorld(vecB)) {
            sneakyThrow(new OvidMishap("Блок вне мира"));
        }
        try {
            env.assertVecInWorld(vecA);
            env.assertVecInWorld(vecB);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        ServerLevel world = env.getWorld();
        BlockPos posA = BlockPos.containing(vecA);
        BlockPos posB = BlockPos.containing(vecB);

        if (posA.equals(posB)) {
            sneakyThrow(new OvidMishap("Нельзя использовать одно и то же депо"));
        }

        if (!DepotHelper.isDepot(world, posA)) {
            sneakyThrow(new OvidMishap("Требуется депо create:depot на позиции A: " + posA.toShortString()));
        }
        if (!DepotHelper.isDepot(world, posB)) {
            sneakyThrow(new OvidMishap("Требуется депо create:depot на позиции B: " + posB.toShortString()));
        }

        ItemStack stackA = DepotHelper.getStack(world, posA);
        ItemStack stackB = DepotHelper.getStack(world, posB);

        if (stackA == null || stackA.isEmpty() || stackB == null || stackB.isEmpty()) {
            sneakyThrow(new OvidMishap("Пустой предмет в депо"));
        }

        if (stackA.getCount() != stackB.getCount()) {
            sneakyThrow(new OvidMishap("Неравное количество"));
        }

        ItemStack resultTemplate = OvidRecipeRegistry.get(stackA, stackB);
        if (resultTemplate == null || resultTemplate.isEmpty()) {
            sneakyThrow(new OvidMishap("Нет рецепта для " + stackA.getItem() + " + " + stackB.getItem()));
        }

        int inputCount = stackA.getCount();
        ItemStack result = resultTemplate.copy();
        int templateCount = resultTemplate.getCount();
        int outCount = templateCount * inputCount;
        result.setCount(outCount);

        Vec3 centerA = Vec3.atCenterOf(posA);
        Vec3 centerB = Vec3.atCenterOf(posB);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(centerA, 1.5, 20),
                ParticleSpray.cloud(centerB, 1.5, 20),
                ParticleSpray.burst(centerA, 1.5, 40)
        );

        return new SpellAction.Result(new DepotSpell(posA, posB, result), COST, particles, 0);
    }

    // ---- Mixed: one Vec3 (depot) + one Entity ----
    private SpellAction.Result executeMixed(List<? extends Iota> args, CastingEnvironment env, boolean aIsVec) {
        Vec3 vecDepot;
        Entity ent;
        boolean depotIsA;
        try {
            if (aIsVec) {
                vecDepot = getVec3(args, 0, getArgc());
                ent = getEntity(args, 1, getArgc());
                depotIsA = true;
            } else {
                ent = getEntity(args, 0, getArgc());
                vecDepot = getVec3(args, 1, getArgc());
                depotIsA = false;
            }
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        if (!(ent instanceof ItemEntity itemEnt)) {
            sneakyThrow(new OvidMishap("Требуется сущность предмета для второго слота"));
            return null;
        }

        try {
            env.assertVecInRange(vecDepot);
            env.assertEntityInRange(ent);
            env.assertVecInWorld(vecDepot);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
        if (!env.isVecInWorld(vecDepot)) {
            sneakyThrow(new OvidMishap("Блок вне мира"));
        }

        ServerLevel world = env.getWorld();
        BlockPos depotPos = BlockPos.containing(vecDepot);

        if (!DepotHelper.isDepot(world, depotPos)) {
            sneakyThrow(new OvidMishap("Требуется депо create:depot на позиции " + depotPos.toShortString()));
        }

        ItemStack depotStack = DepotHelper.getStack(world, depotPos);
        ItemStack entStack = itemEnt.getItem();

        if (depotStack == null || depotStack.isEmpty() || entStack.isEmpty()) {
            sneakyThrow(new OvidMishap("Пустой предмет"));
        }

        // A is top of stack (index 0) -> transformed. Need to respect order for recipe.
        ItemStack stackA;
        ItemStack stackB;
        if (depotIsA) {
            stackA = depotStack;
            stackB = entStack;
        } else {
            stackA = entStack;
            stackB = depotStack;
        }

        if (stackA.getCount() != stackB.getCount()) {
            sneakyThrow(new OvidMishap("Неравное количество"));
        }

        ItemStack resultTemplate = OvidRecipeRegistry.get(stackA, stackB);
        if (resultTemplate == null || resultTemplate.isEmpty()) {
            sneakyThrow(new OvidMishap("Нет рецепта для " + stackA.getItem() + " + " + stackB.getItem()));
        }

        int inputCount = stackA.getCount();
        ItemStack result = resultTemplate.copy();
        int outCount = resultTemplate.getCount() * inputCount;
        result.setCount(outCount);

        Vec3 depotCenter = Vec3.atCenterOf(depotPos);
        Vec3 entPos = ent.position();
        Vec3 aEye = depotIsA ? depotCenter : entPos.add(0, ent.getEyeHeight() / 2.0, 0);
        Vec3 bEye = depotIsA ? entPos.add(0, ent.getEyeHeight() / 2.0, 0) : depotCenter;

        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(aEye, 1.5, 20),
                ParticleSpray.cloud(bEye, 1.5, 20),
                ParticleSpray.burst(aEye, 1.5, 40)
        );

        if (depotIsA) {
            // A=depot, B=entity
            return new SpellAction.Result(new MixedDepotEntitySpell(depotPos, itemEnt, result, true), COST, particles, 0);
        } else {
            // A=entity, B=depot
            return new SpellAction.Result(new MixedDepotEntitySpell(depotPos, itemEnt, result, false), COST, particles, 0);
        }
    }

    // ---- Spells ----

    public static class EntitySpell implements RenderedSpell {
        private final ItemEntity a;
        private final ItemEntity b;
        private final ItemStack result;

        public EntitySpell(ItemEntity a, ItemEntity b, ItemStack result) {
            this.a = a;
            this.b = b;
            this.result = result.copy();
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            if (a.isRemoved() || b.isRemoved()) return;
            Vec3 posA = a.position();
            a.discard();
            b.discard();
            int remaining = result.getCount();
            int max = result.getMaxStackSize();
            while (remaining > 0) {
                int c = Math.min(remaining, max);
                ItemStack part = result.copy();
                part.setCount(c);
                ItemEntity out = new ItemEntity(world, posA.x, posA.y, posA.z, part);
                out.setDeltaMovement(world.random.nextGaussian() * 0.02, 0.1, world.random.nextGaussian() * 0.02);
                world.addFreshEntity(out);
                remaining -= c;
            }
        }
    }

    public static class DepotSpell implements RenderedSpell {
        private final BlockPos aPos;
        private final BlockPos bPos;
        private final ItemStack result;

        public DepotSpell(BlockPos aPos, BlockPos bPos, ItemStack result) {
            this.aPos = aPos.immutable();
            this.bPos = bPos.immutable();
            this.result = result.copy();
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            // Clear B first to avoid any item duplication glitches if A and B somehow reference same handler
            DepotHelper.clearStack(world, bPos);

            // Set A to result, handle overflow by spawning extra entities
            int total = result.getCount();
            int max = result.getMaxStackSize();
            if (total <= max) {
                DepotHelper.setStack(world, aPos, result.copy());
            } else {
                ItemStack main = result.copy();
                main.setCount(max);
                DepotHelper.setStack(world, aPos, main);
                int remaining = total - max;
                Vec3 center = Vec3.atCenterOf(aPos).add(0, 0.5, 0);
                while (remaining > 0) {
                    int c = Math.min(remaining, max);
                    ItemStack part = result.copy();
                    part.setCount(c);
                    ItemEntity out = new ItemEntity(world, center.x, center.y, center.z, part);
                    out.setDeltaMovement(world.random.nextGaussian() * 0.02, 0.1, world.random.nextGaussian() * 0.02);
                    world.addFreshEntity(out);
                    remaining -= c;
                }
            }
        }
    }

    public static class MixedDepotEntitySpell implements RenderedSpell {
        private final BlockPos depotPos;
        private final ItemEntity entity;
        private final ItemStack result;
        private final boolean depotIsA; // true if depot is A (transformed), false if depot is B (sacrificed)

        public MixedDepotEntitySpell(BlockPos depotPos, ItemEntity entity, ItemStack result, boolean depotIsA) {
            this.depotPos = depotPos.immutable();
            this.entity = entity;
            this.result = result.copy();
            this.depotIsA = depotIsA;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            if (depotIsA) {
                // Depot A -> result, Entity B -> discard
                if (!entity.isRemoved()) entity.discard();
                int total = result.getCount();
                int max = result.getMaxStackSize();
                if (total <= max) {
                    DepotHelper.setStack(world, depotPos, result.copy());
                } else {
                    ItemStack main = result.copy();
                    main.setCount(max);
                    DepotHelper.setStack(world, depotPos, main);
                    int remaining = total - max;
                    Vec3 center = Vec3.atCenterOf(depotPos).add(0, 0.5, 0);
                    while (remaining > 0) {
                        int c = Math.min(remaining, max);
                        ItemStack part = result.copy();
                        part.setCount(c);
                        ItemEntity out = new ItemEntity(world, center.x, center.y, center.z, part);
                        out.setDeltaMovement(world.random.nextGaussian() * 0.02, 0.1, world.random.nextGaussian() * 0.02);
                        world.addFreshEntity(out);
                        remaining -= c;
                    }
                }
            } else {
                // Entity A -> result entity, Depot B -> clear
                Vec3 posA = entity.position();
                if (!entity.isRemoved()) entity.discard();
                DepotHelper.clearStack(world, depotPos);
                int remaining = result.getCount();
                int max = result.getMaxStackSize();
                while (remaining > 0) {
                    int c = Math.min(remaining, max);
                    ItemStack part = result.copy();
                    part.setCount(c);
                    ItemEntity out = new ItemEntity(world, posA.x, posA.y, posA.z, part);
                    out.setDeltaMovement(world.random.nextGaussian() * 0.02, 0.1, world.random.nextGaussian() * 0.02);
                    world.addFreshEntity(out);
                    remaining -= c;
                }
            }
        }
    }
}
