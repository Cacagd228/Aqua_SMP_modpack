package com.meowaddons;
import com.meowaddons.tier.Tier;
import com.meowaddons.tier.TieredPressItem;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
public class ModItems{
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(MeowAddons.MODID);
 public static final DeferredItem<BlockItem> INTERDIMENSIONAL_TRANSMITTER=ITEMS.registerSimpleBlockItem(ModBlocks.INTERDIMENSIONAL_TRANSMITTER);
 public static final DeferredItem<BlockItem> FRAME_ANDESITE=ITEMS.registerSimpleBlockItem(ModBlocks.FRAME_ANDESITE);
 public static final DeferredItem<BlockItem> FRAME_STEEL=ITEMS.registerSimpleBlockItem(ModBlocks.FRAME_STEEL);
 public static final DeferredItem<BlockItem> FRAME_LATUN=ITEMS.registerSimpleBlockItem(ModBlocks.FRAME_LATUN);
 public static final DeferredItem<BlockItem> FRAME_SHADOW_STEEL=ITEMS.registerSimpleBlockItem(ModBlocks.FRAME_SHADOW_STEEL);
 public static final DeferredItem<BlockItem> FRAME_REFINED_RADIANCE=ITEMS.registerSimpleBlockItem(ModBlocks.FRAME_REFINED_RADIANCE);
 public static final DeferredItem<BlockItem> FRAME_CHROMATIC_COMPOUND=ITEMS.registerSimpleBlockItem(ModBlocks.FRAME_CHROMATIC_COMPOUND);
 public static final DeferredItem<BlockItem> PRESS_T1=ITEMS.register("press_t1",()->new TieredPressItem(ModBlocks.PRESS_T1.get(), new net.minecraft.world.item.Item.Properties(), Tier.ANDESITE));
 public static final DeferredItem<BlockItem> PRESS_T2=ITEMS.register("press_t2",()->new TieredPressItem(ModBlocks.PRESS_T2.get(), new net.minecraft.world.item.Item.Properties(), Tier.BRASS));
 public static final DeferredItem<BlockItem> PRESS_T3=ITEMS.register("press_t3",()->new TieredPressItem(ModBlocks.PRESS_T3.get(), new net.minecraft.world.item.Item.Properties(), Tier.STEEL));
 public static final DeferredItem<BlockItem> PRESS_T4=ITEMS.register("press_t4",()->new TieredPressItem(ModBlocks.PRESS_T4.get(), new net.minecraft.world.item.Item.Properties(), Tier.SHADOW_STEEL));
 public static final DeferredItem<BlockItem> PRESS_T5=ITEMS.register("press_t5",()->new TieredPressItem(ModBlocks.PRESS_T5.get(), new net.minecraft.world.item.Item.Properties(), Tier.REFINED_RADIANCE));
 public static final DeferredItem<BlockItem> PRESS_T6=ITEMS.register("press_t6",()->new TieredPressItem(ModBlocks.PRESS_T6.get(), new net.minecraft.world.item.Item.Properties(), Tier.CHROMATIC));
 public static void register(IEventBus b){ITEMS.register(b);}
}