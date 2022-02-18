package bia.model

import bia.model.expressions.buildTypeVariableMapping
import bia.parser.StaticScope
import bia.type_checker.TypeCheckError

// An entity that resides in the type namespace
sealed interface TypeAlike

interface TypeConstructor : TypeAlike {
    val typeArguments: List<TypeVariable>

    fun construct(passedTypeArguments: List<Type>): Type
}

sealed interface Type : TypeAlike {
    fun toPrettyString(): String

    fun isAssignableDirectlyTo(other: Type): Boolean = false

    fun resolveTypeVariables(mapping: TypeVariableMapping): Type
}

data class UnionTypeConstructor(
    override val typeArguments: List<TypeVariable>,
    private val typeStructure: WideUnionType,
) : TypeConstructor, UnionTypeAlike {
    override fun construct(passedTypeArguments: List<Type>): WideUnionType {
        val typeVariableMapping = buildTypeVariableMapping(
            typeArguments = typeArguments,
            passedTypeArguments = passedTypeArguments,
        )

        return typeStructure.resolveTypeVariables(mapping = typeVariableMapping)
    }
}

interface TypeExpressionB {
    fun build(scope: StaticScope): Type
}

data class TypeReference(
    val referredName: String,
    val passedTypeArguments: List<TypeExpressionB> = emptyList(),
) : TypeExpressionB {
    override fun build(scope: StaticScope): Type {
        val typeAlike = scope.getTypeAlike(givenName = referredName)

        return if (passedTypeArguments.isEmpty()) {
            (typeAlike as? Type)
                ?: throw TypeCheckError("$referredName does not refer to an ordinary type (it's a type constructor), but no type arguments were provided")
        } else {
            val typeConstructor = (typeAlike as? TypeConstructor)
                ?: throw TypeCheckError("$referredName does not refer to a type constructor, but type arguments were provided")

            typeConstructor.construct(
                passedTypeArguments = passedTypeArguments.map {
                    it.build(scope = scope)
                },
            )
        }
    }
}

data class TypeVariableMapping(
    val mapping: Map<TypeVariable, Type>,
) {
    companion object {
        val empty = TypeVariableMapping(
            mapping = emptyMap(),
        )
    }

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

data class TypeVariableB(
    val givenName: String,
)

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

object NumberType : SpecificType, TypeExpressionB {
    override fun toPrettyString(): String = "Number"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): SpecificType =
        this

    override fun build(scope: StaticScope) = this
}

object BooleanType : SpecificType, TypeExpressionB {
    override fun toPrettyString(): String = "Boolean"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): SpecificType = this

    override fun build(scope: StaticScope) = this
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

