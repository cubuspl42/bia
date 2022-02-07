package bia.parser

import bia.model.AdditionExpression
import bia.model.AndExpression
import bia.model.ArgumentDeclaration
import bia.model.ArgumentListDeclaration
import bia.model.BasicArgumentListDeclaration
import bia.model.BigIntegerType
import bia.model.ValueDeclaration
import bia.model.BooleanLiteralExpression
import bia.model.BooleanType
import bia.model.CallExpression
import bia.model.DivisionExpression
import bia.model.EqualsExpression
import bia.model.Expression
import bia.model.FunctionBody
import bia.model.DefDeclaration
import bia.model.FunctionDefinition
import bia.model.FunctionType
import bia.model.GreaterThenExpression
import bia.model.IfExpression
import bia.model.IntLiteralExpression
import bia.model.IntegerDivisionExpression
import bia.model.IsExpression
import bia.model.LambdaExpression
import bia.model.LessThenExpression
import bia.model.ListType
import bia.model.MultiplicationExpression
import bia.model.NarrowUnionType
import bia.model.NotExpression
import bia.model.NullableType
import bia.model.NumberType
import bia.model.ObjectFieldReadExpression
import bia.model.ObjectLiteralExpression
import bia.model.ObjectType
import bia.model.OrExpression
import bia.model.Program
import bia.model.ReferenceExpression
import bia.model.ReminderExpression
import bia.model.SequenceType
import bia.model.SmartCastDeclaration
import bia.model.SubtractionExpression
import bia.model.TagExpression
import bia.model.TopLevelDeclaration
import bia.model.Type
import bia.model.TypeAliasDeclaration
import bia.model.TypeVariable
import bia.model.UnionAlternative
import bia.model.UnionDeclaration
import bia.model.UnionType
import bia.model.UntagExpression
import bia.model.ValDeclaration
import bia.model.ValueDefinition
import bia.model.VarargArgumentListDeclaration
import bia.model.WideUnionType
import bia.parser.antlr.BiaLexer
import bia.parser.antlr.BiaParser
import bia.parser.antlr.BiaParserBaseVisitor
import bia.type_checker.TypeCheckError
import org.antlr.v4.runtime.ParserRuleContext

fun transformProgram(
    outerScope: StaticScope,
    parser: BiaParser,
): Program = Program(
    topLevelDeclarations = transformTopLevelDeclarations(
        scope = outerScope,
        inputDeclarations = parser.program().topLevelDeclaration(),
    ),
)

fun transformTopLevelDeclarations(
    scope: StaticScope,
    inputDeclarations: List<BiaParser.TopLevelDeclarationContext>,
): List<TopLevelDeclaration> = inputDeclarations.firstOrNull()?.let {
    val topLevelDeclaration = transformTopLevelDeclaration(
        scope = scope,
        topLevelDeclaration = it,
    )

    val newScope = when (topLevelDeclaration) {
        is ValueDeclaration -> scope.extendClosed(
            name = topLevelDeclaration.givenName,
            declaration = topLevelDeclaration,
        )
        is TypeAliasDeclaration -> scope.extendType(
            name = topLevelDeclaration.aliasName,
            type = topLevelDeclaration.aliasedType,
        )
        is UnionDeclaration -> scope.extendType(
            name = topLevelDeclaration.unionName,
            type = topLevelDeclaration.unionType,
        )
    }

    listOf(topLevelDeclaration) + transformTopLevelDeclarations(
        scope = newScope,
        inputDeclarations = inputDeclarations.drop(1),
    )
} ?: emptyList()

private fun transformTopLevelDeclaration(
    scope: StaticScope,
    topLevelDeclaration: BiaParser.TopLevelDeclarationContext,
): TopLevelDeclaration = object : BiaParserBaseVisitor<TopLevelDeclaration>() {
    override fun visitDeclaration(ctx: BiaParser.DeclarationContext): TopLevelDeclaration =
        transformValueDeclaration(
            scope = scope,
            declaration = ctx,
        )

    override fun visitTypeAlias(ctx: BiaParser.TypeAliasContext) =
        TypeAliasDeclaration(
            aliasName = ctx.aliasName.text,
            aliasedType = transformTypeExpression(
                scope = scope,
                typeExpression = ctx.aliasedType,
            )
        )

    override fun visitUnionDeclaration(
        ctx: BiaParser.UnionDeclarationContext,
    ): TopLevelDeclaration = UnionDeclaration(
        unionName = ctx.givenName.text,
        unionType = WideUnionType(
            alternatives = ctx.unionEntryDeclaration().map {
                UnionAlternative(
                    tagName = it.typeReference().text,
                    type = transformTypeReference(
                        scope = scope,
                        typeReference = it.typeReference(),
                    ),
                )
            }.toSet()
        ),
    )
}.visit(topLevelDeclaration)

