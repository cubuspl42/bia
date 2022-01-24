package bia.model

import bia.interpreter.Scope
import bia.interpreter.evaluateBody

sealed interface Value {
    val value: Any
}

data class NumberValue(
    override val value: Double,
) : Value

data class BooleanValue(
    override val value: Boolean,
) : Value

class FunctionValue(
    private val closure: Scope,
    private val definition: FunctionDefinition,
) : Value {
    fun call(argumentValue: Value): Value = evaluateBody(
        outerScope = closure.extend(
            name = definition.argumentName,
            value = argumentValue,
        ),
        body = definition.body,
    )

    override val value: Any
        get() = this
}
