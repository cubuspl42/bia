package bia.parser

import bia.model.expressions.CallExpression
import bia.model.expressions.LessThenExpression
import bia.model.expressions.MatchBranch
import bia.model.expressions.MatchExpression
import bia.model.expressions.ReferenceExpression
import bia.model.expressions.TagExpression
import bia.model.TypeVariable
import bia.model.expressions.UntagExpression
import bia.test_utils.parseExpression
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ParsingExpressionsTest {
    @Test
    fun parseLt() {
        assertEquals(
            expected = LessThenExpression(
                left = ReferenceExpression(
                    referredName = "a",
                    referredDeclaration = null,
                ),
                right = ReferenceExpression(
                    referredName = "b",
                    referredDeclaration = null,
                ),
            ),
            actual = parseExpression(
                source = "a < b",
            ),
        )
    }

    @Test
    fun parseBasicGenericCall() {
        val aTv = TypeVariable(givenName = "A", id = 0)

        assertEquals(
            expected = CallExpression(
                callee = ReferenceExpression(
                    referredName = "f",
                    referredDeclaration = null,
                ),
                typeArguments = listOf(aTv),
                arguments = listOf(
                    ReferenceExpression(
                        referredName = "a",
                        referredDeclaration = null,
                    ),
                ),
            ),
            actual = parseExpression(
                scopeTypeVariables = listOf(aTv),
                // Please note that this has a potential for being parsed as
                // f less-then A greater-then (a)
                source = "f<A>(a)",
            ),
        )
    }

    @Test
    fun parseParenGenericCall() {
        val aTv = TypeVariable(givenName = "A", id = 0)

        assertEquals(
            expected = CallExpression(
                callee = LessThenExpression(
                    left = ReferenceExpression(
                        referredName = "a",
                        referredDeclaration = null,
                    ),
                    right = ReferenceExpression(
                        referredName = "b",
                        referredDeclaration = null,
                    ),
                ),
                typeArguments = listOf(aTv),
                arguments = listOf(
                    ReferenceExpression(
                        referredName = "c",
                        referredDeclaration = null,
                    ),
                ),
            ),
            actual = parseExpression(
                scopeTypeVariables = listOf(aTv),
                source = "(a < b)<A>(c)",
            ),
        )
    }

    @Test
    fun parseTag() {
        assertEquals(
            expected = TagExpression(
                expression = ReferenceExpression(
                    referredName = "foo",
                    referredDeclaration = null,
                ),
                attachedTagName = "Tag1",
            ),
            actual = parseExpression(
                scopeTypeVariables = listOf(),
                source = "foo # Tag1",
            ),
        )
    }

    @Test
    fun parseUntag() {
        assertEquals(
            expected = UntagExpression(
                expression = ReferenceExpression(
                    referredName = "foo",
                    referredDeclaration = null,
                ),
            ),
            actual = parseExpression(
                scopeTypeVariables = listOf(),
                source = "untag foo",
            ),
        )
    }

    @Test
    fun parseMatch() {
        assertEquals(
            expected = MatchExpression(
                matchee = ReferenceExpression(
                    referredName = "expr",
                    referredDeclaration = null,
                ),
                taggedBranches = listOf(
                    MatchBranch(
                        requiredTagName = "Foo",
                        branch = ReferenceExpression(
                            referredName = "foo",
                            referredDeclaration = null,
                        ),
                    ),
                    MatchBranch(
                        requiredTagName = "Bar",
                        branch = ReferenceExpression(
                            referredName = "bar",
                            referredDeclaration = null,
                        ),
                    ),
                ),
                elseBranch = ReferenceExpression(
                    referredName = "baz",
                    referredDeclaration = null,
                ),
            ),
            actual = parseExpression(
                scopeTypeVariables = listOf(),
                source = """
                    match expr {
                        case Foo => foo
                        case Bar => bar
                        else => baz
                    }
                """.trimIndent(),
            ),
        )
    }
}
