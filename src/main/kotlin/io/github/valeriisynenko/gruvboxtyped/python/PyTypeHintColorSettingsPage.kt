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
        AttributesDescriptor(GruvboxTypedBundle.message("color.settings.python.builtin.name"), PyHighlighter.PY_BUILTIN_NAME),
        AttributesDescriptor(GruvboxTypedBundle.message("color.settings.python.parameter"), PyHighlighter.PY_PARAMETER),
        AttributesDescriptor(GruvboxTypedBundle.message("color.settings.python.function.definition"), PyHighlighter.PY_FUNC_DEFINITION),
        AttributesDescriptor(GruvboxTypedBundle.message("color.settings.python.class.definition"), PyHighlighter.PY_CLASS_DEFINITION),
    )

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = mapOf(
        "hint" to PyTypeHintHighlighting.TYPE_HINT,
        "builtin" to PyHighlighter.PY_BUILTIN_NAME,
        "param" to PyHighlighter.PY_PARAMETER,
        "func" to PyHighlighter.PY_FUNC_DEFINITION,
        "class" to PyHighlighter.PY_CLASS_DEFINITION,
    )

    override fun getDemoText(): String = """
        from typing import Optional, TypeAlias

        type <hint>Pair</hint>[<hint>T</hint>] = tuple[<hint>T</hint>, <hint>T</hint>]
        <hint>UserId</hint>: <hint>TypeAlias</hint> = <hint>int</hint>


        class <class>AuthenticatedUser</class>:
            ...


        def <func>authenticate</func>(<param>token</param>: <hint>str</hint>, <param>strict</param>: <hint>bool</hint> | <hint>None</hint> = None) -> <hint>AuthenticatedUser</hint>:
            <builtin>print</builtin>(<builtin>len</builtin>(token))


        def <func>first</func>[<hint>T</hint>](<param>items</param>: <hint>list</hint>[<hint>T</hint>], <param>index</param>: <hint>Optional</hint>[<hint>int</hint>]) -> <hint>T</hint>:
            ...


        def <func>load</func>(<param>raw</param>: <hint>dict</hint>[<hint>str</hint>, <hint>list</hint>[<hint>int</hint>]]) -> "<hint>AuthenticatedUser</hint>":
            ...
    """.trimIndent()
}
