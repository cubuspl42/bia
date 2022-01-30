package bia.model

sealed interface Type

object NumberType : Type

object BooleanType : Type

data class ListType(val elementType: Type)

data class SequenceType(val elementType: Type)

object BigIntegerType : Type

data class FunctionType(
    val argumentDeclarations: List<ArgumentDeclaration>,
    val returnType: Type,
) : Type
