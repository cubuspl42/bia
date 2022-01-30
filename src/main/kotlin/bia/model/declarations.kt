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

class FunctionDeclaration(
    override val givenName: String,
    val buildDefinition: () -> FunctionDefinition,
) : BodyDeclaration {
    val definition by lazy { buildDefinition() }

    val explicitType: FunctionType?
        get() = definition.explicitType

    override val type: FunctionType by lazy {
        definition.type
    }

    override fun validate() {
        definition.validate()
    }
}

data class ExternalFunctionDeclaration(
    override val givenName: String,
    val argumentDeclarations: List<ArgumentDeclaration>,
    val returnType: Type,
) : BodyDeclaration {
    override val type: FunctionType by lazy {
        FunctionType(
            argumentDeclarations = argumentDeclarations,
            returnType = returnType,
        )
    }

    override fun validate() {
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
    val argumentDeclarations: List<ArgumentDeclaration>,
    val explicitReturnType: Type?,
    val body: FunctionBody,
) {
    val explicitType: FunctionType? = explicitReturnType?.let {
        FunctionType(
            argumentDeclarations = argumentDeclarations,
            returnType = it,
        )
    }

    val type: FunctionType by lazy {
        explicitType ?: FunctionType(
            argumentDeclarations = argumentDeclarations,
            returnType = body.returned.type,
        )
    }

    fun validate() {
        val givenNameEachCount = argumentDeclarations.groupingBy { it.givenName }.eachCount()

        givenNameEachCount.forEach { (givenName, count) ->
            if (count > 1) {
                throw TypeCheckError("Duplicate argument name: $givenName")
            }
        }

        body.validate()

        val explicitReturnType = this.explicitReturnType

        if (explicitReturnType != null) {
            val inferredReturnType = body.returned.type

            if (!inferredReturnType.isAssignableTo(explicitReturnType)) {
                throw TypeCheckError("Inferred return type ${inferredReturnType.toPrettyString()} " +
                        "is not compatible with the explicitly declared return type: ${explicitReturnType.toPrettyString()}")
            }
        }
    }
}

data class FunctionBody(
    val declarations: List<BodyDeclaration>,
    val returned: Expression,
) {
    fun validate() {
        declarations.forEach { it.validate() }
        returned.validate()
    }
}
