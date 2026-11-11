package io.github.valeriisynenko.gruvboxtyped

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.python.highlighting.PyHighlighter
import io.github.valeriisynenko.gruvboxtyped.python.PyTypeHintHighlighting
import java.awt.Color

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
        assertEquals(Color(0xFE, 0x80, 0x19), scheme.getAttributes(PyHighlighter.PY_BUILTIN_NAME).foregroundColor)
        assertEquals(Color(0x68, 0x9D, 0x6A), scheme.getAttributes(PyHighlighter.PY_FUNCTION_CALL).foregroundColor)
        assertEquals(Color(0xFA, 0xBD, 0x2F), scheme.getAttributes(PyHighlighter.PY_ANNOTATION).foregroundColor)
    }

    fun `test theme references existing scheme`() {
        val theme = loadTheme()

        assertEquals("Gruvbox Typed Dark", theme["name"].asString)
        assertTrue(theme["dark"].asBoolean)
        assertNotNull(javaClass.getResource(theme["editorScheme"].asString))
    }

    fun `test theme colors are named palette colors or hex values`() {
        val theme = loadTheme()
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

        assertEmpty(unknown)
    }

    private fun loadScheme(): EditorColorsSchemeImpl {
        val element = JDOMUtil.load(javaClass.getResourceAsStream("/colors/Gruvbox Typed Dark Medium.xml")!!)
        val parent = EditorColorsManager.getInstance().getScheme("Darcula")!!
        return EditorColorsSchemeImpl(parent).apply { readExternal(element) }
    }

    private fun loadTheme(): JsonObject =
        javaClass.getResourceAsStream("/themes/GruvboxTypedDark.theme.json")!!.reader().use { JsonParser.parseReader(it).asJsonObject }

    private companion object {
        val HEX = Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")
    }
}
