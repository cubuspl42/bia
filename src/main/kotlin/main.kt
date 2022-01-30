import bia.interpreter.evaluateProgramBody
import bia.parser.parseProgram

const val sourceName = "test.bia"

fun main() {
    val prelude = getResourceAsText("prelude.bia") ?: throw RuntimeException("Couldn't load the prelude")

    val source = getResourceAsText(sourceName) ?: throw RuntimeException("Couldn't load the source file")

    val joinedSource = listOf(prelude, source).joinToString(separator = "\n\n")

    val programBody = parseProgram(
        sourceName = sourceName,
        source = joinedSource,
    )

    programBody.validate()

    val result = evaluateProgramBody(
        programBody = programBody,
    )

    println("Result: $result")
}

private fun getResourceAsText(path: String): String? =
    object {}.javaClass.getResource(path)?.readText()
