package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.model.BooleanType
import bia.model.BooleanValue
import bia.model.NumberType
import bia.model.NumberValue
import bia.model.ObjectType
import bia.model.ObjectValue
import bia.model.Type
import bia.model.Value

data class IntLiteralExpression(
    val value: Long,
) : Expression {
    override val type: Type = NumberType

    override fun evaluate(scope: DynamicScope): Value =
        NumberValue(
            value = value.toDouble(),
        )
}

data class BooleanLiteralExpression(
    val value: Boolean,
) : Expression {
    override val type: Type = BooleanType

    override fun evaluate(scope: DynamicScope): Value =
        BooleanValue(
            value = value,
        )
}

data class ObjectLiteralExpression(
    val entries: Map<String, Expression>,
) : Expression {
    override val type: Type by lazy {
        ObjectType(
            entries = entries.mapValues { (_, expression) ->
                expression.type
            },
        )
    }

    override fun evaluate(scope: DynamicScope): Value =
        ObjectValue(
            entries = entries.mapValues { (_, expression) ->
                expression.evaluate(scope = scope)
            },
        )
}
