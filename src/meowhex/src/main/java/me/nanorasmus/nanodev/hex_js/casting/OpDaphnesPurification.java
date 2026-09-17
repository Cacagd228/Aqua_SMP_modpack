package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getVec3;

/**
 * Daphne's Purification — рандомная трансмутация саженцев/цветов.
 * Стек: [Vec3] позиция блока. Блок должен быть саженцем или цветком (кроме визер розы).
 * Если саженец — меняет на случайный другой саженец, если цветок — на случайный другой цветок (без визер розы).
 * Стоимость 20 пыли.
 */
public class OpDaphnesPurification implements SpellAction {

    public static final OpDaphnesPurification INSTANCE = new OpDaphnesPurification();
    private static final long COST = 20L * 10000L;

    private OpDaphnesPurification() {}

    @Override
    public int getArgc() {
        return 1;
    }

    @Override
    public boolean hasCastingSound(CastingEnvironment env) { return true; }

    @Override
    public boolean awardsCastingStat(CastingEnvironment env) { return true; }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) {
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T { throw (T) t; }

    private static final List<Block> SAPLINGS = new ArrayList<>();
    private static final List<Block> FLOWERS = new ArrayList<>();

    static {
        // Build sapling list via tag + instanceof
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState def = block.defaultBlockState();
            if (def.is(BlockTags.SAPLINGS) || block instanceof SaplingBlock) {
                SAPLINGS.add(block);
            }
        }
        // Fallback if tag empty (should not)
        if (SAPLINGS.isEmpty()) {
            SAPLINGS.addAll(List.of(
                    Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING,
                    Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING,
                    Blocks.MANGROVE_PROPAGULE, Blocks.CHERRY_SAPLING,
                    Blocks.BAMBOO_SAPLING, Blocks.AZALEA, Blocks.FLOWERING_AZALEA
            ));
        }
        // Flowers: small flowers tag + flowers tag, exclude wither rose
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState def = block.defaultBlockState();
            boolean isFlower = def.is(BlockTags.FLOWERS) || def.is(BlockTags.SMALL_FLOWERS);
            // Fallback: check vanilla flower blocks via registry name
            if (isFlower && !block.equals(Blocks.WITHER_ROSE) && !block.equals(Blocks.POTTED_WITHER_ROSE)) {
                FLOWERS.add(block);
            }
        }
        if (FLOWERS.isEmpty()) {
            FLOWERS.addAll(List.of(
                    Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM,
                    Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP,
                    Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.TORCHFLOWER, Blocks.PITCHER_PLANT
            ));
        }
        // Ensure wither rose excluded
        FLOWERS.remove(Blocks.WITHER_ROSE);
        FLOWERS.remove(Blocks.POTTED_WITHER_ROSE);
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        Vec3 vec;
        try {
            vec = getVec3(args, 0, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        try {
            env.assertVecInRange(vec);
        } catch (Throwable t) { sneakyThrow(t); }

        if (!env.isVecInWorld(vec)) {
            sneakyThrow(new OvidMishap("Блок вне мира"));
        }
        try {
            env.assertVecInWorld(vec);
        } catch (Throwable t) { sneakyThrow(t); }

        ServerLevel world = env.getWorld();
        BlockPos pos = BlockPos.containing(vec);
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Wither rose cannot be source
        if (block.equals(Blocks.WITHER_ROSE) || block.equals(Blocks.POTTED_WITHER_ROSE)) {
            sneakyThrow(new OvidMishap("Визер роза не поддаётся очищению"));
        }

        boolean isSapling = state.is(BlockTags.SAPLINGS) || block instanceof SaplingBlock || SAPLINGS.contains(block);
        boolean isFlower = (state.is(BlockTags.FLOWERS) || state.is(BlockTags.SMALL_FLOWERS) || FLOWERS.contains(block))
                && !block.equals(Blocks.WITHER_ROSE);

        List<Block> pool;
        if (isSapling) {
            pool = SAPLINGS;
        } else if (isFlower) {
            pool = FLOWERS;
        } else {
            sneakyThrow(new OvidMishap("Блок не саженец и не цветок"));
            return null;
        }

        // Filter out current block
        List<Block> candidates = new ArrayList<>();
        for (Block b : pool) {
            if (!b.equals(block)) candidates.add(b);
        }
        if (candidates.isEmpty()) {
            sneakyThrow(new OvidMishap("Нет вариантов для превращения"));
        }

        Block target = candidates.get(world.random.nextInt(candidates.size()));
        BlockState newState = target.defaultBlockState();

        Vec3 center = Vec3.atCenterOf(pos);
        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(center, 1.2, 20),
                ParticleSpray.burst(center, 1.2, 40)
        );

        return new SpellAction.Result(new Spell(pos, newState), COST, particles, 0);
    }

    public static class Spell implements RenderedSpell {
        private final BlockPos pos;
        private final BlockState newState;

        public Spell(BlockPos pos, BlockState newState) {
            this.pos = pos;
            this.newState = newState;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            // Ensure still valid? Just set
            world.setBlockAndUpdate(pos, newState);
        }
    }
}
