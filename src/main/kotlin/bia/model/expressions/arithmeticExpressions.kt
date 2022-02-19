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
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        determineTypeDirectlyForNumberBinaryExpression(
            expression = this,
            context = context,
            left = augend,
            right = addend,
            errorMessage = { l, r -> "Tried to add expressions of type $l and $r" },
        )

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
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        determineTypeDirectlyForNumberBinaryExpression(
            expression = this,
            context = context,
            left = minuend,
            right = subtrahend,
            errorMessage = { l, r -> "Tried to subtract expressions of type $l and $r" },
        )

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
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        determineTypeDirectlyForNumberBinaryExpression(
            expression = this,
            context = context,
            left = multiplier,
            right = multiplicand,
            errorMessage = { l, r -> "Tried to multiply expressions of type $l and $r" },
        )

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
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        determineTypeDirectlyForNumberBinaryExpression(
            expression = this,
            context = context,
            left = dividend,
            right = divisor,
            errorMessage = { l, r -> "Tried to divide expressions of type $l and $r" },
        )

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
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        determineTypeDirectlyForNumberBinaryExpression(
            expression = this,
            context = context,
            left = dividend,
            right = divisor,
            errorMessage = { l, r -> "Tried to integer-divide expressions of type $l and $r" },
        )

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
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        determineTypeDirectlyForNumberBinaryExpression(
            expression = this,
            context = context,
            left = dividend,
            right = divisor,
            errorMessage = { l, r -> "Tried to reminder-divide expressions of type $l and $r" },
        )

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

private fun determineTypeDirectlyForNumberBinaryExpression(
    expression: Expression,
    context: TypeDeterminationContext,
    left: Expression,
    right: Expression,
    errorMessage: (leftType: Type, rightType: Type) -> String,
): Type {
    val extendedContext = context.withVisited(expression = expression)

    val leftType = left.determineType(context = extendedContext)
    val rightType = right.determineType(context = extendedContext)

    return if (leftType !is NumberType || rightType !is NumberType) {
        throw TypeCheckError(message = errorMessage(leftType, rightType))
    } else NumberType
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
