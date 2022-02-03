package bia.model

import bia.type_checker.TypeCheckError

sealed interface Declaration {
    val givenName: String
    val type: Type
}

sealed interface BodyDeclaration : Declaration {
    fun validate()
}

data class ValueDeclaration(
    override val givenName: String,
    val initializer: Expression,
) : BodyDeclaration {
    override val type: Type by lazy {
        initializer.type
    }

    override fun validate() {
        initializer.validate()
    }
}

data class FunctionDeclaration(
    override val givenName: String,
    val typeVariables: List<TypeVariable>,
    val argumentDeclarations: List<ArgumentDeclaration>,
    val explicitReturnType: Type?,
    val buildDefinition: () -> FunctionDefinition?,
) : BodyDeclaration {
    val definition by lazy { buildDefinition() }

    val body: FunctionBody? by lazy { definition?.body }

    val explicitType: FunctionType? = explicitReturnType?.let {
        FunctionType(
            typeVariables = typeVariables,
            argumentDeclarations = argumentDeclarations,
            returnType = it,
        )
    }

    override val type: FunctionType by lazy {
        val returnType = explicitReturnType ?: body?.returned?.type
        ?: throw RuntimeException("Could not determine function return type")

        explicitType ?: FunctionType(
            typeVariables = typeVariables,
            argumentDeclarations = argumentDeclarations,
            returnType = returnType,
        )
    }

    override fun validate() {
        val givenNameEachCount = argumentDeclarations.groupingBy { it.givenName }.eachCount()

        givenNameEachCount.forEach { (givenName, count) ->
            if (count > 1) {
                throw TypeCheckError("Duplicate argument name: $givenName")
            }
        }

        body?.validate()

        val explicitReturnType = this.explicitReturnType
        val inferredReturnType = body?.returned?.type

        if (explicitReturnType != null && inferredReturnType != null) {
            if (!inferredReturnType.isAssignableTo(explicitReturnType)) {
                throw TypeCheckError("Inferred return type ${inferredReturnType.toPrettyString()} " +
                        "is not compatible with the explicitly declared return type: ${explicitReturnType.toPrettyString()}")
            }
        }
    }
}

data class ArgumentDeclaration(
    override val givenName: String,
    override val type: Type,
) : Declaration {
    fun toPrettyString(): String =
        "$givenName : ${type.toPrettyString()}"
}

data class FunctionDefinition(
    val body: FunctionBody,
)

data class FunctionBody(
    val declarations: List<BodyDeclaration>,
    val returned: Expression,
) {
    fun validate() {
        declarations.forEach { it.validate() }
        returned.validate()
    }
}
