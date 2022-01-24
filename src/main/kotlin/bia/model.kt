package bia

sealed interface Value

data class NumberValue(val value: Double) : Value

data class BooleanValue(val value: Boolean) : Value

class FunctionValue(
    private val argumentName: String,
    private val body: BiaParser.BodyContext,
) : Value {
    fun call(argumentValue: Value): Value = evaluateBody(
        outerScope = Scope(
            values = mapOf(argumentName to argumentValue),
        ),
        body = body,
    )
}
