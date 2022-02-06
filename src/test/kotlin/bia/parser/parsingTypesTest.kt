package bia.parser

import bia.model.BooleanType
import bia.model.NumberType
import bia.model.ObjectType
import bia.model.Type
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
