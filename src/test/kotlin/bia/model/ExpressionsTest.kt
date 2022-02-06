package bia.model

import bia.interpreter.DynamicScope
import bia.interpreter.executeValueDeclaration
import bia.parser.ClosedDeclaration
import bia.test_utils.parseExpression
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExpressionsTest {
    @Test
    fun testObjectLiteral() {
        val xDeclaration = ValDeclaration(
            givenName = "x",
            initializer = IntLiteralExpression(value = -1),
        )

        val expression = parseExpression(
            scopeDeclarations = listOf(
                xDeclaration,
            ),
            source = "{a = 1, b = x, c = false}",
        )

        assertIs<ObjectLiteralExpression>(expression)

        assertEquals(
            expected = ObjectLiteralExpression(
                entries = mapOf(
                    "a" to IntLiteralExpression(
                        value = 1L,
                    ),
                    "b" to ReferenceExpression(
                        referredName = "x",
                        referredDeclaration = ClosedDeclaration(xDeclaration),
                    ),
                    "c" to BooleanLiteralExpression(
                        value = false,
                    ),
                )
            ),
            actual = expression,
        )

        expression.validate()

        assertEquals(
            expected = ObjectType(
                entries = mapOf(
                    "a" to NumberType,
                    "b" to NumberType,
                    "c" to BooleanType,
                )
            ),
            actual = expression.type,
        )

        assertEquals(
            expected = ObjectValue(
                entries = mapOf(
                    "a" to NumberValue(1.0),
                    "b" to NumberValue(2.0),
                    "c" to BooleanValue(false),
                )
            ),
            actual = expression.evaluate(
                DynamicScope.of(
                    values = mapOf(
                        "x" to NumberValue(2.0),
                    ),
                ),
            ),
        )
    }

    @Test
    fun testObjectFieldRead() {
        val objectDeclaration = ValDeclaration(
            givenName = "obj",
            initializer = ObjectLiteralExpression(
                entries = mapOf(
                    "a" to IntLiteralExpression(
                        value = 1L,
                    ),
                    "b" to IntLiteralExpression(
                        value = 2L,
                    ),
                    "c" to IntLiteralExpression(
                        value = 3L,
                    ),
                )
            ),
        )

        val expression = parseExpression(
            scopeDeclarations = listOf(
                objectDeclaration,
            ),
            source = "obj.a",
        )

        assertIs<ObjectFieldReadExpression>(expression)

        assertEquals(
            expected = ObjectFieldReadExpression(
                objectExpression = ReferenceExpression(
                    referredName = "obj",
                    referredDeclaration = ClosedDeclaration(objectDeclaration),
                ),
                readFieldName = "a",
            ),
            actual = expression,
        )

        expression.validate()

        assertEquals(
            expected = NumberType,
            actual = expression.type,
        )

        assertEquals(
            expected = NumberValue(1.0),
            actual = expression.evaluate(
                scope = executeValueDeclaration(
                    scope = DynamicScope.empty,
                    declaration = objectDeclaration,
                ),
            ),
        )
    }

    @Test
    fun testIs() {
        val unionDeclaration = UnionDeclaration(
            unionName = "Union1",
            unionType = WideUnionType(
                alternatives = setOf(
                    UnionAlternative(
                        tagName = "Tag1",
                        type = NumberType,
                    ),
                    UnionAlternative(
                        tagName = "Tag2",
                        type = BooleanType,
                    ),
                ),
            ),
        )

        val argumentDeclaration = ArgumentDeclaration(
            givenName = "arg1",
            valueType = unionDeclaration.unionType,
        )

        val expression = parseExpression(
            scopeDeclarations = listOf(
                argumentDeclaration,
            ),
            source = "arg1 is Tag1",
        )

        assertIs<IsExpression>(expression)

        assertEquals(
            expected = IsExpression(
                expression = ReferenceExpression(
                    referredName = "arg1",
                    referredDeclaration = ClosedDeclaration(argumentDeclaration),
                ),
                checkedTagName = "Tag1",
            ),
            actual = expression,
        )

        expression.validate()
    }
}
