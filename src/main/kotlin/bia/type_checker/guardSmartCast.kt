package bia.type_checker

import bia.model.NarrowUnionType
import bia.model.SmartCastDeclaration
import bia.model.UnionType
import bia.model.expressions.Expression
import bia.model.expressions.IsExpression
import bia.model.expressions.ReferenceExpression
import bia.parser.StaticScope

fun processGuardSmartCast(
    scope: StaticScope,
    guard: Expression,
): StaticScope = if (guard is IsExpression) {
    val isExpression = guard.checkee

    if (isExpression is ReferenceExpression) {
        val referredDeclaration = isExpression.referredDeclaration

        if (referredDeclaration != null) {
            val declaration = referredDeclaration.declaration
            val valueType = declaration.valueType

            if (valueType is UnionType) {
                val checkedAlternative = valueType.getAlternative(tagName = guard.checkedTagName)

                if (checkedAlternative != null) {
                    scope.extendClosed(
                        name = declaration.givenName,
                        declaration = SmartCastDeclaration(
                            givenName = declaration.givenName,
                            valueType = NarrowUnionType(
                                alternatives = valueType.alternatives,
                                narrowedAlternative = checkedAlternative,
                            ),
                        ),
                    )
                } else scope
            } else scope
        } else scope
    } else scope
} else scope
