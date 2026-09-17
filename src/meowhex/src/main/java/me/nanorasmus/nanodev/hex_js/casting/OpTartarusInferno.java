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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Tartarus's Inferno — печная переплавка.
 * Стек: [A] где A — либо Vec3 на create:depot, либо EntityIota на ItemEntity.
 * Если предмет имеет печной рецепт (SMELTING), он переплавляется в результат.
 * Стоимость 2.5 пыли за каждый предмет (2.5 * 10000 * count). Если нельзя переплавить — mishap.
 */
public class OpTartarusInferno implements SpellAction {

    public static final OpTartarusInferno INSTANCE = new OpTartarusInferno();

    private static final long COST_PER = 25000L; // 2.5 dust per item (10000 = 1 dust)

    private OpTartarusInferno() {}

    @Override
    public int getArgc() {
        return 1;
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
        return SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        if (args.isEmpty()) {
            sneakyThrow(new OvidMishap("Требуется предмет или депо"));
        }
        Iota a = args.get(0);
        if (a instanceof EntityIota) {
            return executeEntity(args, env);
        } else if (a instanceof Vec3Iota) {
            return executeDepot(args, env);
        } else {
            sneakyThrow(new OvidMishap("Требуется сущность предмета или вектор депо (create:depot)"));
            return null;
        }
    }

    private SpellAction.Result executeEntity(List<? extends Iota> args, CastingEnvironment env) {
        ItemEntity itemEnt;
        try {
            var ent = at.petrak.hexcasting.api.casting.OperatorUtils.getEntity(args, 0, getArgc());
            if (!(ent instanceof ItemEntity)) {
                sneakyThrow(new OvidMishap("Требуется сущность предмета"));
            }
            itemEnt = (ItemEntity) ent;
            env.assertEntityInRange(itemEnt);
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        ItemStack stack = itemEnt.getItem();
        if (stack.isEmpty()) {
            sneakyThrow(new OvidMishap("Пустой предмет"));
        }

        ServerLevel world = env.getWorld();
        ItemStack smelted = getSmeltResult(world, stack);
        if (smelted == null || smelted.isEmpty()) {
            sneakyThrow(new OvidMishap("Предмет нельзя переплавить"));
        }

        int count = stack.getCount();
        long cost = COST_PER * count;

        // Scale result by input count (furnace typically 1 -> N)
        ItemStack result = smelted.copy();
        int outPer = smelted.getCount(); // usually 1
        int totalOut = outPer * count;
        // If recipe output has different count, scale accordingly; but for furnace it's 1.
        result.setCount(totalOut);

        Vec3 pos = itemEnt.position();
        try {
            env.assertVecInRange(pos);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
        Vec3 eye = pos.add(0, itemEnt.getEyeHeight() / 2.0, 0);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(eye, 1.2, 20),
                ParticleSpray.burst(eye, 1.5, 30)
        );

        return new SpellAction.Result(new EntitySmeltSpell(itemEnt, result), cost, particles, 0);
    }

    private SpellAction.Result executeDepot(List<? extends Iota> args, CastingEnvironment env) {
        Vec3 vec;
        try {
            vec = at.petrak.hexcasting.api.casting.OperatorUtils.getVec3(args, 0, getArgc());
            env.assertVecInRange(vec);
            env.assertVecInWorld(vec);
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }
        if (!env.isVecInWorld(vec)) {
            sneakyThrow(new OvidMishap("Блок вне мира"));
        }

        ServerLevel world = env.getWorld();
        BlockPos pos = BlockPos.containing(vec);

        if (!DepotHelper.isDepot(world, pos)) {
            sneakyThrow(new OvidMishap("Требуется депо create:depot на " + pos.toShortString()));
        }

        ItemStack stack = DepotHelper.getStack(world, pos);
        if (stack == null || stack.isEmpty()) {
            sneakyThrow(new OvidMishap("Пустой предмет в депо"));
        }

        ItemStack smelted = getSmeltResult(world, stack);
        if (smelted == null || smelted.isEmpty()) {
            sneakyThrow(new OvidMishap("Предмет нельзя переплавить"));
        }

        int count = stack.getCount();
        long cost = COST_PER * count;

        ItemStack result = smelted.copy();
        int outPer = smelted.getCount();
        int totalOut = outPer * count;
        result.setCount(totalOut);

        Vec3 center = Vec3.atCenterOf(pos);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(center, 1.2, 20),
                ParticleSpray.burst(center, 1.5, 30)
        );

        return new SpellAction.Result(new DepotSmeltSpell(pos, result), cost, particles, 0);
    }

    @org.jetbrains.annotations.Nullable
    private static ItemStack getSmeltResult(ServerLevel level, ItemStack input) {
        try {
            // Use SingleRecipeInput for furnace
            SingleRecipeInput single = new SingleRecipeInput(input.copyWithCount(1));
            Optional<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe>> opt =
                    level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, single, level);
            if (opt.isEmpty()) return null;
            var holder = opt.get();
            // getResultItem or assemble — try both for version compat
            try {
                // 1.21.1: getResultItem(RegistryAccess)
                var res = holder.value().getResultItem(level.registryAccess());
                if (res != null && !res.isEmpty()) return res.copy();
            } catch (Throwable ignored) {}
            try {
                var res2 = holder.value().assemble(single, level.registryAccess());
                if (res2 != null && !res2.isEmpty()) return res2.copy();
            } catch (Throwable ignored) {}
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static class EntitySmeltSpell implements RenderedSpell {
        private final ItemEntity entity;
        private final ItemStack result;

        public EntitySmeltSpell(ItemEntity entity, ItemStack result) {
            this.entity = entity;
            this.result = result.copy();
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            if (entity.isRemoved()) return;
            Vec3 pos = entity.position();
            entity.discard();
            int remaining = result.getCount();
            int max = result.getMaxStackSize();
            while (remaining > 0) {
                int c = Math.min(remaining, max);
                ItemStack part = result.copy();
                part.setCount(c);
                ItemEntity out = new ItemEntity(world, pos.x, pos.y, pos.z, part);
                out.setDeltaMovement(world.random.nextGaussian() * 0.02, 0.1, world.random.nextGaussian() * 0.02);
                world.addFreshEntity(out);
                remaining -= c;
            }
        }
    }

    public static class DepotSmeltSpell implements RenderedSpell {
        private final BlockPos pos;
        private final ItemStack result;

        public DepotSmeltSpell(BlockPos pos, ItemStack result) {
            this.pos = pos.immutable();
            this.result = result.copy();
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            int total = result.getCount();
            int max = result.getMaxStackSize();
            if (total <= max) {
                DepotHelper.setStack(world, pos, result.copy());
            } else {
                ItemStack main = result.copy();
                main.setCount(max);
                DepotHelper.setStack(world, pos, main);
                int remaining = total - max;
                Vec3 center = Vec3.atCenterOf(pos).add(0, 0.5, 0);
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
}
