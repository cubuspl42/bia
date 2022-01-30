package bia.model

import bia.interpreter.DynamicScope
import bia.type_checker.TypeCheckError

sealed interface Expression {
    val type: Type

    fun validate() {
        type
    }

    fun evaluate(scope: DynamicScope): Value
}

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

data class LessThenExpression(
    val left: Expression,
    val right: Expression,
) : Expression {
    override val type: Type by lazy {
        if (left.type !is NumberType || right.type !is NumberType) {
            throw TypeCheckError("Tried compare (<) expressions of type ${left.type} and ${right.type}")
        } else NumberType
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
        } else NumberType
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

data class ReferenceExpression(
    val referredName: String,
    val referredDeclaration: Declaration?,
) : Expression {
    override val type: Type by lazy {
        referredDeclaration?.type ?: throw TypeCheckError("Unresolved reference ($referredName) has no type")
    }

    override fun evaluate(scope: DynamicScope): Value =
        scope.getValue(name = referredName)
            ?: throw UnsupportedOperationException("Unresolved reference at runtime: $referredName")
}

data class CallExpression(
    val callee: Expression,
    val arguments: List<Expression>,
) : Expression {
    private val calleeType: FunctionType by lazy {
        val calleeType = callee.type
        calleeType as? FunctionType ?: throw TypeCheckError("Tried to call a non-function: $calleeType")
    }

    override val type: Type by lazy {
        calleeType.returnType
    }

    override fun validate() {
        val argumentDeclarations = calleeType.argumentDeclarations

        val definedArgumentCount = argumentDeclarations.size
        val passedArgumentCount = arguments.size

        if (passedArgumentCount != definedArgumentCount) {
            throw TypeCheckError(
                listOf(
                    "Function",
                    if (callee is ReferenceExpression) callee.referredName else "",
                    "was defined with $definedArgumentCount arguments, $passedArgumentCount passed"
                ).joinToString(separator = " ")
            )
        }

        arguments.zip(argumentDeclarations).forEachIndexed { index, (argument, argumentDeclaration) ->
            if (!argument.type.isAssignableTo(argumentDeclaration.type)) {
                throw TypeCheckError(
                    "Argument #${index + 1} has type ${argument.type.toPrettyString()} " +
                            "which can't be assigned to ${argumentDeclaration.type.toPrettyString()}",
                )
            }
        }

        super.validate()
    }

    override fun evaluate(scope: DynamicScope): Value {
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
    override val type: Type = NumberType

    override fun evaluate(scope: DynamicScope): Value =
        NumberValue(
            value = value.toDouble(),
        )
}

data class BooleanLiteralExpression(
    val value: Boolean,
) : Expression {
    override val type: Type = BooleanType

    override fun evaluate(scope: DynamicScope): Value =
        BooleanValue(
            value = value,
        )
}

data class IfExpression(
    val guard: Expression,
    val trueBranch: Expression,
    val falseBranch: Expression,
) : Expression {
    override val type: Type by lazy {
        val trueBranchType = trueBranch.type
        val falseBranchType = falseBranch.type

        if (trueBranchType == falseBranchType) {
            trueBranchType
        } else {
            throw TypeCheckError("If expression has incompatible true- and false-branch types: $trueBranchType, $falseBranchType")
        }
    }

    override fun evaluate(scope: DynamicScope): Value {
        val guardValue = guard.evaluate(scope = scope).asBooleanValue(
            message = "Guard has to be a boolean",
        )

        fun evaluateTrueBranchValue() = trueBranch.evaluate(scope = scope)
        fun evaluateFalseBranchValue() = falseBranch.evaluate(scope = scope)

        return if (guardValue.value) evaluateTrueBranchValue() else evaluateFalseBranchValue()
    }
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

private fun asMathOperandValue(value: Value) = value.asNumberValue(
    message = "Cannot perform mathematical operations on non-number",
)

private fun asLogicalValue(value: Value) = value.asBooleanValue(
    message = "Cannot perform logical operations on non-boolean",
)
