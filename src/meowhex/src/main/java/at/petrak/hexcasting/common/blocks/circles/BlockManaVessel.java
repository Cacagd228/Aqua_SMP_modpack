package at.petrak.hexcasting.common.blocks.circles;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import at.petrak.hexcasting.api.block.circle.BlockCircleComponent;
import at.petrak.hexcasting.api.casting.circles.ICircleComponent;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.common.lib.HexBlockEntities;
import at.petrak.hexcasting.common.lib.HexBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BlockManaVessel extends BlockCircleComponent implements EntityBlock, SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<AttachFace> ATTACH_FACE = BlockStateProperties.ATTACH_FACE;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public BlockManaVessel(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ATTACH_FACE, AttachFace.WALL)
            .setValue(WATERLOGGED, false)
            .setValue(ENERGIZED, false));
    }

    @Override
    public @Nullable net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityManaVessel(pos, state);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ATTACH_FACE, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // Tight to the jar model; on walls/ceiling the model rotates like a slate
        // (see blockstate), so shapes follow the rotated geometry. Support side
        // is FACING.getOpposite() — shapes reach toward it.
        return switch (state.getValue(ATTACH_FACE)) {
            case FLOOR -> net.minecraft.world.level.block.Block.box(4, 0, 4, 12, 13, 12);
            case CEILING -> net.minecraft.world.level.block.Block.box(4, 3, 4, 12, 16, 12);
            case WALL -> switch (state.getValue(FACING)) {
                case NORTH -> net.minecraft.world.level.block.Block.box(4, 3, 3, 12, 13, 16);
                case SOUTH -> net.minecraft.world.level.block.Block.box(4, 3, 0, 12, 13, 13);
                case WEST -> net.minecraft.world.level.block.Block.box(3, 3, 4, 16, 13, 12);
                case EAST -> net.minecraft.world.level.block.Block.box(0, 3, 4, 13, 13, 12);
                default -> net.minecraft.world.level.block.Block.box(4, 3, 4, 12, 13, 12);
            };
        };
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public Direction normalDir(BlockPos pos, BlockState bs, Level world, int recursionLeft) {
        return normalDir(pos, bs, world);
    }

    @Override
    public Direction normalDir(BlockPos pos, BlockState bs, Level world) {
        return switch (bs.getValue(ATTACH_FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> bs.getValue(FACING);
        };
    }

    @Override
    public float particleHeight(BlockPos pos, BlockState bs, Level world) {
        return 0.5f;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        // Floor only: walls/ceiling placement disabled (jar model is floor-built).
        // Wall/ceiling states stay valid for already placed vessels so they don't pop.
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockState state = this.defaultBlockState()
            .setValue(WATERLOGGED, fluid.is(Fluids.WATER) && fluid.getAmount() == 8)
            .setValue(ATTACH_FACE, AttachFace.FLOOR)
            .setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        if (state.canSurvive(ctx.getLevel(), ctx.getClickedPos())) {
            return state;
        }
        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return canAttach(level, pos, getConnectedDirection(state).getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, net.minecraft.world.level.LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return getConnectedDirection(state).getOpposite() == facing
            && !state.canSurvive(level, currentPos) ?
            state.getFluidState().createLegacyBlock() : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    public static boolean canAttach(net.minecraft.world.level.LevelReader level, BlockPos pos, Direction dir) {
        BlockPos blockpos = pos.relative(dir);
        return level.getBlockState(blockpos).isFaceSturdy(level, blockpos, dir.getOpposite());
    }

    protected static Direction getConnectedDirection(BlockState state) {
        return switch (state.getValue(ATTACH_FACE)) {
            case CEILING -> Direction.DOWN;
            case FLOOR -> Direction.UP;
            default -> state.getValue(FACING);
        };
    }

    // ICircleComponent — behave like a slate
    @Override
    public ICircleComponent.ControlFlow acceptControlFlow(CastingImage imageIn, CircleCastEnv env, Direction enterDir,
                                                 BlockPos pos, BlockState bs, ServerLevel world) {
        var be = world.getBlockEntity(pos);
        if (be instanceof BlockEntityManaVessel vessel) {
            env.circleState().setCurrentVessel(vessel);
        }
        EnumSet<Direction> exits = possibleExitDirections(pos, bs, world);
        exits.remove(enterDir.getOpposite());
        return new ICircleComponent.ControlFlow.Continue(imageIn, exits.stream()
            .map(dir -> exitPositionFromDirection(pos, dir)).toList());
    }

    @Override
    public boolean canEnterFromDirection(Direction enterDir, BlockPos pos, BlockState bs, ServerLevel world) {
        var normal = normalDir(pos, bs, world);
        return enterDir != normal.getOpposite();
    }

    @Override
    public EnumSet<Direction> possibleExitDirections(BlockPos pos, BlockState bs, Level world) {
        EnumSet<Direction> dirs = EnumSet.allOf(Direction.class);
        var normal = normalDir(pos, bs, world);
        dirs.remove(normal);
        return dirs;
    }

    public static class BlockEntityManaVessel extends HexBlockEntity {
        private static final String TAG_MEDIA = "stored_media";
        // Max 10000 player mana (1 mana = 1000 media, same units as OpChargeVessel and ManaHelper).
        public static final long CAPACITY = 10_000L * (long) at.petrak.hexcasting.api.misc.ManaHelper.MEDIA_PER_MANA;

        private long storedMedia = 0;

        public BlockEntityManaVessel(BlockPos pos, BlockState state) {
            super(HexBlockEntities.MANA_VESSEL_TILE, pos, state);
        }

        public long getStoredMedia() {
            return storedMedia;
        }

        public void setStoredMedia(long media) {
            this.storedMedia = Math.max(0, Math.min(media, CAPACITY));
            this.sync();
        }

        public long getRemainingCapacity() {
            return CAPACITY - storedMedia;
        }

        public boolean addMedia(long amount) {
            if (storedMedia >= CAPACITY || amount <= 0) return false;
            long before = storedMedia;
            storedMedia = Math.min(storedMedia + amount, CAPACITY);
            if (storedMedia != before) {
                this.sync();
                return true;
            }
            return false;
        }

        public long extractMedia(long amount, boolean simulate) {
            if (storedMedia <= 0 || amount <= 0) return 0;
            long canTake = Math.min(amount, storedMedia);
            if (!simulate) {
                storedMedia -= canTake;
                this.sync();
            }
            return canTake;
        }

        @Override
        protected void saveModData(CompoundTag tag) {
            tag.putLong(TAG_MEDIA, storedMedia);
        }

@Override
        protected void loadModData(CompoundTag tag) {
            if (tag.contains(TAG_MEDIA, Tag.TAG_LONG)) {
                // Clamp: capacity used to be 10x bigger (dust units), don't keep overfull vessels.
                storedMedia = Math.max(0, Math.min(tag.getLong(TAG_MEDIA), CAPACITY));
            } else {
                storedMedia = 0;
            }
        }

    public void applyScryingLensOverlay(java.util.List<com.mojang.datafixers.util.Pair<net.minecraft.world.item.ItemStack, net.minecraft.network.chat.Component>> lines,
        net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player observer, net.minecraft.world.level.Level world, net.minecraft.core.Direction hitFace) {
        // Show stored mana in player-mana units (1 mana = 1000 media),
        // same units as OpChargeVessel takes and ManaHelper uses.
        // Was DUST_UNIT (10000) — showed 10x less than deposited (500 -> 50).
        double manaCount = (double) storedMedia / at.petrak.hexcasting.api.misc.ManaHelper.MEDIA_PER_MANA;
        var manaCmp = net.minecraft.network.chat.Component.translatable("hexcasting.tooltip.mana_vessel.mana",
            new java.text.DecimalFormat("###,###.##").format(manaCount));
        lines.add(new com.mojang.datafixers.util.Pair<>(new net.minecraft.world.item.ItemStack(at.petrak.hexcasting.common.lib.HexItems.AMETHYST_DUST), manaCmp));
    }
}
}