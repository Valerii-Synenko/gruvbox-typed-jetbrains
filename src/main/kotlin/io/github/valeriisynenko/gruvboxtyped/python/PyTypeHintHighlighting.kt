package io.github.valeriisynenko.gruvboxtyped.python

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.jetbrains.python.highlighting.PyHighlighter

object PyTypeHintHighlighting {
    /**
     * Classes and other non-built-in names inside a type hint (`AuthenticatedUser`, `UUID`, `Optional`, `T`, ...).
     * Schemes that don't define it fall back to the class reference color.
     */
    @JvmField
    val TYPE_HINT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("PY_TYPE_HINT", DefaultLanguageHighlighterColors.CLASS_REFERENCE)

    /**
     * Built-in types inside a type hint (`bool`, `list`, `dict`, `None`, ...).
     * Schemes that don't define it fall back to the regular built-in name color.
     */
    @JvmField
    val BUILTIN_TYPE_HINT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("PY_BUILTIN_TYPE_HINT", PyHighlighter.PY_BUILTIN_NAME)
}
