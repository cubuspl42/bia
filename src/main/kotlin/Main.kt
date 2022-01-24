import bia.BiaLexer
import bia.BiaParser
import bia.BiaParserBaseVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

const val source = """
    val a = 10
    val b = a + 20
    val c = 15 * 2
    val d = b == c
    
    def f(x) {
        val a = x * 2
        val b = a + 10
        return b
    }
    
    return if d then f(7) else 22
"""

const val sourceName = "<main>"

data class Token(
    val type: Int,
    val name: String,
)

sealed interface Value

data class NumberValue(val value: Double) : Value

data class BooleanValue(val value: Boolean) : Value

class FunctionValue(
    private val argumentName: String,
    private val body: BiaParser.BodyContext,
) : Value {
    fun call(argumentValue: Value): Value = evaluateBody(
        outerScope = Scope(
            values = mapOf(argumentName to argumentValue),
        ),
        body = body,
    )
}

data class Scope(
    val values: Map<String, Value>,
)

class EvaluateExpressionVisitor(
    private val scope: Scope,
) : BiaParserBaseVisitor<Value>() {
    companion object {
        private fun asNumberValue(value: Value, message: String): NumberValue =
            value as? NumberValue ?: throw UnsupportedOperationException("$message: $value")

        private fun asBooleanValue(value: Value, message: String): BooleanValue =
            value as? BooleanValue ?: throw UnsupportedOperationException("$message: $value")

        private fun asFunctionValue(value: Value, message: String): FunctionValue =
            value as? FunctionValue ?: throw UnsupportedOperationException("$message: $value")
    }

    override fun visitIntLiteral(ctx: BiaParser.IntLiteralContext): Value =
        NumberValue(
            value = ctx.IntLiteral().text.toInt(radix = 10).toDouble(),
        )

    override fun visitBinaryOperation(ctx: BiaParser.BinaryOperationContext): Value {
        val leftValue = visit(ctx.left)
        val rightValue = visit(ctx.right)

        fun asOperandValue(value: Value) = asNumberValue(
            value = value,
            message = "Cannot perform mathematical operations on non-number",
        )

        val leftNumber = asOperandValue(value = leftValue)
        val rightNumber = asOperandValue(value = rightValue)

        val operator = ctx.operator

        return NumberValue(
            value = when (operator.type) {
                BiaLexer.Plus -> leftNumber.value + rightNumber.value
                BiaLexer.Multiplication -> leftNumber.value * rightNumber.value
                else -> throw UnsupportedOperationException("Unrecognized operator: ${operator.text}")
            },
        )
    }

    override fun visitEqualsOperation(ctx: BiaParser.EqualsOperationContext): Value {
        val leftValue = visit(ctx.left)
        val rightValue = visit(ctx.right)

        val leftNumber = leftValue as? NumberValue
        val leftBoolean = leftValue as? BooleanValue

        val rightNumber = rightValue as? NumberValue
        val rightBoolean = rightValue as? BooleanValue

        return BooleanValue(
            value = when {
                leftNumber != null && rightNumber != null -> leftNumber.value == rightNumber.value
                leftBoolean != null && rightBoolean != null -> leftBoolean.value == rightBoolean.value
                else -> throw UnsupportedOperationException("Cannot compare values of different type: $leftValue, $rightValue")
            },
        )
    }

    override fun visitParenExpression(ctx: BiaParser.ParenExpressionContext): Value =
        visit(ctx.expression())

    override fun visitIfExpression(ctx: BiaParser.IfExpressionContext): Value {
        val guardValue = asBooleanValue(value = visit(ctx.guard), "Guard has to be a boolean")
        val ifTrueValue = visit(ctx.ifTrue)
        val ifFalseValue = visit(ctx.ifFalse)

        return if (guardValue.value) ifTrueValue
        else ifFalseValue
    }

    override fun visitReference(ctx: BiaParser.ReferenceContext): Value {
        val valueName = ctx.Identifier().text

        return scope.values[valueName] ?: throw UnsupportedOperationException("Unresolved reference: $valueName")
    }

    override fun visitCallExpression(ctx: BiaParser.CallExpressionContext): Value {
        val callee = asFunctionValue(
            value = evaluateExpression(
                scope = scope,
                expression = ctx.callee,
            ),
            message = "Only functions can be called, tried",
        )

        val argument = evaluateExpression(
            scope = scope,
            expression = ctx.argument,
        )

        return callee.call(argument)
    }
}

private fun evaluateExpression(
    scope: Scope,
    expression: BiaParser.ExpressionContext,
): Value = EvaluateExpressionVisitor(scope = scope).visit(expression)

class ExecuteDeclarationVisitor(
    private val scope: Scope,
) : BiaParserBaseVisitor<Scope>() {
    companion object {
        private fun asNumberValue(value: Value, message: String): NumberValue =
            value as? NumberValue ?: throw UnsupportedOperationException("$message: $value")

        private fun asBooleanValue(value: Value, message: String): BooleanValue =
            value as? BooleanValue ?: throw UnsupportedOperationException("$message: $value")
    }

    override fun visitValueDeclaration(ctx: BiaParser.ValueDeclarationContext): Scope {
        val valueName = ctx.identifier.text

        val value = evaluateExpression(
            scope = scope,
            expression = ctx.expression(),
        )

        return Scope(
            values = scope.values + (valueName to value)
        )
    }

    override fun visitFunctionDeclaration(ctx: BiaParser.FunctionDeclarationContext): Scope {
        val valueName = ctx.name.text

        val value = FunctionValue(
            argumentName = ctx.argument.text,
            body = ctx.body(),
        )

        return Scope(
            values = scope.values + (valueName to value)
        )
    }
}

private fun executeDeclaration(
    scope: Scope,
    declaration: BiaParser.DeclarationContext,
): Scope = ExecuteDeclarationVisitor(scope = scope).visit(declaration)

class ExecuteProgramVisitor : BiaParserBaseVisitor<Value>() {
    override fun visitProgram(ctx: BiaParser.ProgramContext): Value {
        val initialScope = Scope(values = emptyMap())

        return evaluateBody(
            outerScope = initialScope,
            body = ctx.body(),
        )
    }
}

private fun evaluateBody(
    outerScope: Scope,
    body: BiaParser.BodyContext,
): Value {
    val finalScope = body.declaration().fold(outerScope) { scope, declaration ->
        executeDeclaration(scope = scope, declaration = declaration)
    }

    val returnValue = evaluateExpression(
        scope = finalScope,
        expression = body.return_().expression(),
    )

    return returnValue
}

fun main() {
    printTokens()

    val lexer = BiaLexer(CharStreams.fromString(source, sourceName))
    val tokenStream = CommonTokenStream(lexer)
    val parser = BiaParser(tokenStream)

    val result = ExecuteProgramVisitor().visit(parser.program())

    println("Result: $result")
}

private fun printTokens() {
    val lexer = BiaLexer(CharStreams.fromString(source, sourceName))

    val tokens = sequence {
        while (true) {
            when (val tokenType = lexer.nextToken().type) {
                -1 -> break
                else -> yield(
                    Token(
                        type = tokenType,
                        name = lexer.ruleNames[tokenType - 1],
                    ),
                )
            }
        }
    }.toList()

    println(tokens)
}
