package bia.parser

import bia.model.ArgumentDeclarationB
import bia.model.ArgumentListDeclarationB
import bia.model.BasicArgumentListDeclarationB
import bia.model.BigIntegerType
import bia.model.BooleanType
import bia.model.DefDeclarationB
import bia.model.FunctionBodyB
import bia.model.FunctionTypeB
import bia.model.ListTypeB
import bia.model.NullableTypeB
import bia.model.NumberType
import bia.model.ObjectTypeB
import bia.model.ProgramB
import bia.model.SequenceTypeB
import bia.model.TopLevelDeclarationB
import bia.model.Type
import bia.model.TypeAliasDeclarationB
import bia.model.TypeExpression
import bia.model.TypeVariableB
import bia.model.UnionAlternativeB
import bia.model.UnionDeclarationB
import bia.model.ValDeclarationB
import bia.model.ValueDefinition
import bia.model.ValueDefinitionB
import bia.model.VarargArgumentListDeclarationB
import bia.model.expressions.AdditionExpressionB
import bia.model.expressions.AndExpressionB
import bia.model.expressions.BooleanLiteralExpression
import bia.model.expressions.CallExpressionB
import bia.model.expressions.DivisionExpressionB
import bia.model.expressions.EqualsExpressionB
import bia.model.expressions.ExpressionB
import bia.model.expressions.GreaterThenExpressionB
import bia.model.expressions.IfExpressionB
import bia.model.expressions.IntLiteralExpression
import bia.model.expressions.IntegerDivisionExpressionB
import bia.model.expressions.IsExpressionB
import bia.model.expressions.LambdaExpressionB
import bia.model.expressions.LessThenExpressionB
import bia.model.expressions.MatchBranchB
import bia.model.expressions.MatchExpressionB
import bia.model.expressions.MultiplicationExpressionB
import bia.model.expressions.NotExpressionB
import bia.model.expressions.ObjectFieldReadExpressionB
import bia.model.expressions.ObjectLiteralExpressionB
import bia.model.expressions.OrExpressionB
import bia.model.expressions.ReferenceExpressionB
import bia.model.expressions.ReminderExpressionB
import bia.model.expressions.SubtractionExpressionB
import bia.model.expressions.TagExpressionB
import bia.model.expressions.UntagExpressionB
import bia.parser.antlr.BiaLexer
import bia.parser.antlr.BiaParser
import bia.parser.antlr.BiaParserBaseVisitor
import bia.type_checker.TypeCheckError
import org.antlr.v4.runtime.ParserRuleContext

fun transformProgram(
    parser: BiaParser,
): ProgramB = ProgramB(
    topLevelDeclarations = parser.program().topLevelDeclaration().map {
        transformTopLevelDeclaration(topLevelDeclaration = it)
    }
)

private fun transformTopLevelDeclaration(
    topLevelDeclaration: BiaParser.TopLevelDeclarationContext,
): TopLevelDeclarationB = object : BiaParserBaseVisitor<TopLevelDeclarationB>() {
    override fun visitDeclaration(ctx: BiaParser.DeclarationContext): TopLevelDeclarationB =
        transformValueDefinition(
            declaration = ctx,
        )

    override fun visitTypeAlias(ctx: BiaParser.TypeAliasContext) =
        TypeAliasDeclarationB(
            aliasName = ctx.aliasName.text,
            aliasedType = transformTypeExpression(
                typeExpression = ctx.aliasedType,
            )
        )

    override fun visitUnionDeclaration(
        ctx: BiaParser.UnionDeclarationContext,
    ): TopLevelDeclarationB = UnionDeclarationB(
        unionName = ctx.givenName.text,
        alternatives = ctx.unionEntryDeclaration().map {
            UnionAlternativeB(
                tagName = it.typeReference().text,
                type = transformTypeReference(
                    typeReference = it.typeReference(),
                ),
            )
        },
    )
}.visit(topLevelDeclaration)

fun transformBody(
    body: BiaParser.BodyContext,
): FunctionBodyB {
    val result = transformDeclarations(
        inputDeclarations = body.declarationList().declaration(),
    )

    val returned = transformExpression(
        expression = body.return_().expression(),
    )

    return FunctionBodyB(
        definitions = result,
        returned = returned,
    )
}

