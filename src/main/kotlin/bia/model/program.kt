package bia.model

data class Program(
    val topLevelDeclarations: List<TopLevelDeclaration>,
) {
    fun validate() {
        topLevelDeclarations.forEach { it.validate() }
    }
}
