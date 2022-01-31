package bia.parser

import bia.model.AdditionExpression
import bia.model.AndExpression
import bia.model.ArgumentDeclaration
import bia.model.BigIntegerType
import bia.model.BodyDeclaration
import bia.model.BooleanLiteralExpression
import bia.model.BooleanType
import bia.model.CallExpression
import bia.model.DivisionExpression
import bia.model.EqualsExpression
import bia.model.Expression
import bia.model.ExternalFunctionDeclaration
import bia.model.FunctionBody
import bia.model.FunctionDeclaration
import bia.model.FunctionDefinition
import bia.model.FunctionType
import bia.model.GreaterThenExpression
import bia.model.IfExpression
import bia.model.IntLiteralExpression
import bia.model.IntegerDivisionExpression
import bia.model.LessThenExpression
import bia.model.ListType
import bia.model.MultiplicationExpression
import bia.model.NotExpression
import bia.model.NullableType
import bia.model.NumberType
import bia.model.OrExpression
import bia.model.ReferenceExpression
import bia.model.ReminderExpression
import bia.model.SequenceType
import bia.model.SubtractionExpression
import bia.model.Type
import bia.model.ValueDeclaration
import bia.parser.antlr.BiaLexer
import bia.parser.antlr.BiaParser
import bia.parser.antlr.BiaParserBaseVisitor

fun transformProgram(
    outerScope: StaticScope,
    parser: BiaParser,
): FunctionBody = transformBody(
    outerScope = outerScope,
    body = parser.program().body(),
)

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

data class TransformDeclarationsResult(
    val finalScope: StaticScope,
    val declarations: List<BodyDeclaration>,
)

fun transformDeclarations(
    scope: StaticScope,
    inputDeclarations: List<BiaParser.DeclarationContext>,
    outputDeclarations: List<BodyDeclaration>,
): TransformDeclarationsResult = inputDeclarations.firstOrNull()?.let {
    val declaration = transformBodyDeclaration(
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

fun transformBodyDeclaration(
    scope: StaticScope,
    declaration: BiaParser.DeclarationContext,
): BodyDeclaration = object : BiaParserBaseVisitor<BodyDeclaration>() {
    override fun visitValueDeclaration(
        ctx: BiaParser.ValueDeclarationContext,
    ) = ValueDeclaration(
        givenName = ctx.name.text,
        initializer = transformExpression(
            scope = scope,
            expression = ctx.initializer,
        ),
    )

    override fun visitFunctionDeclaration(
        ctx: BiaParser.FunctionDeclarationContext,
    ): FunctionDeclaration {
        val functionGivenName = ctx.name.text

        val argumentDeclarations = transformArgumentDeclarations(
            argumentListDeclaration = ctx.argumentListDeclaration(),
        )

        val explicitReturnType = ctx.explicitReturnType?.let {
            transformType(type = it)
        }

        return object {
            val functionDeclaration: FunctionDeclaration by lazy {
                FunctionDeclaration(
                    givenName = functionGivenName,
                    buildDefinition = {
                        FunctionDefinition(
                            argumentDeclarations = argumentDeclarations,
                            explicitReturnType = explicitReturnType,
                            body = transformBody(
                                outerScope = scope
                                    .extendOpen(
                                        name = functionGivenName,
                                        declaration = functionDeclaration,
                                    )
                                    .extendClosed(
                                        namedDeclarations = argumentDeclarations.map { it.givenName to it },
                                    ),
                                body = ctx.body(),
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

    override fun visitExternalFunctionDeclaration(
        ctx: BiaParser.ExternalFunctionDeclarationContext,
    ) = ExternalFunctionDeclaration(
        givenName = ctx.name.text,
        argumentDeclarations = transformArgumentDeclarations(
            argumentListDeclaration = ctx.argumentListDeclaration(),
        ),
        returnType = transformType(ctx.returnType),
    )
}.visit(declaration)

fun transformArgumentDeclarations(
    argumentListDeclaration: BiaParser.ArgumentListDeclarationContext,
): List<ArgumentDeclaration> = argumentListDeclaration.argumentDeclaration().map {
    ArgumentDeclaration(
        givenName = it.name.text,
        type = transformType(type = it.type()),
    )
}

fun transformExpression(
    scope: StaticScope,
    expression: BiaParser.ExpressionContext,
): Expression = object : BiaParserBaseVisitor<Expression>() {
    override fun visitReference(ctx: BiaParser.ReferenceContext): Expression {
        val referredName: String = ctx.text

        return ReferenceExpression(
            referredName = referredName,
            referredDeclaration = scope.getScopedDeclaration(name = referredName)
        )
    }

    override fun visitCallExpression(ctx: BiaParser.CallExpressionContext): Expression =
        CallExpression(
            callee = transformExpression(
                scope = scope,
                expression = ctx.callee,
            ),
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

    override fun visitIfExpression(ctx: BiaParser.IfExpressionContext): Expression =
        IfExpression(
            guard = transformExpression(
                scope = scope,
                expression = ctx.guard,
            ),
            trueBranch = transformExpression(
                scope = scope,
                expression = ctx.trueBranch,
            ),
            falseBranch = transformExpression(
                scope = scope,
                expression = ctx.falseBranch,
            ),
        )
}.visit(expression)

fun transformType(
    type: BiaParser.TypeContext,
): Type = object : BiaParserBaseVisitor<Type>() {
    override fun visitNumberType(ctx: BiaParser.NumberTypeContext) = NumberType

    override fun visitBooleanType(ctx: BiaParser.BooleanTypeContext) = BooleanType

    override fun visitBigIntegerType(ctx: BiaParser.BigIntegerTypeContext) = BigIntegerType

    override fun visitFunctionType(ctx: BiaParser.FunctionTypeContext) = FunctionType(
        argumentDeclarations = transformArgumentDeclarations(
            argumentListDeclaration = ctx.argumentListDeclaration(),
        ),
        returnType = transformType(type = ctx.returnType),
    )

    override fun visitConstructedType(ctx: BiaParser.ConstructedTypeContext): Type {
        val typeConstructor: BiaParser.TypeConstructorContext = ctx.typeConstructor()
        val argumentType = transformType(type = ctx.type())

        return transformTypeConstructor(
            typeConstructor = typeConstructor,
            argumentType = argumentType
        )
    }

    override fun visitNullableType(ctx: BiaParser.NullableTypeContext) = NullableType(
        baseType = transformType(type = ctx.type()),
    )
}.visit(type)

fun transformTypeConstructor(
    typeConstructor: BiaParser.TypeConstructorContext,
    argumentType: Type,
): Type = object : BiaParserBaseVisitor<Type>() {
    override fun visitListConstructor(ctx: BiaParser.ListConstructorContext) =
        ListType(elementType = argumentType)

    override fun visitSequenceConstructor(ctx: BiaParser.SequenceConstructorContext) =
        SequenceType(elementType = argumentType)
}.visit(typeConstructor)
