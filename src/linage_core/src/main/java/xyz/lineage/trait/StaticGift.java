package xyz.lineage.trait;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import xyz.lineage.LineageCore;

/**
 * A flat attribute endowment granted while the lineage is worn.
 * One class replaces the many single-attribute powers.
 */
public final class StaticGift implements Trait {
    private final ResourceLocation sigil;
    private final Holder<Attribute> attribute;
    private final double amount;
    private final AttributeModifier.Operation mode;

    public StaticGift(String name, Holder<Attribute> attribute, double amount, AttributeModifier.Operation mode) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
        this.attribute = attribute;
        this.amount = amount;
        this.mode = mode;
    }

    public static StaticGift byId(String name, ResourceLocation attributeId, double amount, AttributeModifier.Operation mode) {
        var holder = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).orElseThrow();
        return new StaticGift(name, holder, amount, mode);
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
    public void worn(ServerPlayer player) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) {
            inst.removeModifier(sigil);
            inst.addTransientModifier(new AttributeModifier(sigil, amount, mode));
        }
    }

    @Override
    public void stripped(ServerPlayer player) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) {
            inst.removeModifier(sigil);
        }
    }
}
