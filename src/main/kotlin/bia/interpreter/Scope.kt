package bia.interpreter

import bia.model.Value

abstract class Scope {
    companion object {
        fun of(values: Map<String, Value>): Scope = SimpleScope(
            values = values,
        )

        fun delegated(delegate: () -> Scope): Scope = object : Scope() {
            override val values: Map<String, Value> by lazy { delegate().values }
        }
    }

    abstract val values: Map<String, Value>

    fun extend(name: String, value: Value) = Scope.of(
        values = values + (name to value)
    )

    fun extend(namedValues: List<Pair<String, Value>>) = Scope.of(
        values = values + namedValues.toMap()
    )

    fun getValue(name: String): Value? = values[name]
}

data class SimpleScope(
    override val values: Map<String, Value>,
) : Scope()
