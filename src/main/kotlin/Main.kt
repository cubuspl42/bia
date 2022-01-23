import bia.BiaLexer
import org.antlr.v4.runtime.CharStreams

const val source = "1234"

const val sourceName = "<main>"

data class Token(
    val type: Int,
    val name: String,
)

fun main() {
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
