package com.roxiun.mellow.hud

import com.google.gson.JsonObject
import com.roxiun.mellow.config.LegacyHudMigration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.polyfrost.compose.composables.*
import org.polyfrost.compose.layout.PolyInsets
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Include
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.TextHud

/** TextHud with real multiline layout for both fonts and separately coloured section headings. */
abstract class MellowTextHud(id: String, title: String, prefix: String) :
    TextHud(id, title, Category.INFO, prefix, "") {

    @field:Include
    var nativeAppearanceMigrated = false

    private var lines by mutableStateOf(listOf(""))
    private var legacySettings: JsonObject? = null
    private var sectionColor by mutableStateOf<PolyColor?>(null)

    init {
        showBackground = false
        showShadow = true
        padLeft = 0f
        padRight = 0f
        padTop = 0f
        padBottom = 0f
    }

    fun prepareLegacySettings(settings: JsonObject) { legacySettings = settings }

    override fun showByDefault() = legacySettings != null

    override fun setup() {
        val old = legacySettings
        legacySettings = null
        val needsSave = !nativeAppearanceMigrated || old != null
        // Earlier Ornithe HUDs disabled backgrounds entirely, even if this stored flag was true.
        if (!nativeAppearanceMigrated) showBackground = false
        if (old != null) LegacyHudMigration.apply(this, old)
        migrateAppearance()
        nativeAppearanceMigrated = true
        super.setup()
        if (needsSave) save()
    }

    protected open fun migrateAppearance() {}

    protected open fun headingColor(): PolyColor? = null
    protected open fun isHeading(line: String): Boolean = false

    override fun multipleInstancesAllowed() = false

    override val alwaysRedraw: Boolean
        get() = super.alwaysRedraw || sectionColor?.chroma == true

    override fun update(): Boolean {
        lines = decorate(concat(prefix, getText(), suffix)).lines()
        sectionColor = headingColor()?.let { PolyColor(it.rawArgb, it.chroma, it.chromaSpeed) }
        return true
    }

    @Composable
    override fun Content() {
        val fixed = staticWidth && staticW > 0f && staticH > 0f
        val padding = PolyInsets(padLeft, padTop, padRight, padBottom)
        val background = hudBackground()
        val outer = if (fixed) background.size(staticW, staticH).padding(padding) else background.padding(padding)
        val foreground = PolyColor(textColor, textChroma, textChromaSpeed)
        PolyBox(modifier = outer) {
            PolyColumn(modifier = if (fixed) PolyModifier.align(alignment) else PolyModifier) {
                for (raw in lines) {
                    val heading = isHeading(raw)
                    val value = when (caseType) {
                        1 -> raw.uppercase()
                        2 -> raw.lowercase()
                        else -> raw
                    }
                    val color = if (heading) sectionColor ?: foreground else foreground
                    if (font == Font.Minecraft) {
                        val formatted = buildString {
                            if (textBold || heading) append("§l")
                            if (textItalic) append("§o")
                            if (textUnderline) append("§n")
                            append(value)
                        }
                        PolyMcText(text = formatted, color = color, shadow = showShadow, scale = textScale)
                    } else {
                        PoppinsLine(value, color, heading)
                    }
                }
            }
        }
    }

    @Composable
    private fun PoppinsLine(value: String, color: PolyColor, heading: Boolean) {
        // Poppins does not interpret Minecraft formatting codes. Prefixes may still contain them.
        val plain = value.replace(Regex("(?i)§[0-9a-fk-or]"), "")
        val fontName = if (heading) {
            if (textItalic) "poppins-bold-italic" else "poppins-bold"
        } else getPoppinsFontName()
        val fontSize = 8f * textScale
        val skiaFont = FontManager.getFont(fontSize, fontName)
        val metrics = skiaFont.metrics
        val width = skiaFont.measureTextWidth(plain)
        val height = metrics.descent - metrics.ascent + metrics.leading
        val shadow = PolyColor(shadowColor, shadowChroma, shadowChromaSpeed)
        PolyCanvas(modifier = PolyModifier.size(width, height)) { x, y, _, _ ->
            val baseline = y - metrics.ascent
            if (showShadow) text(plain, x + shadowOffsetX, baseline + shadowOffsetY, shadow, skiaFont)
            text(plain, x, baseline, color, skiaFont)
            if (textUnderline) {
                val underlineY = baseline + (metrics.underlinePosition ?: fontSize * 0.08f)
                val thickness = metrics.underlineThickness ?: fontSize * 0.06f
                if (showShadow) line(x + shadowOffsetX, underlineY + shadowOffsetY,
                    x + width + shadowOffsetX, underlineY + shadowOffsetY, shadow, thickness)
                line(x, underlineY, x + width, underlineY, color, thickness)
            }
        }
    }
}
