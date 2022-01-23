import bia.BiaLexer
import bia.BiaParser
import bia.BiaParserBaseVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

const val source = "if 0 == 0 then 42 else 43"

const val sourceName = "<main>"

data class Token(
    val type: Int,
    val name: String,
)

sealed interface Value

data class NumberValue(val value: Double) : Value

data class BooleanValue(val value: Boolean) : Value

class Visitor : BiaParserBaseVisitor<Value>() {
    companion object {
        private fun asNumberValue(value: Value, message: String): NumberValue =
            value as? NumberValue ?: throw UnsupportedOperationException("$message: $value")

        private fun asBooleanValue(value: Value, message: String): BooleanValue =
            value as? BooleanValue ?: throw UnsupportedOperationException("$message: $value")
    }

    override fun visitProgram(ctx: BiaParser.ProgramContext): Value =
        visit(ctx.root)

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
}

fun main() {
    printTokens()

    val lexer = BiaLexer(CharStreams.fromString(source, sourceName))
    val tokenStream = CommonTokenStream(lexer)
    val parser = BiaParser(tokenStream)

    val result = Visitor().run {
        visit(parser.program())
    }

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
