package bia.parser

import bia.BiaLexer
import bia.BiaParser
import bia.BiaParserBaseVisitor
import bia.model.AdditionExpression
import bia.model.CallExpression
import bia.model.Declaration
import bia.model.EqualsExpression
import bia.model.Expression
import bia.model.FunctionBody
import bia.model.FunctionDeclaration
import bia.model.FunctionDefinition
import bia.model.IfExpression
import bia.model.IntLiteralExpression
import bia.model.MultiplicationExpression
import bia.model.ReferenceExpression
import bia.model.SubtractionExpression
import bia.model.ValueDeclaration

fun transformProgram(
    parser: BiaParser,
): FunctionBody = transformBody(
    body = parser.program().body(),
)

fun transformBody(
    body: BiaParser.BodyContext,
): FunctionBody = FunctionBody(
    declarations = body.declaration().map(::transformDeclaration),
    returned = transformExpression(
        expression = body.return_().expression(),
    ),
)

fun transformDeclaration(
    declaration: BiaParser.DeclarationContext,
): Declaration = object : BiaParserBaseVisitor<Declaration>() {
    override fun visitValueDeclaration(
        ctx: BiaParser.ValueDeclarationContext,
    ) = ValueDeclaration(
        givenName = ctx.name.text,
        initializer = transformExpression(
            expression = ctx.initializer,
        ),
    )

    override fun visitFunctionDeclaration(
        ctx: BiaParser.FunctionDeclarationContext,
    ): Declaration = FunctionDeclaration(
        givenName = ctx.name.text,
        definition = FunctionDefinition(
            argumentName = ctx.argument.text,
            body = transformBody(body = ctx.body()),
        )
    )
}.visit(declaration)

fun transformExpression(
    expression: BiaParser.ExpressionContext,
): Expression = object : BiaParserBaseVisitor<Expression>() {
    override fun visitReference(ctx: BiaParser.ReferenceContext): Expression =
        ReferenceExpression(referredName = ctx.text)

    override fun visitCallExpression(ctx: BiaParser.CallExpressionContext): Expression =
        CallExpression(
            callee = transformExpression(expression = ctx.callee),
            argument = transformExpression(expression = ctx.argument),
        )

    override fun visitEqualsOperation(ctx: BiaParser.EqualsOperationContext): Expression =
        EqualsExpression(
            left = transformExpression(expression = ctx.left),
            right = transformExpression(expression = ctx.right),
        )

    override fun visitIntLiteral(ctx: BiaParser.IntLiteralContext): Expression =
        IntLiteralExpression(
            value = ctx.IntLiteral().text.toInt(radix = 10),
        )

    override fun visitParenExpression(ctx: BiaParser.ParenExpressionContext): Expression =
        transformExpression(
            expression = ctx.expression(),
        )

    override fun visitBinaryOperation(ctx: BiaParser.BinaryOperationContext): Expression {
        val left = transformExpression(expression = ctx.left)
        val right = transformExpression(expression = ctx.right)

        val operator = ctx.operator

        return when (operator.type) {
            BiaLexer.Plus -> AdditionExpression(left, right)
            BiaLexer.Minus -> SubtractionExpression(left, right)
            BiaLexer.Multiplication -> MultiplicationExpression(left, right)
            else -> throw UnsupportedOperationException("Unrecognized operator: ${operator.text}")
        }
    }

    override fun visitIfExpression(ctx: BiaParser.IfExpressionContext): Expression =
        IfExpression(
            guard = transformExpression(expression = ctx.guard),
            trueBranch = transformExpression(expression = ctx.trueBranch),
            falseBranch = transformExpression(expression = ctx.falseBranch),
        )
}.visit(expression)
