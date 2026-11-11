package io.github.valeriisynenko.gruvboxtyped.python

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object PyTypeHintHighlighting {
    /**
     * Single color for every name inside a type hint (`bool`, `list`, `AuthenticatedUser`, `T`, ...).
     * Schemes that don't define it fall back to the class reference color.
     */
    @JvmField
    val TYPE_HINT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("PY_TYPE_HINT", DefaultLanguageHighlighterColors.CLASS_REFERENCE)
}
