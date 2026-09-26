package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xyz.lineage.LineageCore;

/**
 * Display marker for the ascended translucency. The veil itself is drawn by
 * the client ({@code VeilRender}) off the sworn lineage; this trait only
 * speaks its name in the oath-book.
 */
public final class EtherealVeil implements Trait {
    private final ResourceLocation sigil;

    public EtherealVeil(String name) {
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
    public void worn(ServerPlayer player) {
    }
}
