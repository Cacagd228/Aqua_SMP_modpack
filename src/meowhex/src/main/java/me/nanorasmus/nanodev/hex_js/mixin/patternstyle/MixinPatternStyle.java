package me.nanorasmus.nanodev.hex_js.mixin.patternstyle;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.RenderLib;
import kotlin.Pair;
import me.nanorasmus.nanodev.hex_js.client.render.PatternStyle;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;

/**
 * Mixes {@link PatternStyle} into {@link Style}: stores a {@link HexPattern} (and
 * cached zappy/pathfinder points) on the style and keeps it intact through style
 * composition ({@link Style#applyTo}) and equality checks.
 */
@Mixin(Style.class)
public abstract class MixinPatternStyle implements PatternStyle {

    private static final float RENDER_SIZE = 128f;

    private HexPattern pattern = null;
    private List<Vec2> zappyPoints = null;
    private List<Vec2> pathfinderDots = null;

    @Override
    public HexPattern getPattern() {
        return this.pattern;
    }

    @Override
    public Style setPattern(HexPattern pattern) {
        this.pattern = pattern;
        if (pattern != null) {
            Pair<Float, List<Vec2>> pair = RenderLib.getCenteredPattern(pattern, RENDER_SIZE, RENDER_SIZE, 16f);
            List<Vec2> dots = pair.getSecond();
            this.zappyPoints = RenderLib.makeZappy(
                dots, RenderLib.findDupIndices(pattern.positions()),
                10, 0.8f, 0f, 0f,
                RenderLib.DEFAULT_READABILITY_OFFSET, RenderLib.DEFAULT_LAST_SEGMENT_LEN_PROP, 0.0);
            this.pathfinderDots = dots;
        } else {
            this.zappyPoints = null;
            this.pathfinderDots = null;
        }
        return (Style) (Object) this;
    }

    @Override
    public List<Vec2> getZappyPoints() {
        return this.zappyPoints;
    }

    @Override
    public List<Vec2> getPathfinderDots() {
        return this.pathfinderDots;
    }

    @Inject(method = "<init>(Lnet/minecraft/network/chat/TextColor;Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/Boolean;Lnet/minecraft/network/chat/ClickEvent;Lnet/minecraft/network/chat/HoverEvent;Ljava/lang/String;Lnet/minecraft/resources/ResourceLocation;)V",
        at = @At("TAIL"))
    private void hexjs$initPatternStyle(@Nullable TextColor color, @Nullable Boolean bold, @Nullable Boolean italic,
                                        @Nullable Boolean underlined, @Nullable Boolean strikethrough,
                                        @Nullable Boolean obfuscated, @Nullable ClickEvent clickEvent,
                                        @Nullable HoverEvent hoverEvent, @Nullable String insertion,
                                        @Nullable ResourceLocation font, CallbackInfo ci) {
        this.pattern = null;
        this.zappyPoints = null;
        this.pathfinderDots = null;
    }

    @Inject(method = "applyTo(Lnet/minecraft/network/chat/Style;)Lnet/minecraft/network/chat/Style;",
        at = @At("RETURN"), cancellable = true)
    private void hexjs$preservePatternApplyTo(Style parent, CallbackInfoReturnable<Style> cir) {
        Style result = cir.getReturnValue();
        if (this.getPattern() != null) {
            ((PatternStyle) result).setPattern(this.getPattern());
        } else {
            HexPattern parentPattern = ((PatternStyle) parent).getPattern();
            if (parentPattern != null) {
                ((PatternStyle) result).setPattern(parentPattern);
            }
        }
        cir.setReturnValue(result);
    }

    @Inject(method = "equals(Ljava/lang/Object;)Z", at = @At("HEAD"), cancellable = true)
    private void hexjs$patternStyleEquals(Object obj, CallbackInfoReturnable<Boolean> cir) {
        if (this != obj && obj instanceof PatternStyle other) {
            if (!Objects.equals(this.getPattern(), other.getPattern())) {
                cir.setReturnValue(false);
            }
        }
    }
}