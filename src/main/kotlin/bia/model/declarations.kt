package bia.model

sealed interface Declaration {
    val givenName: String
}

data class ValueDeclaration(
    override val givenName: String,
    val initializer: Expression,
) : Declaration

data class FunctionDeclaration(
    override val givenName: String,
    val definition: FunctionDefinition,
) : Declaration

data class FunctionDefinition(
    val argumentName: String,
    val body: FunctionBody,
)

data class FunctionBody(
    val declarations: List<Declaration>,
    val returned: Expression,
)
