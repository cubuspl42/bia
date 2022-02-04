package bia.model

import bia.parser.StaticScope
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
    val argumentListDeclaration: ArgumentListDeclaration,
    val explicitReturnType: Type?,
    val buildDefinition: () -> FunctionDefinition?,
) : BodyDeclaration {
    val definition by lazy { buildDefinition() }

    val body: FunctionBody? by lazy { definition?.body }

    val explicitType: FunctionType? = explicitReturnType?.let {
        FunctionType(
            typeVariables = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            returnType = it,
        )
    }

    override val type: FunctionType by lazy {
        val returnType = explicitReturnType ?: body?.returned?.type
        ?: throw RuntimeException("Could not determine function return type")

        explicitType ?: FunctionType(
            typeVariables = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            returnType = returnType,
        )
    }

    override fun validate() {

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

sealed interface ArgumentListDeclaration {
    fun isAssignableDirectlyTo(other: ArgumentListDeclaration): Boolean

    fun toPrettyString(): String

    fun resolveTypeVariables(mapping: TypeVariableMapping): ArgumentListDeclaration

    fun validate()

    fun extendScope(scope: StaticScope): StaticScope

    fun validateCall(functionName: String, arguments: List<Expression>)
}

data class BasicArgumentListDeclaration(
    val argumentDeclarations: List<ArgumentDeclaration>,
) : ArgumentListDeclaration {
    override fun isAssignableDirectlyTo(other: ArgumentListDeclaration) =
        if (other is BasicArgumentListDeclaration) {
            val argumentDeclarations = argumentDeclarations
            val otherArgumentDeclarations = other.argumentDeclarations

            fun areArgumentsAssignable() = argumentDeclarations.zip(otherArgumentDeclarations)
                .all { (argumentDeclaration, otherArgumentDeclaration) ->
                    argumentDeclaration.type.isAssignableTo(otherArgumentDeclaration.type)
                }

            argumentDeclarations.size <= otherArgumentDeclarations.size && areArgumentsAssignable()
        } else false

    override fun toPrettyString(): String {
        val argumentDeclarationsStr = argumentDeclarations.joinToString { it.toPrettyString() }
        return "($argumentDeclarationsStr)"
    }

    override fun resolveTypeVariables(mapping: TypeVariableMapping) = BasicArgumentListDeclaration(
        argumentDeclarations = argumentDeclarations.map {
            it.copy(type = it.type.resolveTypeVariables(mapping = mapping))
        },
    )

    override fun validate() {
        val givenNameEachCount = argumentDeclarations.groupingBy { it.givenName }.eachCount()

        givenNameEachCount.forEach { (givenName, count) ->
            if (count > 1) {
                throw TypeCheckError("Duplicate argument name: $givenName")
            }
        }
    }

    override fun extendScope(scope: StaticScope): StaticScope = scope.extendClosed(
        namedDeclarations = argumentDeclarations.map { it.givenName to it },
    )

    override fun validateCall(
        functionName: String,
        arguments: List<Expression>,
    ) {
        val definedArgumentCount = argumentDeclarations.size
        val passedArgumentCount = arguments.size

        if (passedArgumentCount != definedArgumentCount) {
            throw TypeCheckError(
                "Function $functionName was defined with $definedArgumentCount arguments, $passedArgumentCount passed",
            )
        }

        arguments.zip(argumentDeclarations)
            .forEachIndexed { index, (argument, argumentDeclaration) ->
                if (!argument.type.isAssignableTo(argumentDeclaration.type)) {
                    throw TypeCheckError(
                        "Function $functionName argument #${index + 1} has type ${argument.type.toPrettyString()} " +
                                "which can't be assigned to ${argumentDeclaration.type.toPrettyString()}",
                    )
                }
            }
    }
}

data class VarargArgumentListDeclaration(
    val givenName: String,
    val type: Type,
) : ArgumentListDeclaration {
    override fun isAssignableDirectlyTo(other: ArgumentListDeclaration) =
        when (other) {
            is BasicArgumentListDeclaration -> {
                val argumentDeclarations = other.argumentDeclarations

                fun areArgumentsAssignable() = argumentDeclarations.all { argumentDeclaration ->
                    argumentDeclaration.type.isAssignableTo(type)
                }

                areArgumentsAssignable()
            }
            is VarargArgumentListDeclaration -> other.type.isAssignableTo(type)
        }

    override fun toPrettyString(): String =
        "($givenName ${type.toPrettyString()}...)"

    override fun resolveTypeVariables(mapping: TypeVariableMapping) = copy(
        type = type.resolveTypeVariables(mapping = mapping),
    )

    override fun validate() {
    }

    override fun extendScope(scope: StaticScope): StaticScope {
        TODO("Not yet implemented")
    }

    override fun validateCall(
        functionName: String,
        arguments: List<Expression>,
    ) {
        arguments.forEachIndexed { index, argument ->
            if (!argument.type.isAssignableTo(type)) {
                throw TypeCheckError(
                    "Vararg function $functionName argument #${index + 1} has type ${argument.type.toPrettyString()} " +
                            "which can't be assigned to ${type.toPrettyString()}",
                )
            }
        }
    }
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
