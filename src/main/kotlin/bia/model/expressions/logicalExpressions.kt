package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.model.BooleanType
import bia.model.BooleanValue
import bia.model.NumberType
import bia.model.Type
import bia.model.Value
import bia.model.asBooleanValue
import bia.model.asNumberValue
import bia.parser.StaticScope
import bia.type_checker.TypeCheckError

data class LessThenExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        determineTypeDirectlyForLogicalBinaryExpression(
            expression = this,
            context = context,
            left = left,
            right = right,
            errorMessage = { l, r -> "Tried compare (<) expressions of type $l and $r" },
        )

    override fun evaluate(scope: DynamicScope): Value =
        evaluateNumberLogicalExpression(
            scope = scope,
            left = left,
            right = right,
        ) { a, b -> a < b }
}

data class LessThenExpressionB(
    val left: ExpressionB,
    val right: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope) = LessThenExpression(
        left = left.build(scope = scope),
        right = right.build(scope = scope),
    )
}

data class GreaterThenExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        determineTypeDirectlyForLogicalBinaryExpression(
            expression = this,
            context = context,
            left = left,
            right = right,
            errorMessage = { l, r -> "Tried compare (>) expressions of type $l and $r" },
        )

    override fun evaluate(scope: DynamicScope): Value =
        evaluateNumberLogicalExpression(
            scope = scope,
            left = left,
            right = right,
        ) { a, b -> a > b }
}

data class GreaterThenExpressionB(
    val left: ExpressionB,
    val right: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope) = GreaterThenExpression(
        left = left.build(scope = scope),
        right = right.build(scope = scope),
    )
}

data class OrExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        determineTypeDirectlyForLogicalBinaryExpression(
            expression = this,
            context = context,
            left = left,
            right = right,
            errorMessage = { l, r -> "Tried to perform logical operation (or) on expressions of type $l and $r" },
        )

    override fun evaluate(scope: DynamicScope): Value =
        evaluateLogicalBinaryExpression(
            scope = scope,
            left = left,
            right = right,
        ) { a, b -> a || b }
}

data class OrExpressionB(
    val left: ExpressionB,
    val right: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope) = OrExpression(
        left = left.build(scope = scope),
        right = right.build(scope = scope),
    )
}

data class AndExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        determineTypeDirectlyForLogicalBinaryExpression(
            expression = this,
            context = context,
            left = left,
            right = right,
            errorMessage = { l, r -> "Tried to perform logical operation (and) on expressions of type $l and $r" },
        )

    override fun evaluate(scope: DynamicScope): Value =
        evaluateLogicalBinaryExpression(
            scope = scope,
            left = left,
            right = right,
        ) { a, b -> a && b }
}

data class AndExpressionB(
    val left: ExpressionB,
    val right: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope) = AndExpression(
        left = left.build(scope = scope),
        right = right.build(scope = scope),
    )
}

data class NotExpression(
    val negated: Expression,
) : Expression {
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type {
        val extendedContext = context.withVisited(expression = this)

        val negatedType = negated.determineType(context = extendedContext)

        return if (negatedType !is BooleanType) {
            throw TypeCheckError("Tried to perform logical operation (not) on expression of type ${negatedType.toPrettyString()}")
        } else BooleanType
    }

    override fun evaluate(scope: DynamicScope): Value {
        val negatedBoolean = asLogicalValue(value = negated.evaluate(scope = scope))

        return BooleanValue(
            value = !negatedBoolean.value,
        )
    }
}

data class NotExpressionB(
    val negated: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope) = NotExpression(
        negated = negated.build(scope = scope),
    )
}

data class EqualsExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type {
        val extendedContext = context.withVisited(expression = this)

        val leftType = left.determineType(context = extendedContext)
        val rightType = right.determineType(context = extendedContext)

        return if (leftType != rightType) {
            throw TypeCheckError("Tried to compare expressions of type ${leftType.toPrettyString()} and ${rightType.toPrettyString()}")
        } else BooleanType
    }

    override fun evaluate(scope: DynamicScope): Value {
        val leftValue = left.evaluate(scope = scope)
        val rightValue = right.evaluate(scope = scope)

        return BooleanValue(
            value = leftValue.value == rightValue.value,
        )
    }
}

data class EqualsExpressionB(
    val left: ExpressionB,
    val right: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope) = EqualsExpression(
        left = left.build(scope = scope),
        right = right.build(scope = scope),
    )
}

private fun evaluateNumberLogicalExpression(
    scope: DynamicScope,
    left: Expression,
    right: Expression,
    calculate: (left: Double, right: Double) -> Boolean,
): BooleanValue {
    val leftNumber = left.evaluate(scope = scope).asNumberValue()
    val rightNumber = right.evaluate(scope = scope).asNumberValue()

    return BooleanValue(
        value = calculate(leftNumber.value, rightNumber.value),
    )
}

private fun determineTypeDirectlyForLogicalBinaryExpression(
    expression: Expression,
    context: TypeDeterminationContext,
    left: Expression,
    right: Expression,
    errorMessage: (leftType: String, rightType: String) -> String,
): Type {
    val extendedContext = context.withVisited(expression = expression)

    val leftType = left.determineType(context = extendedContext)
    val rightType = right.determineType(context = extendedContext)

    return if (leftType !is BooleanType || rightType !is BooleanType) {
        throw TypeCheckError(
            message = errorMessage(
                leftType.toPrettyString(),
                rightType.toPrettyString(),
            ),
        )
    } else NumberType
}

private fun evaluateLogicalBinaryExpression(
    scope: DynamicScope,
    left: Expression,
    right: Expression,
    calculate: (left: Boolean, right: Boolean) -> Boolean,
): BooleanValue {
    val leftBoolean = asLogicalValue(value = left.evaluate(scope = scope))
    val rightBoolean = asLogicalValue(value = right.evaluate(scope = scope))

    return BooleanValue(
        value = calculate(leftBoolean.value, rightBoolean.value),
    )
}

private fun asLogicalValue(value: Value) = value.asBooleanValue(
    message = "Cannot perform logical operations on non-boolean",
)
