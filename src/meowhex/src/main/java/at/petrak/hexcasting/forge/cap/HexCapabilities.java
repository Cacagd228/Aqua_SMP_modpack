package at.petrak.hexcasting.forge.cap;

import at.petrak.hexcasting.api.addldata.*;
import at.petrak.hexcasting.api.client.ClientCastingStack;
import net.neoforged.neoforge.capabilities.CapabilityRegistry;
import net.neoforged.neoforge.common.capabilities.Capability;

public final class HexCapabilities {
    public static final Capability<ADMediaHolder> MEDIA = new CapabilityRegistry<>(
        (rl, iface, impl) -> null).create(net.minecraft.resources.ResourceLocation.parse("hexcasting:media"), ADMediaHolder.class, Object.class);
    public static final Capability<ADIotaHolder> IOTA = new CapabilityRegistry<>(
        (rl, iface, impl) -> null).create(net.minecraft.resources.ResourceLocation.parse("hexcasting:iota"), ADIotaHolder.class, Object.class);
    public static final Capability<ADHexHolder> STORED_HEX = new CapabilityRegistry<>(
        (rl, iface, impl) -> null).create(net.minecraft.resources.ResourceLocation.parse("hexcasting:hex"), ADHexHolder.class, Object.class);
    public static final Capability<ADVariantItem> VARIANT_ITEM = new CapabilityRegistry<>(
        (rl, iface, impl) -> null).create(net.minecraft.resources.ResourceLocation.parse("hexcasting:variant"), ADVariantItem.class, Object.class);
    public static final Capability<ADPigment> COLOR = new CapabilityRegistry<>(
        (rl, iface, impl) -> null).create(net.minecraft.resources.ResourceLocation.parse("hexcasting:pigment"), ADPigment.class, Object.class);
    public static final Capability<java.util.function.Supplier<ClientCastingStack>> CLIENT_CASTING_STACK = new CapabilityRegistry<>(
        (rl, iface, impl) -> null).create(net.minecraft.resources.ResourceLocation.parse("hexcasting:client_stack"), java.util.function.Supplier.class, ClientCastingStack.class);
}
