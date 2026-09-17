package com.meowaddons.ponder;

import com.meowaddons.MeowAddons;
import com.meowaddons.ModBlocks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.resources.ResourceLocation;

public class MeowPonderPlugin implements PonderPlugin {
    @Override public String getModId() { return MeowAddons.MODID; }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // Не регистрируем отдельные сцены для tiered прессов — они используют ponder из Create
        // Через тег kinetic_appliances T6 (и остальные) появятся в том же разделе что и Mechanical Press,
        // а при вызове ponder на T6 откроется оригинальный ponder Create (W на блоке/предмете)
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation kineticAppliances = ResourceLocation.fromNamespaceAndPath("create", "kinetic_appliances");
        helper.addToTag(kineticAppliances)
            .add(ModBlocks.PRESS_T1.getId())
            .add(ModBlocks.PRESS_T2.getId())
            .add(ModBlocks.PRESS_T3.getId())
            .add(ModBlocks.PRESS_T4.getId())
            .add(ModBlocks.PRESS_T5.getId())
            .add(ModBlocks.PRESS_T6.getId());
    }

    @Override public void registerSharedText(SharedTextRegistrationHelper helper) {}
    @Override public void onPonderLevelRestore(PonderLevel ponderLevel) {}
    @Override public void indexExclusions(IndexExclusionHelper helper) {}
}
