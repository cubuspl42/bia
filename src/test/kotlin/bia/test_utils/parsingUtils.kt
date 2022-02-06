package bia.test_utils

import bia.model.ValueDeclaration
import bia.model.Expression
import bia.model.TypeVariable
import bia.parser.ClosedDeclaration
import bia.parser.StaticScope
import bia.parser.buildAntlrParser
import bia.parser.transformExpression

fun parseExpression(
    scopeDeclarations: List<ValueDeclaration> = emptyList(),
    scopeTypeVariables: List<TypeVariable> = emptyList(),
    source: String,
): Expression {
    val parser = buildAntlrParser(source = source, sourceName = "<expression>")

    return transformExpression(
        scope = StaticScope.of(
            declarations = scopeDeclarations.associate {
                it.givenName to ClosedDeclaration(declaration = it)
            },
            typeVariables = scopeTypeVariables.associate {
                it.givenName to listOf(it)
            },
        ),
        expression = parser.expression(),
    )
}
