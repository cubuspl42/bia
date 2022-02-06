package bia.parser

import bia.model.CallExpression
import bia.model.LessThenExpression
import bia.model.ReferenceExpression
import bia.model.TagExpression
import bia.model.TypeVariable
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
}