fun transformDeclarations(
    inputDeclarations: List<BiaParser.DeclarationContext>,
): List<ValueDefinitionB> = inputDeclarations.map {
    transformValueDefinition(declaration = it)
}

fun transformValueDefinition(
    declaration: BiaParser.DeclarationContext,
): ValueDefinitionB = object : BiaParserBaseVisitor<ValueDefinitionB>() {
    override fun visitValueDeclaration(
        ctx: BiaParser.ValueDeclarationContext,
    ) = ValDeclarationB(
        givenName = ctx.name.text,
        initializer = transformExpression(
            expression = ctx.initializer,
        ),
    )

    override fun visitFunctionDeclaration(
        ctx: BiaParser.FunctionDeclarationContext,
    ): ValueDefinitionB {
        val functionGivenName = ctx.name.text

        val isExternal = ctx.External() != null

        val typeVariables = transformTypeVariableDeclarations(
            genericArgumentDeclarationList = ctx.genericArgumentListDeclaration(),
        )

        val argumentListDeclaration = transformArgumentListDeclarations(
            argumentListDeclaration = ctx.argumentListDeclaration(),
        )

        val explicitReturnType = ctx.explicitReturnType?.let {
            transformTypeExpression(
                typeExpression = it,
            )
        }

        val bodyOrNull: BiaParser.BodyContext? = ctx.body()

        fun transformDefinedFunction(): DefDeclarationB {
            val body = bodyOrNull ?: throw TypeCheckError("Non-external function needs to have a body")

            return DefDeclarationB(
                givenName = functionGivenName,
                typeVariables = typeVariables,
                argumentListDeclaration = argumentListDeclaration,
                explicitReturnType = explicitReturnType,
                body = transformBody(body = body),
            )
        }

        fun transformExternalFunction(): DefDeclarationB {
            if (bodyOrNull != null) throw TypeCheckError("External functions cannot have a body")

            if (explicitReturnType == null) throw TypeCheckError("External functions needs an explicit return type")

            return DefDeclarationB(
                givenName = ctx.name.text,
                typeVariables = typeVariables,
                argumentListDeclaration = argumentListDeclaration,
                explicitReturnType = explicitReturnType,
                body = null,
            )
        }

        return if (isExternal) transformExternalFunction()
        else transformDefinedFunction()
    }
}.visit(declaration)

fun transformTypeVariableDeclarations(
    genericArgumentDeclarationList: BiaParser.GenericArgumentListDeclarationContext?,
): List<TypeVariableB> {
    val genericArgumentDeclarations = genericArgumentDeclarationList?.generitArgumentDeclaration() ?: emptyList()

    return genericArgumentDeclarations.map {
        TypeVariableB(
            givenName = it.name.text,
        )
    }
}

fun transformArgumentListDeclarations(
    argumentListDeclaration: BiaParser.ArgumentListDeclarationContext,
): ArgumentListDeclarationB = object : BiaParserBaseVisitor<ArgumentListDeclarationB>() {
    override fun visitBasicArgumentListDeclaration(
        ctx: BiaParser.BasicArgumentListDeclarationContext,
    ) = BasicArgumentListDeclarationB(
        argumentDeclarations = ctx.argumentDeclaration().map {
            ArgumentDeclarationB(
                givenName = it.name.text,
                valueType = transformTypeExpression(
                    typeExpression = it.typeExpression(),
                ),
            )
        },
    )

    override fun visitVarargArgumentListDeclaration(
        ctx: BiaParser.VarargArgumentListDeclarationContext,
    ) = VarargArgumentListDeclarationB(
        givenName = ctx.givenName.text,
        type = transformTypeExpression(
            typeExpression = ctx.typeExpression(),
        ),
    )
}.visit(argumentListDeclaration)