fun transformBody(
    outerScope: StaticScope,
    body: BiaParser.BodyContext,
): FunctionBody {
    val result = transformDeclarations(
        scope = outerScope,
        inputDeclarations = body.declarationList().declaration(),
        outputDeclarations = emptyList(),
    )

    val returned = transformExpression(
        scope = result.finalScope,
        expression = body.return_().expression(),
    )

    return FunctionBody(
        declarations = result.declarations,
        returned = returned,
    )
}

fun transformFunction(
    outerScope: StaticScope,
    body: BiaParser.BodyContext,
): FunctionBody {
    val result = transformDeclarations(
        scope = outerScope,
        inputDeclarations = body.declarationList().declaration(),
        outputDeclarations = emptyList(),
    )

    val returned = transformExpression(
        scope = result.finalScope,
        expression = body.return_().expression(),
    )

    return FunctionBody(
        declarations = result.declarations,
        returned = returned,
    )
}

data class TransformDeclarationsResult(
    val finalScope: StaticScope,
    val declarations: List<ValueDefinition>,
)

fun transformDeclarations(
    scope: StaticScope,
    inputDeclarations: List<BiaParser.DeclarationContext>,
    outputDeclarations: List<ValueDefinition>,
): TransformDeclarationsResult = inputDeclarations.firstOrNull()?.let {
    val declaration = transformValueDeclaration(
        scope = scope,
        declaration = it,
    )

    transformDeclarations(
        scope = scope.extendClosed(
            name = declaration.givenName,
            declaration = declaration,
        ),
        inputDeclarations = inputDeclarations.drop(1),
        outputDeclarations = outputDeclarations + declaration,
    )
} ?: TransformDeclarationsResult(
    finalScope = scope,
    declarations = outputDeclarations,
)

fun transformValueDeclaration(
    scope: StaticScope,
    declaration: BiaParser.DeclarationContext,
): ValueDefinition = object : BiaParserBaseVisitor<ValueDefinition>() {
    override fun visitValueDeclaration(
        ctx: BiaParser.ValueDeclarationContext,
    ) = ValDeclaration(
        givenName = ctx.name.text,
        initializer = transformExpression(
            scope = scope,
            expression = ctx.initializer,
        ),
    )

    override fun visitFunctionDeclaration(
        ctx: BiaParser.FunctionDeclarationContext,
    ): ValueDefinition {
        val functionGivenName = ctx.name.text

        val isExternal = ctx.External() != null

        val (typeVariables, scopeWithTypeVariables) = transformTypeVariableDeclarations(
            scope = scope,
            genericArgumentDeclarationList = ctx.genericArgumentListDeclaration(),
        )

        val argumentListDeclaration = transformArgumentListDeclarations(
            scope = scopeWithTypeVariables,
            argumentListDeclaration = ctx.argumentListDeclaration(),
        )

        val explicitReturnType = ctx.explicitReturnType?.let {
            transformTypeExpression(
                scope = scopeWithTypeVariables,
                typeExpression = it,
            )
        }

        val bodyOrNull: BiaParser.BodyContext? = ctx.body()

        fun transformDefinedFunction(): DefDeclaration {
            val body = bodyOrNull ?: throw TypeCheckError("Non-external function needs to have a body")

            return object {
                val functionDeclaration: DefDeclaration by lazy {
                    DefDeclaration(
                        givenName = functionGivenName,
                        typeVariables = typeVariables,
                        argumentListDeclaration = argumentListDeclaration,
                        explicitReturnType = explicitReturnType,
                        buildDefinition = {
                            FunctionDefinition(
                                body = transformBody(
                                    outerScope = argumentListDeclaration.extendScope(
                                        scope = scopeWithTypeVariables.extendOpen(
                                            name = functionGivenName,
                                            declaration = functionDeclaration,
                                        ),
                                    ),
                                    body = body,
                                ),
                            )
                        }
                    )
                }

                init {
                    functionDeclaration.definition
                }
            }.functionDeclaration
        }

        fun transformExternalFunction(): DefDeclaration {
            if (bodyOrNull != null) throw TypeCheckError("External functions cannot have a body")

            if (explicitReturnType == null) throw TypeCheckError("External functions needs an explicit return type")

            return DefDeclaration(
                givenName = ctx.name.text,
                typeVariables = typeVariables,
                argumentListDeclaration = argumentListDeclaration,
                explicitReturnType = explicitReturnType,
                buildDefinition = { null },
            )
        }

        return if (isExternal) transformExternalFunction()
        else transformDefinedFunction()
    }
}.visit(declaration)

