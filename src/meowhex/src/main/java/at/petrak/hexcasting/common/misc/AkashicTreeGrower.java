package at.petrak.hexcasting.common.misc;

import at.petrak.hexcasting.common.lib.HexConfiguredFeatures;
import com.google.common.collect.Lists;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.List;

public class AkashicTreeGrower {
    public static final AkashicTreeGrower INSTANCE = new AkashicTreeGrower();

    public static final List<ResourceKey<ConfiguredFeature<?, ?>>> GROWERS = Lists.newArrayList();

    public static void init() {
        GROWERS.add(HexConfiguredFeatures.AMETHYST_EDIFIED_TREE);
        GROWERS.add(HexConfiguredFeatures.AVENTURINE_EDIFIED_TREE);
        GROWERS.add(HexConfiguredFeatures.CITRINE_EDIFIED_TREE);
    }

    private ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource pRandom) {
        return GROWERS.get(pRandom.nextInt(GROWERS.size()));
    }

    public boolean growTree(ServerLevel level, ChunkGenerator generator, BlockPos pos, BlockState state, RandomSource random) {
        var holder = level.registryAccess()
            .registryOrThrow(Registries.CONFIGURED_FEATURE)
            .getHolder(getConfiguredFeature(random))
            .orElse(null);
        if (holder == null) {
            return false;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 4);
        if (holder.value().place(level, generator, random, pos)) {
            return true;
        }

        level.setBlock(pos, state, 4);
        return false;
    }
}
