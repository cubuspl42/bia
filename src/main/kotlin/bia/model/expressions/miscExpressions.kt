package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.model.ArgumentListDeclaration
import bia.model.ArgumentListDeclarationB
import bia.model.BasicArgumentListDeclaration
import bia.model.DefDeclaration
import bia.model.DefinedFunctionValue
import bia.model.FunctionBody
import bia.model.FunctionBodyB
import bia.model.FunctionType
import bia.model.ObjectType
import bia.model.Type
import bia.model.TypeExpressionB
import bia.model.TypeVariable
import bia.model.TypeVariableB
import bia.model.TypeVariableMapping
import bia.model.Value
import bia.model.asBooleanValue
import bia.model.asFunctionValue
import bia.model.asObjectValue
import bia.model.buildType
import bia.model.buildTypeVariables
import bia.model.validateFunction
import bia.parser.ClosedDeclaration
import bia.parser.OpenDeclaration
import bia.parser.ScopedDeclaration
import bia.parser.StaticScope
import bia.type_checker.TypeCheckError
import bia.type_checker.inferTypeVariableMappingForCall
import bia.type_checker.processGuardSmartCast
import java.lang.IllegalArgumentException

data class ReferenceExpression(
    val referredName: String,
    val referredDeclaration: ScopedDeclaration?,
) : Expression {
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type {
        val referredDeclaration =
            this.referredDeclaration ?: throw TypeCheckError("Unresolved reference ($referredName) has no type")

        val extendedContext = context.withVisited(expression = this)

        return referredDeclaration.declaration.determineValueType(
            context = extendedContext,
        )
    }

    override fun evaluate(scope: DynamicScope): Value =
        scope.getValue(name = referredName)
            ?: throw UnsupportedOperationException("Unresolved reference at runtime: $referredName")
}

data class ReferenceExpressionB(
    val referredName: String,
) : ExpressionB {
    override fun build(scope: StaticScope) = ReferenceExpression(
        referredName = referredName,
        referredDeclaration = scope.getScopedDeclaration(name = referredName),
    )
}

data class CallExpression(
    val callee: Expression,
    val explicitTypeArguments: List<Type>?,
    val passedArguments: List<Expression>,
) : Expression {
    private fun determineCalleeType(context: TypeDeterminationContext): FunctionType {
        val calleeType = callee.determineType(context = context)
        return calleeType as? FunctionType ?: throw TypeCheckError("Tried to call a non-function: $calleeType")
    }

    private fun determineResolvedCalleeType(context: TypeDeterminationContext): FunctionType {
        val calleeType = determineCalleeType(context = context)

        return if (calleeType.typeArguments.isNotEmpty()) {
            val typeVariableMapping = if (explicitTypeArguments != null) buildTypeVariableMapping(
                typeArguments = calleeType.typeArguments,
                passedTypeArguments = explicitTypeArguments,
            ) else inferTypeVariableMappingForCall(
                typeArguments = calleeType.typeArguments.toSet(),
                argumentList = calleeType.argumentListDeclaration,
                passedTypes = passedArguments.map {
                    it.determineType(context = context)
                },
            )

            calleeType.resolveTypeVariables(
                mapping = typeVariableMapping
            )
        } else calleeType
    }

    private val calleeType: FunctionType by lazy {
        determineCalleeType(context = TypeDeterminationContext.empty)
    }

    private val resolvedCalleeType: FunctionType by lazy {
        determineResolvedCalleeType(context = TypeDeterminationContext.empty)
    }

    override fun determineTypeDirectly(context: TypeDeterminationContext): Type {
        val extendedContext = context.withVisited(this)

        val resolvedCalleeType = determineResolvedCalleeType(context = extendedContext)

        return resolvedCalleeType.returnType
    }

    override fun validate() {
        callee.validate()

        val functionName = if (callee is ReferenceExpression) callee.referredName else "(unnamed)"

        explicitTypeArguments?.let {
            validateTypeArguments(
                typeArguments = calleeType.typeArguments,
                passedTypeArguments = it,
                message = { definedTypeVariableCount, passedTypeArgumentCount ->
                    "Function $functionName was defined with $definedTypeVariableCount type variables, $passedTypeArgumentCount passed"
                }
            )
        }

        resolvedCalleeType.argumentListDeclaration.validateCall(
            functionName = functionName,
            arguments = passedArguments,
        )

        super.validate()
    }

    override fun evaluate(scope: DynamicScope): Value {
        val calleeValue = callee.evaluate(scope = scope).asFunctionValue(
            message = "Only functions can be called, tried",
        )

        val argumentValues = passedArguments.map {
            it.evaluate(scope = scope)
        }

        return calleeValue.call(
            arguments = argumentValues,
        )
    }
}

fun buildTypeVariableMapping(
    typeArguments: List<TypeVariable>,
    passedTypeArguments: List<Type>,
): TypeVariableMapping {
    if (typeArguments.size != passedTypeArguments.size) {
        throw TypeCheckError("${typeArguments.size} type arguments declared, ${passedTypeArguments.size} passed")
    }

    return TypeVariableMapping(
        mapping = passedTypeArguments.zip(typeArguments) { passedTypeArgument, typeArgument ->
            typeArgument to passedTypeArgument
        }.toMap(),
    )
}

fun validateTypeArguments(
    typeArguments: List<TypeVariable>,
    passedTypeArguments: List<Type>,
    message: (definedTypeVariableCount: Int, passedTypeArgumentCount: Int) -> String,
) {
    val definedTypeVariableCount = typeArguments.size
    val passedTypeArgumentCount = passedTypeArguments.size

    if (passedTypeArgumentCount != definedTypeVariableCount) {
        throw TypeCheckError(
            message(definedTypeVariableCount, passedTypeArgumentCount),
        )
    }
}

