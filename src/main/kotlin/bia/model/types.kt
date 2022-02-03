package bia.model

import bia.type_checker.TypeCheckError

sealed interface Type {
    fun toPrettyString(): String

    fun isAssignableDirectlyTo(other: Type): Boolean = false

    fun resolveTypeVariables(mapping: TypeVariableMapping): Type
}

data class TypeVariableMapping(
    private val mapping: Map<TypeVariable, Type>,
) {
    fun getMappedType(variable: TypeVariable): Type? =
        mapping[variable]
}

sealed interface SpecificType : Type

data class TypeVariable(
    val givenName: String,
    val id: Int,
) : SpecificType {
    override fun toPrettyString(): String = "$givenName#$id"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): Type =
        mapping.getMappedType(this) ?: this
}

@Suppress("IntroduceWhenSubject")
fun Type.isAssignableTo(other: Type): Boolean =
    when {
        this == other -> true
        other is NullableType -> isAssignableTo(other.baseType)
        else -> isAssignableDirectlyTo(other)
    }

object NumberType : SpecificType {
    override fun toPrettyString(): String = "Number"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): SpecificType =
        this
}

object BooleanType : SpecificType {
    override fun toPrettyString(): String = "Boolean"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): SpecificType = this
}

data class ListType(val elementType: Type) : SpecificType {
    override fun toPrettyString(): String = typeConstructorToPrettyString(
        typeConstructor = "List",
        argumentType = elementType,
    )

    override fun resolveTypeVariables(mapping: TypeVariableMapping): SpecificType =
        ListType(
            elementType = elementType.resolveTypeVariables(mapping = mapping),
        )
}

data class SequenceType(val elementType: Type) : SpecificType {
    override fun toPrettyString(): String = typeConstructorToPrettyString(
        typeConstructor = "Sequence",
        argumentType = elementType,
    )

    override fun resolveTypeVariables(mapping: TypeVariableMapping): SpecificType =
        SequenceType(
            elementType = elementType.resolveTypeVariables(mapping = mapping),
        )
}

data class NullableType(val baseType: Type) : SpecificType {
    override fun toPrettyString(): String =
        "${baseType.toPrettyString()}?"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): SpecificType =
        NullableType(
            baseType = baseType.resolveTypeVariables(mapping = mapping),
        )
}

private fun typeConstructorToPrettyString(
    typeConstructor: String,
    argumentType: Type,
): String =
    "$typeConstructor<${argumentType.toPrettyString()}>"

object BigIntegerType : SpecificType {
    override fun toPrettyString(): String = "BigInteger"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): SpecificType = this
}

data class FunctionType(
    val typeVariables: List<TypeVariable>,
    val argumentDeclarations: List<ArgumentDeclaration>,
    val returnType: Type,
) : SpecificType {
    override fun isAssignableDirectlyTo(other: Type): Boolean = if (other is FunctionType) {
        fun areArgumentsAssignable() = argumentDeclarations.zip(other.argumentDeclarations)
            .all { (argumentDeclaration, otherArgumentDeclaration) ->
                argumentDeclaration.type.isAssignableTo(otherArgumentDeclaration.type)
            }

        argumentDeclarations.size <= other.argumentDeclarations.size && areArgumentsAssignable()
    } else false

    override fun toPrettyString(): String {
        val argumentDeclarationsStr = argumentDeclarations.joinToString { it.toPrettyString() }
        val returnTypeStr = returnType.toPrettyString()

        return "($argumentDeclarationsStr) : $returnTypeStr"
    }

    override fun resolveTypeVariables(mapping: TypeVariableMapping): FunctionType = FunctionType(
        typeVariables = emptyList(),
        argumentDeclarations = argumentDeclarations.map {
            it.copy(type = it.type.resolveTypeVariables(mapping = mapping))
        },
        returnType = returnType.resolveTypeVariables(mapping = mapping),
    )
}
