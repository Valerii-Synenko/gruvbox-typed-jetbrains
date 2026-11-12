package io.github.valeriisynenko.gruvboxtyped.icons

import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.valeriisynenko.gruvboxtyped.icons.GruvboxTypedFileIconProvider.Companion.iconFor

class GruvboxTypedFileIconProviderTest : BasePlatformTestCase() {

    fun `test icons by file name`() {
        for (name in listOf("uv.lock", "uv.toml", "poetry.lock", "poetry.toml", "ruff.toml", ".ruff.toml", "ty.toml", "pyrefly.toml", ".gitignore", ".pre-commit-config.yaml", ".pre-commit-hooks.yaml")) {
            val icon = iconFor(name, isDirectory = false)
            assertNotNull(name, icon)
            assertEquals(name, 16, icon!!.iconWidth)
        }
        for (name in listOf(".claude", ".codex", ".cursor", ".gemini", ".junie", ".windsurf")) {
            assertNotNull(name, iconFor(name, isDirectory = true))
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
