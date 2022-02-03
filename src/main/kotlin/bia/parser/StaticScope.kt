package bia.parser

import bia.model.Declaration
import bia.model.FunctionDeclaration
import bia.model.TypeVariable
import bia.type_checker.TypeCheckError

sealed interface ScopedDeclaration

data class ClosedDeclaration(
    val declaration: Declaration,
) : ScopedDeclaration

data class OpenFunctionDeclaration(
    val functionDeclaration: FunctionDeclaration,
) : ScopedDeclaration

data class AllocateTypeVariableResult(
    val allocatedVariable: TypeVariable,
    val newScope: StaticScope,
)

abstract class StaticScope {
    companion object {
        fun of(
            declarations: Map<String, ScopedDeclaration>,
            typeVariables: Map<String, List<TypeVariable>>,
        ): StaticScope = SimpleStaticScope(
            declarations = declarations,
            typeVariables = typeVariables,
        )
    }

    abstract val declarations: Map<String, ScopedDeclaration>

    abstract val typeVariables: Map<String, List<TypeVariable>>

    fun extendClosed(name: String, declaration: Declaration) = StaticScope.of(
        declarations = declarations + (name to ClosedDeclaration(declaration)),
        typeVariables = typeVariables,
    )

    fun extendClosed(namedDeclarations: List<Pair<String, Declaration>>) = StaticScope.of(
        declarations = declarations + namedDeclarations.associate { (name, declaration) ->
            name to ClosedDeclaration(declaration)
        },
        typeVariables = typeVariables,
    )

    fun extendOpen(name: String, declaration: FunctionDeclaration) = StaticScope.of(
        declarations = declarations + (name to OpenFunctionDeclaration(declaration)),
        typeVariables = typeVariables,
    )

    fun allocateTypeVariable(givenName: String): AllocateTypeVariableResult {
        val variablesWithGivenName: List<TypeVariable> = typeVariables
            .getOrElse(givenName) { emptyList() }

        val allocatedVariable = TypeVariable(
            givenName = givenName,
            id = variablesWithGivenName.size,
        )

        return AllocateTypeVariableResult(
            allocatedVariable = allocatedVariable,
            newScope = of(
                declarations = declarations,
                typeVariables = typeVariables +
                        (givenName to (variablesWithGivenName + allocatedVariable))
            ),
        )
    }

    fun getScopedDeclaration(name: String): ScopedDeclaration? = declarations[name]

    fun getClosestTypeVariable(givenName: String): TypeVariable {
        return typeVariables[givenName]?.last() ?: throw TypeCheckError("There's no type variable named $givenName")
    }
}

data class SimpleStaticScope(
    override val declarations: Map<String, ScopedDeclaration>,
    override val typeVariables: Map<String, List<TypeVariable>>,
) : StaticScope()
