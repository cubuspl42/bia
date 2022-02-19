package bia.interpreter

import bia.model.BasicArgumentListDeclaration
import bia.model.expressions.Expression
import bia.model.FunctionBody
import bia.model.DefDeclaration
import bia.model.DefinedFunctionValue
import bia.model.Program
import bia.model.SingletonDeclaration
import bia.model.SingletonValue
import bia.model.TopLevelDeclaration
import bia.model.TypeAliasDeclaration
import bia.model.UnionDeclaration
import bia.model.Value
import bia.model.ValDeclaration
import bia.model.ValueDefinition

fun evaluateProgram(
    program: Program,
): DynamicScope {
    val topLevelDeclarations = program.topLevelDeclarations

    val finalScope = topLevelDeclarations.fold(builtinScope) { scope, declaration ->
        executeTopLevelDeclaration(scope = scope, declaration = declaration)
    }

    return finalScope
}

fun evaluateBody(
    outerScope: DynamicScope,
    body: FunctionBody,
): Value {
    val finalScope = body.definitions.fold(outerScope) { scope, declaration ->
        executeValueDefinition(scope = scope, declaration = declaration)
    }

    val returnedValue = evaluateExpression(
        scope = finalScope,
        expression = body.returned,
    )

    return returnedValue
}

private fun executeTopLevelDeclaration(
    scope: DynamicScope,
    declaration: TopLevelDeclaration,
): DynamicScope = when (declaration) {
    is SingletonDeclaration -> executeSingletonDeclaration(
        scope = scope,
        declaration = declaration,
    )
    is ValueDefinition -> executeValueDefinition(
        scope = scope,
        declaration = declaration,
    )
    is TypeAliasDeclaration -> scope
    is UnionDeclaration -> scope
}

private fun executeValueDefinition(
    scope: DynamicScope,
    declaration: ValueDefinition,
): DynamicScope = when (declaration) {
    is ValDeclaration -> executeValueDeclaration(
        scope = scope,
        declaration = declaration,
    )
    is DefDeclaration -> executeFunctionDeclaration(
        scope = scope,
        declaration = declaration,
    )
}

fun executeValueDeclaration(
    scope: DynamicScope,
    declaration: ValDeclaration,
): DynamicScope = scope.extend(
    name = declaration.givenName,
    value = evaluateExpression(
        scope = scope,
        expression = declaration.initializer,
    ),
)

// TODO

// fun executeValueDeclaration(
//    scope: DynamicScope,
//    declaration: ValDeclaration,
//): DynamicScope = object {
//    val extendedScope: DynamicScope by lazy {
//        scope.extend(
//            name = declaration.givenName,
//            value = evaluateExpression(
//                scope = extendedScope,
//                expression = declaration.initializer,
//            ),
//        )
//    }
//}.extendedScope

private fun executeFunctionDeclaration(
    scope: DynamicScope,
    declaration: DefDeclaration,
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

private fun executeSingletonDeclaration(
    scope: DynamicScope,
    declaration: SingletonDeclaration,
): DynamicScope = scope.extend(
    name = declaration.givenName,
    value = SingletonValue(declaration.givenName),
)

private fun evaluateExpression(
    scope: DynamicScope,
    expression: Expression,
): Value = expression.evaluate(scope = scope)
