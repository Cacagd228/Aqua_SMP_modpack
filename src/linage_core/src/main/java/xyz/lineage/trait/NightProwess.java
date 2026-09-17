package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xyz.lineage.LineageCore;

/** Moonlit vigor: keener strikes and a swifter stride while night reigns. */
public final class NightProwess implements Trait {
    private final ResourceLocation sigil;
    private final ResourceLocation fang;
    private final ResourceLocation stride;

    public NightProwess(String name) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
        this.fang = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name + "_fang");
        this.stride = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name + "_stride");
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
    public void stripped(ServerPlayer player) {
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.removeModifier(fang);
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(stride);
        }
    }

    @Override
    public void pulse(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        boolean moonlit = !player.level().isDay();
        AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            if (moonlit && damage.getModifier(fang) == null) {
                damage.addTransientModifier(new AttributeModifier(fang, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!moonlit) {
                damage.removeModifier(fang);
            }
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (moonlit && speed.getModifier(stride) == null) {
                speed.addTransientModifier(new AttributeModifier(stride, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!moonlit) {
                speed.removeModifier(stride);
            }
        }
    }
}
