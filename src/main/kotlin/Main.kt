import bia.BiaLexer
import bia.BiaParser
import bia.ExecuteProgramVisitor
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

fun main() {
    val lexer = BiaLexer(CharStreams.fromString(source, sourceName))
    val tokenStream = CommonTokenStream(lexer)
    val parser = BiaParser(tokenStream)

    val result = ExecuteProgramVisitor().visit(parser.program())

    println("Result: $result")
}
