package bia.parser

import bia.model.ValueDeclaration
import bia.model.DefDeclaration
import bia.model.Type
import bia.model.TypeVariable
import bia.type_checker.TypeCheckError

sealed interface ScopedDeclaration {
    val declaration: ValueDeclaration
}

data class ClosedDeclaration(
    override val declaration: ValueDeclaration,
) : ScopedDeclaration

data class OpenFunctionDeclaration(
    val functionDeclaration: DefDeclaration,
) : ScopedDeclaration {
    override val declaration: ValueDeclaration
        get() = functionDeclaration
}

data class AllocateTypeVariableResult(
    val allocatedVariable: TypeVariable,
    val newScope: StaticScope,
)

abstract class StaticScope {
    companion object {
        fun of(
            declarations: Map<String, ScopedDeclaration>,
            typeVariables: Map<String, List<Type>>,
        ): StaticScope = SimpleStaticScope(
            declarations = declarations,
            types = typeVariables,
        )

        val empty = of(
            declarations = emptyMap(),
            typeVariables = emptyMap(),
        )
    }

    abstract val declarations: Map<String, ScopedDeclaration>

    abstract val types: Map<String, List<Type>>

    fun extendClosed(name: String, declaration: ValueDeclaration) = StaticScope.of(
        declarations = declarations + (name to ClosedDeclaration(declaration)),
        typeVariables = types,
    )

    fun extendClosed(namedDeclarations: List<Pair<String, ValueDeclaration>>) = StaticScope.of(
        declarations = declarations + namedDeclarations.associate { (name, declaration) ->
            name to ClosedDeclaration(declaration)
        },
        typeVariables = types,
    )

    fun extendOpen(name: String, declaration: DefDeclaration) = StaticScope.of(
        declarations = declarations + (name to OpenFunctionDeclaration(declaration)),
        typeVariables = types,
    )

    fun extendScoped(name: String, declaration: ScopedDeclaration) = StaticScope.of(
        declarations = declarations + (name to declaration),
        typeVariables = types,
    )

    fun extendType(name: String, type: Type): StaticScope {
        val typesWithGivenName = types
            .getOrElse(name) { emptyList() }

        return of(
            declarations = declarations,
            typeVariables = types + (name to (typesWithGivenName + type)),
        )
    }

    fun allocateTypeVariable(givenName: String): AllocateTypeVariableResult {
        val typesWithGivenName = types
            .getOrElse(givenName) { emptyList() }

        val allocatedVariable = TypeVariable(
            givenName = givenName,
            id = typesWithGivenName.size,
        )

        return AllocateTypeVariableResult(
            allocatedVariable = allocatedVariable,
            newScope = extendType(name = givenName, type = allocatedVariable),
        )
    }

    fun getScopedDeclaration(name: String): ScopedDeclaration? = declarations[name]

    fun getType(givenName: String): Type {
        return types[givenName]?.last() ?: throw TypeCheckError("There's no type variable named $givenName")
    }
}

data class SimpleStaticScope(
    override val declarations: Map<String, ScopedDeclaration>,
    override val types: Map<String, List<Type>>,
) : StaticScope()
