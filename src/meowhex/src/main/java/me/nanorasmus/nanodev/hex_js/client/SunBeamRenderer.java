package me.nanorasmus.nanodev.hex_js.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.nanorasmus.nanodev.hex_js.entity.EntitySunBeam;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Renders the Sun Strike beam: two crossed vertical quads (gold core +
 * wider faint halo) with the vanilla lightning render type, fading out
 * over the entity's lifetime. No textures, no block-entity tricks.
 */
public class SunBeamRenderer extends EntityRenderer<EntitySunBeam> {
    private static final ResourceLocation DUMMY =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    public SunBeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySunBeam entity) {
        return DUMMY;
    }

    @Override
    public void render(EntitySunBeam beam, float entityYaw, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = beam.tickCount + partialTicks;
        float alpha = 1.0f - age / EntitySunBeam.MAX_AGE;
        if (alpha <= 0.0f) {
            return;
        }
        // Плавное появление в первые ~4 тика.
        float appear = Math.min(1.0f, age / 4.0f);
        float a = alpha * appear;

        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        float h = EntitySunBeam.HEIGHT;

        poseStack.pushPose();
        // Крест из двух плоскостей.
        drawPlane(vc, poseStack.last().pose(), 0.28f, h, 1.0f, 0.82f, 0.35f, a);
        drawPlane(vc, poseStack.last().pose(), 0.9f, h, 1.0f, 0.6f, 0.15f, a * 0.35f);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        drawPlane(vc, poseStack.last().pose(), 0.28f, h, 1.0f, 0.82f, 0.35f, a);
        drawPlane(vc, poseStack.last().pose(), 0.9f, h, 1.0f, 0.6f, 0.15f, a * 0.35f);
        poseStack.popPose();

        super.render(beam, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private static void drawPlane(VertexConsumer vc, Matrix4f mat,
            float halfWidth, float height, float r, float g, float b, float a) {
        // Вертикальный квад от ног сущности вверх; двусторонний (cull включён).
        // Fullbright даёт сам шейдер lightning, lightmap-элемента в формате нет.
        quad(vc, mat, -halfWidth, halfWidth, height, r, g, b, a);
        quad(vc, mat, halfWidth, -halfWidth, height, r, g, b, a);
    }

    private static void quad(VertexConsumer vc, Matrix4f mat,
            float x0, float x1, float height, float r, float g, float b, float a) {
        vc.addVertex(mat, x0, 0.0f, 0.0f).setColor(r, g, b, a);
        vc.addVertex(mat, x1, 0.0f, 0.0f).setColor(r, g, b, a);
        vc.addVertex(mat, x1, height, 0.0f).setColor(r, g, b, a);
        vc.addVertex(mat, x0, height, 0.0f).setColor(r, g, b, a);
    }
}
