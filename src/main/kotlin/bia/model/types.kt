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
//        other is UnionType -> isAssignableToUnion(other)
        else -> isAssignableDirectlyTo(other)
    }

//fun Type.isAssignableToUnion(union: UnionType): Boolean =
//    union.alternatives.count { this.isAssignableTo(it.type) } == 1

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

object BigIntegerType : SpecificType {
    override fun toPrettyString(): String = "BigInteger"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): SpecificType = this
}

data class FunctionType(
    val typeVariables: List<TypeVariable>,
    val argumentListDeclaration: ArgumentListDeclaration,
    val returnType: Type,
) : SpecificType {
    override fun isAssignableDirectlyTo(other: Type): Boolean = if (other is FunctionType) {
        argumentListDeclaration.isAssignableDirectlyTo(other.argumentListDeclaration) &&
                returnType.isAssignableTo(other.returnType)
    } else false

    override fun toPrettyString(): String {
        val returnTypeStr = returnType.toPrettyString()
        return "${argumentListDeclaration.toPrettyString()} : $returnTypeStr"
    }

    override fun resolveTypeVariables(mapping: TypeVariableMapping): FunctionType = FunctionType(
        typeVariables = emptyList(),
        argumentListDeclaration = argumentListDeclaration.resolveTypeVariables(mapping = mapping),
        returnType = returnType.resolveTypeVariables(mapping = mapping),
    )
}

data class ObjectType(
    val entries: Map<String, Type>,
) : SpecificType {
    override fun toPrettyString(): String =
        "{ ${entries.entries.joinToString { "${it.key} : ${it.value.toPrettyString()}" }} }"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): Type =
        copy(
            entries = entries.mapValues { (_, type) ->
                type.resolveTypeVariables(mapping = mapping)
            },
        )
}

data class UnionAlternative(
    val tagName: String,
    val type: Type,
)

sealed class UnionType : SpecificType {
    abstract val alternatives: Set<UnionAlternative>

    override fun toPrettyString(): String =
        alternatives.joinToString(separator = " | ") { it.tagName }

    override fun resolveTypeVariables(mapping: TypeVariableMapping): Type =
        this

    fun getAlternative(tagName: String): UnionAlternative? =
        alternatives.singleOrNull { it.tagName == tagName }
}

data class WideUnionType(
    override val alternatives: Set<UnionAlternative>,
) : UnionType()

data class NarrowUnionType(
    override val alternatives: Set<UnionAlternative>,
    val narrowedAlternative: UnionAlternative,
) : UnionType() {
    override fun toPrettyString(): String =
        "${alternatives.joinToString(separator = " | ") { it.tagName }} [narrowed to ${narrowedAlternative.tagName}]"

    val narrowedType: Type
        get() = narrowedAlternative.type
}

data class TaggedType(
    val taggedType: Type,
    val attachedTagName: String,
) : SpecificType {
    override fun toPrettyString(): String =
        "${taggedType.toPrettyString()} # $attachedTagName"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): Type =
        copy(
            taggedType = taggedType.resolveTypeVariables(mapping = mapping),
        )

    override fun isAssignableDirectlyTo(other: Type): Boolean {
        return if (other is UnionType) {
            val alternative = other.alternatives.singleOrNull { it.tagName == attachedTagName }
                ?: throw TypeCheckError("Union type $other doesn't have an alternative tagged '$attachedTagName'")

            taggedType.isAssignableTo(alternative.type)
        } else false
    }
}

private fun typeConstructorToPrettyString(
    typeConstructor: String,
    argumentType: Type,
): String =
    "$typeConstructor<${argumentType.toPrettyString()}>"