data class CallExpressionB(
    val callee: ExpressionB,
    val explicitTypeArguments: List<TypeExpressionB>?,
    val arguments: List<ExpressionB>,
) : ExpressionB {
    override fun build(scope: StaticScope) = CallExpression(
        callee = callee.build(scope = scope),
        explicitTypeArguments = explicitTypeArguments?.map { it.buildType(scope = scope) },
        passedArguments = arguments.map { it.build(scope = scope) },
    )
}

data class IfExpression(
    val guard: Expression,
    val trueBranch: Expression,
    val falseBranch: Expression,
) : Expression {
    override fun determineTypeDirectly(context: TypeDeterminationContext): Type {
        val extendedContext = context.withVisited(expression = this)

        val trueBranchType = trueBranch.determineType(context = extendedContext)
        val falseBranchType = falseBranch.determineType(context = extendedContext)

        return if (trueBranchType == falseBranchType) {
            trueBranchType
        } else {
            throw TypeCheckError("If expression has incompatible true- and false-branch types: " +
                    "${trueBranchType.toPrettyString()}, ${falseBranchType.toPrettyString()}")
        }
    }

    override fun evaluate(scope: DynamicScope): Value {
        val guardValue = guard.evaluate(scope = scope).asBooleanValue(
            message = "Guard has to be a boolean",
        )

        fun evaluateTrueBranchValue() = trueBranch.evaluate(scope = scope)
        fun evaluateFalseBranchValue() = falseBranch.evaluate(scope = scope)

        return if (guardValue.value) evaluateTrueBranchValue() else evaluateFalseBranchValue()
    }
}

data class IfExpressionB(
    val guard: ExpressionB,
    val trueBranch: ExpressionB,
    val falseBranch: ExpressionB,
) : ExpressionB {
    override fun build(scope: StaticScope): IfExpression {
        val guardExpression = guard.build(scope = scope)

        return IfExpression(
            guard = guardExpression,
            trueBranch = trueBranch.build(
                scope = processGuardSmartCast(
                    scope = scope,
                    guard = guardExpression,
                ),
            ),
            falseBranch = falseBranch.build(scope = scope),
        )
    }
}

data class LambdaExpression(
    val typeVariables: List<TypeVariable>,
    val argumentListDeclaration: ArgumentListDeclaration,
    val explicitReturnType: Type?,
    val body: FunctionBody,
) : Expression {
    private val argumentDeclarations by lazy {
        (argumentListDeclaration as BasicArgumentListDeclaration).argumentDeclarations
    }

    override fun determineTypeDirectly(context: TypeDeterminationContext): Type {
        val extendedContext = context.withVisited(this)

        return FunctionType(
            typeArguments = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            returnType = explicitReturnType ?: body.returned.determineType(
                context = extendedContext,
            ),
        )
    }

    override fun validate() {
        validateFunction(
            argumentListDeclaration = argumentListDeclaration,
            body = body,
            explicitReturnType = explicitReturnType,
        )
    }

    override fun evaluate(scope: DynamicScope): Value = DefinedFunctionValue(
        closure = scope,
        argumentDeclarations = argumentDeclarations,
        body = body,
    )
}

data class LambdaExpressionB(
    val typeVariables: List<TypeVariableB>,
    val argumentListDeclaration: ArgumentListDeclarationB,
    val explicitReturnType: TypeExpressionB?,
    val body: FunctionBodyB,
) : ExpressionB {
    @Suppress("NAME_SHADOWING")
    override fun build(scope: StaticScope): Expression {
        val builtTypeVariables = buildTypeVariables(
            scope = scope,
            typeVariables = typeVariables,
        )

        var scope = builtTypeVariables.extendedScope

        val argumentListDeclaration = argumentListDeclaration.build(
            scope = scope,
        )

        scope = argumentListDeclaration.extendScope(
            scope = scope,
        )

        return LambdaExpression(
            typeVariables = builtTypeVariables.typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            explicitReturnType = explicitReturnType?.buildType(
                scope = scope,
            ),
            body = body.build(scope = scope),
        )
    }
}

data class ObjectFieldReadExpression(
    val objectExpression: Expression,
    val readFieldName: String,
) : Expression {
    private fun determineObjectType(
        context: TypeDeterminationContext,
    ): ObjectType {
        val objectExpressionType = objectExpression.determineType(context = context)

        return objectExpressionType as? ObjectType
            ?: throw TypeCheckError("Tried to read a field from a non-object: ${objectExpressionType.toPrettyString()}")
    }

    override fun determineTypeDirectly(context: TypeDeterminationContext): Type {
        val extendedContext = context.withVisited(expression = this)

        val objectType = determineObjectType(context = extendedContext)

        return objectType.entries[readFieldName]
            ?: throw TypeCheckError("Object with type ${objectType.toPrettyString()} does not have a field named $readFieldName")
    }

    override fun evaluate(scope: DynamicScope): Value {
        val objectValue = objectExpression.evaluate(scope = scope).asObjectValue(
            message = "Only objects can have fields read",
        )

        return objectValue.entries[readFieldName]
            ?: throw IllegalArgumentException("Object doesn't have field $readFieldName at runtime")
    }
}

data class ObjectFieldReadExpressionB(
    val objectExpression: ExpressionB,
    val readFieldName: String,
) : ExpressionB {
    override fun build(scope: StaticScope) = ObjectFieldReadExpression(
        objectExpression = objectExpression.build(scope = scope),
        readFieldName = readFieldName,
    )
}
