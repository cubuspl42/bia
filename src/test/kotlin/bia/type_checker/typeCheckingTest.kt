package bia.type_checker

import bia.model.ArgumentDeclaration
import bia.model.BooleanType
import bia.model.IfExpression
import bia.model.IntLiteralExpression
import bia.model.IsExpression
import bia.model.MatchBranch
import bia.model.MatchExpression
import bia.model.NarrowUnionType
import bia.model.NumberType
import bia.model.ObjectFieldReadExpression
import bia.model.ObjectType
import bia.model.ReferenceExpression
import bia.model.SmartCastDeclaration
import bia.model.TaggedType
import bia.model.Type
import bia.model.UnionAlternative
import bia.model.UntagExpression
import bia.model.ValueDeclaration
import bia.model.WideUnionType
import bia.model.isAssignableTo
import bia.parser.ClosedDeclaration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
                expression = argumentReference,
                checkedTagName = "Tag1",
            ),
            trueBranch = ObjectFieldReadExpression(
                objectExpression = UntagExpression(
                    expression = objectExpression,
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
    fun testMatch() {
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
                    type = BooleanType,
                )
            )
        )

        assertEquals(
            expected = NumberType,
            actual = MatchExpression(
                matchee = argumentReference(
                    referredName = "arg",
                    valueType = unionType,
                ),
                taggedBranches = listOf(
                    MatchBranch(
                        requiredTagName = "Foo",
                        branch = ObjectFieldReadExpression(
                            objectExpression = UntagExpression(
                                expression = argumentReference(
                                    referredName = "foo",
                                    valueType = objectType1,
                                ),
                            ),
                            readFieldName = "field1",
                        ),
                    ),
                    MatchBranch(
                        requiredTagName = "Bar",
                        branch = ObjectFieldReadExpression(
                            objectExpression = UntagExpression(
                                expression = argumentReference(
                                    referredName = "bar",
                                    valueType = objectType2,
                                ),
                            ),
                            readFieldName = "field2",
                        ),
                    ),
                ),
                elseBranch = null,
            ).type,
        )
    }
}

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
