import bia.interpreter.evaluateProgram
import bia.Prelude
import bia.model.asFunctionValue
import bia.parser.parseProgram
import java.lang.IllegalStateException

const val sourceName = "test.bia"

fun main() {
    val preludeSource = getResourceAsText("prelude.bia") ?: throw RuntimeException("Couldn't load the prelude")

    val prelude = Prelude.load(preludeSource = preludeSource)

    val source = getResourceAsText(sourceName) ?: throw RuntimeException("Couldn't load the source file")

    val program = parseProgram(
        prelude = prelude,
        sourceName = sourceName,
        source = source,
    )

    program.validate()

    val scope = evaluateProgram(
        program = program,
    )

    val mainValue = scope.getValue("main") ?: throw IllegalStateException("There's no value named main")

    val mainFunction = mainValue.asFunctionValue("main is not a function")

    val result = mainFunction.call(emptyList())

    println("Result: $result")
}

private fun getResourceAsText(path: String): String? =
    object {}.javaClass.getResource(path)?.readText()
