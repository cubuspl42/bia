import bia.interpreter.evaluateProgramBody
import bia.parser.parseProgram

const val sourceName = "problem3.bia"

fun main() {
    val source = getResourceAsText(sourceName) ?: throw RuntimeException("Couldn't load the source file")

    val programBody = parseProgram(
        sourceName = sourceName,
        source = source,
    )

    val result = evaluateProgramBody(
        programBody = programBody,
    )

    println("Result: $result")
}

private fun getResourceAsText(path: String): String? =
    object {}.javaClass.getResource(path)?.readText()
