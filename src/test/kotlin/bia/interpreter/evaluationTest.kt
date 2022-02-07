package bia.interpreter

import bia.model.IntLiteralExpression
import bia.model.NumberValue
import bia.model.TagExpression
import bia.model.UntagExpression
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class EvaluationTest {
    @Test
    fun testUntag() {
        val taggedExpression = IntLiteralExpression(123)

        assertEquals(
            expected = NumberValue(123.0),
            actual = UntagExpression(
                expression = TagExpression(
                    expression = taggedExpression,
                    attachedTagName = "Foo",
                ),
            ).evaluate(scope = DynamicScope.empty),
        )
    }
}
