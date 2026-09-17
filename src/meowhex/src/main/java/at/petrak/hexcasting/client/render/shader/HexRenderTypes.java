package at.petrak.hexcasting.client.render.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

// https://github.com/VazkiiMods/Botania/blob/3a43accc2fbc439c9f2f00a698f8f8ad017503db/Common/src/main/java/vazkii/botania/client/core/helper/RenderHelper.java
public final class HexRenderTypes extends RenderType {

    private HexRenderTypes(String string, VertexFormat vertexFormat, VertexFormat.Mode mode, int i, boolean bl,
        boolean bl2, Runnable runnable, Runnable runnable2) {
        super(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2);
        throw new UnsupportedOperationException("Should not be instantiated");
    }

    public static RenderType getGrayscaleLayer(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

}
