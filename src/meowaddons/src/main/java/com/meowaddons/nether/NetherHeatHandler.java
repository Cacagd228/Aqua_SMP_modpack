package com.meowaddons.nether;
import java.util.List;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
/** Жара Незера: без маски и медного баллона Create с воздухом игрок постоянно горит. Воздух тратится в 2 раза быстрее, чем в воде (1 ед. в 10 тиков против 1 ед. в 20 тиков у Create). */
public class NetherHeatHandler{
 private static final ResourceLocation COPPER_BACKTANK_ID=ResourceLocation.fromNamespaceAndPath("create","copper_backtank");
 private static final long AIR_CONSUME_INTERVAL=10L;
 private static final int HEAT_FIRE_TICKS=100;
 public static void onPlayerTick(PlayerTickEvent.Post e){
  Player p=e.getEntity();
  Level l=p.level();
  if(l.isClientSide)return;
  if(!l.dimension().equals(Level.NETHER))return;
  if(!p.isAlive())return;
  if(p.isCreative()||p.isSpectator())return;
  if(p.fireImmune())return;
  if(p.hasEffect(MobEffects.FIRE_RESISTANCE))return;
  if(hasFireProtection(p,l))return;
  ItemStack tank=findCopperBacktankWithAir(p);
  boolean masked=!DivingHelmetItem.getWornItem(p).isEmpty();
  if(tank!=null&&masked){
   if(l.getGameTime()%AIR_CONSUME_INTERVAL==0)BacktankUtil.consumeAir(p,tank,1);
   if(!p.isInLava())p.clearFire();
  }else{
   if(p.getRemainingFireTicks()<HEAT_FIRE_TICKS)p.setRemainingFireTicks(HEAT_FIRE_TICKS);
  }
 }
 private static ItemStack findCopperBacktankWithAir(Player p){
  List<ItemStack> tanks=BacktankUtil.getAllWithAir(p);
  for(ItemStack t:tanks)if(COPPER_BACKTANK_ID.equals(BuiltInRegistries.ITEM.getKey(t.getItem())))return t;
  return null;
 }
 private static boolean hasFireProtection(Player p,Level l){
  Holder<Enchantment> fireProt=l.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_PROTECTION);
  for(ItemStack a:p.getArmorSlots()){
   if(a.isEmpty())continue;
   if(EnchantmentHelper.getItemEnchantmentLevel(fireProt,a)>0)return true;
  }
  return false;
 }
}
