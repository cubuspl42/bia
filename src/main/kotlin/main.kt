import bia.interpreter.evaluateProgramBody
import bia.Prelude
import bia.parser.parseProgram

const val sourceName = "problem4.bia"

fun main() {
    val preludeSource = getResourceAsText("prelude.bia") ?: throw RuntimeException("Couldn't load the prelude")

    val prelude = Prelude.load(preludeSource = preludeSource)

    val source = getResourceAsText(sourceName) ?: throw RuntimeException("Couldn't load the source file")

    val programBody = parseProgram(
        prelude = prelude,
        sourceName = sourceName,
        source = source,
    )

    programBody.validate()

    val result = evaluateProgramBody(
        programBody = programBody,
    )

    println("Result: $result")
}

private fun getResourceAsText(path: String): String? =
    object {}.javaClass.getResource(path)?.readText()
