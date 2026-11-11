package io.github.valeriisynenko.gruvboxtyped

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.JDOMUtil
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.psi.PyReferenceExpression
import io.github.valeriisynenko.gruvboxtyped.python.PyTypeHintHighlighting
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Renders the README "before / after" images from demo/type_hints.py with the bundled color scheme.
 * Runs only with `./gradlew test --tests '*ReadmeScreenshotsTest*' -Pscreenshots`.
 */
class ReadmeScreenshotsTest : BasePlatformTestCase() {

    fun `test render readme screenshots`() {
        val outputDir = System.getProperty("gruvbox.screenshots.dir") ?: return
        val demo = File(System.getProperty("gruvbox.demo.file")).readText()

        // Test projects have no Python SDK, so built-in names are marked the way PyBuiltinHighlightingAnnotator does it
        LanguageAnnotators.INSTANCE.addExplicitExtension(PythonLanguage.INSTANCE, BuiltinNamesAnnotator(), testRootDisposable)

        render(demo, File(outputDir, "python-after.png")) {}
        render(demo, File(outputDir, "python-before.png")) {
            // Type hint color turned off: PyCharm's own PY.ANNOTATION / PY.BUILTIN_NAME tie decides again
            setAttributes(PyTypeHintHighlighting.TYPE_HINT, TextAttributes())
        }
    }

    private fun render(text: String, file: File, customize: EditorColorsSchemeImpl.() -> Unit) {
        myFixture.configureByText("type_hints.py", text)
        val editor = myFixture.editor as EditorEx
        editor.colorsScheme = loadScheme().apply {
            editorFontName = "JetBrains Mono"
            editorFontSize = 14
            customize()
        }
        editor.settings.apply {
            isLineNumbersShown = true
            isCaretRowShown = false
            isIndentGuidesShown = false
            isRightMarginShown = false
        }
        myFixture.doHighlighting()
        editor.foldingModel.runBatchFoldingOperation { editor.foldingModel.allFoldRegions.forEach { it.isExpanded = true } }
        editor.caretModel.moveToOffset(0)
        editor.scrollingModel.scrollVertically(0)

        val scale = 2
        val width = 760
        val height = editor.lineHeight * (editor.document.lineCount + 1)
        val component = editor.component
        component.setSize(width, height)
        layoutRecursively(component)

        val image = BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            scale(scale.toDouble(), scale.toDouble())
            component.paint(this)
            dispose()
        }
        file.parentFile.mkdirs()
        ImageIO.write(image, "png", file)
    }

    private fun layoutRecursively(component: java.awt.Component) {
        component.doLayout()
        if (component is java.awt.Container) component.components.forEach(::layoutRecursively)
    }

    private fun loadScheme(): EditorColorsSchemeImpl {
        val element = JDOMUtil.load(javaClass.getResourceAsStream("/colors/Gruvbox Typed Dark Medium.xml")!!)
        val parent = EditorColorsManager.getInstance().getScheme("Darcula")!!
        return EditorColorsSchemeImpl(parent).apply { readExternal(element) }
    }

    private class BuiltinNamesAnnotator : Annotator {
        override fun annotate(element: PsiElement, holder: AnnotationHolder) {
            if (element !is PyReferenceExpression || element.isQualified || element.referencedName !in BUILTINS) return
            holder.newSilentAnnotation(HighlightInfoType.SYMBOL_TYPE_SEVERITY)
                .range(element.nameElement ?: return)
                .textAttributes(PyHighlighter.PY_BUILTIN_NAME)
                .create()
        }

        companion object {
            val BUILTINS = setOf("bool", "int", "str", "list", "dict", "tuple", "len", "print", "ValueError")
        }
    }
}
