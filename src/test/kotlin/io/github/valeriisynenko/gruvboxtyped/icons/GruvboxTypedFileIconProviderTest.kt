package io.github.valeriisynenko.gruvboxtyped.icons

import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.valeriisynenko.gruvboxtyped.icons.GruvboxTypedFileIconProvider.Companion.iconFor
import java.io.File

class GruvboxTypedFileIconProviderTest : BasePlatformTestCase() {

    fun `test icons by file name`() {
        for (name in listOf("uv.lock", "uv.toml", "poetry.lock", "poetry.toml", "ruff.toml", ".ruff.toml", "ty.toml", "pyrefly.toml", ".gitignore", ".pre-commit-config.yaml", ".pre-commit-hooks.yaml")) {
            val icon = iconFor(name, isDirectory = false)
            assertNotNull(name, icon)
            assertTrue(name, icon!!.iconWidth > 0)
        }
        for (name in listOf(".claude", ".codex", ".cursor", ".gemini", ".junie", ".windsurf")) {
            assertNotNull(name, iconFor(name, isDirectory = true))
        }
    }

    // The rendered size depends on the screen scale (it differs on CI), so the declared SVG size is checked instead
    fun `test icons are 16x16`() {
        // Tests run from the project directory; the plugin resources are packed into a jar on the classpath
        val icons = File("src/main/resources/icons").listFiles { file -> file.extension == "svg" }!!
        assertNotEmpty(icons.toList())
        for (file in icons) {
            val svg = JDOMUtil.load(file)
            assertEquals(file.name, "16", svg.getAttributeValue("width"))
            assertEquals(file.name, "16", svg.getAttributeValue("height"))
        }
    }

    fun `test other files keep their icons`() {
        assertNull(iconFor(".claude", isDirectory = false))
        assertNull(iconFor("uv.lock", isDirectory = true))
        assertNull(iconFor("pyproject.toml", isDirectory = false))
        assertNull(iconFor(".pre-commit-config.yml", isDirectory = false))
        assertNull(iconFor("config.yaml", isDirectory = false))
        assertNull(iconFor("claude", isDirectory = true))
    }

    fun `test icons only with gruvbox typed themes`() {
        // The headless test IDE runs with its default theme
        assertFalse(GruvboxTypedFileIconProvider.isGruvboxTypedThemeActive())
        assertNull(GruvboxTypedFileIconProvider().getIcon(myFixture.addFileToProject("uv.lock", "").virtualFile, 0, project))
    }

    fun `test theme ids match plugin xml`() {
        val pluginXml = JDOMUtil.load(javaClass.getResourceAsStream("/META-INF/plugin.xml")!!)
        val ids = pluginXml.getChild("extensions").getChildren("themeProvider").map { it.getAttributeValue("id") }.toSet()

        assertEquals(ids, GruvboxTypedFileIconProvider.THEME_IDS)
    }
}
