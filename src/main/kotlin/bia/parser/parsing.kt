package bia.parser

import bia.BiaLexer
import bia.BiaParser
import bia.model.FunctionBody
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

fun parseProgram(
    sourceName: String,
    source: String,
): FunctionBody {
    val lexer = BiaLexer(CharStreams.fromString(source, sourceName))
    val tokenStream = CommonTokenStream(lexer)
    val parser = BiaParser(tokenStream)

    val programBody = transformProgram(parser = parser)

    return programBody
}
