package bia.interpreter

import bia.model.Declaration
import bia.model.Expression
import bia.model.FunctionBody
import bia.model.FunctionDeclaration
import bia.model.FunctionValue
import bia.model.Value
import bia.model.ValueDeclaration

fun evaluateBody(
    outerScope: Scope,
    body: FunctionBody,
): Value {
    val finalScope = body.declarations.fold(outerScope) { scope, declaration ->
        executeDeclaration(scope = scope, declaration = declaration)
    }

    val returnedValue = evaluateExpression(
        scope = finalScope,
        expression = body.returned,
    )

    return returnedValue
}

private fun executeDeclaration(
    scope: Scope,
    declaration: Declaration,
): Scope = scope.extend(
    name = declaration.givenName,
    value = when (declaration) {
        is ValueDeclaration -> evaluateExpression(
            scope = scope,
            expression = declaration.initializer,
        )
        is FunctionDeclaration -> FunctionValue(
            closure = scope,
            definition = declaration.definition,
        )
    },
)

private fun evaluateExpression(
    scope: Scope,
    expression: Expression,
): Value = expression.evaluate(scope = scope)
