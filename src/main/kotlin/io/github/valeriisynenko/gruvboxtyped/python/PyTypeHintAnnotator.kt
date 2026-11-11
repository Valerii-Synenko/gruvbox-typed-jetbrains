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
 * Gives names inside Python type hints stable colors: built-in types (`bool`, `list`, `None`) get
 * [PyTypeHintHighlighting.BUILTIN_TYPE_HINT], everything else (`AuthenticatedUser`, `UUID`, `T`) gets
 * [PyTypeHintHighlighting.TYPE_HINT]. Without it PyCharm's `PY.BUILTIN_NAME` and `PY.ANNOTATION` tie on the
 * same range and the visible color depends on highlighter order.
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
        val key = if (isBuiltinType(element)) PyTypeHintHighlighting.BUILTIN_TYPE_HINT else PyTypeHintHighlighting.TYPE_HINT

        // TEXT_ATTRIBUTES puts the highlighter on a higher layer than INFORMATION-level annotations
        // (PY.BUILTIN_NAME, PY.ANNOTATION, ...), so the type hint color wins regardless of annotator order,
        // while warnings and errors (unresolved / unused references) still stay on top.
        holder.newSilentAnnotation(HighlightSeverity.TEXT_ATTRIBUTES)
            .range(target)
            .textAttributes(key)
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

    private fun isBuiltinType(element: PsiElement): Boolean = when (element) {
        is PyNoneLiteralExpression -> true
        is PyReferenceExpression -> !element.isQualified && element.referencedName in BUILTIN_TYPES
        else -> false
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

        /** Classes of Python's `builtins` module, the names PyCharm paints as `PY.BUILTIN_NAME`. */
        val BUILTIN_TYPES = setOf(
            "bool", "bytearray", "bytes", "classmethod", "complex", "dict", "enumerate", "filter", "float",
            "frozendict", "frozenset", "int", "list", "map", "memoryview", "object", "property", "range",
            "reversed", "sentinel", "set", "slice", "staticmethod", "str", "super", "tuple", "type", "zip",
            "ArithmeticError", "AssertionError", "AttributeError", "BaseException", "BaseExceptionGroup",
            "BlockingIOError", "BrokenPipeError", "BufferError", "BytesWarning", "ChildProcessError",
            "ConnectionAbortedError", "ConnectionError", "ConnectionRefusedError", "ConnectionResetError",
            "DeprecationWarning", "EOFError", "EncodingWarning", "EnvironmentError", "Exception", "ExceptionGroup",
            "FileExistsError", "FileNotFoundError", "FloatingPointError", "FutureWarning", "GeneratorExit",
            "IOError", "ImportCycleError", "ImportError", "ImportWarning", "IndentationError", "IndexError",
            "InterruptedError", "IsADirectoryError", "KeyError", "KeyboardInterrupt", "LookupError", "MemoryError",
            "ModuleNotFoundError", "NameError", "NotADirectoryError", "NotImplementedError", "OSError",
            "OverflowError", "PendingDeprecationWarning", "PermissionError", "ProcessLookupError",
            "PythonFinalizationError", "RecursionError", "ReferenceError", "ResourceWarning", "RuntimeError",
            "RuntimeWarning", "StopAsyncIteration", "StopIteration", "SyntaxError", "SyntaxWarning", "SystemError",
            "SystemExit", "TabError", "TimeoutError", "TypeError", "UnboundLocalError", "UnicodeDecodeError",
            "UnicodeEncodeError", "UnicodeError", "UnicodeTranslateError", "UnicodeWarning", "UserWarning",
            "ValueError", "Warning", "ZeroDivisionError",
        )
    }
}
