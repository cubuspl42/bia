import bia.interpreter.Scope
import bia.interpreter.evaluateBody
import bia.parser.parseProgram

const val sourceName = "<main>"

fun main() {
    val source = getResourceAsText("test.bia") ?: throw RuntimeException("Couldn't load the source file")

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

private fun getResourceAsText(path: String): String? =
    object {}.javaClass.getResource(path)?.readText()
