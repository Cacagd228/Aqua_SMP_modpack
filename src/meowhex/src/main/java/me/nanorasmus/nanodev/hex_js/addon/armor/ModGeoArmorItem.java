package me.nanorasmus.nanodev.hex_js.addon.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Gecko-броня без анимаций: статичная geo-модель, движение задаёт
 * {@link ModGeoArmorRenderer} по ванильному скелету.
 * Важно: geo-файл ботинок обязан содержать и кости поножей
 * ({@code armorLeftLeg}/{@code armorRightLeg}), иначе GeckoLib не применит
 * трансформацию ног к ботинкам.
 */
public class ModGeoArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String set;
    private final String part;

    public ModGeoArmorItem(Holder<ArmorMaterial> material, Type type, String set, String part, Properties properties) {
        super(material, type, properties);
        this.set = set;
        this.part = part;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ModGeoArmorRenderer renderer;

            @Override
            public <T extends net.minecraft.world.entity.LivingEntity> net.minecraft.client.model.HumanoidModel<?> getGeoArmorRenderer(
                    T livingEntity,
                    net.minecraft.world.item.ItemStack itemStack,
                    net.minecraft.world.entity.EquipmentSlot equipmentSlot,
                    net.minecraft.client.model.HumanoidModel<T> original) {
                if (this.renderer == null) {
                    this.renderer = new ModGeoArmorRenderer(new ModGeoArmorModel(set, part));
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // статичная броня без анимаций
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