data class TransformTypeVariableDeclarationsResult(
    val typeVariables: List<TypeVariable>,
    val newScope: StaticScope,
)

fun transformTypeVariableDeclarations(
    scope: StaticScope,
    genericArgumentDeclarationList: BiaParser.GenericArgumentListDeclarationContext?,
): TransformTypeVariableDeclarationsResult {
    val genericArgumentDeclarations = genericArgumentDeclarationList?.generitArgumentDeclaration() ?: emptyList()

    fun transformRecursively(
        baseResult: TransformTypeVariableDeclarationsResult,
        genericArgumentDeclarations: List<BiaParser.GeneritArgumentDeclarationContext>,
    ): TransformTypeVariableDeclarationsResult {
        if (genericArgumentDeclarations.isEmpty()) return baseResult

        val head = genericArgumentDeclarations.first()
        val tail = genericArgumentDeclarations.drop(1)

        val givenName = head.name.text

        val allocationResult = baseResult.newScope.allocateTypeVariable(givenName = givenName)

        return transformRecursively(
            baseResult = TransformTypeVariableDeclarationsResult(
                typeVariables = baseResult.typeVariables + allocationResult.allocatedVariable,
                newScope = allocationResult.newScope,
            ),
            genericArgumentDeclarations = tail,
        )
    }

    return transformRecursively(
        baseResult = TransformTypeVariableDeclarationsResult(
            typeVariables = emptyList(),
            newScope = scope,
        ),
        genericArgumentDeclarations = genericArgumentDeclarations,
    )
}

fun transformArgumentListDeclarations(
    scope: StaticScope,
    argumentListDeclaration: BiaParser.ArgumentListDeclarationContext,
): ArgumentListDeclaration = object : BiaParserBaseVisitor<ArgumentListDeclaration>() {
    override fun visitBasicArgumentListDeclaration(
        ctx: BiaParser.BasicArgumentListDeclarationContext,
    ): ArgumentListDeclaration = BasicArgumentListDeclaration(
        argumentDeclarations = ctx.argumentDeclaration().map {
            ArgumentDeclaration(
                givenName = it.name.text,
                valueType = transformTypeExpression(
                    scope = scope,
                    typeExpression = it.typeExpression(),
                ),
            )
        },
    )

    override fun visitVarargArgumentListDeclaration(
        ctx: BiaParser.VarargArgumentListDeclarationContext,
    ): ArgumentListDeclaration = VarargArgumentListDeclaration(
        givenName = ctx.givenName.text,
        type = transformTypeExpression(
            scope = scope,
            typeExpression = ctx.typeExpression(),
        ),
    )
}.visit(argumentListDeclaration)

