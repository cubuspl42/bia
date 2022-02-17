package bia.model

import bia.parser.StaticScope

data class Program(
    val topLevelDeclarations: List<TopLevelDeclaration>,
) {
    fun validate() {
        topLevelDeclarations.forEach { it.validate() }
    }
}

data class ProgramB(
    val topLevelDeclarations: List<TopLevelDeclarationB>,
) {
    companion object {
        private fun buildTopLevelDeclarations(
            scope: StaticScope,
            inputDeclarations: List<TopLevelDeclarationB>,
        ): List<TopLevelDeclaration> = inputDeclarations.firstOrNull()?.let {
            val builtTopLevelDeclaration = it.build(
                scope = scope,
            )

            if ("A" in builtTopLevelDeclaration.extendedScope.typeAlikes.keys) {
                println("A!")
            }

            listOf(builtTopLevelDeclaration.topLevelDeclaration) + buildTopLevelDeclarations(
                scope = builtTopLevelDeclaration.extendedScope,
                inputDeclarations = inputDeclarations.drop(1),
            )
        } ?: emptyList()
    }

    fun build(scope: StaticScope): Program = Program(
        topLevelDeclarations = buildTopLevelDeclarations(
            scope = scope,
            inputDeclarations = topLevelDeclarations,
        ),
    )
}
