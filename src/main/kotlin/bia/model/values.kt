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

data class ListValue(
    override val value: List<Value>,
) : Value

data class SequenceValue(
    override val value: Sequence<Value>,
) : Value

abstract class FunctionValue : Value {
    abstract fun call(arguments: List<Value>): Value

    final override val value: Any
        get() = this
}

class DefinedFunctionValue(
    private val closure: Scope,
    private val definition: FunctionDefinition,
) : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        if (arguments.size != definition.argumentNames.size) {
            throw UnsupportedOperationException("Function has to be called with as many arguments as it was defined with")
        }

        val namedArguments = definition.argumentNames.zip(arguments) { name, value ->
            name to value
        }

        return evaluateBody(
            outerScope = closure.extend(
                namedValues = namedArguments,
            ),
            body = definition.body,
        )
    }
}

fun Value.asNumberValue(message: String = "Expected a number, got"): NumberValue =
    this as? NumberValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asBooleanValue(message: String = "Expected a boolean, got"): BooleanValue =
    this as? BooleanValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asListValue(message: String = "Expected a list, got"): ListValue =
    this as? ListValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asSequenceValue(message: String = "Expected a sequence, got"): SequenceValue =
    this as? SequenceValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asFunctionValue(message: String = "Expected a function, got"): FunctionValue =
    this as? FunctionValue ?: throw UnsupportedOperationException("$message: $this")
