package io.github.valeriisynenko.gruvboxtyped.python

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.PyAnnotation
import com.jetbrains.python.psi.PyAssignmentStatement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyNoneLiteralExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.PyTypeAliasStatement
import com.jetbrains.python.psi.PyTypeParameter

/**
 * Paints every name inside a Python type hint with [PyTypeHintHighlighting.TYPE_HINT], so that
 * `bool` (normally `PY.BUILTIN_NAME`) and `AuthenticatedUser` (normally `PY.ANNOTATION`) look the same.
 *
 * Purely syntactic (no resolve), so it is safe to run in dumb mode.
 */
class PyTypeHintAnnotator : Annotator, DumbAware {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val target = when (element) {
            is PyReferenceExpression -> element.nameElement?.psi
            is PyNoneLiteralExpression -> element
            is PyTypeParameter -> element.nameIdentifier
            else -> null
        } ?: return

        if (!isInTypeHint(element)) return

        // TEXT_ATTRIBUTES puts the highlighter on a higher layer than INFORMATION-level annotations
        // (PY.BUILTIN_NAME, PY.ANNOTATION, ...), so the type hint color wins regardless of annotator order,
        // while warnings and errors (unresolved / unused references) still stay on top.
        holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
            .range(target)
            .textAttributes(PyTypeHintHighlighting.TYPE_HINT)
            .create()
    }

    private fun isInTypeHint(element: PsiElement): Boolean {
        var current: PsiElement? = element
        var child: PsiElement = element
        while (current != null) {
            when (current) {
                // Metadata inside Annotated[int, Field(gt=0)] is a value expression, not a type
                is PyCallExpression -> return false
                is PyAnnotation, is PyTypeParameter -> return true
                // `type Alias = ...`: only the aliased expression, not the alias name
                is PyTypeAliasStatement -> return child == current.typeExpression
                // `Alias: TypeAlias = ...`
                is PyAssignmentStatement -> return child == current.assignedValue && isExplicitTypeAlias(current)
                // String annotations ("Foo") and `# type:` comments are parsed as injected Python fragments
                is PyStatement -> return (current.parent as? PsiFile)?.let(::isTypeHintInjectionHost) ?: false
                is PsiFile -> return isTypeHintInjectionHost(current)
            }
            child = current
            current = current.parent
        }
        return false
    }

    private fun isExplicitTypeAlias(assignment: PyAssignmentStatement): Boolean {
        val target = assignment.targets.singleOrNull() as? PyTargetExpression ?: return false
        val annotationValue = target.annotation?.value as? PyReferenceExpression ?: return false
        return annotationValue.referencedName == "TypeAlias"
    }

    private fun isTypeHintInjectionHost(file: PsiFile): Boolean {
        val host = InjectedLanguageManager.getInstance(file.project).getInjectionHost(file) ?: return false
        if (host is PsiComment) return TYPE_COMMENT.containsMatchIn(host.text)
        return isInTypeHint(host)
    }

    private companion object {
        val TYPE_COMMENT = Regex("""^#\s*type:""")
    }
}
