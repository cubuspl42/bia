package bia.interpreter

import bia.model.FunctionValue
import bia.model.ListValue
import bia.model.NumberValue
import bia.model.SequenceValue
import bia.model.Value
import bia.model.asBooleanValue
import bia.model.asFunctionValue
import bia.model.asListValue
import bia.model.asNumberValue
import bia.model.asSequenceValue

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
            value = list.value.filter { callPredicate(predicate, it) },
        )
    }
}

private val filterSq = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val sequence = getArgument(arguments, 0) { asSequenceValue() }
        val predicate = getArgument(arguments, 1) { asFunctionValue() }

        return SequenceValue(
            value = sequence.value.filter { callPredicate(predicate, it) },
        )
    }
}

private val seqOf = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value = SequenceValue(
        value = arguments.asSequence(),
    )
}

private val takeWhileSq = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val sequence = getArgument(arguments, 0) { asSequenceValue() }
        val predicate = getArgument(arguments, 1) { asFunctionValue() }

        return SequenceValue(
            value = sequence.value.takeWhile { callPredicate(predicate, it) },
        )
    }
}

private val sumSq = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val sequence = getArgument(arguments, 0) { asSequenceValue() }

        return NumberValue(
            value = sequence.value.sumOf { it.asNumberValue().value },
        )
    }
}

private val concatSq = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val left = getArgument(arguments, 0) { asSequenceValue() }
        val right = getArgument(arguments, 1) { asSequenceValue() }

        return SequenceValue(
            value = left.value + right.value,
        )
    }
}

private val consLazy = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val head = getArgument(arguments, 0)
        val buildTail = getArgument(arguments, 1) { asFunctionValue() }

        return SequenceValue(
            value = object : Sequence<Value> {
                val sequence by lazy {
                    val tail = callProvide(buildTail).asSequenceValue().value
                    sequenceOf(head) + tail
                }

                override fun iterator(): Iterator<Value> = sequence.iterator()
            },
        )
    }
}

val builtinScope = Scope.of(
    values = mapOf(
        "until" to until,
        "filter" to filter,
        "filter:Sq" to filterSq,
        "seqOf" to seqOf,
        "takeWhile:Sq" to takeWhileSq,
        "sum:Sq" to sumSq,
        "concat:Sq" to concatSq,
        "consLazy" to consLazy,
    ),
)

private fun getArgument(
    arguments: List<Value>,
    index: Int,
): Value =
    arguments.getOrNull(index) ?: throw UnsupportedOperationException("Expected at least ${index + 1} arguments")

private fun <SpecificValue : Value> getArgument(
    arguments: List<Value>,
    index: Int,
    cast: Value.() -> SpecificValue,
): SpecificValue {
    val castedArgumentValue = getArgument(arguments, index).cast()

    return castedArgumentValue
}

private fun callPredicate(predicate: FunctionValue, it: Value): Boolean =
    predicate.call(listOf(it)).asBooleanValue().value

private fun callProvide(provide: FunctionValue): Value =
    provide.call(emptyList())
