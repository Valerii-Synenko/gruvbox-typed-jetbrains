package io.github.valeriisynenko.gruvboxtyped.python

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Expected type hint ranges are marked with `<hint>...</hint>` in the source; everything else must stay unmarked.
 */
class PyTypeHintAnnotatorTest : BasePlatformTestCase() {

    fun `test parameter and return annotations`() = doTest(
        """
        class AuthenticatedUser: ...

        def login(name: <hint>str</hint>, admin: <hint>bool</hint>) -> <hint>AuthenticatedUser</hint>:
            return AuthenticatedUser()
        """
    )

    fun `test union with None`() = doTest(
        """
        def f(flag: <hint>bool</hint> | <hint>None</hint> = None) -> <hint>int</hint> | <hint>None</hint>:
            return None
        """
    )

    fun `test generic builtins`() = doTest(
        """
        def f(ids: <hint>list</hint>[<hint>int</hint>], index: <hint>dict</hint>[<hint>str</hint>, <hint>list</hint>[<hint>int</hint>]]) -> <hint>tuple</hint>[<hint>int</hint>, ...]:
            ...
        """
    )

    fun `test typing aliases and qualified names`() = doTest(
        """
        import typing
        from typing import Optional

        def f(a: <hint>Optional</hint>[<hint>str</hint>], b: <hint>typing</hint>.<hint>Optional</hint>[<hint>str</hint>]) -> <hint>typing</hint>.<hint>Any</hint>:
            ...
        """
    )

    fun `test variable and attribute annotations`() = doTest(
        """
        count: <hint>int</hint> = 0

        class Config:
            name: <hint>str</hint>
            tags: <hint>set</hint>[<hint>str</hint>] = set()

            def __init__(self) -> <hint>None</hint>:
                self.debug: <hint>bool</hint> = False
        """
    )

    fun `test string annotations`() = doTest(
        """
        class Node:
            def parent(self) -> "<hint>Node</hint>":
                ...

            def children(self) -> <hint>list</hint>["<hint>Node</hint>"]:
                ...
        """
    )

    fun `test explicit TypeAlias`() = doTest(
        """
        from typing import TypeAlias

        UserId: <hint>TypeAlias</hint> = <hint>int</hint>
        Matrix: <hint>TypeAlias</hint> = <hint>list</hint>[<hint>list</hint>[<hint>float</hint>]]
        plain = int
        """
    )

    fun `test PEP 695 type statement`() = doTest(
        """
        type Pair[<hint>T</hint>] = <hint>tuple</hint>[<hint>T</hint>, <hint>T</hint>]
        """
    )

    fun `test PEP 695 generic function and class`() = doTest(
        """
        def first[<hint>T</hint>](items: <hint>list</hint>[<hint>T</hint>]) -> <hint>T</hint>:
            return items[0]

        class Box[<hint>T</hint>: <hint>int</hint>]:
            def get(self) -> <hint>T</hint>:
                ...
        """
    )

    fun `test type comments`() = doTest(
        """
        x = []  # type: <hint>list</hint>[<hint>int</hint>]
        """
    )

    fun `test values outside type hints stay untouched`() = doTest(
        """
        from typing import Annotated

        MAX = 10

        def f(x: <hint>Annotated</hint>[<hint>int</hint>, Field(gt=MAX)], y: <hint>int</hint> = int("1")) -> <hint>bool</hint>:
            kinds = [int, str]
            return isinstance(x, bool)
        """
    )

    private fun doTest(markedText: String) {
        val (text, expected) = parseMarkup(markedText.trimIndent())
        myFixture.configureByText("main.py", text)

        val actual = myFixture.doHighlighting()
            .filter { it.forcedTextAttributesKey == PyTypeHintHighlighting.TYPE_HINT }
            .map { it.startOffset to it.endOffset }
            .distinct()
            .sortedBy { it.first }

        assertEquals(render(text, expected), render(text, actual))
    }

    /** Strips `<hint>` tags and returns the plain text with the expected ranges. */
    private fun parseMarkup(marked: String): Pair<String, List<Pair<Int, Int>>> {
        val text = StringBuilder()
        val ranges = mutableListOf<Pair<Int, Int>>()
        var start = -1
        var i = 0
        while (i < marked.length) {
            when {
                marked.startsWith(OPEN, i) -> { start = text.length; i += OPEN.length }
                marked.startsWith(CLOSE, i) -> { ranges += start to text.length; i += CLOSE.length }
                else -> text.append(marked[i++])
            }
        }
        return text.toString() to ranges
    }

    /** Readable form for assertion diffs: the source with ranges wrapped back in tags. */
    private fun render(text: String, ranges: List<Pair<Int, Int>>): String {
        val result = StringBuilder(text)
        for ((start, end) in ranges.sortedByDescending { it.first }) {
            result.insert(end, CLOSE)
            result.insert(start, OPEN)
        }
        return result.toString()
    }

    private companion object {
        const val OPEN = "<hint>"
        const val CLOSE = "</hint>"
    }
}
