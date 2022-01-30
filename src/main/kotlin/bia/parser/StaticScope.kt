package bia.parser

import bia.model.Declaration

abstract class StaticScope {
    companion object {
        fun of(declarations: Map<String, Declaration>): StaticScope = SimpleStaticScope(
            declarations = declarations,
        )

        fun delegated(delegate: () -> StaticScope): StaticScope = object : StaticScope() {
            override val declarations: Map<String, Declaration> by lazy { delegate().declarations }
        }
    }

    abstract val declarations: Map<String, Declaration>

    fun extend(name: String, declaration: Declaration) = StaticScope.of(
        declarations = declarations + (name to declaration)
    )

    fun extend(namedDeclarations: List<Pair<String, Declaration>>) = StaticScope.of(
        declarations = declarations + namedDeclarations.toMap()
    )

    fun getDeclaration(name: String): Declaration? = declarations[name]
}

data class SimpleStaticScope(
    override val declarations: Map<String, Declaration>,
) : StaticScope()