fun transformExpression(
    expression: ParserRuleContext,
): ExpressionB = object : BiaParserBaseVisitor<ExpressionB>() {
    override fun visitReferenceExpression(ctx: BiaParser.ReferenceExpressionContext) =
        transformReferenceExpression(
            expression = ctx,
        )

    override fun visitCallExpression(ctx: BiaParser.CallExpressionContext) =
        CallExpressionB(
            callee = transformExpression(
                expression = ctx.callee,
            ),
            typeArguments = ctx.callTypeVariableList()?.typeExpression()?.map {
                transformTypeExpression(
                    typeExpression = it,
                )
            } ?: emptyList(),
            arguments = ctx.callArgumentList().expression().map {
                transformExpression(
                    expression = it,
                )
            },
        )

    override fun visitEqualsOperation(ctx: BiaParser.EqualsOperationContext): ExpressionB =
        EqualsExpressionB(
            left = transformExpression(
                expression = ctx.left,
            ),
            right = transformExpression(
                expression = ctx.right,
            ),
        )

    override fun visitIntLiteral(ctx: BiaParser.IntLiteralContext): ExpressionB =
        IntLiteralExpression(
            value = ctx.IntLiteral().text.toLong(radix = 10),
        )

    override fun visitTrueLiteral(ctx: BiaParser.TrueLiteralContext?): ExpressionB =
        BooleanLiteralExpression(
            value = true,
        )

    override fun visitFalseLiteral(ctx: BiaParser.FalseLiteralContext?): ExpressionB =
        BooleanLiteralExpression(
            value = false,
        )

    override fun visitObjectLiteral(ctx: BiaParser.ObjectLiteralContext) =
        ObjectLiteralExpressionB(
            entries = ctx.objectLiteralEntry().associate {
                it.assignedFieldName.text to transformExpression(
                    expression = it.initializer,
                )
            },
        )

    override fun visitParenExpression(ctx: BiaParser.ParenExpressionContext): ExpressionB =
        transformExpression(
            expression = ctx.expression(),
        )

    override fun visitBinaryOperation(ctx: BiaParser.BinaryOperationContext): ExpressionB {
        val left = transformExpression(
            expression = ctx.left,
        )

        val right = transformExpression(
            expression = ctx.right,
        )

        val operator = ctx.operator

        return when (operator.type) {
            BiaLexer.Plus -> AdditionExpressionB(left, right)
            BiaLexer.Minus -> SubtractionExpressionB(left, right)
            BiaLexer.Multiplication -> MultiplicationExpressionB(left, right)
            BiaLexer.Division -> DivisionExpressionB(left, right)
            BiaLexer.IntegerDivision -> IntegerDivisionExpressionB(left, right)
            BiaLexer.Reminder -> ReminderExpressionB(left, right)
            BiaLexer.Or -> OrExpressionB(left, right)
            BiaLexer.And -> AndExpressionB(left, right)
            BiaLexer.Lt -> LessThenExpressionB(left, right)
            BiaLexer.Gt -> GreaterThenExpressionB(left, right)
            else -> throw UnsupportedOperationException("Unrecognized operator: ${operator.text}")
        }
    }

    override fun visitUnaryOperation(ctx: BiaParser.UnaryOperationContext): ExpressionB {
        val argument = transformExpression(
            expression = ctx.expression(),
        )

        val operator = ctx.operator

        return when (operator.type) {
            BiaLexer.Not -> NotExpressionB(argument)
            else -> throw UnsupportedOperationException("Unrecognized operator: ${operator.text}")
        }
    }

    override fun visitIfExpression(ctx: BiaParser.IfExpressionContext): ExpressionB {
        val guard = transformExpression(
            expression = ctx.guard,
        )

        return IfExpressionB(
            guard = guard,
            trueBranch = transformExpression(
                expression = ctx.trueBranch,
            ),
            falseBranch = transformExpression(
                expression = ctx.falseBranch,
            ),
        )
    }

    override fun visitLambdaExpression(ctx: BiaParser.LambdaExpressionContext): ExpressionB {
        val typeVariables = transformTypeVariableDeclarations(
            genericArgumentDeclarationList = ctx.genericArgumentListDeclaration(),
        )

        val argumentListDeclaration = transformArgumentListDeclarations(
            argumentListDeclaration = ctx.argumentListDeclaration(),
        )

        val explicitReturnType = ctx.explicitReturnType?.let {
            transformTypeExpression(
                typeExpression = it,
            )
        }

        val body = transformBody(
            body = ctx.body(),
        )

        return LambdaExpressionB(
            typeVariables = typeVariables,
            argumentListDeclaration = argumentListDeclaration,
            explicitReturnType = explicitReturnType,
            body = body,
        )
    }

    override fun visitObjectFieldRead(ctx: BiaParser.ObjectFieldReadContext) =
        ObjectFieldReadExpressionB(
            objectExpression = transformExpression(
                expression = ctx.expression(),
            ),
            readFieldName = ctx.readFieldName.text,
        )

    override fun visitIsExpression(
        ctx: BiaParser.IsExpressionContext,
    ): ExpressionB = IsExpressionB(
        checkee = transformExpression(
            expression = ctx.expression(),
        ),
        checkedTagName = ctx.tagName.text,
    )

    override fun visitTagExpression(
        ctx: BiaParser.TagExpressionContext,
    ): ExpressionB = TagExpressionB(
        tagee = transformExpression(
            expression = ctx.expression(),
        ),
        attachedTagName = ctx.attachedTagName.text,
    )

    override fun visitUntagExpression(
        ctx: BiaParser.UntagExpressionContext,
    ): ExpressionB = UntagExpressionB(
        untagee = transformExpression(
            expression = ctx.expression(),
        )
    )

    override fun visitMatchExpression(
        ctx: BiaParser.MatchExpressionContext,
    ): ExpressionB = MatchExpressionB(
        matchee = transformExpression(
            expression = ctx.matchee,
        ),
        taggedBranches = ctx.matchTaggedBranch().map {
            MatchBranchB(
                requiredTagName = it.tagName.text,
                branch = transformExpression(
                    expression = it.branch,
                ),
            )
        },
        elseBranch = ctx.matchElseBranch()?.let {
            transformExpression(
                expression = it,
            )
        },
    )
}.visit(expression)

