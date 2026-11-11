package io.github.valeriisynenko.gruvboxtyped.python

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Expected ranges are marked in the source: `<hint>` for [PyTypeHintHighlighting.TYPE_HINT],
 * `<bhint>` for [PyTypeHintHighlighting.BUILTIN_TYPE_HINT]; everything else must stay unmarked.
 */
class PyTypeHintAnnotatorTest : BasePlatformTestCase() {

    fun `test parameter and return annotations`() = doTest(
        """
        class AuthenticatedUser: ...

        def login(name: <bhint>str</bhint>, admin: <bhint>bool</bhint>) -> <hint>AuthenticatedUser</hint>:
            return AuthenticatedUser()
        """
    )

    fun `test union with None`() = doTest(
        """
        def f(flag: <bhint>bool</bhint> | <bhint>None</bhint> = None) -> <bhint>int</bhint> | <bhint>None</bhint>:
            return None
        """
    )

    fun `test generic builtins`() = doTest(
        """
        def f(ids: <bhint>list</bhint>[<bhint>int</bhint>], index: <bhint>dict</bhint>[<bhint>str</bhint>, <bhint>list</bhint>[<bhint>int</bhint>]]) -> <bhint>tuple</bhint>[<bhint>int</bhint>, ...]:
            ...
        """
    )

    fun `test typing aliases and qualified names`() = doTest(
        """
        import typing
        from typing import Optional

        def f(a: <hint>Optional</hint>[<bhint>str</bhint>], b: <hint>typing</hint>.<hint>Optional</hint>[<bhint>str</bhint>]) -> <hint>typing</hint>.<hint>Any</hint>:
            ...
        """
    )

    fun `test variable and attribute annotations`() = doTest(
        """
        count: <bhint>int</bhint> = 0

        class Config:
            name: <bhint>str</bhint>
            tags: <bhint>set</bhint>[<bhint>str</bhint>] = set()

            def __init__(self) -> <bhint>None</bhint>:
                self.debug: <bhint>bool</bhint> = False
        """
    )

    fun `test string annotations`() = doTest(
        """
        class Node:
            def parent(self) -> "<hint>Node</hint>":
                ...

            def children(self) -> <bhint>list</bhint>["<hint>Node</hint>"]:
                ...
        """
    )

    fun `test explicit TypeAlias`() = doTest(
        """
        from typing import TypeAlias

        UserId: <hint>TypeAlias</hint> = <bhint>int</bhint>
        Matrix: <hint>TypeAlias</hint> = <bhint>list</bhint>[<bhint>list</bhint>[<bhint>float</bhint>]]
        plain = int
        """
    )

    fun `test PEP 695 type statement`() = doTest(
        """
        type Pair[<hint>T</hint>] = <bhint>tuple</bhint>[<hint>T</hint>, <hint>T</hint>]
        """
    )

    fun `test PEP 695 generic function and class`() = doTest(
        """
        def first[<hint>T</hint>](items: <bhint>list</bhint>[<hint>T</hint>]) -> <hint>T</hint>:
            return items[0]

        class Box[<hint>T</hint>: <bhint>int</bhint>]:
            def get(self) -> <hint>T</hint>:
                ...
        """
    )

    fun `test type comments`() = doTest(
        """
        x = []  # type: <bhint>list</bhint>[<bhint>int</bhint>]
        """
    )

    fun `test values outside type hints stay untouched`() = doTest(
        """
        from typing import Annotated

        MAX = 10

        def f(x: <hint>Annotated</hint>[<bhint>int</bhint>, Field(gt=MAX)], y: <bhint>int</bhint> = int("1")) -> <bhint>bool</bhint>:
            kinds = [int, str]
            return isinstance(x, bool)
        """
    )

    fun `test built-in exceptions and qualified names`() = doTest(
        """
        import builtins

        def f(a: <hint>builtins</hint>.<hint>list</hint>[<bhint>int</bhint>]) -> <bhint>ExceptionGroup</bhint>[<bhint>ValueError</bhint>]:
            ...
        """
    )

    private fun doTest(markedText: String) {
        val (text, expected) = parseMarkup(markedText.trimIndent())
        myFixture.configureByText("main.py", text)

        val actual = myFixture.doHighlighting()
            .mapNotNull { info -> TAGS.entries.find { it.value == info.forcedTextAttributesKey }?.let { Range(info.startOffset, info.endOffset, it.key) } }
            .distinct()
            .sortedBy { it.start }

        assertEquals(render(text, expected), render(text, actual))
    }

    private data class Range(val start: Int, val end: Int, val tag: String)

    /** Strips the tags and returns the plain text with the expected ranges. */
    private fun parseMarkup(marked: String): Pair<String, List<Range>> {
        val text = StringBuilder()
        val ranges = mutableListOf<Range>()
        var start = -1
        var i = 0
        while (i < marked.length) {
            val open = TAGS.keys.find { marked.startsWith("<$it>", i) }
            val close = TAGS.keys.find { marked.startsWith("</$it>", i) }
            when {
                open != null -> { start = text.length; i += open.length + 2 }
                close != null -> { ranges += Range(start, text.length, close); i += close.length + 3 }
                else -> text.append(marked[i++])
            }
        }
        return text.toString() to ranges
    }

    /** Readable form for assertion diffs: the source with ranges wrapped back in tags. */
    private fun render(text: String, ranges: List<Range>): String {
        val result = StringBuilder(text)
        for (range in ranges.sortedByDescending { it.start }) {
            result.insert(range.end, "</${range.tag}>")
            result.insert(range.start, "<${range.tag}>")
        }
        return result.toString()
    }

    private companion object {
        val TAGS = mapOf("hint" to PyTypeHintHighlighting.TYPE_HINT, "bhint" to PyTypeHintHighlighting.BUILTIN_TYPE_HINT)
    }
}
