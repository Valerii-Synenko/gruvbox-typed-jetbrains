package io.github.valeriisynenko.gruvboxtyped.icons

import com.intellij.ide.FileIconProvider
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/**
 * File and folder icons for tool files (AI assistant folders, uv, Poetry, Ruff, ty, Pyrefly, `.gitignore`, pre-commit).
 * They are part of the Gruvbox Typed look, so they show up only while a Gruvbox Typed UI theme is active.
 */
class GruvboxTypedFileIconProvider : FileIconProvider, DumbAware {

    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? =
        if (isGruvboxTypedThemeActive()) iconFor(file.name, file.isDirectory) else null

    companion object {
        /** Ids of the `themeProvider` extensions in plugin.xml. */
        val THEME_IDS = setOf("io.github.valerii-synenko.gruvbox-typed.dark", "io.github.valerii-synenko.gruvbox-typed.dark-hard")

        private val UV = icon("uv")
        private val POETRY = icon("poetry")
        private val RUFF = icon("ruff")

        /** AI assistant folders get the standard folder icon with the tool's badge. */
        private val FOLDERS = mapOf(
            ".claude" to icon("folderClaude"),
            ".codex" to icon("folderCodex"),
            ".cursor" to icon("folderCursor"),
            ".gemini" to icon("folderGemini"),
            ".junie" to icon("folderJunie"),
            ".windsurf" to icon("folderWindsurf"),
        )

        private val FILES = mapOf(
            "uv.lock" to UV,
            "uv.toml" to UV,
            "poetry.lock" to POETRY,
            "poetry.toml" to POETRY,
            "ruff.toml" to RUFF,
            ".ruff.toml" to RUFF,
            "ty.toml" to icon("ty"),
            "pyrefly.toml" to icon("pyrefly"),
            ".gitignore" to icon("gitignore"),
            ".pre-commit-config.yaml" to icon("preCommit"),
            ".pre-commit-hooks.yaml" to icon("preCommit"),
        )

        fun iconFor(name: String, isDirectory: Boolean): Icon? = if (isDirectory) FOLDERS[name] else FILES[name]

        fun isGruvboxTypedThemeActive(): Boolean = LafManager.getInstance().currentUIThemeLookAndFeel?.id in THEME_IDS

        private fun icon(name: String): Icon = IconLoader.getIcon("/icons/$name.svg", GruvboxTypedFileIconProvider::class.java)
    }
}
