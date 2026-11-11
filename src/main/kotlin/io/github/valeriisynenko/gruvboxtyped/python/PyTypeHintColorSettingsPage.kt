package io.github.valeriisynenko.gruvboxtyped.python

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.highlighting.PyHighlighter
import io.github.valeriisynenko.gruvboxtyped.GruvboxTypedBundle
import javax.swing.Icon

/**
 * Settings | Editor | Color Scheme | Python Type Hints.
 *
 * The platform has no API to add descriptors to the Python page itself, so the key lives on its own page,
 * listed right after "Python".
 */
class PyTypeHintColorSettingsPage : ColorSettingsPage {

    override fun getDisplayName(): String = GruvboxTypedBundle.message("color.settings.python.type.hints.page")

    override fun getIcon(): Icon? = PythonLanguage.INSTANCE.associatedFileType?.icon

    override fun getHighlighter(): SyntaxHighlighter =
        SyntaxHighlighterFactory.getSyntaxHighlighter(PythonLanguage.INSTANCE, null, null)

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor(GruvboxTypedBundle.message("color.settings.python.type.hint"), PyTypeHintHighlighting.TYPE_HINT),
        AttributesDescriptor(GruvboxTypedBundle.message("color.settings.python.builtin.type.hint"), PyTypeHintHighlighting.BUILTIN_TYPE_HINT),
        AttributesDescriptor(GruvboxTypedBundle.message("color.settings.python.builtin.name"), PyHighlighter.PY_BUILTIN_NAME),
        AttributesDescriptor(GruvboxTypedBundle.message("color.settings.python.parameter"), PyHighlighter.PY_PARAMETER),
        AttributesDescriptor(GruvboxTypedBundle.message("color.settings.python.function.definition"), PyHighlighter.PY_FUNC_DEFINITION),
        AttributesDescriptor(GruvboxTypedBundle.message("color.settings.python.class.definition"), PyHighlighter.PY_CLASS_DEFINITION),
    )

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = mapOf(
        "hint" to PyTypeHintHighlighting.TYPE_HINT,
        "bhint" to PyTypeHintHighlighting.BUILTIN_TYPE_HINT,
        "builtin" to PyHighlighter.PY_BUILTIN_NAME,
        "param" to PyHighlighter.PY_PARAMETER,
        "func" to PyHighlighter.PY_FUNC_DEFINITION,
        "class" to PyHighlighter.PY_CLASS_DEFINITION,
    )

    override fun getDemoText(): String = """
        from typing import Optional, TypeAlias

        type Pair[<hint>T</hint>] = <bhint>tuple</bhint>[<hint>T</hint>, <hint>T</hint>]
        UserId: <hint>TypeAlias</hint> = <bhint>int</bhint>


        class <class>AuthenticatedUser</class>:
            ...


        def <func>authenticate</func>(<param>token</param>: <bhint>str</bhint>, <param>strict</param>: <bhint>bool</bhint> | <bhint>None</bhint> = None) -> <hint>AuthenticatedUser</hint>:
            <builtin>print</builtin>(<builtin>len</builtin>(token))


        def <func>first</func>[<hint>T</hint>](<param>items</param>: <bhint>list</bhint>[<hint>T</hint>], <param>index</param>: <hint>Optional</hint>[<bhint>int</bhint>]) -> <hint>T</hint>:
            ...


        def <func>load</func>(<param>raw</param>: <bhint>dict</bhint>[<bhint>str</bhint>, <bhint>list</bhint>[<bhint>int</bhint>]]) -> "<hint>AuthenticatedUser</hint>":
            ...
    """.trimIndent()
}
