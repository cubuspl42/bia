package bia.test_utils

import bia.model.ValueDeclaration
import bia.model.expressions.Expression
import bia.model.TypeVariable
import bia.model.expressions.ExpressionB
import bia.parser.ClosedDeclaration
import bia.parser.StaticScope
import bia.parser.buildAntlrParser
import bia.parser.transformExpression

fun parseExpression(
    scopeDeclarations: List<ValueDeclaration> = emptyList(),
    scopeTypeVariables: List<TypeVariable> = emptyList(),
    source: String,
): Expression = parseExpressionB(
    source = source,
).build(
    scope = StaticScope.of(
        declarations = scopeDeclarations.associate {
            it.givenName to ClosedDeclaration(declaration = it)
        },
        typeAlikes = scopeTypeVariables.associate {
            it.givenName to listOf(it)
        },
    ),
)

fun parseExpressionB(
    source: String,
): ExpressionB {
    val parser = buildAntlrParser(source = source, sourceName = "<expression>")

    return transformExpression(
        expression = parser.expression(),
    )
}
