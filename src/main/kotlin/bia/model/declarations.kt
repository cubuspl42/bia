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
            typeArguments = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            returnType = it,
        )
    }

    override val valueType: FunctionType by lazy {
        val returnType = explicitReturnType ?: body?.returned?.type
        ?: throw RuntimeException("Could not determine function return type")

        explicitType ?: FunctionType(
            typeArguments = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            returnType = returnType,
        )
    }

    override fun validate() {
        validateFunction(
            argumentListDeclaration = argumentListDeclaration,
            body = body,
            explicitReturnType = explicitReturnType,
        )
    }
}

data class DefDeclarationB(
    val givenName: String,
    val typeVariables: List<TypeVariableB>,
    val argumentListDeclaration: ArgumentListDeclarationB,
    val explicitReturnType: TypeExpressionB?,
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

    @Suppress("NAME_SHADOWING")
    override fun build(scope: StaticScope): Built {
        val builtTypeVariables = buildTypeVariables(
            scope = scope,
            typeVariables = typeVariables,
        )

        val scopeWithTypeVariables = builtTypeVariables.extendedScope

        val builtArgumentListDeclaration: ArgumentListDeclaration =
            argumentListDeclaration.build(
                scope = scopeWithTypeVariables,
            )

        val functionDeclaration = object {
            val functionDeclaration: DefDeclaration by lazy {
                DefDeclaration(
                    givenName = givenName,
                    typeVariables = builtTypeVariables.typeVariables,
                    argumentListDeclaration = builtArgumentListDeclaration,
                    explicitReturnType = explicitReturnType?.build(
                        scope = scopeWithTypeVariables,
                    ),
                    buildDefinition = {
                        body?.let {
                            FunctionDefinition(
                                body = it.build(
                                    scope = builtArgumentListDeclaration.extendScope(
                                        scope = scopeWithTypeVariables.extendOpen(
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
    argumentListDeclaration: ArgumentListDeclaration,
    body: FunctionBody?,
    explicitReturnType: Type?,
) {
    argumentListDeclaration.validate()

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
    val valueType: TypeExpressionB,
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
    @Suppress("NAME_SHADOWING")
    fun build(scope: StaticScope): FunctionBody {
        val builtValueDefinitions = buildValueDefinitions(
            scope = scope,
            definitions = definitions,
        )

        val scope = builtValueDefinitions.extendedScope

        return FunctionBody(
            definitions = builtValueDefinitions.definitions,
            returned = returned.build(scope = scope),
        )
    }
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
    val aliasedType: TypeExpressionB,
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
                typeAlike = typeAliasDeclaration.aliasedType,
            ),
            typeAliasDeclaration = typeAliasDeclaration,
        )
    }
}

data class UnionDeclaration(
    val unionName: String,
    val unionTypeAlike: UnionTypeAlike,
) : TopLevelDeclaration {
    override fun validate() {
    }
}

data class UnionAlternativeB(
    val tagName: String,
    val type: TypeExpressionB,
) {
    fun build(scope: StaticScope) = UnionAlternative(
        tagName = tagName,
        type = type.build(scope),
    )
}

data class UnionDeclarationB(
    val unionName: String,
    val typeArguments: List<TypeVariableB>,
    val alternatives: List<UnionAlternativeB>,
) : TopLevelDeclarationB {
    data class Built(
        override val extendedScope: StaticScope,
        val unionDeclaration: UnionDeclaration,
    ) : TopLevelDeclarationB.Built {
        override val topLevelDeclaration: TopLevelDeclaration
            get() = unionDeclaration
    }

    @Suppress("NAME_SHADOWING")
    override fun build(scope: StaticScope): Built =
        if (typeArguments.isNotEmpty()) {
            val builtTypeVariables = buildTypeVariables(
                scope = scope,
                typeVariables = typeArguments,
            )

            buildResult(
                scope = scope,
                unionTypeAlike = UnionTypeConstructor(
                    typeArguments = builtTypeVariables.typeVariables,
                    typeStructure = buildTypeStructure(
                        scope = builtTypeVariables.extendedScope,
                    ),
                )
            )
        } else buildResult(
            scope = scope,
            unionTypeAlike = buildTypeStructure(scope = scope),
        )

    private fun buildTypeStructure(
        scope: StaticScope,
    ): WideUnionType = WideUnionType(
        alternatives = alternatives.map {
            it.build(scope = scope)
        }.toSet(),
    )

    private fun buildResult(
        scope: StaticScope,
        unionTypeAlike: UnionTypeAlike,
    ): Built {
        val unionDeclaration = UnionDeclaration(
            unionName = unionName,
            unionTypeAlike = unionTypeAlike,
        )

        return Built(
            extendedScope = scope.extendType(
                name = unionName,
                typeAlike = unionDeclaration.unionTypeAlike,
            ),
            unionDeclaration = unionDeclaration,
        )
    }
}

data class SingletonDeclaration(
    override val givenName: String,
    override val valueType: SingletonType,
) : ValueDeclaration, TopLevelDeclaration {
    override fun validate() {
    }
}

data class SingletonDeclarationB(
    val singletonName: String,
) : TopLevelDeclarationB {
    data class Built(
        override val extendedScope: StaticScope,
        val singletonDeclaration: SingletonDeclaration,
    ) : TopLevelDeclarationB.Built {
        override val topLevelDeclaration: TopLevelDeclaration
            get() = singletonDeclaration
    }

    override fun build(scope: StaticScope): Built {
        val singletonType = SingletonType(
            singletonName = singletonName,
        )

        val singletonDeclaration = SingletonDeclaration(
            givenName = singletonName,
            valueType = singletonType,
        )

        return Built(
            extendedScope = scope
                .extendType(
                    name = singletonName,
                    typeAlike = singletonType,
                ).extendClosed(
                    name = singletonName,
                    declaration = singletonDeclaration,
                ),
            singletonDeclaration = singletonDeclaration,
        )
    }
}
