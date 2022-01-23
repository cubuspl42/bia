import bia.BiaLexer
import bia.BiaParser
import bia.BiaParserBaseVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

const val source = "12 + 13"

const val sourceName = "<main>"

data class Token(
    val type: Int,
    val name: String,
)

class Visitor : BiaParserBaseVisitor<Double>() {
    override fun visitProgram(ctx: BiaParser.ProgramContext): Double =
        visit(ctx.root)

    override fun visitIntLiteral(ctx: BiaParser.IntLiteralContext): Double =
        ctx.INTLIT().text.toInt(radix = 10).toDouble()

    override fun visitBinaryOperation(ctx: BiaParser.BinaryOperationContext): Double =
        visit(ctx.left) + visit(ctx.right)
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
