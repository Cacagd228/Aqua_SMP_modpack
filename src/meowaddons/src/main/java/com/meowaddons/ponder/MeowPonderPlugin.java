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
        // Прессы — pressing + compacting (как в AllCreatePonderScenes для MECHANICAL_PRESS)
        com.meowaddons.tier.Tier[] tiers = new com.meowaddons.tier.Tier[]{com.meowaddons.tier.Tier.ANDESITE, com.meowaddons.tier.Tier.BRASS, com.meowaddons.tier.Tier.STEEL, com.meowaddons.tier.Tier.SHADOW_STEEL, com.meowaddons.tier.Tier.REFINED_RADIANCE, com.meowaddons.tier.Tier.CHROMATIC};
        ResourceLocation[] pressBlocks = new ResourceLocation[]{ModBlocks.PRESS_T1.getId(), ModBlocks.PRESS_T2.getId(), ModBlocks.PRESS_T3.getId(), ModBlocks.PRESS_T4.getId(), ModBlocks.PRESS_T5.getId(), ModBlocks.PRESS_T6.getId()};
        for (int i = 0; i < tiers.length; i++) {
            ResourceLocation block = pressBlocks[i];
            helper.forComponents(block)
                .addStoryBoard(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "mechanical_press/pressing_t" + tiers[i].level), com.simibubi.create.infrastructure.ponder.scenes.ProcessingScenes::pressing)
                .addStoryBoard(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "mechanical_press/compacting_t" + tiers[i].level), com.simibubi.create.infrastructure.ponder.scenes.ProcessingScenes::compacting);
        }
        // Дробильные колёса — стандарт crushing_wheels (только crushing_wheels, без контроллера)
        ResourceLocation[] wheelBlocks = new ResourceLocation[]{ModBlocks.CRUSHING_WHEEL_T1.getId(), ModBlocks.CRUSHING_WHEEL_T2.getId(), ModBlocks.CRUSHING_WHEEL_T3.getId(), ModBlocks.CRUSHING_WHEEL_T4.getId(), ModBlocks.CRUSHING_WHEEL_T5.getId(), ModBlocks.CRUSHING_WHEEL_T6.getId()};
        for (int i = 0; i < tiers.length; i++) {
            ResourceLocation block = wheelBlocks[i];
            helper.forComponents(block)
                .addStoryBoard(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "crushing_wheel/crushing_t" + tiers[i].level), com.simibubi.create.infrastructure.ponder.scenes.ProcessingScenes::crushingWheels);
        }
        // Жернова — стандарт millstone
        ResourceLocation[] millBlocks = new ResourceLocation[]{ModBlocks.MILLSTONE_T1.getId(), ModBlocks.MILLSTONE_T2.getId(), ModBlocks.MILLSTONE_T3.getId(), ModBlocks.MILLSTONE_T4.getId(), ModBlocks.MILLSTONE_T5.getId(), ModBlocks.MILLSTONE_T6.getId()};
        for (int i = 0; i < tiers.length; i++) {
            ResourceLocation block = millBlocks[i];
            helper.forComponents(block)
                .addStoryBoard(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "millstone/milling_t" + tiers[i].level), com.simibubi.create.infrastructure.ponder.scenes.ProcessingScenes::millstone);
        }
        // Смешиватели — стандарт mixing
        ResourceLocation[] mixerBlocks = new ResourceLocation[]{ModBlocks.MIXER_T1.getId(), ModBlocks.MIXER_T2.getId(), ModBlocks.MIXER_T3.getId(), ModBlocks.MIXER_T4.getId(), ModBlocks.MIXER_T5.getId(), ModBlocks.MIXER_T6.getId()};
        for (int i = 0; i < tiers.length; i++) {
            ResourceLocation block = mixerBlocks[i];
            helper.forComponents(block)
                .addStoryBoard(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "mixer/mixing_t" + tiers[i].level), com.simibubi.create.infrastructure.ponder.scenes.ProcessingScenes::mixing);
        }
        // Пилы — стандарт processing (MechanicalSawScenes)
        ResourceLocation[] sawBlocks = new ResourceLocation[]{ModBlocks.SAW_T1.getId(), ModBlocks.SAW_T2.getId(), ModBlocks.SAW_T3.getId(), ModBlocks.SAW_T4.getId(), ModBlocks.SAW_T5.getId(), ModBlocks.SAW_T6.getId()};
        for (int i = 0; i < tiers.length; i++) {
            ResourceLocation block = sawBlocks[i];
            helper.forComponents(block)
                .addStoryBoard(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "saw/cutting_t" + tiers[i].level), com.simibubi.create.infrastructure.ponder.scenes.MechanicalSawScenes::processing);
        }
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
            .add(ModBlocks.PRESS_T6.getId())
            .add(ModBlocks.CRUSHING_WHEEL_T1.getId())
            .add(ModBlocks.CRUSHING_WHEEL_T2.getId())
            .add(ModBlocks.CRUSHING_WHEEL_T3.getId())
            .add(ModBlocks.CRUSHING_WHEEL_T4.getId())
            .add(ModBlocks.CRUSHING_WHEEL_T5.getId())
            .add(ModBlocks.CRUSHING_WHEEL_T6.getId())
            .add(ModBlocks.MILLSTONE_T1.getId())
            .add(ModBlocks.MILLSTONE_T2.getId())
            .add(ModBlocks.MILLSTONE_T3.getId())
            .add(ModBlocks.MILLSTONE_T4.getId())
            .add(ModBlocks.MILLSTONE_T5.getId())
            .add(ModBlocks.MILLSTONE_T6.getId())
            .add(ModBlocks.MIXER_T1.getId())
            .add(ModBlocks.MIXER_T2.getId())
            .add(ModBlocks.MIXER_T3.getId())
            .add(ModBlocks.MIXER_T4.getId())
            .add(ModBlocks.MIXER_T5.getId())
            .add(ModBlocks.MIXER_T6.getId())
            .add(ModBlocks.SAW_T1.getId())
            .add(ModBlocks.SAW_T2.getId())
            .add(ModBlocks.SAW_T3.getId())
            .add(ModBlocks.SAW_T4.getId())
            .add(ModBlocks.SAW_T5.getId())
            .add(ModBlocks.SAW_T6.getId());
    }

    @Override public void registerSharedText(SharedTextRegistrationHelper helper) {}
    @Override public void onPonderLevelRestore(PonderLevel ponderLevel) {}
    @Override public void indexExclusions(IndexExclusionHelper helper) {}
}
