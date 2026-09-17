package me.nanorasmus.nanodev.hex_js.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.nanorasmus.nanodev.hex_js.entity.EntityDeception;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.Minecraft;

public class DeceptionRenderer extends EntityRenderer<EntityDeception> {

    public DeceptionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }

    // 10% darker via color tint, fixed, not cumulative
    private static class DarkBufferSource implements MultiBufferSource {
        private final MultiBufferSource parent;
        DarkBufferSource(MultiBufferSource parent) { this.parent = parent; }
        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(net.minecraft.client.renderer.RenderType type) {
            var orig = parent.getBuffer(type);
            return (com.mojang.blaze3d.vertex.VertexConsumer) java.lang.reflect.Proxy.newProxyInstance(
                orig.getClass().getClassLoader(),
                new Class[]{com.mojang.blaze3d.vertex.VertexConsumer.class},
                (proxy, method, args) -> {
                    String n = method.getName().toLowerCase();
                    if (n.contains("color") && args != null) {
                        // setColor(int r,g,b,a) or setColor(float r,g,b,a) or color(int)
                        if (args.length == 4) {
                            if (args[0] instanceof Integer) {
                                args[0] = Math.max(0, (int)((int)args[0] * 0.9f));
                                args[1] = Math.max(0, (int)((int)args[1] * 0.9f));
                                args[2] = Math.max(0, (int)((int)args[2] * 0.9f));
                            } else if (args[0] instanceof Float) {
                                args[0] = (float)args[0] * 0.9f;
                                args[1] = (float)args[1] * 0.9f;
                                args[2] = (float)args[2] * 0.9f;
                            } else if (args[0] instanceof Number) {
                                // generic
                            }
                        } else if (args.length == 1 && args[0] instanceof Integer) {
                            int col = (int)args[0];
                            int a = (col >> 24) & 0xFF;
                            int r = (col >> 16) & 0xFF;
                            int g = (col >> 8) & 0xFF;
                            int b = col & 0xFF;
                            r = Math.max(0, (int)(r * 0.9f));
                            g = Math.max(0, (int)(g * 0.9f));
                            b = Math.max(0, (int)(b * 0.9f));
                            args[0] = (a << 24) | (r << 16) | (g << 8) | b;
                        }
                    }
                    Object res = method.invoke(orig, args);
                    // most VertexConsumer methods return VertexConsumer for chaining
                    if (res instanceof com.mojang.blaze3d.vertex.VertexConsumer) return proxy;
                    return res;
                }
            );
        }
    }

    @Override
    public void render(EntityDeception entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        MultiBufferSource darkBuffer = new DarkBufferSource(buffer);
        EntityType<?> targetType = entity.getTargetType();
        if (targetType == null) {
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            return;
        }
        // Player special case — targetType.create returns null for player
        if (targetType == EntityType.PLAYER) {
            try {
                var clientLevel = Minecraft.getInstance().level;
                if (clientLevel != null) {
                    String uuidStr = entity.getPlayerUUID();
                    String name = entity.getPlayerName();
                    com.mojang.authlib.GameProfile profile;
                    if (uuidStr != null && !uuidStr.isEmpty()) {
                        try {
                            profile = new com.mojang.authlib.GameProfile(java.util.UUID.fromString(uuidStr), name != null && !name.isEmpty() ? name : "Player");
                        } catch (Throwable t) {
                            profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name != null && !name.isEmpty() ? name : "Player");
                        }
                    } else {
                        profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name != null && !name.isEmpty() ? name : "Player");
                    }
                    // Try to reuse real player info for skin
                    net.minecraft.client.multiplayer.PlayerInfo info = null;
                    try {
                        var conn = Minecraft.getInstance().getConnection();
                        if (conn != null && uuidStr != null && !uuidStr.isEmpty()) {
                            info = conn.getPlayerInfo(java.util.UUID.fromString(uuidStr));
                        }
                    } catch (Throwable ignored) {}
                    net.minecraft.client.player.AbstractClientPlayer dummy;
                    if (info != null) {
                        // Use existing player info via RemotePlayer with profile, but skin will be fetched via info
                        dummy = new net.minecraft.client.player.RemotePlayer(clientLevel, profile);
                    } else {
                        dummy = new net.minecraft.client.player.RemotePlayer(clientLevel, profile);
                    }
                    dummy.setPos(entity.getX(), entity.getY(), entity.getZ());
                    dummy.setYRot(entity.getYRot());
                    dummy.setXRot(entity.getXRot());
                    dummy.yHeadRot = entity.yHeadRot;
                    dummy.yBodyRot = entity.yBodyRot;
                    dummy.yHeadRotO = entity.yHeadRotO;
                    dummy.yBodyRotO = entity.yBodyRotO;
                    dummy.yRotO = entity.yRotO;
                    dummy.xRotO = entity.xRotO;
                    dummy.tickCount = entity.tickCount;
                    dummy.setPose(entity.getPose());
                    dummy.setGlowingTag(entity.hasGlowingTag());
                    if (entity.hasCustomName()) dummy.setCustomName(entity.getCustomName());
                    dummy.setCustomNameVisible(entity.isCustomNameVisible());
                    for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                        dummy.setItemSlot(slot, entity.getItemBySlot(slot).copy());
                    }
                    EntityRenderer<? super net.minecraft.world.entity.player.Player> renderer = (EntityRenderer<? super net.minecraft.world.entity.player.Player>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummy);
                    poseStack.pushPose();
                    renderer.render(dummy, entityYaw, partialTicks, poseStack, darkBuffer, packedLight);
                    poseStack.popPose();
                    return;
                }
            } catch (Throwable t) {
                // fallback to generic
            }
        }
        // Try to get renderer for target
        try {
            var level = entity.level();
            var dummyOpt = targetType.create(level);
            if (dummyOpt instanceof LivingEntity dummy) {
                // Copy transform and animation to avoid тряска
                dummy.setPos(entity.getX(), entity.getY(), entity.getZ());
                dummy.setYRot(entity.getYRot());
                dummy.setXRot(entity.getXRot());
                dummy.yHeadRot = entity.yHeadRot;
                dummy.yBodyRot = entity.yBodyRot;
                dummy.yHeadRotO = entity.yHeadRotO;
                dummy.yBodyRotO = entity.yBodyRotO;
                dummy.yRotO = entity.yRotO;
                dummy.xRotO = entity.xRotO;
                dummy.tickCount = entity.tickCount;
                // Walk animation
                try {
                    dummy.walkDist = entity.walkDist;
                    dummy.walkDistO = entity.walkDistO;
                    dummy.attackAnim = entity.attackAnim;
                    dummy.oAttackAnim = entity.oAttackAnim;
                } catch (Throwable ignored) {}
                dummy.setPose(entity.getPose());
                dummy.setGlowingTag(entity.hasGlowingTag());
                if (entity.hasCustomName()) dummy.setCustomName(entity.getCustomName());
                dummy.setCustomNameVisible(entity.isCustomNameVisible());
                for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                    dummy.setItemSlot(slot, entity.getItemBySlot(slot).copy());
                }
                EntityRenderer<? super LivingEntity> renderer = (EntityRenderer<? super LivingEntity>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummy);
                poseStack.pushPose();
                renderer.render(dummy, entityYaw, partialTicks, poseStack, darkBuffer, packedLight);
                poseStack.popPose();
                return;
            } else if (dummyOpt != null) {
                EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummyOpt);
                poseStack.pushPose();
                ((EntityRenderer)renderer).render(dummyOpt, entityYaw, partialTicks, poseStack, darkBuffer, packedLight);
                poseStack.popPose();
                return;
            }
        } catch (Throwable t) {
            // fallback
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityDeception entity) {
        // Not used as we delegate, but return missing
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/creeper/creeper.png");
    }
}
