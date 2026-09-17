package xyz.lineage.trait;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import xyz.lineage.LineageCore;

/** The rod chooses the scholar: bare hands strike feebly without a staff. */
public final class StaffOath implements Trait {
    private final ResourceLocation sigil;
    private final ResourceLocation fetter;

    public StaffOath(String name) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
        this.fetter = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name + "_fetter");
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
        AttributeInstance arm = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (arm != null) {
            arm.removeModifier(fetter);
        }
    }

    @Override
    public void pulse(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        AttributeInstance arm = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (arm == null) {
            return;
        }
        if (rodInHand(player.getMainHandItem()) || rodInHand(player.getOffhandItem())) {
            arm.removeModifier(fetter);
        } else if (arm.getModifier(fetter) == null) {
            arm.addTransientModifier(new AttributeModifier(fetter, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static boolean rodInHand(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String space = id.getNamespace();
        return (space.equals("hexcasting") || space.equals("meowhex")) && id.getPath().contains("staff");
    }
}
