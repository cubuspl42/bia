package bia.model

import bia.model.expressions.Expression
import bia.model.expressions.ExpressionB
import bia.parser.StaticScope
import bia.type_checker.TypeCheckError

sealed interface TopLevelDeclaration {
    fun validate()
}

interface TopLevelDeclarationB {
    interface Built {
        val extendedScope: StaticScope
        val topLevelDeclaration: TopLevelDeclaration
    }

    fun build(scope: StaticScope): Built
}

sealed interface ValueDeclaration {
    val givenName: String
    val valueType: Type
}

sealed interface ValueDefinition : ValueDeclaration, TopLevelDeclaration

interface ValueDefinitionB : TopLevelDeclarationB {
    interface Built : TopLevelDeclarationB.Built {
        override val extendedScope: StaticScope
        val valueDefinition: ValueDefinition
    }

    override fun build(scope: StaticScope): Built
}

data class ValDeclaration(
    override val givenName: String,
    val initializer: Expression,
) : ValueDefinition {
    override val valueType: Type by lazy {
        initializer.type
    }

    override fun validate() {
        initializer.validate()
    }
}

data class ValDeclarationB(
    val givenName: String,
    val initializer: ExpressionB,
) : TopLevelDeclarationB, ValueDefinitionB {
    data class Built(
        override val extendedScope: StaticScope,
        val valDeclaration: ValDeclaration,
    ) : TopLevelDeclarationB.Built, ValueDefinitionB.Built {
        override val topLevelDeclaration: TopLevelDeclaration
            get() = valDeclaration

        override val valueDefinition: ValueDefinition
            get() = valDeclaration
    }

    override fun build(scope: StaticScope): Built {
        val valDeclaration = ValDeclaration(
            givenName = givenName,
            initializer = initializer.build(scope = scope),
        )

        return Built(
            extendedScope = scope.extendClosed(
                name = givenName,
                declaration = valDeclaration,
            ),
            valDeclaration = valDeclaration,
        )
    }
}

data class DefDeclaration(
    override val givenName: String,
    val typeVariables: List<TypeVariable>,
    val argumentListDeclaration: ArgumentListDeclaration,
    val explicitReturnType: Type?,
    val buildDefinition: () -> FunctionDefinition?,
) : ValueDefinition {
    val definition by lazy { buildDefinition() }

    val body: FunctionBody? by lazy { definition?.body }

    val explicitType: FunctionType? = explicitReturnType?.let {
        FunctionType(
            typeVariables = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            returnType = it,
        )
    }

    override val valueType: FunctionType by lazy {
        val returnType = explicitReturnType ?: body?.returned?.type
        ?: throw RuntimeException("Could not determine function return type")

        explicitType ?: FunctionType(
            typeVariables = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            returnType = returnType,
        )
    }

    override fun validate() {
        validateFunction(
            body = body,
            explicitReturnType = explicitReturnType,
        )
    }
}

data class DefDeclarationB(
    val givenName: String,
    val typeVariables: List<TypeVariableB>,
    val argumentListDeclaration: ArgumentListDeclarationB,
    val explicitReturnType: TypeExpression?,
    val body: FunctionBodyB?,
) : TopLevelDeclarationB, ValueDefinitionB {
    data class Built(
        override val extendedScope: StaticScope,
        val defDeclaration: DefDeclaration,
    ) : TopLevelDeclarationB.Built, ValueDefinitionB.Built {
        override val topLevelDeclaration: TopLevelDeclaration
            get() = defDeclaration

        override val valueDefinition: ValueDefinition
            get() = defDeclaration
    }

    override fun build(scope: StaticScope): Built {
        val builtTypeVariables = buildTypeVariables(
            scope = scope,
            typeVariables = typeVariables,
        )

        val builtArgumentListDeclaration: ArgumentListDeclaration =
            argumentListDeclaration.build(scope = scope)

        val functionDeclaration = object {
            val functionDeclaration: DefDeclaration by lazy {
                DefDeclaration(
                    givenName = givenName,
                    typeVariables = builtTypeVariables.typeVariables,
                    argumentListDeclaration = builtArgumentListDeclaration,
                    explicitReturnType = explicitReturnType?.build(scope = scope),
                    buildDefinition = {
                        body?.let {
                            FunctionDefinition(
                                body = it.build(
                                    scope = builtArgumentListDeclaration.extendScope(
                                        scope = builtTypeVariables.extendedScope.extendOpen(
                                            name = givenName,
                                            declaration = functionDeclaration,
                                        ),
                                    ),
                                ),
                            )
                        }
                    }
                )
            }

            init {
                functionDeclaration.definition
            }
        }.functionDeclaration

        return Built(
            extendedScope = scope.extendClosed(
                name = givenName,
                declaration = functionDeclaration,
            ),
            defDeclaration = functionDeclaration,
        )
    }
}

data class BuiltTypeVariables(
    val extendedScope: StaticScope,
    val typeVariables: List<TypeVariable>,
)

