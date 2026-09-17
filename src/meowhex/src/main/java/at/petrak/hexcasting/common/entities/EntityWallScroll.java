package at.petrak.hexcasting.common.entities;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.client.render.HexPatternPoints;
import at.petrak.hexcasting.client.render.RenderLib;
import at.petrak.hexcasting.common.items.storage.ItemScroll;
import at.petrak.hexcasting.common.lib.HexItems;
import at.petrak.hexcasting.common.lib.HexSounds;
import at.petrak.hexcasting.common.msgs.MsgNewWallScrollS2C;
import at.petrak.hexcasting.common.msgs.MsgRecalcWallScrollDisplayS2C;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EntityWallScroll extends HangingEntity {
    private static final EntityDataAccessor<Boolean> SHOWS_STROKE_ORDER = SynchedEntityData.defineId(
        EntityWallScroll.class,
        EntityDataSerializers.BOOLEAN);

    public ItemStack scroll;
    @Nullable
    public HexPattern pattern;
    public boolean isAncient;
    public int blockSize;
    // Client-side only!
    @Nullable
    public HexPatternPoints points;
    // Client-side only! True when point computation failed — renderer won't retry every frame.
    public boolean pointsFailed;

    public EntityWallScroll(EntityType<? extends EntityWallScroll> type, Level world) {
        super(type, world);
    }

    public EntityWallScroll(Level world, BlockPos pos, Direction dir, ItemStack scroll, boolean showStrokeOrder,
        int blockSize) {
        super(HexEntities.WALL_SCROLL, world, pos);
        this.setDirection(dir);
        this.blockSize = blockSize;

        this.entityData.set(SHOWS_STROKE_ORDER, showStrokeOrder);
        this.scroll = scroll;
        this.recalculateDisplay();
        this.recalculateBoundingBox();
    }

    public void recalculateDisplay() {
        CompoundTag patternTag = NBTHelper.getCompound(scroll, ItemScroll.TAG_PATTERN);
        if (patternTag != null) {
            this.pattern = HexPattern.fromNBT(patternTag);
            this.isAncient = NBTHelper.hasString(scroll, ItemScroll.TAG_OP_ID);
            if (this.level().isClientSide) {
                this.computePoints();
            }
        } else {
            this.pattern = null;
            this.points = null;
            this.pointsFailed = false;
            this.isAncient = false;
        }
    }

    /**
     * Client-side only! (Re)builds the render points from {@link #pattern}.
     * Never throws: on failure keeps {@code points} null and sets {@link #pointsFailed},
     * so the renderer won't spam retries every frame (a null {@code points} with a
     * non-null {@code pattern} then visibly means "points failed" in the log).
     */
    public void computePoints() {
        this.points = null;
        this.pointsFailed = false;
        if (this.pattern == null) {
            return;
        }
        try {
            var pair = RenderLib.getCenteredPattern(pattern, 128f / 3 * blockSize, 128f / 3 * blockSize,
                16f / 3 * blockSize);
            var dots = pair.getSecond();
            var readOffset = this.getShowsStrokeOrder() ? RenderLib.DEFAULT_READABILITY_OFFSET : 0f;
            var lastProp = this.getShowsStrokeOrder() ? RenderLib.DEFAULT_LAST_SEGMENT_LEN_PROP : 1f;
            var zappyPoints = RenderLib.makeZappy(dots, RenderLib.findDupIndices(pattern.positions()), 10, 0.4f,
                0f, 0f, readOffset, lastProp, this.getId());
            this.points = new HexPatternPoints(zappyPoints);
        } catch (Throwable t) {
            this.pointsFailed = true;
            HexAPI.LOGGER.warn("Failed to compute wall scroll display points for {}", this.scroll, t);
        }
    }

    /** Client-side only! Rebuilds points if they were missed (e.g. failed earlier). */
    public void ensurePoints() {
        if (this.points == null && this.pattern != null && !this.pointsFailed) {
            this.computePoints();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SHOWS_STROKE_ORDER, false);
    }

    public boolean getShowsStrokeOrder() {
        return this.entityData.get(SHOWS_STROKE_ORDER);
    }

    public void setShowsStrokeOrder(boolean b) {
        this.entityData.set(SHOWS_STROKE_ORDER, b);
    }

    public int getWidth() {
        return 16 * blockSize;
    }

    public int getHeight() {
        return 16 * blockSize;
    }

    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        double width = this.getWidth() / 16.0;
        double height = this.getHeight() / 16.0;
        Vec3 center = Vec3.atCenterOf(pos).relative(direction, -0.46875);
        Direction sideways = direction.getCounterClockWise();
        center = center.relative(sideways, this.offsetForSize(this.getWidth()))
            .relative(Direction.UP, this.offsetForSize(this.getHeight()));
        Direction.Axis axis = direction.getAxis();
        double xSize = axis == Direction.Axis.X ? 0.0625 : width;
        double zSize = axis == Direction.Axis.Z ? 0.0625 : width;
        return AABB.ofSize(center, xSize, height, zSize);
    }

    private double offsetForSize(int size) {
        return size % 32 == 0 ? 0.5 : 0.0;
    }

    @Override
    public void dropItem(@Nullable Entity pBrokenEntity) {
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (pBrokenEntity instanceof Player player) {
                if (player.getAbilities().instabuild) {
                    return;
                }
            }

            this.spawnAtLocation(this.scroll);
        }
    }

    @Override
    public InteractionResult interactAt(Player pPlayer, Vec3 pVec, InteractionHand pHand) {
        var handStack = pPlayer.getItemInHand(pHand);
        if (handStack.is(HexItems.AMETHYST_DUST) && !this.getShowsStrokeOrder()) {
            if (!pPlayer.getAbilities().instabuild) {
                handStack.shrink(1);
            }
            this.setShowsStrokeOrder(true);

            pPlayer.level().playSound(pPlayer, this, HexSounds.SCROLL_DUST, SoundSource.PLAYERS, 1f, 1f);

            if (pPlayer.level() instanceof ServerLevel slevel) {
                IXplatAbstractions.INSTANCE.sendPacketNear(this.position(), 32.0, slevel,
                    new MsgRecalcWallScrollDisplayS2C(this.getId(), true));
            } else {
                // Beat the packet roundtrip to the punch to get a quicker visual
                this.recalculateDisplay();
            }
            return InteractionResult.SUCCESS;
        }
        return super.interactAt(pPlayer, pVec, pHand);
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return IXplatAbstractions.INSTANCE.toVanillaClientboundPacket(
            new MsgNewWallScrollS2C(new ClientboundAddEntityPacket(this, serverEntity),
                pos, direction, scroll, getShowsStrokeOrder(), blockSize));
    }

    public void readSpawnData(BlockPos pos, Direction dir, ItemStack scrollItem,
        boolean showsStrokeOrder, int blockSize) {
        this.pos = pos;
        this.scroll = scrollItem;
        this.blockSize = blockSize;

        this.setDirection(dir);
        this.setShowsStrokeOrder(showsStrokeOrder);

        this.recalculateDisplay();
        this.recalculateBoundingBox();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putByte("direction", (byte) this.direction.ordinal());
        tag.put("scroll", this.scroll.save(this.registryAccess()));
        tag.putBoolean("showsStrokeOrder", this.getShowsStrokeOrder());
        tag.putInt("blockSize", this.blockSize);
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.direction = Direction.values()[tag.getByte("direction")];
        this.scroll = ItemStack.parse(this.registryAccess(), tag.getCompound("scroll")).orElse(ItemStack.EMPTY);
        this.blockSize = tag.getInt("blockSize");

        this.setDirection(this.direction);
        this.setShowsStrokeOrder(tag.getBoolean("showsStrokeOrder"));

        this.recalculateDisplay();
        this.recalculateBoundingBox();

        super.readAdditionalSaveData(tag);
    }

    @Override
    public void moveTo(double pX, double pY, double pZ, float pYaw, float pPitch) {
        this.setPos(pX, pY, pZ);
    }

    @Override
    public void lerpTo(double pX, double pY, double pZ, float pYaw, float pPitch, int pPosRotationIncrements) {
        BlockPos blockpos = this.pos.offset((int) (pX - this.getX()), (int) (pY - this.getY()), (int) (pZ - this.getZ()));
        this.setPos(blockpos.getX(), blockpos.getY(), blockpos.getZ());
    }

    public ItemStack getPickResult() {
        return this.scroll.copy();
    }
}
