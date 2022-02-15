package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.interpreter.EvaluationError
import bia.model.BooleanType
import bia.model.BooleanValue
import bia.model.NarrowUnionType
import bia.model.SmartCastDeclaration
import bia.model.TaggedType
import bia.model.TaggedValue
import bia.model.Type
import bia.model.UnionAlternative
import bia.model.UnionType
import bia.model.Value
import bia.model.asTaggedValue
import bia.parser.StaticScope
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

data class TagExpressionB(
    val tagee: ExpressionB,
    val attachedTagName: String,
) : ExpressionB {
    override fun build(scope: StaticScope): Expression = TagExpression(
        expression = tagee.build(scope = scope),
        attachedTagName = attachedTagName,
    )
}

data class IsExpression(
    val checkee: Expression,
    val checkedTagName: String,
) : Expression {
    private val expressionUnionType by lazy {
        checkee.type as? UnionType ?: throw TypeCheckError("Tried to use 'is' expression on non-union")
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
        val taggedValue = checkee.evaluate(scope = scope).asTaggedValue()

        return BooleanValue(
            value = taggedValue.tag == checkedTagName,
        )
    }
}

data class IsExpressionB(
    val checkee: ExpressionB,
    val checkedTagName: String,
) : ExpressionB {
    override fun build(scope: StaticScope) = IsExpression(
        checkee = checkee.build(scope = scope),
        checkedTagName = checkedTagName,
    )
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

    override fun evaluate(scope: DynamicScope): Value {
        val matchee = matchee.evaluate(scope = scope).asTaggedValue()

        val matchingBranch = taggedBranches.singleOrNull {
            it.requiredTagName == matchee.tag
        }

        return if (matchingBranch != null) {
            matchingBranch.branch.evaluate(scope = scope)
        } else {
            val elseBranch = elseBranch
                ?: throw EvaluationError("Match expression has no matching branch for tag ${matchee.tag}, but there's no else branch either")

            elseBranch.evaluate(scope = scope)
        }
    }
}

data class MatchBranchB(
    val requiredTagName: String,
    val branch: ExpressionB,
) {
    fun build(scope: StaticScope) = MatchBranch(
        requiredTagName,
        branch.build(scope = scope),
    )
}

data class MatchExpressionB(
    val matchee: ExpressionB,
    val taggedBranches: List<MatchBranchB>,
    val elseBranch: ExpressionB?,
) : ExpressionB {
    override fun build(scope: StaticScope): MatchExpression {
        val matcheeExpression = matchee.build(scope = scope)

        fun extendScopeWithNarrowedType(
            tagName: String,
        ): StaticScope {
            val matcheeReferenceExpression =
                matcheeExpression as? ReferenceExpression

            val referredScopedDeclaration =
                matcheeReferenceExpression?.referredDeclaration ?: return scope

            val referredDeclaration = referredScopedDeclaration.declaration

            val matcheeUnionType: UnionType =
                (matcheeReferenceExpression.type as? UnionType) ?: return scope

            val checkedAlternative: UnionAlternative =
                matcheeUnionType.getAlternative(
                    tagName = tagName,
                ) ?: return scope

            return scope.extendClosed(
                name = referredDeclaration.givenName,
                declaration = SmartCastDeclaration(
                    givenName = referredDeclaration.givenName,
                    valueType = NarrowUnionType(
                        alternatives = matcheeUnionType.alternatives,
                        narrowedAlternative = checkedAlternative,
                    ),
                ),
            )
        }

        return MatchExpression(
            matchee = matcheeExpression,
            taggedBranches = taggedBranches.map { matchBranch ->
                matchBranch.build(
                    scope = extendScopeWithNarrowedType(
                        tagName = matchBranch.requiredTagName,
                    ),
                )
            },
            elseBranch = elseBranch?.build(scope = scope),
        )
    }
}

data class UntagExpression(
    val untagee: Expression,
) : Expression {
    private val expressionUnionType by lazy {
        untagee.type as? NarrowUnionType
            ?: throw TypeCheckError("Tried to untag an expression of non-narrow-union type")
    }

    override val type: Type by lazy { expressionUnionType.narrowedType }

    override fun evaluate(scope: DynamicScope): Value =
        untagee.evaluate(scope = scope).asTaggedValue().taggedValue
}

data class UntagExpressionB(
    val untagee: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope) = UntagExpression(
        untagee = untagee.build(scope)
    )
}
