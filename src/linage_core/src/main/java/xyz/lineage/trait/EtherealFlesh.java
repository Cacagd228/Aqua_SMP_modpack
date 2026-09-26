package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xyz.lineage.LineageCore;

/**
 * Flesh turned to open air: worn armor keeps its looks but grants no
 * protection. Armor and toughness are pinned to zero with modifiers that
 * dwarf anything plate can give; both are lifted when the blood is stripped.
 */
public final class EtherealFlesh implements Trait {
    private static final double NULL_ARMOR = -100.0;

    private final ResourceLocation sigil;

    public EtherealFlesh(String name) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
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
    public void worn(ServerPlayer player) {
        pin(player, Attributes.ARMOR);
        pin(player, Attributes.ARMOR_TOUGHNESS);
    }

    @Override
    public void stripped(ServerPlayer player) {
        release(player, Attributes.ARMOR);
        release(player, Attributes.ARMOR_TOUGHNESS);
    }

    private void pin(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> holder) {
        AttributeInstance inst = player.getAttribute(holder);
        if (inst != null) {
            inst.removeModifier(sigil);
            inst.addTransientModifier(new AttributeModifier(sigil, NULL_ARMOR, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private void release(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> holder) {
        AttributeInstance inst = player.getAttribute(holder);
        if (inst != null) {
            inst.removeModifier(sigil);
        }
    }
}
