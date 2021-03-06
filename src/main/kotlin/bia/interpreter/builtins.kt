package bia.interpreter

import bia.model.BigIntegerValue
import bia.model.FunctionValue
import bia.model.ListValue
import bia.model.NullValue
import bia.model.NumberValue
import bia.model.SequenceValue
import bia.model.Value
import bia.model.asBigIntegerValue
import bia.model.asBooleanValue
import bia.model.asFunctionValue
import bia.model.asListValue
import bia.model.asNumberValue
import bia.model.asSequenceValue
import java.math.BigInteger
import kotlin.math.sqrt

private val until = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val start = getArgument(arguments, 0) { asNumberValue() }
        val end = getArgument(arguments, 1) { asNumberValue() }

        return ListValue(
            value = (start.value.toLong() until end.value.toLong()).map {
                NumberValue(value = it.toDouble())
            }.toList(),
        )
    }
}

private val to = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val start = getArgument(arguments, 0) { asNumberValue() }
        val end = getArgument(arguments, 1) { asNumberValue() }

        return ListValue(
            value = (start.value.toLong()..end.value.toLong()).map {
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

private val foldL = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list = getArgument(arguments, 0) { asListValue() }
        val initial = getArgument(arguments, 1)
        val operation = getArgument(arguments, 2) { asFunctionValue() }

        return list.value.fold(initial) { acc, value ->
            operation.call(listOf(acc, value))
        }
    }
}

private val firstWhereOrNull = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list = getArgument(arguments, 0) { asListValue() }
        val predicate = getArgument(arguments, 1) { asFunctionValue() }

        return list.value.find { callPredicate(predicate, it) } ?: NullValue
    }
}

private val productL2 = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list1 = getArgument(arguments, 0) { asListValue() }
        val list2 = getArgument(arguments, 1) { asListValue() }
        val combine = getArgument(arguments, 2) { asFunctionValue() }

        return ListValue(
            value = list1.value.flatMap { a ->
                list2.value.map { b -> combine.call(listOf(a, b)) }
            },
        )
    }
}

private val consL = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val head = getArgument(arguments, 0)
        val tail = getArgument(arguments, 1) { asListValue() }

        return ListValue(
            value = listOf(head) + tail.value,
        )
    }
}

private val firstL = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list = getArgument(arguments, 0) { asListValue() }

        return list.value.firstOrNull() ?: NullValue
    }
}

private val lastL = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list = getArgument(arguments, 0) { asListValue() }

        return list.value.lastOrNull() ?: NullValue
    }
}

private val dropL = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list = getArgument(arguments, 0) { asListValue() }
        val n = getArgument(arguments, 1) { asNumberValue() }

        return ListValue(
            value = list.value.drop(n.value.toInt()),
        )
    }
}

private val dropLastL = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list = getArgument(arguments, 0) { asListValue() }
        val n = getArgument(arguments, 1) { asNumberValue() }

        return ListValue(
            value = list.value.dropLast(n.value.toInt()),
        )
    }
}

private val sizeL = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list = getArgument(arguments, 0) { asListValue() }

        return NumberValue(
            value = list.value.size.toDouble(),
        )
    }
}

private val maxByL = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list = getArgument(arguments, 0) { asListValue() }
        val selector = getArgument(arguments, 1) { asFunctionValue() }

        return list.value.maxByOrNull {
            selector.call(listOf(it)).asNumberValue().value
        } ?: NullValue
    }
}

private val maxL = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val list = getArgument(arguments, 0) { asListValue() }

        return list.value.maxByOrNull { it.asNumberValue().value } ?: NullValue
    }
}

private val listOf = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value =
        ListValue(
            value = arguments,
        )
}

private val emptyList = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value =
        ListValue(
            value = emptyList(),
        )
}

private val concatL = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val left = getArgument(arguments, 0) { asListValue() }
        val right = getArgument(arguments, 1) { asListValue() }

        return ListValue(
            value = left.value + right.value,
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

private val sqrt = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val x = getArgument(arguments, 0) { asNumberValue() }

        return NumberValue(
            value = sqrt(x.value),
        )
    }
}

private val orElse = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val value = getArgument(arguments, 0)
        val provideFallback = getArgument(arguments, 1) { asFunctionValue() }

        return if (value == NullValue) callProvide(provideFallback)
        else value
    }
}

private val bigIntOf = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val value = getArgument(arguments, 0) { asNumberValue() }

        return BigIntegerValue(
            value = BigInteger.valueOf(value.value.toLong()),
        )
    }
}

private val addBi = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val left = getArgument(arguments, 0) { asBigIntegerValue() }
        val right = getArgument(arguments, 1) { asBigIntegerValue() }

        return BigIntegerValue(
            value = left.value + right.value,
        )
    }
}

private val subBi = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val left = getArgument(arguments, 0) { asBigIntegerValue() }
        val right = getArgument(arguments, 1) { asBigIntegerValue() }

        return BigIntegerValue(
            value = left.value - right.value,
        )
    }
}

private val mulBi = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val left = getArgument(arguments, 0) { asBigIntegerValue() }
        val right = getArgument(arguments, 1) { asBigIntegerValue() }

        return BigIntegerValue(
            value = left.value * right.value,
        )
    }
}

private val divBi = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val left = getArgument(arguments, 0) { asBigIntegerValue() }
        val right = getArgument(arguments, 1) { asBigIntegerValue() }

        return BigIntegerValue(
            value = left.value / right.value,
        )
    }
}

private val gcdBi = object : FunctionValue() {
    override fun call(arguments: List<Value>): Value {
        val left = getArgument(arguments, 0) { asBigIntegerValue() }
        val right = getArgument(arguments, 1) { asBigIntegerValue() }

        return BigIntegerValue(
            value = left.value.gcd(right.value),
        )
    }
}

val builtinScope = DynamicScope.of(
    values = mapOf(
        "until" to until,
        "to" to to,
        "filter" to filter,
        "fold:L" to foldL,
        "firstWhereOrNull" to firstWhereOrNull,
        "product:L2" to productL2,
        "cons:L" to consL,
        "first:L" to firstL,
        "last:L" to lastL,
        "drop:L" to dropL,
        "dropLast:L" to dropLastL,
        "size:L" to sizeL,
        "maxBy:L" to maxByL,
        "max:L" to maxL,
        "listOf" to listOf,
        "emptyList" to emptyList,
        "concat:L" to concatL,
        "filter:Sq" to filterSq,
        "seqOf" to seqOf,
        "takeWhile:Sq" to takeWhileSq,
        "sum:Sq" to sumSq,
        "concat:Sq" to concatSq,
        "consLazy" to consLazy,
        "sqrt" to sqrt,
        "orElse" to orElse,
        "bigIntOf" to bigIntOf,
        "add:Bi" to addBi,
        "sub:Bi" to subBi,
        "mul:Bi" to mulBi,
        "div:Bi" to divBi,
        "gcd:Bi" to gcdBi,
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
