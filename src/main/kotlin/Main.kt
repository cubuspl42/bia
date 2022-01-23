import bia.BiaLexer
import bia.BiaParser
import bia.BiaParserBaseVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

const val source = "(12 * 13) * 2"

const val sourceName = "<main>"

data class Token(
    val type: Int,
    val name: String,
)

class Visitor : BiaParserBaseVisitor<Double>() {
    override fun visitProgram(ctx: BiaParser.ProgramContext): Double =
        visit(ctx.root)

    override fun visitIntLiteral(ctx: BiaParser.IntLiteralContext): Double =
        ctx.IntLiteral().text.toInt(radix = 10).toDouble()

    override fun visitBinaryOperation(ctx: BiaParser.BinaryOperationContext): Double {
        val leftValue = visit(ctx.left)
        val rightValue = visit(ctx.right)

        val operator = ctx.operator

        return when (operator.type) {
            BiaLexer.Plus -> leftValue + rightValue
            BiaLexer.Multiplication -> leftValue * rightValue
            else -> throw UnsupportedOperationException("Unrecognized operator: ${operator.text}")
        }
    }

    override fun visitParenExpression(ctx: BiaParser.ParenExpressionContext): Double =
        visit(ctx.expression())
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
