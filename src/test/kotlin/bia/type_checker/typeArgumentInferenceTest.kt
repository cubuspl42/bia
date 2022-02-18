package bia.type_checker

import bia.model.ArgumentDeclaration
import bia.model.BasicArgumentListDeclaration
import bia.model.BooleanType
import bia.model.FunctionType
import bia.model.ListType
import bia.model.NumberType
import bia.model.ObjectType
import bia.model.SequenceType
import bia.model.TypeVariable
import bia.model.TypeVariableMapping
import bia.model.UnionAlternative
import bia.model.WideUnionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class TypeArgumentInferenceTest {
    companion object {
        private val tvA = TypeVariable(givenName = "A", id = 0)

        private val tvB = TypeVariable(givenName = "B", id = 0)
    }

    @Test
    fun testDirectBasic() {
        val typeVariableMapping = inferTypeVariableMappingForPair(
            typeArguments = setOf(tvA, tvB),
            matchee = tvA,
            matcher = NumberType,
        )

        assertEquals(
            expected = TypeVariableMapping(
                mapping = mapOf(
                    tvA to NumberType,
                )
            ),
            actual = typeVariableMapping,
        )

    }

    @Test
    fun testDirectIncompatible() {
        assertThrows<TypeCheckError> {
            inferTypeVariableMappingForPair(
                typeArguments = setOf(tvA, tvB),
                matchee = TypeVariable(givenName = "C", id = 0),
                matcher = NumberType,
            )
        }
    }

    @Test
    fun testDirectList() {
        val typeVariableMapping = inferTypeVariableMappingForPair(
            typeArguments = setOf(tvA, tvB),
            matchee = tvA,
            matcher = ListType(elementType = NumberType),
        )

        assertEquals(
            expected = TypeVariableMapping(
                mapping = mapOf(
                    tvA to ListType(elementType = NumberType),
                )
            ),
            actual = typeVariableMapping,
        )
    }

    @Test
    fun testListIncompatible() {
        assertThrows<TypeCheckError> {
            inferTypeVariableMappingForPair(
                typeArguments = setOf(tvA, tvB),
                matchee = ListType(tvA),
                matcher = NumberType,
            )
        }
    }

    @Test
    fun testList() {
        val typeVariableMapping = inferTypeVariableMappingForPair(
            typeArguments = setOf(tvA, tvB),
            matchee = ListType(tvA),
            matcher = ListType(BooleanType),
        )

        assertEquals(
            expected = TypeVariableMapping(
                mapping = mapOf(
                    tvA to BooleanType,
                ),
            ),
            actual = typeVariableMapping,
        )
    }

    @Test
    fun testUnion() {
        val typeVariableMapping = inferTypeVariableMappingForPair(
            typeArguments = setOf(tvA, tvB),
            matchee = WideUnionType(
                alternatives = setOf(
                    UnionAlternative(
                        tagName = "Foo",
                        type = tvA,
                    ),
                    UnionAlternative(
                        tagName = "Bar",
                        type = NumberType,
                    ),
                )
            ),
            matcher = WideUnionType(
                alternatives = setOf(
                    UnionAlternative(
                        tagName = "Foo",
                        type = BooleanType,
                    ),
                    UnionAlternative(
                        tagName = "Bar",
                        type = NumberType,
                    ),
                )
            ),
        )

        assertEquals(
            expected = TypeVariableMapping(
                mapping = mapOf(
                    tvA to BooleanType,
                )
            ),
            actual = typeVariableMapping,
        )
    }

    @Test
    fun testUnionIncompatible() {
        assertThrows<TypeCheckError> {
            inferTypeVariableMappingForPair(
                typeArguments = setOf(tvA, tvB),
                matchee = WideUnionType(
                    alternatives = setOf(
                        UnionAlternative(
                            tagName = "Foo",
                            type = tvA,
                        ),
                        UnionAlternative(
                            tagName = "Bar",
                            type = NumberType,
                        ),
                    )
                ),
                matcher = WideUnionType(
                    alternatives = setOf(
                        UnionAlternative(
                            tagName = "Foo",
                            type = BooleanType,
                        ),
                        UnionAlternative(
                            tagName = "Baz",
                            type = ListType(elementType = NumberType),
                        ),
                    )
                ),
            )
        }
    }

    @Test
    fun testObject() {
        val typeVariableMapping = inferTypeVariableMappingForPair(
            typeArguments = setOf(tvA, tvB),
            matchee = ObjectType(
                entries = mapOf(
                    "foo" to tvA,
                    "bar" to tvB,
                    "baz" to BooleanType,
                ),
            ),
            matcher = ObjectType(
                entries = mapOf(
                    "foo" to NumberType,
                    "bar" to BooleanType,
                    "baz" to BooleanType,
                ),
            ),
        )

        assertEquals(
            expected = TypeVariableMapping(
                mapping = mapOf(
                    tvA to NumberType,
                    tvB to BooleanType,
                ),
            ),
            actual = typeVariableMapping,
        )
    }

    @Test
    fun testObjectIncompatible() {
        assertThrows<TypeCheckError> {
            inferTypeVariableMappingForPair(
                typeArguments = setOf(tvA, tvB),
                matchee = ObjectType(
                    entries = mapOf(
                        "foo" to tvA,
                        "bar" to NumberType,
                    ),
                ),
                matcher = ObjectType(
                    entries = mapOf(
                        "foo" to NumberType,
                        "baz" to BooleanType,
                    ),
                ),
            )
        }
    }

    @Test
    fun testObjectComplex() {
        val typeVariableMapping = inferTypeVariableMappingForPair(
            typeArguments = setOf(tvA, tvB),
            matchee = ObjectType(
                entries = mapOf(
                    "foo" to WideUnionType(
                        alternatives = setOf(
                            UnionAlternative(
                                tagName = "Foo",
                                type = tvA,
                            ),
                            UnionAlternative(
                                tagName = "Bar",
                                type = NumberType,
                            ),
                        )
                    ),
                    "bar" to ListType(elementType = tvA),
                ),
            ),
            matcher = ObjectType(
                entries = mapOf(
                    "foo" to WideUnionType(
                        alternatives = setOf(
                            UnionAlternative(
                                tagName = "Foo",
                                type = SequenceType(elementType = NumberType),
                            ),
                            UnionAlternative(
                                tagName = "Bar",
                                type = NumberType,
                            ),
                        )
                    ),
                    "bar" to ListType(
                        elementType = SequenceType(elementType = NumberType),
                    ),
                ),
            ),
        )

        assertEquals(
            expected = TypeVariableMapping(
                mapping = mapOf(
                    tvA to SequenceType(elementType = NumberType),
                ),
            ),
            actual = typeVariableMapping,
        )
    }

    @Test
    fun testObjectAmbiguous() {
        assertThrows<TypeCheckError> {
            inferTypeVariableMappingForPair(
                typeArguments = setOf(tvA, tvB),
                matchee = ObjectType(
                    entries = mapOf(
                        "foo" to tvA,
                        "bar" to tvA,
                    ),
                ),
                matcher = ObjectType(
                    entries = mapOf(
                        "foo" to NumberType,
                        "bar" to BooleanType,
                    ),
                ),
            )
        }
    }

    @Test
    fun testFunction() {
        val typeVariableMapping = inferTypeVariableMappingForPair(
            typeArguments = setOf(tvA, tvB),
            matchee = FunctionType(
                typeArguments = emptyList(),
                argumentListDeclaration = BasicArgumentListDeclaration(
                    argumentDeclarations = listOf(
                        ArgumentDeclaration(
                            givenName = "a",
                            valueType = tvA,
                        ),
                        ArgumentDeclaration(
                            givenName = "n",
                            valueType = NumberType,
                        ),
                    ),
                ),
                returnType = tvB,
            ),
            matcher = FunctionType(
                typeArguments = emptyList(),
                argumentListDeclaration = BasicArgumentListDeclaration(
                    argumentDeclarations = listOf(
                        ArgumentDeclaration(
                            givenName = "a2",
                            valueType = BooleanType,
                        ),
                        ArgumentDeclaration(
                            givenName = "n2",
                            valueType = NumberType,
                        ),
                    ),
                ),
                returnType = ObjectType(
                    entries = mapOf(
                        "foo" to NumberType,
                    ),
                ),
            ),
        )

        assertEquals(
            expected = TypeVariableMapping(
                mapping = mapOf(
                    tvA to BooleanType,
                    tvB to ObjectType(
                        entries = mapOf(
                            "foo" to NumberType,
                        ),
                    ),
                ),
            ),
            actual = typeVariableMapping,
        )
    }

}
