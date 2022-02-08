package bia.model.expressions

import bia.interpreter.DynamicScope
import bia.model.ArgumentListDeclaration
import bia.model.BasicArgumentListDeclaration
import bia.model.DefinedFunctionValue
import bia.model.FunctionBody
import bia.model.FunctionType
import bia.model.ObjectType
import bia.model.Type
import bia.model.TypeVariable
import bia.model.TypeVariableMapping
import bia.model.Value
import bia.model.asBooleanValue
import bia.model.asFunctionValue
import bia.model.asObjectValue
import bia.model.validateFunction
import bia.parser.ClosedDeclaration
import bia.parser.OpenFunctionDeclaration
import bia.parser.ScopedDeclaration
import bia.type_checker.TypeCheckError
import java.lang.IllegalArgumentException

data class ReferenceExpression(
    val referredName: String,
    val referredDeclaration: ScopedDeclaration?,
) : Expression {

    override val type: Type by lazy {
        val referredDeclaration =
            this.referredDeclaration ?: throw TypeCheckError("Unresolved reference ($referredName) has no type")

        when (referredDeclaration) {
            is ClosedDeclaration -> referredDeclaration.declaration.valueType
            is OpenFunctionDeclaration -> referredDeclaration.functionDeclaration.explicitType
                ?: throw TypeCheckError("Recursively referenced function $referredName has no explicit return type")
        }
    }

    override fun evaluate(scope: DynamicScope): Value =
        scope.getValue(name = referredName)
            ?: throw UnsupportedOperationException("Unresolved reference at runtime: $referredName")
}

data class CallExpression(
    val callee: Expression,
    val typeArguments: List<Type>,
    val arguments: List<Expression>,
) : Expression {
    private val calleeType: FunctionType by lazy {
        val calleeType = callee.type
        calleeType as? FunctionType ?: throw TypeCheckError("Tried to call a non-function: $calleeType")
    }

    private val resolvedCalleeType: FunctionType by lazy {
        val typeVariableMapping = TypeVariableMapping(
            mapping = typeArguments.zip(calleeType.typeVariables) { typeArgument, typeVariable ->
                typeVariable to typeArgument
            }.toMap(),
        )

        calleeType.resolveTypeVariables(mapping = typeVariableMapping)
    }

    override val type: Type by lazy {
        resolvedCalleeType.returnType
    }

    override fun validate() {
        val typeVariables = calleeType.typeVariables

        val definedTypeVariableCount = typeVariables.size
        val passedTypeArgumentCount = typeArguments.size

        val functionName = if (callee is ReferenceExpression) callee.referredName else "(unnamed)"

        if (passedTypeArgumentCount != definedTypeVariableCount) {
            throw TypeCheckError(
                "Function $functionName was defined with $definedTypeVariableCount type variables, $passedTypeArgumentCount passed",
            )
        }

        resolvedCalleeType.argumentListDeclaration.validateCall(
            functionName = functionName,
            arguments = arguments,
        )

        super.validate()
    }

    override fun evaluate(scope: DynamicScope): Value {
        val calleeValue = callee.evaluate(scope = scope).asFunctionValue(
            message = "Only functions can be called, tried",
        )

        val argumentValues = arguments.map {
            it.evaluate(scope = scope)
        }

        return calleeValue.call(
            arguments = argumentValues,
        )
    }
}

data class IfExpression(
    val guard: Expression,
    val trueBranch: Expression,
    val falseBranch: Expression,
) : Expression {
    override val type: Type by lazy {
        val trueBranchType = trueBranch.type
        val falseBranchType = falseBranch.type

        if (trueBranchType == falseBranchType) {
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

data class LambdaExpression(
    val typeVariables: List<TypeVariable>,
    val argumentListDeclaration: ArgumentListDeclaration,
    val explicitReturnType: Type?,
    val body: FunctionBody,
) : Expression {
    override val type: Type by lazy {
        FunctionType(
            typeVariables = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            returnType = explicitReturnType ?: body.returned.type,
        )
    }

    private val argumentDeclarations by lazy {
        (argumentListDeclaration as BasicArgumentListDeclaration).argumentDeclarations
    }

    override fun validate() {
        validateFunction(
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

data class ObjectFieldReadExpression(
    val objectExpression: Expression,
    val readFieldName: String,
) : Expression {
    private val objectType by lazy {
        objectExpression.type as? ObjectType
            ?: throw TypeCheckError("Tried to read a field from a non-object: ${objectExpression.type.toPrettyString()}")
    }

    override val type: Type by lazy {
        objectType.entries[readFieldName]
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
