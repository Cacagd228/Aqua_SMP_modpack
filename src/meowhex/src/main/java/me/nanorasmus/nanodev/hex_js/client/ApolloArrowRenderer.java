package me.nanorasmus.nanodev.hex_js.client;

import me.nanorasmus.nanodev.hex_js.entity.EntityApolloArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;

public class ApolloArrowRenderer extends EntityRenderer<EntityApolloArrow> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/projectiles/arrow.png");
    private final ArrowRenderer delegate;

    public ApolloArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.delegate = new ArrowRenderer(ctx) {
            @Override
            public ResourceLocation getTextureLocation(Entity entity) {
                return TEXTURE;
            }
        };
        this.shadowRadius = 0.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityApolloArrow entity) {
        return TEXTURE;
    }

    @Override
    public void render(EntityApolloArrow entity, float entityYaw, float partialTicks, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
        // Delegate to vanilla ArrowRenderer logic
        delegate.render((AbstractArrow)(Entity)entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
