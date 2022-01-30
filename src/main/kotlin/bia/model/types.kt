package bia.model

sealed interface Type {
    fun toPrettyString(): String

    fun isAssignableTo(other: Type): Boolean = this == other
}

object NumberType : Type {
    override fun toPrettyString(): String = "Number"
}

object BooleanType : Type {
    override fun toPrettyString(): String = "Boolean"
}

data class ListType(val elementType: Type) : Type {
    override fun toPrettyString(): String = typeConstructorToPrettyString(
        typeConstructor = "List",
        argumentType = elementType,
    )
}

data class SequenceType(val elementType: Type) : Type {
    override fun toPrettyString(): String = typeConstructorToPrettyString(
        typeConstructor = "Sequence",
        argumentType = elementType,
    )
}

private fun typeConstructorToPrettyString(
    typeConstructor: String,
    argumentType: Type,
): String =
    "$typeConstructor<${argumentType.toPrettyString()}>"

object BigIntegerType : Type {
    override fun toPrettyString(): String = "BigInteger"
}

data class FunctionType(
    val argumentDeclarations: List<ArgumentDeclaration>,
    val returnType: Type,
) : Type {
    override fun isAssignableTo(other: Type): Boolean = if (other is FunctionType) {
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
}
