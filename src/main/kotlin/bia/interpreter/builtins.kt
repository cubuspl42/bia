package bia.interpreter

import bia.model.FunctionValue
import bia.model.ListValue
import bia.model.NumberValue
import bia.model.Value
import bia.model.asBooleanValue
import bia.model.asFunctionValue
import bia.model.asListValue
import bia.model.asNumberValue

private val until = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val start = getArgument(arguments, 0) { asNumberValue() }
        val end = getArgument(arguments, 1) { asNumberValue() }

        return ListValue(
            value = (start.value.toInt() until end.value.toInt()).map {
                NumberValue(value = it.toDouble())
            }.toList(),
        )
    }
}

private val filter = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list = getArgument(arguments, 0) { asListValue() }
        val predicate = getArgument(arguments, 1) { asFunctionValue() }

        return ListValue(
            value = list.value.filter {
                predicate.call(listOf(it)).asBooleanValue().value
            }
        )
    }
}

val builtinScope = Scope(
    values = mapOf(
        "until" to until,
        "filter" to filter,
    ),
)

private fun <SpecificValue : Value> getArgument(
    arguments: List<Value>,
    index: Int,
    cast: Value.() -> SpecificValue,
): SpecificValue {
    val argumentValue =
        arguments.getOrNull(index) ?: throw UnsupportedOperationException("Expected at least ${index + 1} arguments")

    val castedArgumentValue = argumentValue.cast()

    return castedArgumentValue
}
