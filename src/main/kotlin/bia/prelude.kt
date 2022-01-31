package bia

import bia.parser.StaticScope
import bia.parser.buildAntlrParser
import bia.parser.transformDeclarations

data class Prelude(
    val scope: StaticScope,
) {
    companion object {
        fun load(preludeSource: String): Prelude {
            val parser = buildAntlrParser(
                source = preludeSource,
                sourceName = "prelude",
            )

            val result = transformDeclarations(
                scope = StaticScope.of(declarations = emptyMap()),
                inputDeclarations = parser.declarationList().declaration(),
                outputDeclarations = emptyList(),
            )

            result.declarations.forEach {
                it.validate()
            }

            return Prelude(
                scope = result.finalScope,
            )
        }
    }
}
