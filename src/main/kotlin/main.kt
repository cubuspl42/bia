import bia.interpreter.Scope
import bia.interpreter.evaluateBody
import bia.parser.parseProgram

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
    val programBody = parseProgram(
        sourceName = sourceName,
        source = source,
    )

    val result = evaluateBody(
        outerScope = Scope.empty,
        body = programBody,
    )

    println("Result: $result")
}
