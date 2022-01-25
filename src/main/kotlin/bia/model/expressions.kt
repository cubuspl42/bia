package bia.model

import bia.interpreter.Scope

sealed interface Expression {
    fun evaluate(scope: Scope): Value
}

data class AdditionExpression(
    val augend: Expression,
    val addend: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value =
        evaluateNumberBinaryExpression(
            scope = scope,
            left = augend,
            right = addend,
        ) { a, b -> a + b }
}

data class SubtractionExpression(
    val minuend: Expression,
    val subtrahend: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value =
        evaluateNumberBinaryExpression(
            scope = scope,
            left = minuend,
            right = subtrahend,
        ) { a, b -> a - b }
}

data class MultiplicationExpression(
    val multiplier: Expression,
    val multiplicand: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value =
        evaluateNumberBinaryExpression(
            scope = scope,
            left = multiplier,
            right = multiplicand,
        ) { a, b -> a * b }
}

data class ReminderExpression(
    val dividend: Expression,
    val divisor: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value =
        evaluateNumberBinaryExpression(
            scope = scope,
            left = dividend,
            right = divisor,
        ) { a, b -> a % b }
}

data class LessThenExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value =
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
    override fun evaluate(scope: Scope): Value =
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
    override fun evaluate(scope: Scope): Value =
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
    override fun evaluate(scope: Scope): Value =
        evaluateLogicalBinaryExpression(
            scope = scope,
            left = left,
            right = right,
        ) { a, b -> a && b }
}

data class NotExpression(
    val negated: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value {
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
    override fun evaluate(scope: Scope): Value {
        val leftValue = left.evaluate(scope = scope)
        val rightValue = right.evaluate(scope = scope)

        return BooleanValue(
            value = leftValue.value == rightValue.value,
        )
    }
}

data class ReferenceExpression(
    val referredName: String,
) : Expression {
    override fun evaluate(scope: Scope): Value =
        scope.getValue(name = referredName)
            ?: throw UnsupportedOperationException("Unresolved reference at runtime: $referredName")
}

data class CallExpression(
    val callee: Expression,
    val arguments: List<Expression>,
) : Expression {
    override fun evaluate(scope: Scope): Value {
        val calleeValue = callee.evaluate(scope = scope).asFunctionValue(
            message = "Only functions can be called, tried",
        )

        val argumentValues = arguments.map {
            it.evaluate(scope = scope)
        }

        return calleeValue.call(
            arguments = argumentValues,
        )
    }
}

data class IntLiteralExpression(
    val value: Long,
) : Expression {
    override fun evaluate(scope: Scope): Value =
        NumberValue(
            value = value.toDouble(),
        )
}

data class IfExpression(
    val guard: Expression,
    val trueBranch: Expression,
    val falseBranch: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value {
        val guardValue = guard.evaluate(scope = scope).asBooleanValue(
            message = "Guard has to be a boolean",
        )

        val trueBranchValue = trueBranch.evaluate(scope = scope)
        val falseBranchValue = falseBranch.evaluate(scope = scope)

        return if (guardValue.value) trueBranchValue else falseBranchValue
    }
}

private fun evaluateNumberBinaryExpression(
    scope: Scope,
    left: Expression,
    right: Expression,
    calculate: (left: Double, right: Double) -> Double,
): NumberValue {
    val leftNumber = asMathOperandValue(value = left.evaluate(scope = scope))
    val rightNumber = asMathOperandValue(value = right.evaluate(scope = scope))

    return NumberValue(
        value = calculate(leftNumber.value, rightNumber.value),
    )
}

private fun evaluateNumberLogicalExpression(
    scope: Scope,
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
    scope: Scope,
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

private fun asMathOperandValue(value: Value) = value.asNumberValue(
    message = "Cannot perform mathematical operations on non-number",
)

private fun asLogicalValue(value: Value) = value.asBooleanValue(
    message = "Cannot perform logical operations on non-boolean",
)
