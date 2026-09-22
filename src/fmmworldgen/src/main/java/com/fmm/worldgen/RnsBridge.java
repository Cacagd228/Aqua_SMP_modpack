package com.fmm.worldgen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.attachment.AttachmentType;

/**
 * Soft (reflection-only) bridge to Create: Rock &amp; Stone.
 *
 * <p>FMMWorldgen does not hard-depend on RNS, so every access goes through
 * reflection. If RNS is missing, all methods safely report "unavailable" and
 * the caller skips deposit work. Used to register guaranteed island veins as
 * custom deposits so the RNS deposit scanner can find them.
 */
public final class RnsBridge {
   private static volatile boolean initialized;
   private static volatile boolean available;
   private static AttachmentType<?> levelDepositDataType;
   private static Class<?> customLocationClass;
   private static Constructor<?> customLocationCtor;
   private static Method addCustomDepositMethod;
   private static Field customDepositsField;
   private static Method getLocationMethod;

   private RnsBridge() {
   }

   private static synchronized void init() {
      if (initialized) {
         return;
      }
      initialized = true;
      try {
         Class<?> rnsMisc = Class.forName("com.bmaster.createrns.RNSMisc");
         Field supplierField = rnsMisc.getField("LEVEL_DEPOSIT_DATA");
         Object supplier = supplierField.get(null);
         Object attachmentType = ((Supplier<?>) supplier).get();
         if (!(attachmentType instanceof AttachmentType)) {
            return;
         }
         Class<?> levelDataClass = Class.forName("com.bmaster.createrns.content.deposit.info.LevelDepositData");
         customLocationClass = Class.forName("com.bmaster.createrns.content.deposit.info.CustomServerDepositLocation");
         customLocationCtor = customLocationClass.getConstructor(ResourceKey.class, BlockPos.class);
         addCustomDepositMethod = levelDataClass.getMethod("addCustomDeposit", customLocationClass);
         customDepositsField = levelDataClass.getDeclaredField("customDeposits");
         customDepositsField.setAccessible(true);
         getLocationMethod = customLocationClass.getMethod("getLocation");
         levelDepositDataType = (AttachmentType<?>) attachmentType;
         available = true;
      } catch (Throwable t) {
         available = false;
      }
   }

   public static boolean isAvailable() {
      if (!initialized) {
         init();
      }
      return available;
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static Object levelDepositData(ServerLevel level) {
      if (!isAvailable()) {
         return null;
      }
      try {
         return level.getData((AttachmentType) levelDepositDataType);
      } catch (Throwable t) {
         return null;
      }
   }

   /**
    * Registers a custom deposit so the RNS scanner finds the island vein.
    * Returns false when RNS is missing or the spot is already covered by a
    * vanilla RNS structure (the scanner finds that one instead).
    */
   public static boolean registerCustomDeposit(ServerLevel level, ResourceKey<Structure> structureKey, BlockPos pos) {
      Object depData = levelDepositData(level);
      if (depData == null) {
         return false;
      }
      try {
         Object loc = customLocationCtor.newInstance(structureKey, pos);
         Object result = addCustomDepositMethod.invoke(depData, loc);
         return Boolean.TRUE.equals(result);
      } catch (Throwable t) {
         return false;
      }
   }

   /** True if a custom deposit of the same type is already registered nearby. */
   @SuppressWarnings("unchecked")
   public static boolean hasCustomDepositNear(
      ServerLevel level, ResourceKey<Structure> structureKey, BlockPos pos, double radiusBlocks
   ) {
      Object depData = levelDepositData(level);
      if (depData == null) {
         return false;
      }
      try {
         Object raw = customDepositsField.get(depData);
         if (!(raw instanceof Map)) {
            return false;
         }
         Set<?> set = (Set<?>) ((Map<?, ?>) raw).get(structureKey.location());
         if (set == null || set.isEmpty()) {
            return false;
         }
         double r2 = radiusBlocks * radiusBlocks;
         for (Object o : set) {
            if (o == null || !customLocationClass.isInstance(o)) {
               continue;
            }
            Object loc = getLocationMethod.invoke(o);
            if (loc instanceof BlockPos bp && (double) bp.distSqr(pos) <= r2) {
               return true;
            }
         }
         return false;
      } catch (Throwable t) {
         return false;
      }
   }
}
