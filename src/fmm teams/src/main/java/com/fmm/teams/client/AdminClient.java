package com.fmm.teams.client;

import java.util.List;

import com.fmm.teams.net.AdminNet;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client cache for the operator panel. Kept separate from {@link TeamClient} because the payload
 * is large and only operators ever receive it — a non-operator gets an empty, refused snapshot.
 * The panel is a submenu of {@link TeamsScreen}: no keybind, no command, the button that opens it
 * is only rendered when the server said this player is an operator.
 */
@OnlyIn(Dist.CLIENT)
public final class AdminClient {
    private AdminClient() {}

    private static volatile AdminNet.ClientboundAdminData data =
            new AdminNet.ClientboundAdminData(false, List.of(), List.of(), null, "", false);

    public static AdminNet.ClientboundAdminData adminData() {
        return data;
    }

    public static void applySync(AdminNet.ClientboundAdminData payload) {
        data = payload;
    }

    public static void requestSnapshot() {
        PacketDistributor.sendToServer(AdminNet.ServerboundAdminAction.of("refresh", "", ""));
    }

    /** Opens the panel as a submenu; {@link AdminScreen#onClose()} returns to the party screen. */
    public static void openAdmin() {
        Minecraft mc = Minecraft.getInstance();
        requestSnapshot();
        mc.execute(() -> mc.setScreen(new AdminScreen()));
    }
}
