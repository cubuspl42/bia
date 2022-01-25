package bia.interpreter

import bia.model.Value

data class Scope(
    private val values: Map<String, Value>,
) {
    companion object {
        val empty = Scope(
            values = emptyMap(),
        )
    }

    fun extend(name: String, value: Value) = Scope(
        values = values + (name to value)
    )

    fun extend(namedValues: List<Pair<String, Value>>) = Scope(
        values = values + namedValues.toMap()
    )

    fun getValue(name: String): Value? = values[name]
}
