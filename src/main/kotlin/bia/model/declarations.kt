package bia.model

import bia.type_checker.TypeCheckError

sealed interface TopLevelDeclaration {
    fun validate()
}

sealed interface ValueDeclaration {
    val givenName: String
    val valueType: Type
}

sealed interface ValueDefinition : ValueDeclaration, TopLevelDeclaration

data class ValDeclaration(
    override val givenName: String,
    val initializer: Expression,
) : ValueDefinition {
    override val valueType: Type by lazy {
        initializer.type
    }

    override fun validate() {
        initializer.validate()
    }
}

data class DefDeclaration(
    override val givenName: String,
    val typeVariables: List<TypeVariable>,
    val argumentListDeclaration: ArgumentListDeclaration,
    val explicitReturnType: Type?,
    val buildDefinition: () -> FunctionDefinition?,
) : ValueDefinition {
    val definition by lazy { buildDefinition() }

    val body: FunctionBody? by lazy { definition?.body }

    val explicitType: FunctionType? = explicitReturnType?.let {
        FunctionType(
            typeVariables = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            returnType = it,
        )
    }

    override val valueType: FunctionType by lazy {
        val returnType = explicitReturnType ?: body?.returned?.type
        ?: throw RuntimeException("Could not determine function return type")

        explicitType ?: FunctionType(
            typeVariables = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            returnType = returnType,
        )
    }

    override fun validate() {
        validateFunction(
            body = body,
            explicitReturnType = explicitReturnType,
        )
    }
}

fun validateFunction(
    body: FunctionBody?,
    explicitReturnType: Type?,
) {
    body?.validate()

    val inferredReturnType = body?.returned?.type

    if (explicitReturnType != null && inferredReturnType != null) {
        if (!inferredReturnType.isAssignableTo(explicitReturnType)) {
            throw TypeCheckError("Inferred return type ${inferredReturnType.toPrettyString()} " +
                    "is not compatible with the explicitly declared return type: ${explicitReturnType.toPrettyString()}")
        }
    }
}

data class ArgumentDeclaration(
    override val givenName: String,
    override val valueType: Type,
) : ValueDeclaration {
    fun toPrettyString(): String =
        "$givenName : ${valueType.toPrettyString()}"
}

data class SmartCastDeclaration(
    override val givenName: String,
    override val valueType: Type,
) : ValueDeclaration

data class FunctionDefinition(
    val body: FunctionBody,
)

data class FunctionBody(
    val declarations: List<ValueDefinition>,
    val returned: Expression,
) {
    fun validate() {
        declarations.forEach { it.validate() }
        returned.validate()
    }
}

data class TypeAliasDeclaration(
    val aliasName: String,
    val aliasedType: Type,
) : TopLevelDeclaration {
    override fun validate() {
    }
}

data class UnionDeclaration(
    val unionName: String,
    val unionType: UnionType,
) : TopLevelDeclaration {
    override fun validate() {
    }
}
