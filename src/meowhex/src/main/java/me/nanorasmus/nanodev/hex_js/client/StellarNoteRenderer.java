package me.nanorasmus.nanodev.hex_js.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.entity.EntityStellarNote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Рендерер снаряда «Стеллар Тюна»: спрайт 4-конечной искры-ноты + лента-след.
 *
 * <p>Спрайты ({@code textures/entity/stellar_note_0..2.png}) вырезаны из
 * официального демо-ролика Terraria с вики — это те самые искры, что летят за
 * звездой. Каждый выстрел выбирает один из вариантов случайно
 * ({@link EntityStellarNote#getSprite()}), так что залп не выглядит штампованным.
 *
 * <p>Пять-конечную звезду-спрайт убрали: из GIF с 4-цветной палитрой она
 * восстанавливалась заметно хуже нарисованной фигуры, а искра — отдельная
 * частица — извлекается чисто.
 *
 * <p>След — лента по истории позиций ({@link Trail}, только на клиенте). Раньше он
 * был из частиц раз в два тика, отсюда рваные разрывы. Каждый отрезок — квад,
 * развёрнутый к камере, поэтому лента не вырождается в линию и не «пропадает»,
 * когда игрок оказывается с снарядом в одной плоскости.
 *
 * <p>Геометрия везде квадами (по 4 вершины): {@code RenderType} собирает вершины
 * четвёрками, треугольный веер не совпал бы с границами квадов.
 *
 * <p>Существует не «для красоты»: без зарегистрированного рендерера
 * {@code EntityRenderDispatcher.getRenderer} возвращает {@code null}, и клиент
 * падает с NPE в {@code shouldRender} на первом же кадре со снарядом.
 */
public class StellarNoteRenderer extends EntityRenderer<EntityStellarNote> {

    /** Спрайты искр: по одному на выстрел, выбирается случайно. */
    private static final ResourceLocation[] SPRITES = {
            sprite(0), sprite(1), sprite(2)
    };

    /** Полуразмер спрайта звезды, доля блока. */
    private static final float STAR_SIZE = 0.20f;
    /** Оборотов в секунду. */
    private static final float SPIN_PER_SECOND = 2.0f;

    /** Длина следа в точках. */
    private static final int TRAIL_POINTS = 10;
    /** Полная ширина следа у головы, доля блока. */
    private static final float TRAIL_WIDTH = 0.14f;
    /** На сколько сужается каждая точка к хвосту. */
    private static final float TRAIL_TAPER = 0.13f;

    /** Цвет: у головы золотой, к хвосту фиолетовый. */
    private static final float HEAD_R = 1.0f;
    private static final float HEAD_G = 0.88f;
    private static final float HEAD_B = 0.48f;
    private static final float TAIL_R = 0.66f;
    private static final float TAIL_G = 0.40f;
    private static final float TAIL_B = 1.0f;

    /** Следы по id сущности; при её исчезновении запись собирается сборщиком. */
    private static final Map<Integer, Trail> TRAILS = new WeakHashMap<>();

    /** История позиций для ленты следа. */
    private static final class Trail {
        private final List<Vec3> points = new ArrayList<>(TRAIL_POINTS);
        private int lastTick = -1;

        void push(Vec3 pos, int tick) {
            if (tick == lastTick) {
                return; // в пределах тика позиция одна — не дублируем
            }
            lastTick = tick;
            points.add(0, pos);
            while (points.size() > TRAIL_POINTS) {
                points.remove(points.size() - 1);
            }
        }
    }

    private static ResourceLocation sprite(int i) {
        return ResourceLocation.fromNamespaceAndPath(
                HexJS.MOD_ID, "textures/entity/stellar_note_" + i + ".png");
    }

    public StellarNoteRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityStellarNote entity) {
        return SPRITES[entity.getSprite()];
    }

    @Override
    public void render(EntityStellarNote entity, float entityYaw, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Светим сами: иначе в тени снаряд не виден.
        int light = LightTexture.FULL_BRIGHT;
        Vec3 head = new Vec3(entity.getX(), entity.getY(), entity.getZ());
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        // След — сплошной заливкой (текстура не нужна), звезда — спрайтом.
        renderTrail(entity, head, camera, buffer.getBuffer(RenderType.LIGHTNING), light);
        renderStar(entity, partialTicks,
                buffer.getBuffer(RenderType.entityTranslucentEmissive(SPRITES[entity.getSprite()])),
                light, poseStack);
    }

    // ------------------------------------------------------------------
    // След
    // ------------------------------------------------------------------

    /**
     * Лента из истории позиций: каждый отрезок — один квад, развёрнутый к камере,
     * с шириной, падающей к хвосту. Рисуем в мировых координатах, поэтому матрица
     * единичная: переданный poseStack уже сдвинут на позицию сущности.
     */
    private void renderTrail(EntityStellarNote entity, Vec3 head, Vec3 camera,
            VertexConsumer vc, int light) {
        Trail trail = TRAILS.computeIfAbsent(entity.getId(), id -> new Trail());
        trail.push(head, entity.tickCount);
        if (trail.points.size() < 2) {
            return;
        }
        Matrix4f world = new Matrix4f();

        for (int i = 0; i + 1 < trail.points.size(); i++) {
            float width = TRAIL_WIDTH * (1.0f - i * TRAIL_TAPER);
            if (width <= 0.005f) {
                break; // хвост сошёл на ноль
            }
            ribbon(trail.points.get(i), trail.points.get(i + 1), camera, width,
                    world, vc, light);
        }
    }

    /**
     * Один квад между a и b, развёрнутый к камере: боковой вектор — пересечение
     * направления сегмента с направлением на камеру. Получается лента, всегда
     * «лицом» к игроку, поэтому не вырождается в линию.
     */
    private static void ribbon(Vec3 a, Vec3 b, Vec3 camera, float width, Matrix4f world,
            VertexConsumer vc, int light) {
        Vec3 axis = b.subtract(a);
        if (axis.lengthSqr() < 1e-9) {
            return;
        }
        Vec3 toCam = camera.subtract(a);
        Vec3 side = axis.cross(toCam);
        if (side.lengthSqr() < 1e-9) {
            side = axis.cross(new Vec3(0.0, 1.0, 0.0)); // камера легла на ось
        }
        if (side.lengthSqr() < 1e-9) {
            side = axis.cross(new Vec3(1.0, 0.0, 0.0));
        }
        side = side.normalize().scale(width * 0.5);

        emitWorld(world, a.add(side), vc, light, 0.95f, HEAD_R, HEAD_G, HEAD_B);
        emitWorld(world, a.subtract(side), vc, light, 0.95f, HEAD_R, HEAD_G, HEAD_B);
        emitWorld(world, b.subtract(side), vc, light, 0.20f, TAIL_R, TAIL_G, TAIL_B);
        emitWorld(world, b.add(side), vc, light, 0.20f, TAIL_R, TAIL_G, TAIL_B);
    }

    // ------------------------------------------------------------------
    // Звезда
    // ------------------------------------------------------------------

    /**
     * Спрайт искры одним квадом, всегда лицом к камере
     * ({@code cameraOrientation()}), с вращением вокруг оси взгляда.
     *
     * <p>Тип отрисовки — {@code entityTranslucentEmissive}: читает текстуру с альфой
     * и не берёт свет от блока, поэтому искра светится сама. {@code LIGHTNING} не
     * подошёл бы — он текстуру не читает вовсе.
     */
    private void renderStar(EntityStellarNote entity, float partialTicks, VertexConsumer vc,
            int light, PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float age = entity.tickCount + partialTicks;
        poseStack.mulPose(new Quaternionf().rotateZ(
                (float) (age * SPIN_PER_SECOND * 2.0 * Math.PI / 20.0)));
        // Чуть ближе к камере, чтобы лента не прошивала искру насквозь.
        poseStack.translate(0.0, 0.0, 0.01);

        PoseStack.Pose pose = poseStack.last();
        float s = STAR_SIZE;
        // Порядок обхода по часовой — иначе вывернется нормаль.
        emit(pose, -s, -s, vc, light, 0.0f, 1.0f);
        emit(pose, s, -s, vc, light, 1.0f, 0.0f);
        emit(pose, s, s, vc, light, 1.0f, 1.0f);
        emit(pose, -s, s, vc, light, 0.0f, 1.0f);
        poseStack.popPose();
    }

    /** Вершина в локальных координатах модели (матрица уже учтена в pose). */
    private static void emit(PoseStack.Pose pose, float x, float y, VertexConsumer vc,
            int light, float u, float v) {
        vc.addVertex(pose, x, y, 0.0f);
        vc.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        vc.setUv(u, v);
        vc.setOverlay(OverlayTexture.NO_OVERLAY);
        vc.setLight(light);
        vc.setNormal(pose, 0.0f, 0.0f, 1.0f);
    }

    /** Вершина в мировых координатах (для следа, матрица единичная). */
    private static void emitWorld(Matrix4f world, Vec3 v, VertexConsumer vc, int light,
            float alpha, float r, float g, float b) {
        vc.addVertex(world, (float) v.x, (float) v.y, (float) v.z);
        vc.setColor(r, g, b, alpha);
        vc.setUv(0.0f, 0.0f);
        vc.setOverlay(OverlayTexture.NO_OVERLAY);
        vc.setLight(light);
    }
}
