package bia.parser

import bia.model.BooleanType
import bia.model.NumberType
import bia.model.ObjectType
import bia.model.Type
import bia.model.TypeExpressionB
import bia.model.TypeReference
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

    @Test
    fun parseBasicTypeExpression() {
        assertEquals(
            expected = TypeReference(
                referredName = "Foo",
            ),
            actual = parseTypeExpressionB(
                source = "Foo",
            ),
        )
    }

    @Test
    fun parseTypeInstantiationTypeExpression() {
        assertEquals(
            expected = TypeReference(
                referredName = "Foo",
                passedTypeArguments = listOf(
                    NumberType,
                    TypeReference(referredName = "Bar"),
                ),
            ),
            actual = parseTypeExpressionB(
                source = "Foo<Number, Bar>",
            ),
        )
    }
}

private fun parseTypeExpression(
    source: String,
): Type = parseTypeExpressionB(source = source).build(
    scope = StaticScope.empty,
)

private fun parseTypeExpressionB(
    source: String,
): TypeExpressionB {
    val parser = buildAntlrParser(
        source = source,
        sourceName = "<type>",
    )

    return transformTypeExpression(
        typeExpression = parser.typeExpression(),
    )
}
