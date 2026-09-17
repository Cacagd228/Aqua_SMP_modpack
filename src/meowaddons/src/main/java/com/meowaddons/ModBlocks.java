package com.meowaddons;
import com.meowaddons.block.FrameBlock;
import com.meowaddons.tier.Tier;
import com.meowaddons.tier.TieredPressBlock;
import com.meowaddons.transmitter.TransmitterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
public class ModBlocks {
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(MeowAddons.MODID);
 public static final DeferredBlock<TransmitterBlock> INTERDIMENSIONAL_TRANSMITTER=BLOCKS.register("interdimensional_transmitter",()->new TransmitterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0F).sound(SoundType.WOOD)));
 public static final DeferredBlock<FrameBlock> FRAME_ANDESITE=BLOCKS.register("frame_andesite",()->new FrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F).sound(SoundType.STONE).noOcclusion().isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false)));
 public static final DeferredBlock<FrameBlock> FRAME_STEEL=BLOCKS.register("frame_steel",()->new FrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL).noOcclusion().isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false)));
 public static final DeferredBlock<FrameBlock> FRAME_LATUN=BLOCKS.register("frame_latun",()->new FrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2.5F).sound(SoundType.METAL).noOcclusion().isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false)));
 public static final DeferredBlock<FrameBlock> FRAME_SHADOW_STEEL=BLOCKS.register("frame_shadow_steel",()->new FrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0F).sound(SoundType.METAL).noOcclusion().isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false)));
 public static final DeferredBlock<FrameBlock> FRAME_REFINED_RADIANCE=BLOCKS.register("frame_refined_radiance",()->new FrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.5F).sound(SoundType.METAL).noOcclusion().isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false)));
 public static final DeferredBlock<FrameBlock> FRAME_CHROMATIC_COMPOUND=BLOCKS.register("frame_chromatic_compound",()->new FrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.METAL).noOcclusion().isViewBlocking((s,l,p)->false).isSuffocating((s,l,p)->false)));
 public static final DeferredBlock<TieredPressBlock> PRESS_T1=BLOCKS.register("press_t1",()->new TieredPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F).sound(SoundType.METAL), Tier.ANDESITE));
 public static final DeferredBlock<TieredPressBlock> PRESS_T2=BLOCKS.register("press_t2",()->new TieredPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F).sound(SoundType.METAL), Tier.BRASS));
 public static final DeferredBlock<TieredPressBlock> PRESS_T3=BLOCKS.register("press_t3",()->new TieredPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.METAL), Tier.STEEL));
 public static final DeferredBlock<TieredPressBlock> PRESS_T4=BLOCKS.register("press_t4",()->new TieredPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(4.0F).sound(SoundType.METAL), Tier.SHADOW_STEEL));
 public static final DeferredBlock<TieredPressBlock> PRESS_T5=BLOCKS.register("press_t5",()->new TieredPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(4.0F).sound(SoundType.METAL), Tier.REFINED_RADIANCE));
 public static final DeferredBlock<TieredPressBlock> PRESS_T6=BLOCKS.register("press_t6",()->new TieredPressBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(4.5F).sound(SoundType.METAL), Tier.CHROMATIC));
 public static void register(IEventBus b){BLOCKS.register(b);}
}
