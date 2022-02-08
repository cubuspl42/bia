package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.model.NumberType
import bia.model.NumberValue
import bia.model.Type
import bia.model.Value
import bia.model.asNumberValue
import bia.type_checker.TypeCheckError

data class AdditionExpression(
    val augend: Expression,
    val addend: Expression,
) : Expression {
    override val type: Type by lazy {
        if (augend.type !is NumberType || addend.type !is NumberType) {
            throw TypeCheckError("Tried to add expressions of type ${augend.type} and ${addend.type}")
        } else NumberType
    }

    override fun evaluate(scope: DynamicScope): Value =
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
    override val type: Type by lazy {
        if (minuend.type !is NumberType || subtrahend.type !is NumberType) {
            throw TypeCheckError("Tried to subtract expressions of type ${minuend.type} and ${subtrahend.type}")
        } else NumberType
    }

    override fun evaluate(scope: DynamicScope): Value =
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
    override val type: Type by lazy {
        if (multiplier.type !is NumberType || multiplicand.type !is NumberType) {
            throw TypeCheckError("Tried to multiply expressions of type ${multiplier.type} and ${multiplicand.type}")
        } else NumberType
    }

    override fun evaluate(scope: DynamicScope): Value =
        evaluateNumberBinaryExpression(
            scope = scope,
            left = multiplier,
            right = multiplicand,
        ) { a, b -> a * b }
}

data class DivisionExpression(
    val dividend: Expression,
    val divisor: Expression,
) : Expression {
    override val type: Type by lazy {
        if (dividend.type !is NumberType || divisor.type !is NumberType) {
            throw TypeCheckError("Tried to divide expressions of type ${dividend.type} and ${divisor.type}")
        } else NumberType
    }

    override fun evaluate(scope: DynamicScope): Value =
        evaluateNumberBinaryExpression(
            scope = scope,
            left = dividend,
            right = divisor,
        ) { a, b -> a / b }
}

data class IntegerDivisionExpression(
    val dividend: Expression,
    val divisor: Expression,
) : Expression {
    override val type: Type by lazy {
        if (dividend.type !is NumberType || divisor.type !is NumberType) {
            throw TypeCheckError("Tried to integer-divide expressions of type ${dividend.type} and ${divisor.type}")
        } else NumberType
    }

    override fun evaluate(scope: DynamicScope): Value =
        evaluateNumberBinaryExpression(
            scope = scope,
            left = dividend,
            right = divisor,
        ) { a, b -> (a.toLong() / b.toLong()).toDouble() }
}

data class ReminderExpression(
    val dividend: Expression,
    val divisor: Expression,
) : Expression {
    override val type: Type by lazy {
        if (dividend.type !is NumberType || divisor.type !is NumberType) {
            throw TypeCheckError("Tried reminder-divide expressions of type ${dividend.type} and ${divisor.type}")
        } else NumberType
    }

    override fun evaluate(scope: DynamicScope): Value =
        evaluateNumberBinaryExpression(
            scope = scope,
            left = dividend,
            right = divisor,
        ) { a, b -> a % b }
}

private fun evaluateNumberBinaryExpression(
    scope: DynamicScope,
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

private fun asMathOperandValue(value: Value) = value.asNumberValue(
    message = "Cannot perform mathematical operations on non-number",
)