fun transformExpression(
    scope: StaticScope,
    expression: ParserRuleContext,
): Expression = object : BiaParserBaseVisitor<Expression>() {
    override fun visitReferenceExpression(ctx: BiaParser.ReferenceExpressionContext): Expression =
        transformReferenceExpression(
            scope = scope,
            expression = ctx,
        )

    override fun visitCallExpression(ctx: BiaParser.CallExpressionContext): Expression =
        CallExpression(
            callee = transformExpression(
                scope = scope,
                expression = ctx.callee,
            ),
            typeArguments = ctx.callTypeVariableList()?.typeExpression()?.map {
                transformTypeExpression(
                    scope = scope,
                    typeExpression = it,
                )
            } ?: emptyList(),
            arguments = ctx.callArgumentList().expression().map {
                transformExpression(
                    scope = scope,
                    expression = it,
                )
            },
        )

    override fun visitEqualsOperation(ctx: BiaParser.EqualsOperationContext): Expression =
        EqualsExpression(
            left = transformExpression(
                scope = scope,
                expression = ctx.left,
            ),
            right = transformExpression(
                scope = scope,
                expression = ctx.right,
            ),
        )

    override fun visitIntLiteral(ctx: BiaParser.IntLiteralContext): Expression =
        IntLiteralExpression(
            value = ctx.IntLiteral().text.toLong(radix = 10),
        )

    override fun visitTrueLiteral(ctx: BiaParser.TrueLiteralContext?): Expression =
        BooleanLiteralExpression(
            value = true,
        )

    override fun visitFalseLiteral(ctx: BiaParser.FalseLiteralContext?): Expression =
        BooleanLiteralExpression(
            value = false,
        )

    override fun visitObjectLiteral(ctx: BiaParser.ObjectLiteralContext) =
        ObjectLiteralExpression(
            entries = ctx.objectLiteralEntry().associate {
                it.assignedFieldName.text to transformExpression(
                    scope = scope,
                    expression = it.initializer,
                )
            },
        )

    override fun visitParenExpression(ctx: BiaParser.ParenExpressionContext): Expression =
        transformExpression(
            scope = scope,
            expression = ctx.expression(),
        )

    override fun visitBinaryOperation(ctx: BiaParser.BinaryOperationContext): Expression {
        val left = transformExpression(
            scope = scope,
            expression = ctx.left,
        )

        val right = transformExpression(
            scope = scope,
            expression = ctx.right,
        )

        val operator = ctx.operator

        return when (operator.type) {
            BiaLexer.Plus -> AdditionExpression(left, right)
            BiaLexer.Minus -> SubtractionExpression(left, right)
            BiaLexer.Multiplication -> MultiplicationExpression(left, right)
            BiaLexer.Division -> DivisionExpression(left, right)
            BiaLexer.IntegerDivision -> IntegerDivisionExpression(left, right)
            BiaLexer.Reminder -> ReminderExpression(left, right)
            BiaLexer.Or -> OrExpression(left, right)
            BiaLexer.And -> AndExpression(left, right)
            BiaLexer.Lt -> LessThenExpression(left, right)
            BiaLexer.Gt -> GreaterThenExpression(left, right)
            else -> throw UnsupportedOperationException("Unrecognized operator: ${operator.text}")
        }
    }

    override fun visitUnaryOperation(ctx: BiaParser.UnaryOperationContext): Expression {
        val argument = transformExpression(
            scope = scope,
            expression = ctx.expression(),
        )

        val operator = ctx.operator

        return when (operator.type) {
            BiaLexer.Not -> NotExpression(argument)
            else -> throw UnsupportedOperationException("Unrecognized operator: ${operator.text}")
        }
    }

    override fun visitIfExpression(ctx: BiaParser.IfExpressionContext): Expression {
        val guard = transformExpression(
            scope = scope,
            expression = ctx.guard,
        )

        return IfExpression(
            guard = guard,
            trueBranch = transformExpression(
                scope = processGuardSmartCast(
                    scope = scope,
                    guard = guard,
                ),
                expression = ctx.trueBranch,
            ),
            falseBranch = transformExpression(
                scope = scope,
                expression = ctx.falseBranch,
            ),
        )
    }

    override fun visitLambdaExpression(ctx: BiaParser.LambdaExpressionContext): Expression {
        val (typeVariables, scopeWithTypeVariables) = transformTypeVariableDeclarations(
            scope = scope,
            genericArgumentDeclarationList = ctx.genericArgumentListDeclaration(),
        )

        val argumentListDeclaration = transformArgumentListDeclarations(
            scope = scopeWithTypeVariables,
            argumentListDeclaration = ctx.argumentListDeclaration(),
        )

        val explicitReturnType = ctx.explicitReturnType?.let {
            transformTypeExpression(
                scope = scopeWithTypeVariables,
                typeExpression = it,
            )
        }

        val body = transformBody(
            outerScope = argumentListDeclaration.extendScope(scope = scopeWithTypeVariables),
            body = ctx.body(),
        )

        return LambdaExpression(
            typeVariables = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            explicitReturnType = explicitReturnType,
            body = body,
        )
    }

    override fun visitObjectFieldRead(ctx: BiaParser.ObjectFieldReadContext) =
        ObjectFieldReadExpression(
            objectExpression = transformExpression(
                scope = scope,
                expression = ctx.expression(),
            ),
            readFieldName = ctx.readFieldName.text,
        )

    override fun visitIsExpression(
        ctx: BiaParser.IsExpressionContext,
    ): Expression = IsExpression(
        expression = transformExpression(
            scope = scope,
            expression = ctx.expression(),
        ),
        checkedTagName = ctx.tagName.text,
    )

    override fun visitTagExpression(
        ctx: BiaParser.TagExpressionContext,
    ): Expression = TagExpression(
        expression = transformExpression(
            scope = scope,
            expression = ctx.expression(),
        ),
        attachedTagName = ctx.attachedTagName.text,
    )

    override fun visitUntagExpression(
        ctx: BiaParser.UntagExpressionContext,
    ): Expression = UntagExpression(
        expression = transformExpression(
            scope = scope,
            expression = ctx.expression(),
        )
    )
}.visit(expression)