data class ListTypeB(val elementType: TypeExpressionB) : TypeExpressionB {
    override fun build(scope: StaticScope) = ListType(
        elementType = elementType.build(scope),
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

data class SequenceTypeB(val elementType: TypeExpressionB) : TypeExpressionB {
    override fun build(scope: StaticScope) = SequenceType(
        elementType = elementType.build(scope),
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

data class NullableTypeB(val baseType: TypeExpressionB) : TypeExpressionB {
    override fun build(scope: StaticScope) = SequenceType(
        elementType = baseType.build(scope),
    )
}

object BigIntegerType : SpecificType, TypeExpressionB {
    override fun toPrettyString(): String = "BigInteger"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): SpecificType = this

    override fun build(scope: StaticScope) = this
}

data class FunctionType(
    override val typeArguments: List<TypeVariable>,
    val argumentListDeclaration: ArgumentListDeclaration,
    val returnType: Type,
) : SpecificType, TypeConstructor {
    override fun isAssignableDirectlyTo(other: Type): Boolean = if (other is FunctionType) {
        argumentListDeclaration.isAssignableDirectlyTo(other.argumentListDeclaration) &&
                returnType.isAssignableTo(other.returnType)
    } else false

    override fun toPrettyString(): String {
        val returnTypeStr = returnType.toPrettyString()
        return "${argumentListDeclaration.toPrettyString()} : $returnTypeStr"
    }

    override fun resolveTypeVariables(mapping: TypeVariableMapping): FunctionType = FunctionType(
        typeArguments = typeArguments,
        argumentListDeclaration = argumentListDeclaration.resolveTypeVariables(mapping = mapping),
        returnType = returnType.resolveTypeVariables(mapping = mapping),
    )

    override fun construct(passedTypeArguments: List<Type>): Type {
        val typeVariableMapping = buildTypeVariableMapping(
            typeArguments = typeArguments,
            passedTypeArguments = passedTypeArguments,
        )

        return resolveTypeVariables(mapping = typeVariableMapping)
    }
}

data class FunctionTypeB(
    val typeArguments: List<TypeVariableB>,
    val argumentListDeclaration: ArgumentListDeclarationB,
    val returnType: TypeExpressionB,
) : TypeExpressionB {
    override fun build(scope: StaticScope): Type {
        val builtTypeVariables = buildTypeVariables(
            scope = scope,
            typeVariables = typeArguments,
        )

        return FunctionType(
            typeArguments = builtTypeVariables.typeVariables,
            argumentListDeclaration = argumentListDeclaration.build(
                scope = builtTypeVariables.extendedScope,
            ),
            returnType = returnType.build(
                scope = builtTypeVariables.extendedScope,
            ),
        )
    }
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

data class ObjectTypeB(
    val entries: Map<String, TypeExpressionB>,
) : TypeExpressionB {
    override fun build(scope: StaticScope) = ObjectType(
        entries = entries.mapValues { (_, entryType) ->
            entryType.build(scope = scope)
        }
    )
}

data class UnionAlternative(
    val tagName: String,
    val type: Type,
) {
    fun resolveTypeVariables(mapping: TypeVariableMapping) = copy(
        type = type.resolveTypeVariables(mapping = mapping)
    )
}

sealed interface UnionTypeAlike : TypeAlike

sealed class UnionType : SpecificType, UnionTypeAlike {
    abstract val alternatives: Set<UnionAlternative>

    val tagNames: Set<String> by lazy { alternatives.map { it.tagName }.toSet() }

    override fun toPrettyString(): String =
        alternatives.joinToString(separator = " | ") {
            "${it.type.toPrettyString()} #${it.tagName}"
        }

    override fun resolveTypeVariables(mapping: TypeVariableMapping): Type =
        this

    fun getAlternative(tagName: String): UnionAlternative? =
        alternatives.singleOrNull { it.tagName == tagName }
}

data class WideUnionType(
    override val alternatives: Set<UnionAlternative>,
) : UnionType() {
    override fun resolveTypeVariables(mapping: TypeVariableMapping): WideUnionType =
        copy(
            alternatives = alternatives.map {
                it.resolveTypeVariables(mapping = mapping)
            }.toSet(),
        )
}

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
) : UnionType() {
    override val alternatives: Set<UnionAlternative> = setOf(
        UnionAlternative(
            tagName = attachedTagName,
            type = taggedType,
        ),
    )

    override fun toPrettyString(): String =
        "${taggedType.toPrettyString()} # $attachedTagName"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): Type =
        copy(
            taggedType = taggedType.resolveTypeVariables(mapping = mapping),
        )

    override fun isAssignableDirectlyTo(other: Type): Boolean {
        return if (other is UnionType) {
            val alternative = other.alternatives.singleOrNull { it.tagName == attachedTagName }
                ?: throw TypeCheckError("Union type ${other.toPrettyString()} doesn't have an alternative tagged '$attachedTagName'")

            taggedType.isAssignableTo(alternative.type)
        } else false
    }
}

data class SingletonType(
    val singletonName: String,
) : SpecificType {
    override fun toPrettyString(): String =
        "singleton $singletonName"

    override fun resolveTypeVariables(mapping: TypeVariableMapping): Type = this
}

private fun typeConstructorToPrettyString(
    typeConstructor: String,
    argumentType: Type,
): String =
    "$typeConstructor<${argumentType.toPrettyString()}>"
