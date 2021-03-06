package bia.model

import bia.model.expressions.Expression
import bia.model.expressions.type
import bia.parser.StaticScope
import bia.type_checker.TypeCheckError

sealed interface ArgumentListDeclaration {
    fun isAssignableDirectlyTo(other: ArgumentListDeclaration): Boolean

    fun toPrettyString(): String

    fun resolveTypeVariables(mapping: TypeVariableMapping): ArgumentListDeclaration

    fun validate()

    fun extendScope(scope: StaticScope): StaticScope

    fun validateCall(functionName: String, arguments: List<Expression>)
}

interface ArgumentListDeclarationB {
    fun build(scope: StaticScope): ArgumentListDeclaration
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
                    argumentDeclaration.argumentType.isAssignableTo(otherArgumentDeclaration.argumentType)
                }

            argumentDeclarations.size <= otherArgumentDeclarations.size && areArgumentsAssignable()
        } else false

    override fun toPrettyString(): String {
        val argumentDeclarationsStr = argumentDeclarations.joinToString { it.toPrettyString() }
        return "($argumentDeclarationsStr)"
    }

    override fun resolveTypeVariables(mapping: TypeVariableMapping) = BasicArgumentListDeclaration(
        argumentDeclarations = argumentDeclarations.map {
            it.copy(argumentType = it.argumentType.resolveTypeVariables(mapping = mapping))
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
                if (!argument.type.isAssignableTo(argumentDeclaration.argumentType)) {
                    throw TypeCheckError(
                        "Function $functionName argument #${index + 1} has type ${argument.type.toPrettyString()} " +
                                "which can't be assigned to ${argumentDeclaration.argumentType.toPrettyString()}",
                    )
                }
            }
    }
}

data class BasicArgumentListDeclarationB(
    val argumentDeclarations: List<ArgumentDeclarationB>,
) : ArgumentListDeclarationB {
    override fun build(scope: StaticScope): ArgumentListDeclaration =
        BasicArgumentListDeclaration(
            argumentDeclarations = argumentDeclarations.map {
                it.build(scope = scope)
            },
        )
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
                    argumentDeclaration.argumentType.isAssignableTo(type)
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

data class VarargArgumentListDeclarationB(
    val givenName: String,
    val type: TypeExpressionB,
) : ArgumentListDeclarationB {
    override fun build(scope: StaticScope): VarargArgumentListDeclaration =
        VarargArgumentListDeclaration(
            givenName = givenName,
            type = type.buildType(scope = scope),
        )
}
