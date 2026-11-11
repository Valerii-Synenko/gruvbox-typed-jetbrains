package io.github.valeriisynenko.gruvboxtyped.python

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.view.IterationState
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.psi.PyReferenceExpression
import java.awt.Color

/**
 * The main technical risk: our annotation covers exactly the same range as `PY.BUILTIN_NAME`,
 * so the editor has to pick one foreground. These tests check the color the editor actually paints
 * (merged highlighter attributes), not just that our annotation exists.
 */
class PyTypeHintPriorityTest : BasePlatformTestCase() {

    private val typeHintColor = Color(0xFA, 0xBD, 0x2F)
    private val builtinColor = Color(0xFE, 0x80, 0x19)
    private val annotationColor = Color(0x11, 0x22, 0x33)

    fun `test type hint color wins over builtin name registered before`() {
        LanguageAnnotators.INSTANCE.addExplicitExtension(PythonLanguage.INSTANCE, BuiltinLikeAnnotator(), testRootDisposable)
        checkPriority()
    }

    fun `test type hint color wins over builtin name registered after`() {
        // Our annotator is registered from plugin-python.xml; register a second copy before the competitor
        // so both relative orders are exercised.
        LanguageAnnotators.INSTANCE.addExplicitExtension(PythonLanguage.INSTANCE, BuiltinLikeAnnotator(), testRootDisposable)
        LanguageAnnotators.INSTANCE.addExplicitExtension(PythonLanguage.INSTANCE, PyTypeHintAnnotator(), testRootDisposable)
        checkPriority()
    }

    fun `test builtin name keeps its color outside type hints`() {
        LanguageAnnotators.INSTANCE.addExplicitExtension(PythonLanguage.INSTANCE, BuiltinLikeAnnotator(), testRootDisposable)
        configure("def f(x: bool) -> int:\n    types = [bool]\n")

        assertEquals(builtinColor, foregroundAt("bool]"))
    }

    fun `test empty type hint color falls back to regular highlighting`() {
        LanguageAnnotators.INSTANCE.addExplicitExtension(PythonLanguage.INSTANCE, BuiltinLikeAnnotator(), testRootDisposable)
        configure("def f(x: bool) -> int: ...\n")
        myFixture.editor.colorsScheme.setAttributes(PyTypeHintHighlighting.TYPE_HINT, TextAttributes())

        // PyCharm's own PY.ANNOTATION and PY.BUILTIN_NAME tie on this range, so either of them may win
        assertContainsElements(listOf(builtinColor, annotationColor), foregroundAt("bool)"))
    }

    private fun checkPriority() {
        configure(
            """
            class AuthenticatedUser: ...

            def f(x: bool) -> AuthenticatedUser:
                return AuthenticatedUser()
            """.trimIndent()
        )

        assertEquals(typeHintColor, foregroundAt("bool)"))
        assertEquals(typeHintColor, foregroundAt("AuthenticatedUser:\n"))
    }

    private fun configure(text: String) {
        myFixture.configureByText("main.py", text)
        // The editor's own scheme delegate keeps per-editor overrides, so the global scheme stays untouched
        val scheme = myFixture.editor.colorsScheme
        scheme.setAttributes(PyTypeHintHighlighting.TYPE_HINT, attributes(typeHintColor))
        scheme.setAttributes(PyHighlighter.PY_BUILTIN_NAME, attributes(builtinColor))
        scheme.setAttributes(PyHighlighter.PY_ANNOTATION, attributes(annotationColor))
        myFixture.doHighlighting()
    }

    /** Foreground the editor paints at the first character of [marker] (the merged result of all highlighters). */
    private fun foregroundAt(marker: String): Color? {
        val offset = myFixture.editor.document.text.indexOf(marker)
        assertTrue("Marker not found: $marker", offset >= 0)
        val state = IterationState(myFixture.editor as EditorEx, offset, offset + 1, null, false, true, false, false)
        return state.mergedAttributes.foregroundColor
    }

    private fun attributes(color: Color) = TextAttributes().apply { foregroundColor = color }

    /** Same severity, range and key as `PyBuiltinHighlightingAnnotator`, without needing a Python SDK. */
    private class BuiltinLikeAnnotator : Annotator {
        override fun annotate(element: PsiElement, holder: AnnotationHolder) {
            if (element !is PyReferenceExpression || element.isQualified) return
            if (element.referencedName !in BUILTINS) return
            val nameElement = element.nameElement ?: return
            holder.newSilentAnnotation(HighlightInfoType.SYMBOL_TYPE_SEVERITY)
                .range(nameElement)
                .textAttributes(PyHighlighter.PY_BUILTIN_NAME)
                .create()
        }

        companion object {
            val BUILTINS = setOf("bool", "int", "str", "list", "dict", "print", "len")
        }
    }
}
