package xyz.lineage.trait;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xyz.lineage.LineageCore;

/** Stone-born lungs and limbs: water drags the mountain-folk down. */
public final class SwimmerBurden implements Trait {
    private final ResourceLocation sigil;
    private final ResourceLocation anchor;

    public SwimmerBurden(String name) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
        this.anchor = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name + "_anchor");
    }

    @Override
    public ResourceLocation sigil() {
        return sigil;
    }

    @Override
    public Component title() {
        return Component.translatable("trait." + LineageCore.MOD_ID + "." + sigil.getPath());
    }

    @Override
    public Component lore() {
        return Component.translatable("trait." + LineageCore.MOD_ID + "." + sigil.getPath() + ".desc");
    }

    @Override
    public boolean burden() {
        return true;
    }

    @Override
    public void stripped(ServerPlayer player) {
        Optional<? extends Holder<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(
            ResourceLocation.fromNamespaceAndPath("neoforge", "swim_speed"));
        holder.ifPresent(h -> {
            AttributeInstance inst = player.getAttribute(h);
            if (inst != null) {
                inst.removeModifier(anchor);
            }
        });
    }

    @Override
    public void pulse(ServerPlayer player) {
        if (!player.isUnderWater()) {
            return;
        }
        // Mirror the swim_bonus attribute lookup used by StaticGift swimmers.
        Optional<? extends Holder<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(
            ResourceLocation.fromNamespaceAndPath("neoforge", "swim_speed"));
        holder.ifPresent(h -> {
            AttributeInstance inst = player.getAttribute(h);
            if (inst != null && inst.getModifier(anchor) == null) {
                inst.addTransientModifier(new AttributeModifier(anchor, -0.5, AttributeModifier.Operation.ADD_VALUE));
            }
        });
    }

    static ResourceLocation swimSpeedId() {
        return ResourceLocation.fromNamespaceAndPath("neoforge", "swim_speed");
    }
}
