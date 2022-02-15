package bia.type_checker

import bia.model.ArgumentDeclaration
import bia.model.ArgumentDeclarationB
import bia.model.BasicArgumentListDeclaration
import bia.model.BasicArgumentListDeclarationB
import bia.model.BooleanType
import bia.model.DefDeclarationB
import bia.model.FunctionBody
import bia.model.FunctionBodyB
import bia.model.FunctionType
import bia.model.expressions.IfExpression
import bia.model.expressions.IntLiteralExpression
import bia.model.expressions.IsExpression
import bia.model.expressions.MatchBranch
import bia.model.expressions.MatchExpression
import bia.model.NarrowUnionType
import bia.model.NumberType
import bia.model.expressions.ObjectFieldReadExpression
import bia.model.ObjectType
import bia.model.expressions.ReferenceExpression
import bia.model.SmartCastDeclaration
import bia.model.TaggedType
import bia.model.Type
import bia.model.TypeReference
import bia.model.TypeVariable
import bia.model.TypeVariableB
import bia.model.UnionAlternative
import bia.model.ValDeclaration
import bia.model.ValDeclarationB
import bia.model.Value
import bia.model.ValueDefinition
import bia.model.ValueDefinitionB
import bia.model.expressions.UntagExpression
import bia.model.WideUnionType
import bia.model.expressions.BooleanLiteralExpression
import bia.model.expressions.LambdaExpression
import bia.model.expressions.LambdaExpressionB
import bia.model.expressions.MatchBranchB
import bia.model.expressions.MatchExpressionB
import bia.model.expressions.ObjectFieldReadExpressionB
import bia.model.expressions.ReferenceExpressionB
import bia.model.expressions.UntagExpressionB
import bia.model.isAssignableTo
import bia.parser.ClosedDeclaration
import bia.parser.ScopedDeclaration
import bia.parser.StaticScope
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.exp
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TypeCheckingTest {
    @Test
    fun testIfIsTag() {
        val objectType = ObjectType(
            entries = mapOf(
                "field1" to NumberType,
                "field2" to BooleanType,
            ),
        )

        val alternative1 = UnionAlternative(
            tagName = "Tag1",
            type = objectType,
        )

        val alternative2 = UnionAlternative(
            tagName = "Tag2",
            type = BooleanType,
        )

        val unionType = WideUnionType(
            alternatives = setOf(
                alternative1,
                alternative2
            )
        )

        val argumentDeclaration = ArgumentDeclaration(
            givenName = "arg1",
            valueType = unionType,
        )

        val argumentReference = ReferenceExpression(
            referredName = "arg1",
            referredDeclaration = ClosedDeclaration(argumentDeclaration),
        )

        val objectExpression = ReferenceExpression(
            referredName = "arg1",
            referredDeclaration = ClosedDeclaration(
                SmartCastDeclaration(
                    givenName = "arg1",
                    valueType = NarrowUnionType(
                        alternatives = unionType.alternatives,
                        narrowedAlternative = alternative1,
                    )
                ),
            ),
        )

        val ifExpression = IfExpression(
            guard = IsExpression(
                checkee = argumentReference,
                checkedTagName = "Tag1",
            ),
            trueBranch = ObjectFieldReadExpression(
                objectExpression = UntagExpression(
                    untagee = objectExpression,
                ),
                readFieldName = "field1"
            ),
            falseBranch = IntLiteralExpression(
                value = 0L,
            ),
        )

        ifExpression.validate()

        assertEquals(
            expected = NumberType,
            actual = ifExpression.type,
        )
    }

    @Test
    fun testTaggedIsAssignableToUnion() {
        val objectType1 = ObjectType(
            entries = mapOf(
                "field1" to NumberType,
                "field2" to BooleanType,
            ),
        )

        val objectType2 = ObjectType(
            entries = mapOf(
                "field3" to NumberType,
            ),
        )

        val unionType = WideUnionType(
            alternatives = setOf(
                UnionAlternative(
                    tagName = "Tag1",
                    type = objectType1,
                ),
                UnionAlternative(
                    tagName = "Tag2",
                    type = BooleanType,
                )
            )
        )

        assertTrue(
            actual = TaggedType(
                taggedType = objectType1,
                attachedTagName = "Tag1",
            ).isAssignableTo(unionType),
        )

        assertFalse(
            actual = TaggedType(
                taggedType = objectType1,
                attachedTagName = "Tag2",
            ).isAssignableTo(unionType),
        )

        assertFalse(
            actual = TaggedType(
                taggedType = objectType2,
                attachedTagName = "Tag1",
            ).isAssignableTo(unionType),
        )

        assertThrows<TypeCheckError> {
            TaggedType(
                taggedType = objectType1,
                attachedTagName = "Tag3",
            ).isAssignableTo(unionType)
        }
    }

    @Test
    fun testMatchMissingElseBranch() {
        val unionType = WideUnionType(
            alternatives = setOf(
                UnionAlternative(
                    tagName = "Foo",
                    type = NumberType,
                ),
                UnionAlternative(
                    tagName = "Bar",
                    type = BooleanType,
                ),
            ),
        )

        assertThrows<TypeCheckError> {
            val matchExpression = MatchExpressionB(
                matchee = ReferenceExpressionB(
                    referredName = "arg",
                ),
                taggedBranches = listOf(
                    MatchBranchB(
                        requiredTagName = "Foo",
                        branch = BooleanLiteralExpression(value = true),
                    ),
                ),
                elseBranch = null,
            ).build(
                scope = StaticScope.of(
                    declarations = mapOf(
                        argumentDeclaration(
                            givenName = "arg",
                            valueType = unionType,
                        ),
                    ),
                    typeVariables = emptyMap(),
                ),
            )

            matchExpression.validate()
        }
    }

    @Test
    fun testMatchWrongTag() {
        val unionType = WideUnionType(
            alternatives = setOf(
                UnionAlternative(
                    tagName = "Foo",
                    type = NumberType,
                ),
                UnionAlternative(
                    tagName = "Bar",
                    type = BooleanType,
                )
            )
        )

        assertThrows<TypeCheckError> {
            val matchExpression = MatchExpressionB(
                matchee = ReferenceExpressionB(
                    referredName = "arg",
                ),
                taggedBranches = listOf(
                    MatchBranchB(
                        requiredTagName = "Foo",
                        branch = IntLiteralExpression(value = 10),
                    ),
                    MatchBranchB(
                        requiredTagName = "Bar",
                        branch = IntLiteralExpression(value = 20),
                    ),
                    MatchBranchB(
                        requiredTagName = "Baz",
                        branch = IntLiteralExpression(value = 30),
                    ),
                ),
                elseBranch = null,
            ).build(
                scope = StaticScope.of(
                    declarations = mapOf(
                        argumentDeclaration(
                            givenName = "arg",
                            valueType = unionType,
                        ),
                    ),
                    typeVariables = emptyMap(),
                ),
            )

            matchExpression.validate()
        }
    }

    @Test
    fun testMatchSmartCast() {
        val objectType1 = ObjectType(
            entries = mapOf(
                "field1" to NumberType,
            ),
        )

        val objectType2 = ObjectType(
            entries = mapOf(
                "field2" to NumberType,
            ),
        )

        val unionType = WideUnionType(
            alternatives = setOf(
                UnionAlternative(
                    tagName = "Foo",
                    type = objectType1,
                ),
                UnionAlternative(
                    tagName = "Bar",
                    type = objectType2,
                )
            )
        )

        val matchExpression = MatchExpressionB(
            matchee = ReferenceExpressionB(
                referredName = "arg",
            ),
            taggedBranches = listOf(
                MatchBranchB(
                    requiredTagName = "Foo",
                    branch = ObjectFieldReadExpressionB(
                        objectExpression = UntagExpressionB(
                            untagee = ReferenceExpressionB(
                                referredName = "arg",
                            ),
                        ),
                        readFieldName = "field1",
                    ),
                ),
                MatchBranchB(
                    requiredTagName = "Bar",
                    branch = ObjectFieldReadExpressionB(
                        objectExpression = UntagExpressionB(
                            untagee = ReferenceExpressionB(
                                referredName = "arg",
                            ),
                        ),
                        readFieldName = "field2",
                    ),
                ),
            ),
            elseBranch = null,
        ).build(
            scope = StaticScope.of(
                declarations = mapOf(
                    argumentDeclaration(
                        givenName = "arg",
                        valueType = unionType,
                    ),
                ),
                typeVariables = emptyMap(),
            ),
        )

        matchExpression.validate()

        assertEquals(
            expected = NumberType,
            actual = matchExpression.type,
        )
    }

    @Test
    fun testDefDeclarationGeneric() {
        val builtDefDeclaration = DefDeclarationB(
            givenName = "id",
            typeVariables = listOf(
                TypeVariableB("A"),
            ),
            argumentListDeclaration = BasicArgumentListDeclarationB(
                argumentDeclarations = listOf(
                    ArgumentDeclarationB(
                        givenName = "a",
                        valueType = TypeReference(referredName = "A"),
                    ),
                ),
            ),
            explicitReturnType = TypeReference(referredName = "A"),
            body = FunctionBodyB(
                definitions = emptyList(),
                returned = ReferenceExpressionB(
                    referredName = "a",
                ),
            )
        ).build(
            scope = StaticScope.empty,
        )

        val defDeclaration = builtDefDeclaration.defDeclaration

        defDeclaration.validate()

        val extendedScope = builtDefDeclaration.extendedScope

        assertContains(
            iterable = extendedScope.declarations.values,
            element = ClosedDeclaration(defDeclaration),
        )
    }

    @Test
    fun testLambdaExpressionGeneric() {
        val lambdaExpression = LambdaExpressionB(
            typeVariables = listOf(
                TypeVariableB("A"),
            ),
            argumentListDeclaration = BasicArgumentListDeclarationB(
                argumentDeclarations = listOf(
                    ArgumentDeclarationB(
                        givenName = "a",
                        valueType = TypeReference(referredName = "A"),
                    ),
                ),
            ),
            explicitReturnType = TypeReference(referredName = "A"),
            body = FunctionBodyB(
                definitions = emptyList(),
                returned = ReferenceExpressionB(
                    referredName = "a",
                ),
            )
        ).build(
            scope = StaticScope.empty,
        )

        val typeVariable = TypeVariable(givenName = "A", id = 0)

        assertEquals(
            expected = LambdaExpression(
                typeVariables = listOf(
                    typeVariable,
                ),
                argumentListDeclaration = BasicArgumentListDeclaration(
                    argumentDeclarations = listOf(
                        ArgumentDeclaration(
                            givenName = "a",
                            valueType = typeVariable,
                        ),
                    ),
                ),
                explicitReturnType = typeVariable,
                body = FunctionBody(
                    definitions = emptyList(),
                    returned = ReferenceExpression(
                        referredName = "a",
                        referredDeclaration = ClosedDeclaration(
                            declaration = ArgumentDeclaration(
                                givenName = "a",
                                valueType = typeVariable,
                            ),
                        ),
                    ),
                ),
            ),
            actual = lambdaExpression,
        )

        assertEquals(
            expected = FunctionType(
                typeVariables = listOf(
                    typeVariable,
                ),
                argumentListDeclaration = BasicArgumentListDeclaration(
                    argumentDeclarations = listOf(
                        ArgumentDeclaration(
                            givenName = "a",
                            valueType = typeVariable,
                        ),
                    ),
                ),
                returnType = typeVariable,
            ),
            actual = lambdaExpression.type,
        )
    }

    @Test
    fun testFunctionBodyReferences() {
        val functionBody = FunctionBodyB(
            definitions = listOf(
                ValDeclarationB(
                    givenName = "a",
                    initializer = IntLiteralExpression(value = 10),
                ),
                ValDeclarationB(
                    givenName = "b",
                    initializer = ReferenceExpressionB(referredName = "a"),
                ),
            ),
            returned = ReferenceExpressionB(referredName = "b"),
        ).build(
            scope = StaticScope.empty,
        )

        functionBody.validate()

        val aDef = functionBody.definitions.single { it.givenName == "a" }

        assertEquals(
            expected = NumberType,
            actual = aDef.valueType,
        )

        val bDef = functionBody.definitions.single { it.givenName == "b" }

        assertEquals(
            expected = NumberType,
            actual = bDef.valueType,
        )

        assertEquals(
            expected = NumberType,
            actual = functionBody.returned.type,
        )
    }
}

private fun argumentDeclaration(
    givenName: String,
    valueType: Type,
): Pair<String, ScopedDeclaration> = givenName to ClosedDeclaration(
    declaration = ArgumentDeclaration(
        givenName = givenName,
        valueType = valueType,
    )
)

private fun argumentReference(
    referredName: String,
    valueType: Type,
) = ReferenceExpression(
    referredName = referredName,
    referredDeclaration = ClosedDeclaration(
        declaration = ArgumentDeclaration(
            givenName = referredName,
            valueType = valueType,
        ),
    ),
)
