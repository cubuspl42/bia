package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.model.BooleanType
import bia.model.BooleanValue
import bia.model.NarrowUnionType
import bia.model.TaggedType
import bia.model.TaggedValue
import bia.model.Type
import bia.model.UnionType
import bia.model.Value
import bia.model.asTaggedValue
import bia.type_checker.TypeCheckError

data class TagExpression(
    val expression: Expression,
    val attachedTagName: String,
) : Expression {
    override val type: Type
        get() = TaggedType(
            taggedType = expression.type,
            attachedTagName = attachedTagName,
        )

    override fun evaluate(scope: DynamicScope): Value = TaggedValue(
        taggedValue = expression.evaluate(scope = scope),
        tag = attachedTagName,
    )
}

data class IsExpression(
    val expression: Expression,
    val checkedTagName: String,
) : Expression {
    private val expressionUnionType by lazy {
        expression.type as? UnionType ?: throw TypeCheckError("Tried to use 'is' expression on non-union")
    }

    private val checkedUnionAlternative by lazy {
        expressionUnionType.alternatives.singleOrNull { it.tagName == checkedTagName }
            ?: throw TypeCheckError("One union alternative should have a tag '$checkedTagName'")
    }

    override fun validate() {
        checkedUnionAlternative
    }

    override val type = BooleanType

    override fun evaluate(scope: DynamicScope): Value {
        val taggedValue = expression.evaluate(scope = scope).asTaggedValue()

        return BooleanValue(
            value = taggedValue.tag == checkedTagName,
        )
    }
}

data class MatchBranch(
    val requiredTagName: String,
    val branch: Expression,
)

data class MatchExpression(
    val matchee: Expression,
    val taggedBranches: List<MatchBranch>,
    val elseBranch: Expression?,
) : Expression {
    private val allBranches: List<Expression> =
        taggedBranches.map { it.branch } + listOfNotNull(elseBranch)

    private val matcheeUnionType: UnionType by lazy {
        matchee.type as? UnionType
            ?: throw TypeCheckError("Tried to match an expression of non-union type")
    }

    override val type: Type by lazy {
        val firstBranch = allBranches.first()

        if (allBranches.any { it.type != firstBranch.type }) {
            throw TypeCheckError("Not all branches of a match expression have same types")
        }

        firstBranch.type
    }

    override fun evaluate(scope: DynamicScope): Value =
        TODO()
}

data class UntagExpression(
    val expression: Expression,
) : Expression {
    private val expressionUnionType by lazy {
        expression.type as? NarrowUnionType
            ?: throw TypeCheckError("Tried to untag an expression of non-narrow-union type")
    }

    override val type: Type by lazy { expressionUnionType.narrowedType }

    override fun evaluate(scope: DynamicScope): Value =
        expression.evaluate(scope = scope).asTaggedValue().taggedValue
}
