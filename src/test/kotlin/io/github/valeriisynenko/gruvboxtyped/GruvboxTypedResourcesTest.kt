package io.github.valeriisynenko.gruvboxtyped

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.python.highlighting.PyHighlighter
import io.github.valeriisynenko.gruvboxtyped.python.PyTypeHintHighlighting
import java.awt.Color
import java.awt.Font

/**
 * Headless tests don't install bundled themes and schemes, so the resources are loaded directly.
 */
class GruvboxTypedResourcesTest : BasePlatformTestCase() {

    fun `test editor scheme colors`() {
        val scheme = loadScheme()

        assertEquals("Gruvbox Typed Dark Medium", scheme.name)
        assertEquals(Color(0x28, 0x28, 0x28), scheme.defaultBackground)
        assertEquals(Color(0xEB, 0xDB, 0xB2), scheme.defaultForeground)
        assertEquals(Color(0xFA, 0xBD, 0x2F), scheme.getAttributes(PyTypeHintHighlighting.TYPE_HINT).foregroundColor)
        assertEquals(Color(0xFE, 0x80, 0x19), scheme.getAttributes(PyTypeHintHighlighting.BUILTIN_TYPE_HINT).foregroundColor)
        assertEquals(Color(0xFA, 0xBD, 0x2F), scheme.getAttributes(PyHighlighter.PY_DECORATOR).foregroundColor)
        assertEquals(Font.BOLD, scheme.getAttributes(PyHighlighter.PY_DECORATOR).fontType)
        assertEquals(Color(0xFE, 0x80, 0x19), scheme.getAttributes(PyHighlighter.PY_BUILTIN_NAME).foregroundColor)
        assertEquals(Color(0x8E, 0xC0, 0x7C), scheme.getAttributes(PyHighlighter.PY_FUNCTION_CALL).foregroundColor)
        assertEquals(Font.PLAIN, scheme.getAttributes(PyHighlighter.PY_FUNC_DEFINITION).fontType)
        assertEquals(Color(0xFA, 0xBD, 0x2F), scheme.getAttributes(PyHighlighter.PY_ANNOTATION).foregroundColor)
    }

    fun `test hard scheme differs from medium only in backgrounds`() {
        val medium = loadScheme()
        val hard = loadScheme("Gruvbox Typed Dark Hard")

        assertEquals("Gruvbox Typed Dark Hard", hard.name)
        assertEquals(Color(0x1D, 0x20, 0x21), hard.defaultBackground)
        assertEquals(Color(0x1D, 0x20, 0x21), hard.getColor(EditorColors.GUTTER_BACKGROUND))
        assertEquals(Color(0x28, 0x28, 0x28), hard.getColor(EditorColors.CARET_ROW_COLOR))
        for (key in listOf(PyTypeHintHighlighting.TYPE_HINT, PyTypeHintHighlighting.BUILTIN_TYPE_HINT, PyHighlighter.PY_DECORATOR)) {
            assertEquals(key.externalName, medium.getAttributes(key), hard.getAttributes(key))
        }
    }

    fun `test themes reference their schemes`() {
        for ((file, name, scheme) in THEMES) {
            val theme = loadTheme(file)

            assertEquals(name, theme["name"].asString)
            assertTrue(theme["dark"].asBoolean)
            assertEquals("/colors/$scheme.xml", theme["editorScheme"].asString)
            assertNotNull(javaClass.getResource(theme["editorScheme"].asString))
        }
    }

    fun `test hard theme shifts only the background palette`() {
        val dark = loadTheme("GruvboxTypedDark").getAsJsonObject("colors")
        val hard = loadTheme("GruvboxTypedDarkHard").getAsJsonObject("colors")

        assertEquals("#1d2021", hard["bg0"].asString)
        assertEquals("#282828", hard["bg0_s"].asString)
        assertEquals("#141617", hard["bg0_h"].asString)
        for (key in dark.keySet() - setOf("bg0", "bg0_s", "bg0_h")) {
            assertEquals(key, dark[key], hard[key])
        }
        assertEquals(loadTheme("GruvboxTypedDark")["ui"], loadTheme("GruvboxTypedDarkHard")["ui"])
    }

    fun `test themes use islands layout`() {
        for ((file) in THEMES) {
            val theme = loadTheme(file)
            val ui = theme.getAsJsonObject("ui")

            assertEquals(file, "ExperimentalDark", theme["parentTheme"].asString)
            assertEquals(file, 1, ui["Islands"].asInt)
            // Islands and the gap between them must differ, or the islands are invisible
            val colors = theme.getAsJsonObject("colors")
            val island = colors[ui.getAsJsonObject("Island")["borderColor"].asString].asString
            val gap = colors[ui["MainWindow.background"].asString].asString
            assertFalse(file, island.equals(gap, ignoreCase = true))
        }
    }

    fun `test theme colors are named palette colors or hex values`() {
        for ((file) in THEMES) checkThemeColors(file)
    }

    private fun checkThemeColors(file: String) {
        val theme = loadTheme(file)
        val palette = theme.getAsJsonObject("colors").keySet()
        val unknown = mutableListOf<String>()

        fun check(path: String, node: JsonElement) {
            when {
                node.isJsonObject -> node.asJsonObject.entrySet().forEach { (key, value) -> check("$path.$key", value) }
                node.isJsonPrimitive && node.asJsonPrimitive.isString -> {
                    val value = node.asString
                    if (!HEX.matches(value) && value !in palette) unknown += "$path = $value"
                }
            }
        }
        check("ui", theme["ui"])
        check("colors", theme["colors"])

        assertEmpty(file, unknown)
    }

    private fun loadScheme(name: String = "Gruvbox Typed Dark Medium"): EditorColorsSchemeImpl {
        val element = JDOMUtil.load(javaClass.getResourceAsStream("/colors/$name.xml")!!)
        val parent = EditorColorsManager.getInstance().getScheme("Darcula")!!
        return EditorColorsSchemeImpl(parent).apply { readExternal(element) }
    }

    private fun loadTheme(file: String = "GruvboxTypedDark"): JsonObject =
        javaClass.getResourceAsStream("/themes/$file.theme.json")!!.reader().use { JsonParser.parseReader(it).asJsonObject }

    private companion object {
        val HEX = Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")

        /** Theme file, theme name, editor scheme name. */
        val THEMES = listOf(
            Triple("GruvboxTypedDark", "Gruvbox Typed Dark", "Gruvbox Typed Dark Medium"),
            Triple("GruvboxTypedDarkHard", "Gruvbox Typed Dark Hard", "Gruvbox Typed Dark Hard"),
        )
    }
}
