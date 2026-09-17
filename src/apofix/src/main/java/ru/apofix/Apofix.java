package ru.apofix;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

@Mod(Apofix.MODID)
public class Apofix {

    public static final String MODID = "apofix";

    public Apofix(IEventBus bus) {
        ModAttributes.ATTRIBUTES.register(bus);
        bus.register(this);
    }

    /**
     * Навешиваем все 8 атрибутов на каждый LivingEntity-тип (включая игрока),
     * по аналогии с {@code ApothicAttributes#applyAttribs}.
     */
    @SubscribeEvent
    public void applyAttribs(EntityAttributeModificationEvent e) {
        for (EntityType<? extends LivingEntity> type : e.getTypes()) {
            for (var attr : ModAttributes.ALL) {
                if (!e.has(type, attr)) {
                    e.add(type, attr);
                }
            }
        }
    }
}
