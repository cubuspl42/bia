package bia.parser

import bia.model.Declaration
import bia.model.FunctionDeclaration

sealed interface ScopedDeclaration

data class ClosedDeclaration(
    val declaration: Declaration,
) : ScopedDeclaration

data class OpenFunctionDeclaration(
    val functionDeclaration: FunctionDeclaration,
) : ScopedDeclaration

abstract class StaticScope {
    companion object {
        fun of(
            declarations: Map<String, ScopedDeclaration>,
        ): StaticScope = SimpleStaticScope(
            declarations = declarations,
        )
    }

    abstract val declarations: Map<String, ScopedDeclaration>

    fun extendClosed(name: String, declaration: Declaration) = StaticScope.of(
        declarations = declarations + (name to ClosedDeclaration(declaration)),
    )

    fun extendClosed(namedDeclarations: List<Pair<String, Declaration>>) = StaticScope.of(
        declarations = declarations + namedDeclarations.associate { (name, declaration) ->
            name to ClosedDeclaration(declaration)
        },
    )

    fun extendOpen(name: String, declaration: FunctionDeclaration) = StaticScope.of(
        declarations = declarations + (name to OpenFunctionDeclaration(declaration)),
    )

    fun getScopedDeclaration(name: String): ScopedDeclaration? = declarations[name]
}

data class SimpleStaticScope(
    override val declarations: Map<String, ScopedDeclaration>,
) : StaticScope()
