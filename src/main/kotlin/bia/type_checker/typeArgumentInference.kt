package bia.type_checker

import bia.model.ArgumentListDeclaration
import bia.model.BasicArgumentListDeclaration
import bia.model.BigIntegerType
import bia.model.BooleanType
import bia.model.FunctionType
import bia.model.ListType
import bia.model.NullableType
import bia.model.NumberType
import bia.model.ObjectType
import bia.model.SequenceType
import bia.model.SingletonType
import bia.model.TaggedType
import bia.model.Type
import bia.model.TypeVariable
import bia.model.TypeVariableMapping
import bia.model.UnionType
import bia.model.VarargArgumentListDeclaration

fun inferTypeVariableMappingForCall(
    typeArguments: Set<TypeVariable>,
    argumentList: ArgumentListDeclaration,
    passedTypes: List<Type>,
): TypeVariableMapping {
    val argumentListMapping = when (argumentList) {
        is BasicArgumentListDeclaration -> inferTypeVariableMappingForBasicCall(
            typeArguments = typeArguments,
            argumentTypes = argumentList.argumentDeclarations.map { it.argumentType },
            passedTypes = passedTypes,
        )
        is VarargArgumentListDeclaration -> inferTypeVariableMappingForVarargCall(
            typeArguments = typeArguments,
            varargArgumentType = argumentList.type,
            passedTypes = passedTypes,
        )
    }

    val unmappedTypeArgument = typeArguments.firstOrNull { typeArgument ->
        !argumentListMapping.mapping.containsKey(typeArgument)
    }

    if (unmappedTypeArgument != null) {
        throw TypeCheckError("Could not infer type for type argument ${unmappedTypeArgument.toPrettyString()}")
    }

    return argumentListMapping
}

fun inferTypeVariableMappingForBasicCall(
    typeArguments: Set<TypeVariable>,
    argumentTypes: List<Type>,
    passedTypes: List<Type>,
): TypeVariableMapping =
    argumentTypes.foldIndexed(TypeVariableMapping.empty) { index, accumulatedMapping, argumentType ->
        val passedType = passedTypes.getOrNull(index)
            ?: throw TypeCheckError("Could not infer mapping for argument #${index + 1}, as less then ${index + 1} arguments were passed")

        val argumentMapping = inferTypeVariableMappingForPair(
            typeArguments = typeArguments,
            matchee = argumentType,
            matcher = passedType,
        )

        combineTypeVariableMappings(
            first = accumulatedMapping,
            second = argumentMapping,
        )
    }

fun inferTypeVariableMappingForVarargCall(
    typeArguments: Set<TypeVariable>,
    varargArgumentType: Type,
    passedTypes: List<Type>,
): TypeVariableMapping =
    passedTypes.fold(TypeVariableMapping.empty) { accumulatedMapping, passedType ->
        val argumentMapping = inferTypeVariableMappingForPair(
            typeArguments = typeArguments,
            matchee = varargArgumentType,
            matcher = passedType,
        )

        combineTypeVariableMappings(
            first = accumulatedMapping,
            second = argumentMapping,
        )
    }

fun inferTypeVariableMappingForPair(
    typeArguments: Set<TypeVariable>,
    matchee: Type,
    matcher: Type,
): TypeVariableMapping = when (matchee) {
    is TypeVariable -> inferTypeVariableMappingForTypeVariable(
        typeArguments = typeArguments,
        matchee = matchee,
        matcher = matcher,
    )
    is ListType -> inferTypeVariableMappingForList(
        typeArguments = typeArguments,
        matchee = matchee,
        matcher = matcher,
    )
    is SequenceType -> inferTypeVariableMappingForSequence(
        typeArguments = typeArguments,
        matchee = matchee,
        matcher = matcher,
    )
    is ObjectType -> inferTypeVariableMappingForObject(
        typeArguments = typeArguments,
        matchee = matchee,
        matcher = matcher,
    )
    is UnionType -> inferTypeVariableMappingForUnion(
        typeArguments = typeArguments,
        matchee = matchee,
        matcher = matcher,
    )
    is FunctionType -> inferTypeVariableMappingForFunction(
        typeArguments = typeArguments,
        matchee = matchee,
        matcher = matcher,
    )
    is NumberType -> TypeVariableMapping.empty
    is BooleanType -> TypeVariableMapping.empty
    is BigIntegerType -> TypeVariableMapping.empty
    is SingletonType -> TypeVariableMapping.empty
    is NullableType -> throw UnsupportedOperationException()
    is TaggedType -> throw UnsupportedOperationException()
}

