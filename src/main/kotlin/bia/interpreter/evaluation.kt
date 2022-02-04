package bia.interpreter

import bia.model.BasicArgumentListDeclaration
import bia.model.BodyDeclaration
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
    outerScope: DynamicScope,
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
    scope: DynamicScope,
    declaration: BodyDeclaration,
): DynamicScope = when (declaration) {
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
    scope: DynamicScope,
    declaration: ValueDeclaration,
): DynamicScope = scope.extend(
    name = declaration.givenName,
    value = evaluateExpression(
        scope = scope,
        expression = declaration.initializer,
    ),
)

private fun executeFunctionDeclaration(
    scope: DynamicScope,
    declaration: FunctionDeclaration,
): DynamicScope {
    val body = declaration.body

    val argumentDeclarations = (declaration.argumentListDeclaration as BasicArgumentListDeclaration)
        .argumentDeclarations

    return if (body != null) object {
        val resultScope: DynamicScope by lazy {
            scope.extend(
                name = declaration.givenName,
                value = DefinedFunctionValue(
                    closure = DynamicScope.delegated { resultScope },
                    argumentDeclarations = argumentDeclarations,
                    body = body,
                )
            )
        }
    }.resultScope else scope
}

private fun evaluateExpression(
    scope: DynamicScope,
    expression: Expression,
): Value = expression.evaluate(scope = scope)
