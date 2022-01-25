package bia.interpreter

import bia.model.Declaration
import bia.model.Expression
import bia.model.FunctionBody
import bia.model.FunctionDeclaration
import bia.model.DefinedFunctionValue
import bia.model.Value
import bia.model.ValueDeclaration

fun evaluateProgramBody(
    programBody: FunctionBody,
): Value {
    val result = evaluateBody(
        outerScope = builtinScope,
        body = programBody,
    )

    return result
}

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
): Scope = when (declaration) {
    is ValueDeclaration -> executeValueDeclaration(
        scope = scope,
        declaration = declaration,
    )
    is FunctionDeclaration -> executeFunctionDeclaration(
        scope = scope,
        declaration = declaration,
    )
}

private fun executeValueDeclaration(
    scope: Scope,
    declaration: ValueDeclaration,
): Scope = scope.extend(
    name = declaration.givenName,
    value = evaluateExpression(
        scope = scope,
        expression = declaration.initializer,
    ),
)

private fun executeFunctionDeclaration(
    scope: Scope,
    declaration: FunctionDeclaration,
): Scope = object {
    val resultScope: Scope by lazy {
        scope.extend(
            name = declaration.givenName,
            value = DefinedFunctionValue(
                name = declaration.givenName,
                closure = Scope.delegated { resultScope },
                definition = declaration.definition,
            ),
        )
    }
}.resultScope

private fun evaluateExpression(
    scope: Scope,
    expression: Expression,
): Value = expression.evaluate(scope = scope)