fun inferTypeVariableMappingForTypeVariable(
    typeArguments: Set<TypeVariable>,
    matchee: TypeVariable,
    matcher: Type,
): TypeVariableMapping = if (matchee in typeArguments) {
    TypeVariableMapping(
        mapping = mapOf(
            matchee to matcher,
        ),
    )
} else {
    throw TypeCheckError("Could not infer mapping for type variable ${matchee.toPrettyString()} and ${matcher.toPrettyString()}")
}

fun inferTypeVariableMappingForList(
    typeArguments: Set<TypeVariable>,
    matchee: ListType,
    matcher: Type,
): TypeVariableMapping = if (matcher is ListType) {
    inferTypeVariableMappingForPair(
        typeArguments = typeArguments,
        matchee = matchee.elementType,
        matcher = matcher.elementType,
    )
} else {
    throw TypeCheckError("Could not infer mapping for list ${matchee.toPrettyString()} and ${matcher.toPrettyString()}")
}

fun inferTypeVariableMappingForSequence(
    typeArguments: Set<TypeVariable>,
    matchee: SequenceType,
    matcher: Type,
): TypeVariableMapping = if (matcher is SequenceType) {
    inferTypeVariableMappingForPair(
        typeArguments = typeArguments,
        matchee = matchee.elementType,
        matcher = matcher.elementType,
    )
} else {
    throw TypeCheckError("Could not infer mapping for sequence ${matchee.toPrettyString()} and ${matcher.toPrettyString()}")
}

private fun inferTypeVariableMappingForObject(
    typeArguments: Set<TypeVariable>,
    matchee: ObjectType,
    matcher: Type,
): TypeVariableMapping = if (matcher is ObjectType) {
    matchee.entries.entries.fold(TypeVariableMapping.empty) { accumulatedMapping, (entryKey, entryType) ->
        val matcherEntryType = matcher.entries[entryKey]
            ?: throw TypeCheckError("Could not infer mapping for object, as the matcher object misses the key $entryKey")

        val entryMapping = inferTypeVariableMappingForPair(
            typeArguments = typeArguments,
            matchee = entryType,
            matcher = matcherEntryType,
        )

        combineTypeVariableMappings(
            first = accumulatedMapping,
            second = entryMapping,
        )
    }
} else {
    throw TypeCheckError("Could not infer mapping for object ${matchee.toPrettyString()} and ${matcher.toPrettyString()}")
}

private fun inferTypeVariableMappingForUnion(
    typeArguments: Set<TypeVariable>,
    matchee: UnionType,
    matcher: Type,
): TypeVariableMapping = if (matcher is UnionType) {
    matchee.alternatives.fold(TypeVariableMapping.empty) { accumulatedMapping, alternative ->
        val alternativeType = alternative.type

        val matcherAlternative = matcher.getAlternative(tagName = alternative.tagName)
            ?: throw TypeCheckError("Could not infer mapping for union, as the matcher union misses the tag ${alternative.tagName}")

        val alternativeMapping = inferTypeVariableMappingForPair(
            typeArguments = typeArguments,
            matchee = alternativeType,
            matcher = matcherAlternative.type,
        )

        combineTypeVariableMappings(
            first = accumulatedMapping,
            second = alternativeMapping,
        )
    }
} else {
    throw TypeCheckError("Could not infer mapping for union ${matchee.toPrettyString()} and ${matcher.toPrettyString()}")
}

