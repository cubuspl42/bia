package bia.model

import bia.interpreter.DynamicScope
import bia.interpreter.evaluateBody
import java.math.BigInteger

sealed interface Value {
    val value: Any

    fun untag(): Value = this
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

data class BigIntegerValue(
    override val value: BigInteger,
) : Value

abstract class FunctionValue : Value {
    abstract fun call(arguments: List<Value>): Value

    final override val value: Any
        get() = this
}

object NullValue : Value {
    override val value: Any
        get() = this
}

class DefinedFunctionValue(
    private val closure: DynamicScope,
    private val argumentDeclarations: List<ArgumentDeclaration>,
    private val body: FunctionBody,
) : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        if (arguments.size != argumentDeclarations.size) {
            throw UnsupportedOperationException("Function has to be called with as many arguments as it was defined with")
        }

        val namedArguments = argumentDeclarations.zip(arguments) { declaration, value ->
            declaration.givenName to value
        }

        val outerScope = closure.extend(
            namedValues = namedArguments,
        )

        return evaluateBody(
            outerScope = outerScope,
            body = body,
        )
    }
}

data class ObjectValue(
    val entries: Map<String, Value>,
) : Value {
    override val value: Any
        get() = entries
}

data class TaggedValue(
    val taggedValue: Value,
    val tag: String,
) : Value {
    override val value: Any
        get() = this

    override fun untag(): Value = taggedValue
}

fun Value.asNumberValue(message: String = "Expected a number, got"): NumberValue =
    this as? NumberValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asBooleanValue(message: String = "Expected a boolean, got"): BooleanValue =
    this as? BooleanValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asListValue(message: String = "Expected a list, got"): ListValue =
    this as? ListValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asSequenceValue(message: String = "Expected a sequence, got"): SequenceValue =
    this as? SequenceValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asBigIntegerValue(message: String = "Expected a big integer, got"): BigIntegerValue =
    this as? BigIntegerValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asFunctionValue(message: String = "Expected a function, got"): FunctionValue =
    this as? FunctionValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asObjectValue(message: String = "Expected an object, got"): ObjectValue =
    this as? ObjectValue ?: throw UnsupportedOperationException("$message: $this")

fun Value.asTaggedValue(message: String = "Expected a tagged value, got"): TaggedValue =
    this as? TaggedValue ?: throw UnsupportedOperationException("$message: $this")
