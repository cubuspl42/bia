package bia.interpreter

import bia.model.Value

abstract class DynamicScope {
    companion object {
        fun of(values: Map<String, Value>): DynamicScope = SimpleDynamicScope(
            values = values,
        )

        fun delegated(delegate: () -> DynamicScope): DynamicScope = object : DynamicScope() {
            override val values: Map<String, Value> by lazy { delegate().values }
        }

        val empty: DynamicScope = of(values = emptyMap())
    }

    abstract val values: Map<String, Value>

    fun extend(name: String, value: Value) = DynamicScope.of(
        values = values + (name to value)
    )

    fun extend(namedValues: List<Pair<String, Value>>) = DynamicScope.of(
        values = values + namedValues.toMap()
    )

    fun getValue(name: String): Value? = values[name]
}

data class SimpleDynamicScope(
    override val values: Map<String, Value>,
) : DynamicScope()
