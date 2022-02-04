package bia.parser

import bia.model.Declaration
import bia.model.FunctionDeclaration
import bia.model.NumberType
import bia.model.TypeVariable
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

        assertIs<FunctionDeclaration>(declaration)

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
): Declaration {
    val parser = buildAntlrParser(
        source = source,
        sourceName = "<declaration>",
    )

    return transformBodyDeclaration(
        scope = StaticScope.empty,
        declaration = parser.declaration(),
    )
}