fun buildTypeVariables(
    scope: StaticScope,
    typeVariables: List<TypeVariableB>,
): BuiltTypeVariables {
    fun buildRecursively(
        scope: StaticScope,
        typeVariables: List<TypeVariableB>,
        builtTypeVariables: List<TypeVariable>,
    ): BuiltTypeVariables {
        if (typeVariables.isEmpty()) return BuiltTypeVariables(
            extendedScope = scope,
            typeVariables = builtTypeVariables,
        )

        val head = typeVariables.first()
        val tail = typeVariables.drop(1)

        val givenName = head.givenName

        val allocationResult = scope.allocateTypeVariable(
            givenName = givenName,
        )

        return buildRecursively(
            scope = allocationResult.newScope,
            typeVariables = tail,
            builtTypeVariables = builtTypeVariables + allocationResult.allocatedVariable,
        )
    }

    return buildRecursively(
        scope = scope,
        typeVariables = typeVariables,
        builtTypeVariables = emptyList(),
    )
}

fun validateFunction(
    body: FunctionBody?,
    explicitReturnType: Type?,
) {
    body?.validate()

    val inferredReturnType = body?.returned?.type

    if (explicitReturnType != null && inferredReturnType != null) {
        if (!inferredReturnType.isAssignableTo(explicitReturnType)) {
            throw TypeCheckError("Inferred return type ${inferredReturnType.toPrettyString()} " +
                    "is not compatible with the explicitly declared return type: ${explicitReturnType.toPrettyString()}")
        }
    }
}

data class ArgumentDeclaration(
    override val givenName: String,
    override val valueType: Type,
) : ValueDeclaration {
    fun toPrettyString(): String =
        "$givenName : ${valueType.toPrettyString()}"
}

data class ArgumentDeclarationB(
    val givenName: String,
    val valueType: TypeExpression,
) {
    fun build(scope: StaticScope) = ArgumentDeclaration(
        givenName = givenName,
        valueType = valueType.build(scope = scope),
    )
}

data class SmartCastDeclaration(
    override val givenName: String,
    override val valueType: Type,
) : ValueDeclaration

data class FunctionDefinition(
    val body: FunctionBody,
)

data class FunctionBody(
    val definitions: List<ValueDefinition>,
    val returned: Expression,
) {
    fun validate() {
        definitions.forEach { it.validate() }
        returned.validate()
    }
}

data class FunctionBodyB(
    val definitions: List<ValueDefinitionB>,
    val returned: ExpressionB,
) {
    fun build(scope: StaticScope): FunctionBody = FunctionBody(
        definitions = buildValueDefinitions(
            scope = scope,
            definitions = definitions,
        ).definitions,
        returned = returned.build(scope = scope),
    )
}

data class BuiltValueDefinitions(
    val extendedScope: StaticScope,
    val definitions: List<ValueDefinition>,
)

fun buildValueDefinitions(
    scope: StaticScope,
    definitions: List<ValueDefinitionB>,
    builtDefinitions: List<ValueDefinition> = emptyList(),
): BuiltValueDefinitions = definitions.firstOrNull()?.let {
    val definition = it.build(
        scope = scope,
    )

    buildValueDefinitions(
        scope = definition.extendedScope,
        definitions = definitions.drop(1),
        builtDefinitions = builtDefinitions + definition.valueDefinition,
    )
} ?: BuiltValueDefinitions(
    extendedScope = scope,
    definitions = builtDefinitions,
)

data class TypeAliasDeclaration(
    val aliasName: String,
    val aliasedType: Type,
) : TopLevelDeclaration {
    override fun validate() {
    }
}

data class TypeAliasDeclarationB(
    val aliasName: String,
    val aliasedType: TypeExpression,
) : TopLevelDeclarationB {
    data class Built(
        override val extendedScope: StaticScope,
        val typeAliasDeclaration: TypeAliasDeclaration,
    ) : TopLevelDeclarationB.Built {
        override val topLevelDeclaration: TopLevelDeclaration
            get() = typeAliasDeclaration
    }

    override fun build(scope: StaticScope): Built {
        val typeAliasDeclaration = TypeAliasDeclaration(
            aliasName = aliasName,
            aliasedType = aliasedType.build(scope = scope),
        )

        return Built(
            extendedScope = scope.extendType(
                name = aliasName,
                type = typeAliasDeclaration.aliasedType,
            ),
            typeAliasDeclaration = typeAliasDeclaration,
        )
    }
}

data class UnionDeclaration(
    val unionName: String,
    val unionType: UnionType,
) : TopLevelDeclaration {
    override fun validate() {
    }
}

data class UnionAlternativeB(
    val tagName: String,
    val type: TypeExpression,
) {
    fun build(scope: StaticScope) = UnionAlternative(
        tagName = tagName,
        type = type.build(scope),
    )
}

data class UnionDeclarationB(
    val unionName: String,
    val alternatives: List<UnionAlternativeB>,
) : TopLevelDeclarationB {
    data class Built(
        override val extendedScope: StaticScope,
        val unionDeclaration: UnionDeclaration,
    ) : TopLevelDeclarationB.Built {
        override val topLevelDeclaration: TopLevelDeclaration
            get() = unionDeclaration
    }

    override fun build(scope: StaticScope): Built {
        val unionDeclaration = UnionDeclaration(
            unionName = unionName,
            unionType = WideUnionType(
                alternatives = alternatives.map {
                    it.build(scope = scope)
                }.toSet(),
            ),
        )

        return Built(
            extendedScope = scope.extendType(
                name = unionName,
                type = unionDeclaration.unionType,
            ),
            unionDeclaration = unionDeclaration,
        )
    }
}
