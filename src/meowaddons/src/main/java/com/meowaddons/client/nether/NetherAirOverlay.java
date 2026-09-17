package com.meowaddons.client.nether;
import com.meowaddons.MeowAddons;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
/** Индикатор воздуха в Незере в стиле Create: иконка баллона + оставшееся время. Виден при надетых маске и медном баллоне. */
public class NetherAirOverlay implements LayeredDraw.Layer{
 public static final NetherAirOverlay INSTANCE=new NetherAirOverlay();
 public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID,"nether_air");
 private static final ResourceLocation COPPER_BACKTANK_ID=ResourceLocation.fromNamespaceAndPath("create","copper_backtank");
 private NetherAirOverlay(){}
 @Override public void render(GuiGraphics g,DeltaTracker t){
  Minecraft mc=Minecraft.getInstance();
  if(mc.options.hideGui)return;
  if(mc.gameMode!=null&&mc.gameMode.getPlayerMode()==GameType.SPECTATOR)return;
  LocalPlayer p=mc.player;
  if(p==null||p.isCreative())return;
  if(mc.level==null||!p.level().dimension().equals(Level.NETHER))return;
  if(DivingHelmetItem.getWornItem(p).isEmpty())return;
  ItemStack tank=null;
  for(ItemStack s:p.getArmorSlots())if(COPPER_BACKTANK_ID.equals(BuiltInRegistries.ITEM.getKey(s.getItem()))){tank=s;break;}
  if(tank==null)return;
  int air=BacktankUtil.getAir(tank);
  g.pose().pushPose();
  g.pose().translate(g.guiWidth()/2f+90,g.guiHeight()-53,0);
  g.renderItem(tank,0,0);
  Component time=Component.literal(StringUtil.formatTickDuration(Math.max(0,air-1)*10,mc.level.tickRateManager().tickrate()));
  int color=0xFFFFFFFF;
  if(air<=0)color=0xFFFF0000;
  else if(air<60&&(air&1)==0)color=0xFFFF5555;
  g.drawString(mc.font,time,16,5,color);
  g.pose().popPose();
 }
}
