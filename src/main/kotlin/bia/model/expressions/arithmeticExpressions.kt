package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.model.NumberType
import bia.model.NumberValue
import bia.model.Type
import bia.model.Value
import bia.model.asNumberValue
import bia.parser.StaticScope
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

data class AdditionExpressionB(
    val augend: ExpressionB,
    val addend: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope): Expression = AdditionExpression(
        augend = augend.build(scope = scope),
        addend = addend.build(scope = scope),
    )
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

data class SubtractionExpressionB(
    val minuend: ExpressionB,
    val subtrahend: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope): Expression = SubtractionExpression(
        minuend = minuend.build(scope = scope),
        subtrahend = subtrahend.build(scope = scope),
    )
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

data class MultiplicationExpressionB(
    val multiplier: ExpressionB,
    val multiplicand: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope): Expression = MultiplicationExpression(
        multiplier = multiplier.build(scope = scope),
        multiplicand = multiplicand.build(scope = scope),
    )
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

data class DivisionExpressionB(
    val dividend: ExpressionB,
    val divisor: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope): Expression = DivisionExpression(
        dividend = dividend.build(scope = scope),
        divisor = divisor.build(scope = scope),
    )
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

data class IntegerDivisionExpressionB(
    val dividend: ExpressionB,
    val divisor: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope): Expression = IntegerDivisionExpression(
        dividend = dividend.build(scope = scope),
        divisor = divisor.build(scope = scope),
    )
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

data class ReminderExpressionB(
    val dividend: ExpressionB,
    val divisor: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope): Expression = ReminderExpression(
        dividend = dividend.build(scope = scope),
        divisor = divisor.build(scope = scope),
    )
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
