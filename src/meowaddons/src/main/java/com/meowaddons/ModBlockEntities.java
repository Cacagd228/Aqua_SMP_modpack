package com.meowaddons;
import com.meowaddons.tier.Tier;
import com.meowaddons.tier.TieredPressBlockEntity;
import com.meowaddons.transmitter.TransmitterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public class ModBlockEntities {
 public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,MeowAddons.MODID);
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TransmitterBlockEntity>> INTERDIMENSIONAL_TRANSMITTER=BLOCK_ENTITIES.register("interdimensional_transmitter",()->BlockEntityType.Builder.of(TransmitterBlockEntity::new,ModBlocks.INTERDIMENSIONAL_TRANSMITTER.get()).build(null));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TieredPressBlockEntity>> TIERED_PRESS_T1=BLOCK_ENTITIES.register("press_t1",()->BlockEntityType.Builder.of((pos,state)->new TieredPressBlockEntity(pos,state,Tier.ANDESITE),ModBlocks.PRESS_T1.get()).build(null));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TieredPressBlockEntity>> TIERED_PRESS_T2=BLOCK_ENTITIES.register("press_t2",()->BlockEntityType.Builder.of((pos,state)->new TieredPressBlockEntity(pos,state,Tier.BRASS),ModBlocks.PRESS_T2.get()).build(null));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TieredPressBlockEntity>> TIERED_PRESS_T3=BLOCK_ENTITIES.register("press_t3",()->BlockEntityType.Builder.of((pos,state)->new TieredPressBlockEntity(pos,state,Tier.STEEL),ModBlocks.PRESS_T3.get()).build(null));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TieredPressBlockEntity>> TIERED_PRESS_T4=BLOCK_ENTITIES.register("press_t4",()->BlockEntityType.Builder.of((pos,state)->new TieredPressBlockEntity(pos,state,Tier.SHADOW_STEEL),ModBlocks.PRESS_T4.get()).build(null));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TieredPressBlockEntity>> TIERED_PRESS_T5=BLOCK_ENTITIES.register("press_t5",()->BlockEntityType.Builder.of((pos,state)->new TieredPressBlockEntity(pos,state,Tier.REFINED_RADIANCE),ModBlocks.PRESS_T5.get()).build(null));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TieredPressBlockEntity>> TIERED_PRESS_T6=BLOCK_ENTITIES.register("press_t6",()->BlockEntityType.Builder.of((pos,state)->new TieredPressBlockEntity(pos,state,Tier.CHROMATIC),ModBlocks.PRESS_T6.get()).build(null));
 public static void register(IEventBus b){BLOCK_ENTITIES.register(b);}
}
