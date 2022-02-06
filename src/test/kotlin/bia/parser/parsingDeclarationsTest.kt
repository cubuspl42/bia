package bia.parser

import bia.model.ValueDeclaration
import bia.model.DefDeclaration
import bia.model.NumberType
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
}

private fun parseDeclaration(
    source: String,
): ValueDeclaration {
    val parser = buildAntlrParser(
        source = source,
        sourceName = "<declaration>",
    )

    return transformValueDeclaration(
        scope = StaticScope.empty,
        declaration = parser.declaration(),
    )
}