fun transformReferenceExpression(
    expression: BiaParser.ReferenceExpressionContext,
): ReferenceExpressionB {
    val referredName: String = expression.referredName.text

    return ReferenceExpressionB(
        referredName = referredName,
    )
}

fun transformTypeExpression(
    typeExpression: BiaParser.TypeExpressionContext,
): TypeExpression = object : BiaParserBaseVisitor<TypeExpression>() {
    override fun visitNumberType(ctx: BiaParser.NumberTypeContext) = NumberType

    override fun visitBooleanType(ctx: BiaParser.BooleanTypeContext) = BooleanType

    override fun visitBigIntegerType(ctx: BiaParser.BigIntegerTypeContext) = BigIntegerType

    override fun visitFunctionType(ctx: BiaParser.FunctionTypeContext) = FunctionTypeB(
        typeVariables = emptyList(),
        argumentListDeclaration = transformArgumentListDeclarations(
            argumentListDeclaration = ctx.argumentListDeclaration(),
        ),
        returnType = transformTypeExpression(
            typeExpression = ctx.returnType,
        ),
    )

    override fun visitConstructedType(ctx: BiaParser.ConstructedTypeContext): TypeExpression {
        val typeConstructor: BiaParser.TypeConstructorContext = ctx.typeConstructor()
        val argumentType = transformTypeExpression(
            typeExpression = ctx.typeExpression(),
        )

        return transformTypeConstructor(
            typeConstructor = typeConstructor,
            argumentType = argumentType
        )
    }

    override fun visitNullableType(ctx: BiaParser.NullableTypeContext) = NullableTypeB(
        baseType = transformTypeExpression(
            typeExpression = ctx.typeExpression(),
        ),
    )

    override fun visitTypeReference(ctx: BiaParser.TypeReferenceContext): TypeExpression =
        transformTypeReference(typeReference = ctx)

    override fun visitObjectType(ctx: BiaParser.ObjectTypeContext) = ObjectTypeB(
        entries = ctx.objectTypeEntryDeclaration().associate {
            it.fieldName.text to transformTypeExpression(
                typeExpression = it.typeExpression(),
            )
        }
    )
}.visit(typeExpression)

private fun transformTypeReference(
    typeReference: BiaParser.TypeReferenceContext,
): TypeExpression = object : TypeExpression {
    override fun build(scope: StaticScope): Type =
        scope.getType(givenName = typeReference.name.text)
}

fun transformTypeConstructor(
    typeConstructor: BiaParser.TypeConstructorContext,
    argumentType: TypeExpression,
): TypeExpression = object : BiaParserBaseVisitor<TypeExpression>() {
    override fun visitListConstructor(ctx: BiaParser.ListConstructorContext) =
        ListTypeB(elementType = argumentType)

    override fun visitSequenceConstructor(ctx: BiaParser.SequenceConstructorContext) =
        SequenceTypeB(elementType = argumentType)
}.visit(typeConstructor)
