package at.petrak.hexcasting.client.render.shader;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record FakeBufferSource(MultiBufferSource parent,
                               Function<ResourceLocation, RenderType> mapper) implements MultiBufferSource {

    @Override
    public @NotNull VertexConsumer getBuffer(@NotNull RenderType renderType) {
        return parent.getBuffer(renderType);
    }
}
