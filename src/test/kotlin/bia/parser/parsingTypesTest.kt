package bia.parser

import bia.model.BooleanType
import bia.model.CallExpression
import bia.model.Declaration
import bia.model.Expression
import bia.model.LessThenExpression
import bia.model.NumberType
import bia.model.ObjectType
import bia.model.ReferenceExpression
import bia.model.Type
import bia.model.TypeVariable
import bia.test_utils.parseExpression
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ParsingTypesTest {
    @Test
    fun parseObjectType() {
        assertEquals(
            expected = ObjectType(
                entries = mapOf(
                    "a" to NumberType,
                    "b" to BooleanType,
                )
            ),
            actual = parseTypeExpression(
                source = "{a : Number, b : Boolean}",
            ),
        )
    }
}

private fun parseTypeExpression(
    source: String,
): Type {
    val parser = buildAntlrParser(
        source = source,
        sourceName = "<type>",
    )

    return transformTypeExpression(
        scope = StaticScope.empty,
        typeExpression = parser.typeExpression(),
    )
}