private fun inferTypeVariableMappingForFunction(
    typeArguments: Set<TypeVariable>,
    matchee: FunctionType,
    matcher: Type,
): TypeVariableMapping = if (matcher is FunctionType) {
    val argumentListMapping = inferTypeVariableMappingForArgumentListDeclaration(
        typeArguments = typeArguments,
        matchee = matchee.argumentListDeclaration,
        matcher = matcher.argumentListDeclaration,
    )

    val returnTypeMapping = inferTypeVariableMappingForPair(
        typeArguments = typeArguments,
        matchee = matchee.returnType,
        matcher = matcher.returnType,
    )

    combineTypeVariableMappings(
        first = argumentListMapping,
        second = returnTypeMapping,
    )
} else {
    throw TypeCheckError("Could not infer mapping for union ${matchee.toPrettyString()} and ${matcher.toPrettyString()}")
}

private fun inferTypeVariableMappingForArgumentListDeclaration(
    typeArguments: Set<TypeVariable>,
    matchee: ArgumentListDeclaration,
    matcher: ArgumentListDeclaration,
): TypeVariableMapping = when (matchee) {
    is BasicArgumentListDeclaration -> inferTypeVariableMappingForBasicArgumentListDeclaration(
        typeArguments = typeArguments,
        matchee = matchee,
        matcher = matcher,
    )
    is VarargArgumentListDeclaration -> inferTypeVariableMappingForVarargArgumentListDeclaration(
        typeArguments = typeArguments,
        matchee = matchee,
        matcher = matcher,
    )
}

private fun inferTypeVariableMappingForBasicArgumentListDeclaration(
    typeArguments: Set<TypeVariable>,
    matchee: BasicArgumentListDeclaration,
    matcher: ArgumentListDeclaration,
): TypeVariableMapping = if (matcher is BasicArgumentListDeclaration) {
    matchee.argumentDeclarations.foldIndexed(TypeVariableMapping.empty) { index, accumulatedMapping, argument ->
        val matcherArgument = matcher.argumentDeclarations.getOrNull(index)
            ?: throw TypeCheckError("Could not infer mapping for argument #${index + 1}, as the matcher has less then ${index + 1} arguments")

        val argumentMapping = inferTypeVariableMappingForPair(
            typeArguments = typeArguments,
            matchee = argument.argumentType,
            matcher = matcherArgument.argumentType,
        )

        combineTypeVariableMappings(
            first = accumulatedMapping,
            second = argumentMapping,
        )
    }
} else {
    throw TypeCheckError("Could not infer mapping for basic argument list ${matchee.toPrettyString()} and ${matcher.toPrettyString()}")
}

private fun inferTypeVariableMappingForVarargArgumentListDeclaration(
    typeArguments: Set<TypeVariable>,
    matchee: VarargArgumentListDeclaration,
    matcher: ArgumentListDeclaration,
): TypeVariableMapping = if (matcher is VarargArgumentListDeclaration) {
    inferTypeVariableMappingForPair(
        typeArguments = typeArguments,
        matchee = matchee.type,
        matcher = matcher.type,
    )
} else {
    throw TypeCheckError("Could not infer mapping for vararg argument list ${matchee.toPrettyString()} and ${matcher.toPrettyString()}")
}

private fun combineTypeVariableMappings(
    first: TypeVariableMapping,
    second: TypeVariableMapping,
): TypeVariableMapping {
    first.mapping.forEach { (typeVariable, mappedType) ->
        val secondMappedType = second.getMappedType(typeVariable)
        if (secondMappedType != null && mappedType != secondMappedType) {
            throw TypeCheckError("Ambiguous type variable $typeVariable mappings: ${mappedType.toPrettyString()} and ${secondMappedType.toPrettyString()}")
        }
    }

    return TypeVariableMapping(
        mapping = first.mapping + second.mapping,
    )
}
