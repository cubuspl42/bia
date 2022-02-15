package bia

import bia.model.buildValueDefinitions
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

            val result = buildValueDefinitions(
                scope = StaticScope.of(
                    declarations = emptyMap(),
                    typeVariables = emptyMap(),
                ),
                definitions = transformDeclarations(
                    inputDeclarations = parser.declarationList().declaration(),
                )
            )

            result.definitions.forEach {
                it.validate()
            }

            return Prelude(
                scope = result.extendedScope,
            )
        }
    }
}
