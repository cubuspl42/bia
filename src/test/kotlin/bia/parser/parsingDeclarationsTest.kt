package bia.parser

import bia.model.BooleanType
import bia.model.DefDeclaration
import bia.model.NumberType
import bia.model.ObjectTypeB
import bia.model.SingletonDeclarationB
import bia.model.TopLevelDeclarationB
import bia.model.TypeAliasDeclarationB
import bia.model.TypeReference
import bia.model.TypeVariableB
import bia.model.UnionAlternativeB
import bia.model.UnionDeclarationB
import bia.model.ValueDeclaration
import bia.model.VarargArgumentListDeclaration
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class ParsingDeclarationsTest {
    @Test
    fun parseVarargFunctionDeclaration() {
        val declaration = parseDeclaration(
            source = "external def f(a : Number...): Boolean",
        )

        assertIs<DefDeclaration>(declaration)

        assertEquals(
            expected = VarargArgumentListDeclaration(
                givenName = "a",
                type = NumberType,
            ),
            actual = declaration.argumentListDeclaration,
        )
    }

    @Test
    fun parseBasicUnionDeclaration() {
        val declaration = parseTopLevelDeclarationB(
            source = "union Union1 = Foo # Tag | Bar",
        )

        assertEquals(
            expected = UnionDeclarationB(
                unionName = "Union1",
                typeArguments = emptyList(),
                alternatives = listOf(
                    UnionAlternativeB(
                        explicitTagName = "Tag",
                        typeExpression = TypeReference("Foo"),
                    ),
                    UnionAlternativeB(
                        explicitTagName = null,
                        typeExpression = TypeReference("Bar"),
                    ),
                ),
            ),
            actual = declaration,
        )
    }

    @Test
    fun parseGenericUnionDeclaration() {
        val declaration = parseTopLevelDeclarationB(
            source = "union Union1 = <A, B> A | B | Foo",
        )

        assertEquals(
            expected = UnionDeclarationB(
                unionName = "Union1",
                typeArguments = listOf(
                    TypeVariableB(givenName = "A"),
                    TypeVariableB(givenName = "B"),
                ),
                alternatives = listOf(
                    UnionAlternativeB(
                        explicitTagName = null,
                        typeExpression = TypeReference("A"),
                    ),
                    UnionAlternativeB(
                        explicitTagName = null,
                        typeExpression = TypeReference("B"),
                    ),
                    UnionAlternativeB(
                        explicitTagName = null,
                        typeExpression = TypeReference("Foo"),
                    ),
                ),
            ),
            actual = declaration,
        )
    }

    @Test
    fun parseSingletonDeclaration() {
        val declaration = parseTopLevelDeclarationB(
            source = "singleton Foo",
        )

        assertEquals(
            expected = SingletonDeclarationB(
                singletonName = "Foo",
            ),
            actual = declaration,
        )
    }

    @Test
    fun parseTypeAliasObject() {
        val declaration = parseTopLevelDeclarationB(
            source = "typealias Foo = { a : Number, b : Boolean }",
        )

        assertEquals(
            expected = TypeAliasDeclarationB(
                aliasName = "Foo",
                aliasedType = ObjectTypeB(
                    entries = mapOf(
                        "a" to NumberType,
                        "b" to BooleanType,
                    ),
                ),
            ),
            actual = declaration,
        )
    }

    @Test
    fun parseTypeAliasObjectGeneric() {
        val declaration = parseTopLevelDeclarationB(
            source = "typealias Foo = <A, B> { a : A, b : B, c : Number }",
        )

        assertEquals(
            expected = TypeAliasDeclarationB(
                aliasName = "Foo",
                aliasedType = ObjectTypeB(
                    typeArguments = listOf(
                        TypeVariableB(givenName = "A"),
                        TypeVariableB(givenName = "B"),
                    ),
                    entries = mapOf(
                        "a" to TypeReference(referredName = "A"),
                        "b" to TypeReference(referredName = "B"),
                        "c" to NumberType,
                    ),
                ),
            ),
            actual = declaration,
        )
    }
}

private fun parseDeclaration(
    source: String,
): ValueDeclaration {
    val parser = buildAntlrParser(
        source = source,
        sourceName = "<declaration>",
    )

    return transformValueDefinition(
        declaration = parser.declaration(),
    ).build(
        scope = StaticScope.empty,
    ).valueDefinition
}

private fun parseTopLevelDeclarationB(
    source: String,
): TopLevelDeclarationB {
    val parser = buildAntlrParser(
        source = source,
        sourceName = "<top-level declaration>",
    )

    return transformTopLevelDeclaration(
        topLevelDeclaration = parser.topLevelDeclaration(),
    )
}
