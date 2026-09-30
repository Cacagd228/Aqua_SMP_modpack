package at.petrak.hexcasting.common.casting.actions.strings

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.PatternShapeMatch
import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.SpecialPatterns
import at.petrak.hexcasting.api.casting.getPattern
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.common.casting.PatternRegistryManifest
import net.minecraft.resources.ResourceLocation

/**
 * Name a pattern. A pattern the registry does not recognise yields null, so a
 * spell can test whether something is a known rune.
 *
 * The four spell-glyphs are special: the VM handles them before the registry is
 * ever consulted, so they are spelled out here the way a player would write
 * them.
 */
object OpActionString : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pattern = args.getPattern(0, argc)
        val hexapi = HexAPI.instance()

        val glyph = when (pattern) {
            SpecialPatterns.INTROSPECTION -> "hexcasting:open_paren"
            SpecialPatterns.RETROSPECTION -> "hexcasting:close_paren"
            SpecialPatterns.CONSIDERATION -> "hexcasting:escape"
            SpecialPatterns.EVANITION -> "hexcasting:undo"
            else -> null
        }
        if (glyph != null) {
            return hexapi.getRawHookI18n(ResourceLocation.parse(glyph)).string.asActionResult
        }

        return when (val match = PatternRegistryManifest.matchPattern(pattern, env.world, false)) {
            is PatternShapeMatch.Normal -> hexapi.getActionI18n(match.key, false).string.asActionResult
            is PatternShapeMatch.PerWorld -> hexapi.getActionI18n(match.key, false).string.asActionResult
            is PatternShapeMatch.Special -> match.handler.name.string.asActionResult
            is PatternShapeMatch.Nothing -> listOf(NullIota())
        }
    }
}
