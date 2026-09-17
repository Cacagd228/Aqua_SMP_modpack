package me.nanorasmus.nanodev.hex_js.addon.interop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.addon.HextendedItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Client-side Curios wiring: renders the diadem's item model on the wearer's head,
 * following the same Create goggles / hexcasting lens transform chain so the
 * model's own {@code display.head} transform positions the crown.
 */
@OnlyIn(Dist.CLIENT)
public final class HexJsCuriosInteropClient {
    private HexJsCuriosInteropClient() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        // Mirror hexcasting's CuriosRenderers: defer the bake to a Supplier so the
        // layer is baked lazily (after RegisterLayerDefinitions), not eagerly here.
        event.enqueueWork(() -> {
            if (HextendedItems.CHARGED_AMETHYST_DIADEM.isBound()) {
                CuriosRendererRegistry.register(HextendedItems.CHARGED_AMETHYST_DIADEM.get(),
                        HexJsCuriosInteropClient::createRenderer);
            }
        });
    }

    private static ICurioRenderer createRenderer() {
        return new DiademCurioRenderer(
                Minecraft.getInstance().getEntityModels().bakeLayer(DiademCurioRenderer.LAYER));
    }

    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DiademCurioRenderer.LAYER, DiademCurioRenderer::layer);
    }

    static final class DiademCurioRenderer implements ICurioRenderer {
        static final ModelLayerLocation LAYER =
                new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(HexJS.MOD_ID, "diadem"), "head");

        private static LayerDefinition layer() {
            CubeListBuilder builder = new CubeListBuilder();
            MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0);
            mesh.getRoot().addOrReplaceChild("head", builder, net.minecraft.client.model.geom.PartPose.ZERO);
            return LayerDefinition.create(mesh, 1, 1);
        }

        private final HumanoidModel<LivingEntity> model;

        DiademCurioRenderer(ModelPart head) {
            this.model = new HumanoidModel<>(head);
        }

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(
                ItemStack stack, SlotContext slotContext, PoseStack ms,
                RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light,
                float limbSwing, float limbSwingAmount, float partialTicks,
                float ageInTicks, float netHeadYaw, float headPitch) {
            LivingEntity living = slotContext.entity();
            model.setupAnim(living, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            model.prepareMobModel(living, limbSwing, limbSwingAmount, partialTicks);
            ICurioRenderer.followHeadRotations(living, model.head);

            ModelPart head = model.head;
            ms.pushPose();
            ms.translate(head.x / 16.0, head.y / 16.0, head.z / 16.0);
            ms.mulPose(Axis.YP.rotation(head.yRot));
            ms.mulPose(Axis.XP.rotation(head.xRot));
            ms.translate(0.0, -0.25, 0.0);
            ms.mulPose(Axis.ZP.rotationDegrees(180.0f));
            ms.scale(0.625f, 0.625f, 0.625f);

            var mc = Minecraft.getInstance();
            mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.HEAD, light, OverlayTexture.NO_OVERLAY,
                    ms, buffers, mc.level, 0);
            ms.popPose();
        }
    }
}
