package com.meowaddons.tier;
import com.meowaddons.ModBlocks;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TieredCrushingWheelBlock extends CrushingWheelBlock {
 private final Tier tier;
 public TieredCrushingWheelBlock(BlockBehaviour.Properties props, Tier tier){ super(props); this.tier=tier; }
 public Tier getTier(){ return tier; }
 @Override public Class<CrushingWheelBlockEntity> getBlockEntityClass(){ return (Class)TieredCrushingWheelBlockEntity.class; }
 @Override public BlockEntityType<? extends CrushingWheelBlockEntity> getBlockEntityType(){ return TieredCrushingWheelBlockEntity.getType(tier); }

 // Ванильный BE крутится через тикер из registrate; наш тип собран Builder.of(...).build(null),
 // тикера нет -> lazyTick()->fixControllers() не бегал и контроллер не жил/не починялся.
 @Nullable
 @Override
 @SuppressWarnings("unchecked")
 public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
  if (type == getBlockEntityType())
   return (BlockEntityTicker<T>) new SmartBlockEntityTicker<CrushingWheelBlockEntity>();
  return null;
 }

 // Без совместимости с ванильными: только same-tier пары, ваниль не видит тированные
 @Override
 public void updateControllers(BlockState state, Level level, BlockPos pos, Direction dir) {
  if (dir.getAxis() == state.getValue(AXIS)) return;
  if (level == null) return;
  BlockPos center = pos.relative(dir);
  BlockPos otherPos = pos.relative(dir, 2);
  BlockState otherState = level.getBlockState(otherPos);
  boolean isOtherWheel = otherState.getBlock() instanceof CrushingWheelBlock;
  if (isOtherWheel) {
   boolean thisTiered = this instanceof TieredCrushingWheelBlock;
   boolean otherTiered = otherState.getBlock() instanceof TieredCrushingWheelBlock;
   if (thisTiered && otherTiered) {
    Tier otherTier = ((TieredCrushingWheelBlock) otherState.getBlock()).getTier();
    if (otherTier != this.tier) isOtherWheel = false;
   } else if (thisTiered || otherTiered) {
    isOtherWheel = false;
   }
  }
  // ось колёс должна совпадать и быть перпендикулярна dir
  if (isOtherWheel && otherState.getValue(AXIS) != state.getValue(AXIS)) isOtherWheel = false;
  BlockState centerState = level.getBlockState(center);
  boolean hasController = centerState.getBlock() instanceof CrushingWheelControllerBlock;
  // isValid зависит только от наличия пары и оси, а не от скорости — как в ванили при установке без питания
  boolean isValid = isOtherWheel;
  Direction facing = null;
  if (isOtherWheel) {
   // вычисляем facing как в ванили (cross product), но без проверки скорости — скорость учтётся в updateSpeed
   Direction.Axis wheelAxis = state.getValue(AXIS);
   Direction.Axis dirAxis = dir.getAxis();
   Vec3 wheelVec = new Vec3(wheelAxis == Direction.Axis.X ? 1 : 0, wheelAxis == Direction.Axis.Y ? 1 : 0, wheelAxis == Direction.Axis.Z ? 1 : 0);
   Vec3 dirVec = new Vec3(dirAxis == Direction.Axis.X ? 1 : 0, dirAxis == Direction.Axis.Y ? 1 : 0, dirAxis == Direction.Axis.Z ? 1 : 0);
   Vec3 cross = wheelVec.cross(dirVec);
   // если есть скорость — используем знак для facing, иначе дефолт
   var thisBE = level.getBlockEntity(pos) instanceof CrushingWheelBlockEntity be ? be : null;
   float s1 = thisBE != null ? thisBE.getSpeed() : 0;
   int step = s1 != 0 ? (int) Math.signum(s1) * dir.getAxisDirection().getStep() : dir.getAxisDirection().getStep();
   facing = Direction.getNearest(cross.x * step, cross.y * step, cross.z * step);
   if (facing == null) facing = Direction.UP;
  }
  if (!isOtherWheel) {
   if (hasController) {
    level.setBlockAndUpdate(center, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
   }
   return;
  }
  if (hasController && !(centerState.getBlock() instanceof TieredCrushingWheelControllerBlock)) {
   level.setBlockAndUpdate(center, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
   hasController = false;
   centerState = level.getBlockState(center);
  }
  if (!hasController) {
   if (!centerState.canBeReplaced()) return;
   BlockState controllerState = ModBlocks.TIERED_CRUSHING_WHEEL_CONTROLLER.get().defaultBlockState()
     .setValue(CrushingWheelControllerBlock.VALID, isValid)
     .setValue(CrushingWheelControllerBlock.FACING, facing != null ? facing : Direction.UP);
   level.setBlockAndUpdate(center, controllerState);
   if (level.getBlockState(center).getBlock() instanceof CrushingWheelControllerBlock cb) {
    cb.updateSpeed(level.getBlockState(center), level, center);
   }
   return;
  }
  boolean curValid = centerState.getValue(CrushingWheelControllerBlock.VALID);
  Direction curFacing = centerState.getValue(CrushingWheelControllerBlock.FACING);
  if (curValid != isValid || (facing != null && curFacing != facing)) {
   BlockState newState = centerState.setValue(CrushingWheelControllerBlock.VALID, isValid);
   if (facing != null) newState = newState.setValue(CrushingWheelControllerBlock.FACING, facing);
   level.setBlockAndUpdate(center, newState);
   if (level.getBlockState(center).getBlock() instanceof CrushingWheelControllerBlock cb) {
    cb.updateSpeed(level.getBlockState(center), level, center);
   }
  } else {
   // даже если VALID/FACING не менялись, всё равно обновить скорость (важно для засасывания)
   if (level.getBlockState(center).getBlock() instanceof CrushingWheelControllerBlock cb) {
    cb.updateSpeed(level.getBlockState(center), level, center);
   }
  }
 }

 @Override
 public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
  for (Direction dir : Direction.values()) {
   BlockPos nPos = pos.relative(dir);
   BlockState nState = level.getBlockState(nPos);
   if (nState.getBlock() instanceof CrushingWheelControllerBlock) {
    if (dir.getAxis() == state.getValue(AXIS)) return false;
   }
   if (nState.getBlock() instanceof CrushingWheelBlock) {
    if (nState.getValue(AXIS) == state.getValue(AXIS) && dir.getAxis() == nState.getValue(AXIS)) return false;
   }
  }
  return true;
 }
}