private fun processGuardSmartCast(
    scope: StaticScope,
    guard: Expression,
): StaticScope = if (guard is IsExpression) {
    val isExpression = guard.expression

    if (isExpression is ReferenceExpression) {
        val referredDeclaration = isExpression.referredDeclaration

        if (referredDeclaration != null) {
            val declaration = referredDeclaration.declaration
            val valueType = declaration.valueType

            if (valueType is UnionType) {
                val checkedAlternative = valueType.getAlternative(tagName = guard.checkedTagName)

                if (checkedAlternative != null) {
                    scope.extendClosed(
                        name = declaration.givenName,
                        declaration = SmartCastDeclaration(
                            givenName = declaration.givenName,
                            valueType = NarrowUnionType(
                                alternatives = valueType.alternatives,
                                narrowedAlternative = checkedAlternative,
                            ),
                        ),
                    )
                } else scope
            } else scope
        } else scope
    } else scope
} else scope

fun transformReferenceExpression(
    scope: StaticScope,
    expression: BiaParser.ReferenceExpressionContext,
): ReferenceExpression {
    val referredName: String = expression.referredName.text

    return ReferenceExpression(
        referredName = referredName,
        referredDeclaration = scope.getScopedDeclaration(name = referredName)
    )
}

fun transformTypeExpression(
    scope: StaticScope,
    typeExpression: BiaParser.TypeExpressionContext,
): Type = object : BiaParserBaseVisitor<Type>() {
    override fun visitNumberType(ctx: BiaParser.NumberTypeContext) = NumberType

    override fun visitBooleanType(ctx: BiaParser.BooleanTypeContext) = BooleanType

    override fun visitBigIntegerType(ctx: BiaParser.BigIntegerTypeContext) = BigIntegerType

    override fun visitFunctionType(ctx: BiaParser.FunctionTypeContext) = FunctionType(
        typeVariables = emptyList(),
        argumentListDeclaration = transformArgumentListDeclarations(
            scope = scope,
            argumentListDeclaration = ctx.argumentListDeclaration(),
        ),
        returnType = transformTypeExpression(
            scope = scope,
            typeExpression = ctx.returnType,
        ),
    )

    override fun visitConstructedType(ctx: BiaParser.ConstructedTypeContext): Type {
        val typeConstructor: BiaParser.TypeConstructorContext = ctx.typeConstructor()
        val argumentType = transformTypeExpression(
            scope = scope,
            typeExpression = ctx.typeExpression(),
        )

        return transformTypeConstructor(
            typeConstructor = typeConstructor,
            argumentType = argumentType
        )
    }

    override fun visitNullableType(ctx: BiaParser.NullableTypeContext) = NullableType(
        baseType = transformTypeExpression(
            scope = scope,
            typeExpression = ctx.typeExpression(),
        ),
    )

    override fun visitTypeReference(ctx: BiaParser.TypeReferenceContext): Type =
        transformTypeReference(scope = scope, typeReference = ctx)

    override fun visitObjectType(ctx: BiaParser.ObjectTypeContext) = ObjectType(
        entries = ctx.objectTypeEntryDeclaration().associate {
            it.fieldName.text to transformTypeExpression(
                scope = scope,
                typeExpression = it.typeExpression(),
            )
        }
    )
}.visit(typeExpression)

private fun transformTypeReference(
    scope: StaticScope,
    typeReference: BiaParser.TypeReferenceContext,
): Type = scope.getType(givenName = typeReference.name.text)

fun transformTypeConstructor(
    typeConstructor: BiaParser.TypeConstructorContext,
    argumentType: Type,
): Type = object : BiaParserBaseVisitor<Type>() {
    override fun visitListConstructor(ctx: BiaParser.ListConstructorContext) =
        ListType(elementType = argumentType)

    override fun visitSequenceConstructor(ctx: BiaParser.SequenceConstructorContext) =
        SequenceType(elementType = argumentType)
}.visit(typeConstructor)
