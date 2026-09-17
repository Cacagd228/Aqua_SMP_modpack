package com.meowaddons;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public class ModCreativeTabs{
 public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MeowAddons.MODID);
 public static final DeferredHolder<CreativeModeTab,CreativeModeTab> MEOW_TAB=CREATIVE_MODE_TABS.register("meow_tab",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.meowaddons")).icon(()->new ItemStack(ModBlocks.INTERDIMENSIONAL_TRANSMITTER.get())).displayItems((par,out)->{
  out.accept(ModBlocks.INTERDIMENSIONAL_TRANSMITTER.get());
  out.accept(ModBlocks.FRAME_ANDESITE.get());
  out.accept(ModBlocks.FRAME_LATUN.get());
  out.accept(ModBlocks.FRAME_STEEL.get());
  out.accept(ModBlocks.FRAME_SHADOW_STEEL.get());
  out.accept(ModBlocks.FRAME_REFINED_RADIANCE.get());
  out.accept(ModBlocks.FRAME_CHROMATIC_COMPOUND.get());
  out.accept(ModBlocks.PRESS_T1.get());
  out.accept(ModBlocks.PRESS_T2.get());
  out.accept(ModBlocks.PRESS_T3.get());
  out.accept(ModBlocks.PRESS_T4.get());
  out.accept(ModBlocks.PRESS_T5.get());
  out.accept(ModBlocks.PRESS_T6.get());
 }).build());
 public static void register(IEventBus b){CREATIVE_MODE_TABS.register(b);}
}
