package bia.interpreter

import bia.model.expressions.IntLiteralExpression
import bia.model.NumberValue
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
                    expression = taggedExpression,
                    attachedTagName = "Foo",
                ),
            ).evaluate(scope = DynamicScope.empty),
        )
    }
}
