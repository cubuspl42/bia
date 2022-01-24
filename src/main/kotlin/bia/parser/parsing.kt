package bia.parser

import bia.model.FunctionBody
import bia.parser.antlr.BiaLexer
import bia.parser.antlr.BiaParser
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
