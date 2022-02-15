package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.model.Type
import bia.model.Value
import bia.parser.StaticScope

sealed interface Expression {
    val declaredType: Type?
        get() = null

    val type: Type

    fun validate() {
        type
    }

    fun evaluate(scope: DynamicScope): Value
}

interface ExpressionB {
    fun build(scope: StaticScope): Expression
}
