package bia.model

import bia.interpreter.Scope

sealed interface Expression {
    fun evaluate(scope: Scope): Value
}

data class AdditionExpression(
    val augend: Expression,
    val addend: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value {
        val augendNumber = asOperandValue(value = augend.evaluate(scope = scope))
        val addendNumber = asOperandValue(value = addend.evaluate(scope = scope))

        return NumberValue(
            value = augendNumber.value + addendNumber.value,
        )
    }
}

data class SubtractionExpression(
    val minuend: Expression,
    val subtrahend: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value {
        val minuendNumber = asOperandValue(value = minuend.evaluate(scope = scope))
        val subtrahendNumber = asOperandValue(value = subtrahend.evaluate(scope = scope))

        return NumberValue(
            value = minuendNumber.value - subtrahendNumber.value,
        )
    }
}

data class MultiplicationExpression(
    val multiplier: Expression,
    val multiplicand: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value {
        val multiplierNumber = asOperandValue(value = multiplier.evaluate(scope = scope))
        val multiplicandNumber = asOperandValue(value = multiplicand.evaluate(scope = scope))

        return NumberValue(
            value = multiplierNumber.value * multiplicandNumber.value,
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
    val argument: Expression,
) : Expression {
    override fun evaluate(scope: Scope): Value {
        val calleeValue = asFunctionValue(
            value = callee.evaluate(scope = scope),
            message = "Only functions can be called, tried",
        )

        val argumentValue = argument.evaluate(scope = scope)

        return calleeValue.call(
            argumentValue = argumentValue,
        )
    }
}

data class IntLiteralExpression(
    val value: Int,
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
        val guardValue = asBooleanValue(
            value = guard.evaluate(scope = scope),
            message = "Guard has to be a boolean",
        )

        val trueBranchValue = trueBranch.evaluate(scope = scope)
        val falseBranchValue = falseBranch.evaluate(scope = scope)

        return if (guardValue.value) trueBranchValue else falseBranchValue
    }
}

private fun asNumberValue(value: Value, message: String): NumberValue =
    value as? NumberValue ?: throw UnsupportedOperationException("$message: $value")

private fun asBooleanValue(value: Value, message: String): BooleanValue =
    value as? BooleanValue ?: throw UnsupportedOperationException("$message: $value")

private fun asFunctionValue(value: Value, message: String): FunctionValue =
    value as? FunctionValue ?: throw UnsupportedOperationException("$message: $value")

private fun asOperandValue(value: Value) = asNumberValue(
    value = value,
    message = "Cannot perform mathematical operations on non-number",
)
