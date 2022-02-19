package bia.model

import bia.interpreter.DynamicScope
import bia.interpreter.executeValueDeclaration
import bia.model.expressions.BooleanLiteralExpression
import bia.model.expressions.CallExpression
import bia.model.expressions.CallExpressionB
import bia.model.expressions.EqualsExpressionB
import bia.model.expressions.IfExpressionB
import bia.model.expressions.IntLiteralExpression
import bia.model.expressions.LambdaExpressionB
import bia.model.expressions.ReferenceExpressionB
import bia.model.expressions.SubtractionExpressionB
import bia.parser.ClosedDeclaration
import bia.parser.StaticScope
import bia.type_checker.TypeCheckError
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.exp
import kotlin.test.assertEquals

private fun buildSimpleRecursiveFunctionDeclaration(
    explicitReturnType: TypeExpressionB?,
) = ValDeclarationB(
    givenName = "f",
    initializer = LambdaExpressionB(
        typeVariables = emptyList(),
        argumentListDeclaration = BasicArgumentListDeclarationB(
            argumentDeclarations = listOf(
                ArgumentDeclarationB(
                    givenName = "n",
                    argumentType = NumberType,
                ),
            ),
        ),
        body = FunctionBodyB(
            definitions = emptyList(),
            returned = IfExpressionB(
                guard = EqualsExpressionB(
                    left = ReferenceExpressionB(referredName = "n"),
                    right = IntLiteralExpression(value = 0),
                ),
                trueBranch = BooleanLiteralExpression(value = true),
                falseBranch = CallExpressionB(
                    callee = ReferenceExpressionB(referredName = "f"),
                    explicitTypeArguments = null,
                    arguments = listOf(
                        SubtractionExpressionB(
                            minuend = ReferenceExpressionB(referredName = "n"),
                            subtrahend = IntLiteralExpression(value = 1),
                        ),
                    ),
                ),
            ),
        ),
        explicitReturnType = explicitReturnType,
    ),
)

internal class RecursiveFunctionDeclarationTest {
    @Test
    fun testRecursiveFunctionDeclarationTypeCheck() {
        val built = buildSimpleRecursiveFunctionDeclaration(
            explicitReturnType = BooleanType,
        ).build(scope = StaticScope.empty)

        val valDeclaration = built.valDeclaration
        val extendedScope = built.extendedScope

        valDeclaration.validate()

        assertEquals(
            expected = FunctionType(
                typeArguments = emptyList(),
                argumentListDeclaration = BasicArgumentListDeclaration(
                    argumentDeclarations = listOf(
                        ArgumentDeclaration(
                            givenName = "n",
                            argumentType = NumberType,
                        ),
                    ),
                ),
                returnType = BooleanType,
            ),
            actual = valDeclaration.valueType,
        )

        assertEquals(
            expected = mapOf(
                "f" to ClosedDeclaration(
                    declaration = valDeclaration,
                ),
            ),
            actual = extendedScope.declarations,
        )
    }

    @Test
    fun testRecursiveFunctionDeclarationTypeInferenceLoop() {
        assertThrows<TypeCheckError> {
            val built = buildSimpleRecursiveFunctionDeclaration(
                explicitReturnType = null,
            ).build(scope = StaticScope.empty)

            built.valDeclaration.valueType
        }
    }

    @Test
    @Disabled // TODO: Make this pass
    fun testRecursiveFunctionDeclarationEvaluation() {
        val built = buildSimpleRecursiveFunctionDeclaration(
            explicitReturnType = null,
        ).build(scope = StaticScope.empty)

        val valDeclaration = built.valDeclaration

        val extendedScope = executeValueDeclaration(
            scope = DynamicScope.empty,
            declaration = valDeclaration,
        )

        val function = extendedScope.getValue(name = "f")!!.asFunctionValue()

        val result = function.call(
            arguments = listOf(NumberValue(value = 10.0)),
        )

        assertEquals(
            expected = BooleanValue(value = true),
            actual = result,
        )
    }
}
