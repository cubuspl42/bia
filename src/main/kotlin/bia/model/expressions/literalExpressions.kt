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
import bia.parser.StaticScope

sealed class LiteralExpression : Expression, ExpressionB {
    override fun build(scope: StaticScope): Expression = this
}

data class IntLiteralExpression(
    val value: Long,
) : LiteralExpression() {
    override val type: Type = NumberType

    override fun evaluate(scope: DynamicScope): Value =
        NumberValue(
            value = value.toDouble(),
        )
}

data class BooleanLiteralExpression(
    val value: Boolean,
) : LiteralExpression() {
    override val type: Type = BooleanType

    override fun evaluate(scope: DynamicScope): Value =
        BooleanValue(
            value = value,
        )
}

data class ObjectLiteralExpression(
    val entries: Map<String, Expression>,
) : LiteralExpression() {
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

data class ObjectLiteralExpressionB(
    val entries: Map<String, ExpressionB>,
) : ExpressionB {
    override fun build(scope: StaticScope): Expression =
        ObjectLiteralExpression(
            entries = entries.mapValues { (_, expressionB) ->
                expressionB.build(scope)
            }
        )
}
