package bia.parser

import bia.model.ValueDeclaration
import bia.model.DefDeclaration
import bia.model.Type
import bia.model.TypeAlike
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
            typeAlikes: Map<String, List<TypeAlike>>,
        ): StaticScope = SimpleStaticScope(
            declarations = declarations,
            typeAlikes = typeAlikes,
        )

        val empty = of(
            declarations = emptyMap(),
            typeAlikes = emptyMap(),
        )
    }

    abstract val declarations: Map<String, ScopedDeclaration>

    abstract val typeAlikes: Map<String, List<TypeAlike>>

    fun extendClosed(name: String, declaration: ValueDeclaration) = StaticScope.of(
        declarations = declarations + (name to ClosedDeclaration(declaration)),
        typeAlikes = typeAlikes,
    )

    fun extendClosed(namedDeclarations: List<Pair<String, ValueDeclaration>>) = StaticScope.of(
        declarations = declarations + namedDeclarations.associate { (name, declaration) ->
            name to ClosedDeclaration(declaration)
        },
        typeAlikes = typeAlikes,
    )

    fun extendOpen(name: String, declaration: DefDeclaration) = StaticScope.of(
        declarations = declarations + (name to OpenFunctionDeclaration(declaration)),
        typeAlikes = typeAlikes,
    )

    fun extendType(name: String, typeAlike: TypeAlike): StaticScope {
        val typesWithGivenName = typeAlikes
            .getOrElse(name) { emptyList() }

        return of(
            declarations = declarations,
            typeAlikes = typeAlikes + (name to (typesWithGivenName + typeAlike)),
        )
    }

    fun allocateTypeVariable(givenName: String): AllocateTypeVariableResult {
        val typesWithGivenName = typeAlikes
            .getOrElse(givenName) { emptyList() }

        val allocatedVariable = TypeVariable(
            givenName = givenName,
            id = typesWithGivenName.size,
        )

        return AllocateTypeVariableResult(
            allocatedVariable = allocatedVariable,
            newScope = extendType(name = givenName, typeAlike = allocatedVariable),
        )
    }

    fun getScopedDeclaration(name: String): ScopedDeclaration? = declarations[name]

    fun getTypeAlike(givenName: String): TypeAlike {
        return typeAlikes[givenName]?.last() ?: throw TypeCheckError("There's no type or type constructor named $givenName")
    }
}

data class SimpleStaticScope(
    override val declarations: Map<String, ScopedDeclaration>,
    override val typeAlikes: Map<String, List<TypeAlike>>,
) : StaticScope()
