package com.meowaddons.client.transmitter;

import com.meowaddons.MeowAddons;
import com.meowaddons.transmitter.TransmitterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

/**
 * Адаптация FrogportRenderer из Create (MIT): тело/голова/язык через
 * CachedBuffers, перед блоком открывается маленький портал в сторону FACING.
 */
public class TransmitterRenderer implements BlockEntityRenderer<TransmitterBlockEntity> {
    private static final PartialModel BODY = PartialModel.of(rl("block/interdimensional_transmitter/body"));
    private static final PartialModel HEAD = PartialModel.of(rl("block/interdimensional_transmitter/head"));
    private static final PartialModel TONGUE = PartialModel.of(rl("block/interdimensional_transmitter/tongue"));

    private static final ResourceLocation PORTAL_TEX =
            ResourceLocation.withDefaultNamespace("textures/block/nether_portal.png");
    private static final int PORTAL_FRAMES = 32;

    private static final float PIVOT_X = 8 / 16f;
    private static final float PIVOT_Y = 10 / 16f;
    private static final float PIVOT_Z = 11 / 16f;

    public TransmitterRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Вызывается из client setup: заставляет статик-инициализатор с PartialModel отработать до запекания моделей. */
    public static void init() {
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, path);
    }

    @Override
    public void render(TransmitterBlockEntity be, float partialTick, PoseStack ms,
                       MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;

        BlockState state = be.getBlockState();
        // Поворот из BlockEntity (16+ позиций, как у Create)
        float yaw = be.getPassiveYaw();
        // Направление взгляда кваки: yaw=180 (игрок с юга) -> взгляд на юг (+Z)
        float yawRad = yaw * ((float) Math.PI / 180f);
        Vec3 dir = new Vec3(-Mth.sin(yawRad), 0, -Mth.cos(yawRad));

        int phase = be.getAnimPhase();
        boolean animating = phase != TransmitterBlockEntity.PHASE_NONE;
        boolean depositing = phase == TransmitterBlockEntity.PHASE_SEND;
        float progress = animating
                ? Mth.clamp((level.getGameTime() - be.getAnimStartGameTime() + partialTick)
                        / (float) TransmitterBlockEntity.ANIM_DURATION_TICKS, 0f, 1f)
                : 0f;

        float tongueLengthFull = 0.65f;

        float tonguePitch = 0;
        float tongueLength = 0;
        float headPitch = 80;
        float headPitchModifier = 0;
        float itemDistance = 0;
        float itemScale = 1;

        if (animating) {
            tongueLength = tongueLengthFull;
            if (depositing) {
                // Язык летит с посылкой и возвращается пустым
                double modifier = Math.max(0, 1 - Math.pow((progress - 0.25) * 4 - 1, 4));
                itemDistance = (float) Math.max(tongueLength * Math.min(1, (progress - 0.25) * 3),
                        tongueLength * modifier);
                tongueLength *= (float) Math.max(0, 1 - Math.pow((progress * 1.25f - 0.25f) * 4 - 1, 4));
                headPitchModifier = (float) Math.max(0, 1 - Math.pow(progress * 1.25f * 2 - 1, 4));
                itemScale = 0.25f + progress * 3 / 4;
            } else {
                // Язык летит пустым и возвращается с посылкой
                tongueLength *= (float) Math.pow(Math.max(0, 1 - progress * 1.25f), 5);
                headPitchModifier = 1 - (float) Math.min(1, Math.max(0, (Math.pow(progress * 1.5f, 2) - 0.5) * 2));
                itemScale = Math.max(0.5f, 1 - progress * 1.25f);
                itemDistance = tongueLength;
            }
        }
        headPitch *= headPitchModifier;

        // Игрок открыл интерфейс — квака разевает рот (как у Create: manualOpenAnimationProgress)
        if (be.getOpenCount() > 0) {
            headPitch = Math.max(headPitch, 60);
            tongueLength = Math.max(tongueLength, 0.25f);
        }

        VertexConsumer cutout = buffer.getBuffer(RenderType.cutoutMipped());

        SuperByteBuffer bodyBuf = CachedBuffers.partial(BODY, state);
        bodyBuf.center()
                .rotateYDegrees(yaw)
                .uncenter()
                .light(light)
                .overlay(overlay)
                .renderInto(ms, cutout);

        SuperByteBuffer headBuf = CachedBuffers.partial(HEAD, state);
        headBuf.center()
                .rotateYDegrees(yaw)
                .uncenter()
                .translate(PIVOT_X, PIVOT_Y, PIVOT_Z)
                .rotateXDegrees(headPitch)
                .translateBack(PIVOT_X, PIVOT_Y, PIVOT_Z)
                .light(light)
                .overlay(overlay)
                .renderInto(ms, cutout);

        SuperByteBuffer tongueBuf = CachedBuffers.partial(TONGUE, state);
        tongueBuf.center()
                .rotateYDegrees(yaw)
                .uncenter()
                .translate(PIVOT_X, PIVOT_Y, PIVOT_Z)
                .rotateXDegrees(tonguePitch)
                .scale(1f, 1f, tongueLength / (7 / 16f))
                .translateBack(PIVOT_X, PIVOT_Y, PIVOT_Z)
                .light(light)
                .overlay(overlay)
                .renderInto(ms, cutout);

        if (animating && itemDistance > 0.01f && itemScale >= 0.45f) {
            ItemStack box = be.getAnimatedPackage();
            if (!box.isEmpty()) {
                renderPackage(box, ms, buffer, light, dir, itemDistance, itemScale);
            }
        }

        if (animating) {
            renderPortal(level, ms, buffer, yaw, dir, portalOpenAmount(progress));
        }
    }

    /** Насколько портал открыт: раскрывается первые ~20%, схлопывается в последние ~20%. */
    private float portalOpenAmount(float progress) {
        float open;
        if (progress < 0.2f) {
            open = progress / 0.2f;
        } else if (progress > 0.8f) {
            open = (1 - progress) / 0.2f;
        } else {
            open = 1;
        }
        return Mth.clamp(open, 0, 1);
    }

    private void renderPortal(Level level, PoseStack ms, MultiBufferSource buffer, float yaw, Vec3 dir,
                              float openness) {
        if (openness <= 0.01f) return;

        double dist = 0.75; // перед носом кваки
        float half = 0.375f * openness; // 0.75x0.75 при полном открытии

        int frame = (int) (level.getGameTime() % PORTAL_FRAMES);
        float px = 1f / PORTAL_FRAMES;
        float v0 = frame * px;
        float v1 = v0 + px;

        ms.pushPose();
        ms.translate(0.5 + dir.x * dist, 0.5, 0.5 + dir.z * dist);
        // Плоскость портала перпендикулярна взгляду кваки
        ms.mulPose(Axis.YP.rotationDegrees(yaw - 180));

        VertexConsumer vc = buffer.getBuffer(RenderType.text(PORTAL_TEX));

        for (int side = 0; side < 2; side++) {
            var pose = ms.last();
            vc.addVertex(pose, -half, -half, 0)
                    .setColor(255, 255, 255, (int) (220 * openness))
                    .setUv(0, v1)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setOverlay(OverlayTexture.NO_OVERLAY);
            vc.addVertex(pose, half, -half, 0)
                    .setColor(255, 255, 255, (int) (220 * openness))
                    .setUv(1, v1)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setOverlay(OverlayTexture.NO_OVERLAY);
            vc.addVertex(pose, half, half, 0)
                    .setColor(255, 255, 255, (int) (220 * openness))
                    .setUv(1, v0)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setOverlay(OverlayTexture.NO_OVERLAY);
            vc.addVertex(pose, -half, half, 0)
                    .setColor(255, 255, 255, (int) (220 * openness))
                    .setUv(0, v0)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setOverlay(OverlayTexture.NO_OVERLAY);
            ms.mulPose(Axis.YP.rotationDegrees(180));
        }
        ms.popPose();

        // Лёгкая рябь частиц у открытого портала
        if (openness > 0.9f && level.random.nextFloat() < 0.2f) {
            double x = 0.5 + dir.x * dist + (level.random.nextDouble() - 0.5) * 0.6;
            double z = 0.5 + dir.z * dist + (level.random.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.PORTAL, x, 0.5 + (level.random.nextDouble() - 0.3) * 0.7, z,
                    0, 0.02, 0);
        }
    }

    /** Посылка на кончике языка — в мировых координатах относительно блока. */
    private void renderPackage(ItemStack stack, PoseStack ms, MultiBufferSource buffer, int light,
                               Vec3 dir, float distance, float scale) {
        ms.pushPose();
        Vec3 mouth = new Vec3(0.5, PIVOT_Y, 0.5).add(dir.scale(0.1875f));
        ms.translate(mouth.x + dir.x * distance, mouth.y, mouth.z + dir.z * distance);
        ms.mulPose(Axis.YP.rotationDegrees(-dirToYaw(dir)));
        ms.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, ms, buffer, Minecraft.getInstance().level, 0);
        ms.popPose();
    }

    private static float dirToYaw(Vec3 dir) {
        return (float) (Mth.atan2(dir.x, dir.z) * (180 / Math.PI));
    }
}
