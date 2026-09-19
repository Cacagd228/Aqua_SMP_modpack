package me.nanorasmus.nanodev.hex_js.entity;

import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class HexEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, HexJS.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<EntityDeception>> DECEPTION = REGISTER.register("deception",
            () -> EntityType.Builder.<EntityDeception>of(EntityDeception::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("deception"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityHadesSummon>> HADES_SUMMON = REGISTER.register("hades_summon",
            () -> EntityType.Builder.<EntityHadesSummon>of(EntityHadesSummon::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("hades_summon"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityStyxShade>> STYX_SHADE = REGISTER.register("styx_shade",
            () -> EntityType.Builder.<EntityStyxShade>of(EntityStyxShade::new, MobCategory.MONSTER)
                    .sized(0.4f, 0.8f)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("styx_shade"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityApolloArrow>> APOLLO_ARROW = REGISTER.register("apollo_arrow",
            () -> EntityType.Builder.<EntityApolloArrow>of(EntityApolloArrow::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("apollo_arrow"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntitySniperShot>> SNIPER_SHOT = REGISTER.register("sniper_shot",
            () -> EntityType.Builder.<EntitySniperShot>of(EntitySniperShot::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("sniper_shot"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntitySunBeam>> SUN_BEAM = REGISTER.register("sun_beam",
            () -> EntityType.Builder.<EntitySunBeam>of(EntitySunBeam::new, MobCategory.MISC)
                    .sized(2.0f, EntitySunBeam.HEIGHT)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("sun_beam"));

    public static void init(IEventBus modBus) {
        REGISTER.register(modBus);
        modBus.addListener(HexEntities::onAttributeCreate);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(DECEPTION.get(), EntityDeception.createAttributes().build());
        event.put(HADES_SUMMON.get(), me.nanorasmus.nanodev.hex_js.entity.EntityHadesSummon.createAttributes().build());
        event.put(STYX_SHADE.get(), me.nanorasmus.nanodev.hex_js.entity.EntityStyxShade.createAttributes().build());
    }
}
