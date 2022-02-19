package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.model.Type
import bia.model.Value
import bia.parser.StaticScope
import bia.type_checker.TypeCheckError
import kotlin.math.exp

data class TypeDeterminationContext(
    val visitedExpressions: Set<Expression>,
) {
    companion object {
        val empty = TypeDeterminationContext(
            visitedExpressions = emptySet(),
        )
    }

    fun withVisited(expression: Expression): TypeDeterminationContext =
        TypeDeterminationContext(
            visitedExpressions = visitedExpressions + expression,
        )
}

sealed interface Expression {
    val declaredType: Type?
        get() = null

    fun determineTypeDirectly(
        context: TypeDeterminationContext,
    ): Type

    fun validate() {
        type
    }

    fun evaluate(scope: DynamicScope): Value
}

val Expression.type: Type
    get() = determineType(
        context = TypeDeterminationContext.empty,
    )

fun Expression.determineType(
    context: TypeDeterminationContext,
): Type {
    if (context.visitedExpressions.contains(this)) {
        throw TypeCheckError("Type determination for expression entered a loop")
    }

    // TODO: Extend the context?

    return determineTypeDirectly(
        context = context,
    )
}

interface ExpressionB {
    fun build(scope: StaticScope): Expression
}
