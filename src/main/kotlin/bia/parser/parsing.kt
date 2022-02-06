package bia.parser

import bia.Prelude
import bia.model.Program
import bia.parser.antlr.BiaLexer
import bia.parser.antlr.BiaParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

fun parseProgram(
    prelude: Prelude,
    sourceName: String,
    source: String,
): Program {
    val parser = buildAntlrParser(source, sourceName)

    val programBody = transformProgram(
        outerScope = prelude.scope,
        parser = parser,
    )

    return programBody
}

fun buildAntlrParser(source: String, sourceName: String): BiaParser {
    val lexer = BiaLexer(CharStreams.fromString(source, sourceName))
    val tokenStream = CommonTokenStream(lexer)
    val parser = BiaParser(tokenStream)
    return parser
}
