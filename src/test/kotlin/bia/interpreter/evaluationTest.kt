package bia.interpreter

import bia.model.BooleanValue
import bia.model.NumberValue
import bia.model.TaggedValue
import bia.model.Value
import bia.model.expressions.IntLiteralExpression
import bia.model.expressions.MatchBranch
import bia.model.expressions.MatchExpression
import bia.model.expressions.ReferenceExpression
import bia.model.expressions.TagExpression
import bia.model.expressions.UntagExpression
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class EvaluationTest {
    @Test
    fun testUntag() {
        val taggedExpression = IntLiteralExpression(123)

        assertEquals(
            expected = NumberValue(123.0),
            actual = UntagExpression(
                untagee = TagExpression(
                    taggedExpression = taggedExpression,
                    attachedTagName = "Foo",
                ),
            ).evaluate(scope = DynamicScope.empty),
        )
    }

    @Test
    fun testMatchExpressionMatchingBranch() {
        val evaluatedValue: Value = MatchExpression(
            matchee = ReferenceExpression(
                referredName = "arg",
                referredDeclaration = null,
            ),
            taggedBranches = listOf(
                MatchBranch(
                    requiredTagName = "Foo",
                    branch = IntLiteralExpression(value = 10),
                ),
                MatchBranch(
                    requiredTagName = "Bar",
                    branch = IntLiteralExpression(value = 20),
                ),
            ),
            elseBranch = null,
        ).evaluate(
            scope = DynamicScope.of(
                values = mapOf(
                    "arg" to TaggedValue(
                        taggedValue = BooleanValue(value = true),
                        tag = "Foo",
                    ),
                ),
            ),
        )

        assertEquals(
            expected = NumberValue(value = 10.0),
            actual = evaluatedValue,
        )
    }

    @Test
    fun testMatchExpressionElseBranch() {
        val evaluatedValue: Value = MatchExpression(
            matchee = ReferenceExpression(
                referredName = "arg",
                referredDeclaration = null,
            ),
            taggedBranches = listOf(
                MatchBranch(
                    requiredTagName = "Foo",
                    branch = IntLiteralExpression(value = 10),
                ),
                MatchBranch(
                    requiredTagName = "Bar",
                    branch = IntLiteralExpression(value = 20),
                ),
            ),
            elseBranch = IntLiteralExpression(value = 30),
        ).evaluate(
            scope = DynamicScope.of(
                values = mapOf(
                    "arg" to TaggedValue(
                        taggedValue = BooleanValue(value = true),
                        tag = "Baz",
                    ),
                ),
            ),
        )

        assertEquals(
            expected = NumberValue(value = 30.0),
            actual = evaluatedValue,
        )
    }
}
