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
import bia.model.TypeExpressionB
import bia.model.UnionAlternative
import bia.model.UnionType
import bia.model.Value
import bia.model.asTaggedValue
import bia.model.buildType
import bia.model.isAssignableTo
import bia.parser.StaticScope
import bia.type_checker.TypeCheckError
import kotlin.math.exp

data class TagExpression(
    val taggedExpression: Expression,
    val attachedTagName: String,
) : Expression {
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type {
        val extendedContext = context.withVisited(expression = this)

        val taggedType = taggedExpression.determineType(context = extendedContext)

        return TaggedType(
            taggedType = taggedType,
            attachedTagName = attachedTagName,
        )
    }

    override fun evaluate(scope: DynamicScope): Value = TaggedValue(
        taggedValue = taggedExpression.evaluate(scope = scope),
        tag = attachedTagName,
    )
}

data class TagExpressionB(
    val tagee: ExpressionB,
    val attachedTagName: String,
) : ExpressionB {
    override fun build(scope: StaticScope): Expression = TagExpression(
        taggedExpression = tagee.build(scope = scope),
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

    override fun determineTypeDirectly(context: TypeDeterminationContext): Type =
        BooleanType

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
    val explicitType: Type? = null,
    val taggedBranches: List<MatchBranch>,
    val elseBranch: Expression?,
) : Expression {
    private val allBranches: List<Expression> =
        taggedBranches.map { it.branch } + listOfNotNull(elseBranch)

    private val matcheeUnionType: UnionType by lazy {
        matchee.type as? UnionType
            ?: throw TypeCheckError("Tried to match an expression of non-union type")
    }

    override fun determineTypeDirectly(context: TypeDeterminationContext): Type {
        val extendedContext = context.withVisited(expression = this)

        return explicitType ?: run {
            val firstBranch = allBranches.first()
            val firstBranchType = firstBranch.determineType(context = extendedContext)

            val differingBranchType = allBranches.firstNotNullOfOrNull {
                val branchType = it.determineType(context = extendedContext)

                branchType.takeIf { it != firstBranchType }
            }

            if (differingBranchType != null) {
                throw TypeCheckError("Not all branches of a match expression have same types (e.g. ${differingBranchType.toPrettyString()} is not ${firstBranchType.toPrettyString()})")
            }

            firstBranchType
        }
    }

    override fun validate() {
        type

        val unionTags = matcheeUnionType.tagNames

        val checkedTags = taggedBranches.map { it.requiredTagName }.toSet()

        val nonCheckedTags = unionTags - checkedTags

        if (nonCheckedTags.isNotEmpty()) {
            if (elseBranch == null) {
                throw TypeCheckError("Not all tags are checked by match expression and it has no else branch. Non-checked tags: $nonCheckedTags")
            }
        }

        val wrongTags = checkedTags - unionTags

        if (wrongTags.isNotEmpty()) {
            throw TypeCheckError("Not all tags are checked by match expression are declared by the matchee's union type. Wrong tags: $wrongTags")
        }

        if (explicitType != null) {
            allBranches.forEach {
                if (!it.type.isAssignableTo(explicitType)) {
                    throw TypeCheckError("Inferred branch expression type (${it.type.toPrettyString()}) is not assignable to the explicit match type ${explicitType.toPrettyString()}")
                }
            }
        }
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
    val explicitType: TypeExpressionB? = null,
    val taggedBranches: List<MatchBranchB>,
    val elseBranch: ExpressionB?,
) : ExpressionB {
    override fun build(scope: StaticScope): MatchExpression {
        val matcheeExpression = matchee.build(scope = scope)

        val explicitType = explicitType?.buildType(scope = scope)

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
                    castedType = NarrowUnionType(
                        alternatives = matcheeUnionType.alternatives,
                        narrowedAlternative = checkedAlternative,
                    ),
                ),
            )
        }

        return MatchExpression(
            matchee = matcheeExpression,
            explicitType = explicitType,
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
    private fun determineExpressionUnionType(context: TypeDeterminationContext): NarrowUnionType {
        val untageeType = untagee.determineType(context = context)

        return untageeType as? NarrowUnionType
            ?: throw TypeCheckError("Tried to untag an expression of non-narrow-union type")
    }

    override fun determineTypeDirectly(context: TypeDeterminationContext): Type {
        val extendedContext = context.withVisited(expression = this)

        val expressionUnionType = determineExpressionUnionType(
            context = extendedContext,
        )

        return expressionUnionType.narrowedType
    }

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
