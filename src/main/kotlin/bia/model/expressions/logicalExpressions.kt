package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.model.BooleanType
import bia.model.BooleanValue
import bia.model.NumberType
import bia.model.Type
import bia.model.Value
import bia.model.asBooleanValue
import bia.model.asNumberValue
import bia.type_checker.TypeCheckError

data class LessThenExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override val type: Type by lazy {
        if (left.type !is NumberType || right.type !is NumberType) {
            throw TypeCheckError("Tried compare (<) expressions of type ${left.type} and ${right.type}")
        } else BooleanType
    }

    override fun evaluate(scope: DynamicScope): Value =
        evaluateNumberLogicalExpression(
            scope = scope,
            left = left,
            right = right,
        ) { a, b -> a < b }
}

data class GreaterThenExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override val type: Type by lazy {
        if (left.type !is NumberType || right.type !is NumberType) {
            throw TypeCheckError("Tried compare (>) expressions of type ${left.type} and ${right.type}")
        } else BooleanType
    }

    override fun evaluate(scope: DynamicScope): Value =
        evaluateNumberLogicalExpression(
            scope = scope,
            left = left,
            right = right,
        ) { a, b -> a > b }
}

data class OrExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override val type: Type by lazy {
        if (left.type !is BooleanType || right.type !is BooleanType) {
            throw TypeCheckError("Tried to perform logical operation (or) on expressions of type ${left.type} and ${right.type}")
        } else BooleanType
    }

    override fun evaluate(scope: DynamicScope): Value =
        evaluateLogicalBinaryExpression(
            scope = scope,
            left = left,
            right = right,
        ) { a, b -> a || b }
}

data class AndExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override val type: Type by lazy {
        if (left.type !is BooleanType || right.type !is BooleanType) {
            throw TypeCheckError("Tried to perform logical operation (and) on expressions of type ${left.type} and ${right.type}")
        } else BooleanType
    }

    override fun evaluate(scope: DynamicScope): Value =
        evaluateLogicalBinaryExpression(
            scope = scope,
            left = left,
            right = right,
        ) { a, b -> a && b }
}

data class NotExpression(
    val negated: Expression,
) : Expression {
    override val type: Type by lazy {
        if (negated.type !is BooleanType) {
            throw TypeCheckError("Tried to perform logical operation (not) on expression of type ${negated.type}")
        } else BooleanType
    }

    override fun evaluate(scope: DynamicScope): Value {
        val negatedBoolean = asLogicalValue(value = negated.evaluate(scope = scope))

        return BooleanValue(
            value = !negatedBoolean.value,
        )
    }
}

data class EqualsExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override val type: Type by lazy {
        if (left.type != right.type) {
            throw TypeCheckError("Tried to compare expressions of type ${left.type} and ${right.type}")
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
