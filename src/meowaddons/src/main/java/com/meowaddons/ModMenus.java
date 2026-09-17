package com.meowaddons;
import com.meowaddons.transmitter.TransmitterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public class ModMenus {
 public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,MeowAddons.MODID);
 public static final DeferredHolder<MenuType<?>,MenuType<TransmitterMenu>> TRANSMITTER=MENUS.register("transmitter",()->IMenuTypeExtension.create(TransmitterMenu::newClientMenu));
 public static void register(IEventBus b){MENUS.register(b);}
}
