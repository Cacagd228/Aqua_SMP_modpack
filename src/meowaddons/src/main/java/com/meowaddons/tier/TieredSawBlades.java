package com.meowaddons.tier;

import com.meowaddons.MeowAddons;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Tiered blade partial models для пилы. Класс загружается рано (из TieredSawClient.registerRenderers),
 * чтобы PartialModel успели зарегистрироваться до первого model bake.
 * Индексы вариантов: 0=h_active, 1=h_reversed, 2=h_inactive, 3=v_active, 4=v_reversed, 5=v_inactive.
 */
public final class TieredSawBlades {
 public static final PartialModel[][] BLADES = new PartialModel[6][];
 static {
  for(int t=1; t<=6; t++){
   String n = MeowAddons.MODID + ":block/saw_blade_";
   BLADES[t-1] = new PartialModel[]{
    PartialModel.of(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "block/saw_blade_h_active_t" + t)),
    PartialModel.of(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "block/saw_blade_h_reversed_t" + t)),
    PartialModel.of(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "block/saw_blade_h_inactive_t" + t)),
    PartialModel.of(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "block/saw_blade_v_active_t" + t)),
    PartialModel.of(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "block/saw_blade_v_reversed_t" + t)),
    PartialModel.of(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "block/saw_blade_v_inactive_t" + t)),
   };
  }
 }
 private TieredSawBlades(){}
 public static PartialModel[] of(Tier tier){ return BLADES[tier.level - 1]; }
 public static void touch(){}
}
