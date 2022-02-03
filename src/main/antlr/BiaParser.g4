parser grammar BiaParser;

options { tokenVocab = BiaLexer; }

program : body EOF ;

typeExpression
    : NumberType # numberType
    | BooleanType # booleanType
    | BigIntegerType # bigIntegerType
    | LeftParen argumentListDeclaration RightParen Colon returnType=typeExpression # functionType
    | typeConstructor Lt typeExpression Gt # constructedType
    | typeExpression QuestionMark  # nullableType
    | name=Identifier # genericArgumentReference ;

callTypeVariableList: Lt typeExpression (Comma typeExpression)* Gt ;

expression
    : callee=referenceExpression callTypeVariableList? LeftParen callArgumentList RightParen # callExpression
    | left=expression operator=Multiplication right=expression # binaryOperation
    | left=expression operator=Division right=expression # binaryOperation
    | left=expression operator=IntegerDivision right=expression # binaryOperation
    | left=expression operator=Reminder right=expression # binaryOperation
    | left=expression operator=Plus right=expression # binaryOperation
    | left=expression operator=Minus right=expression # binaryOperation
    | left=expression operator=Or right=expression # binaryOperation
    | left=expression operator=And right=expression # binaryOperation
    | left=expression operator=Lt right=expression # binaryOperation
    | left=expression operator=Gt right=expression # binaryOperation
    | operator=Not expression # unaryOperation
    | left=expression operator=Equals right=expression # equalsOperation
    | LeftParen expression RightParen # parenExpression
    | IntLiteral # intLiteral
    | TrueLiteral # trueLiteral
    | FalseLiteral # falseLiteral
    | referenceExpression # reference
    | If guard=expression Then trueBranch=expression Else falseBranch=expression # ifExpression ;

referenceExpression : referredName=Identifier ;

typeConstructor : ListTypeConstructor # listConstructor
                | SequenceTypeConstructor # sequenceConstructor ;

callArgumentList: (expression (Comma expression)*)? ;

valueDeclaration : Val name=Identifier Assign initializer=expression ;

functionDeclaration : External? Def genericArgumentListDeclaration? name=Identifier LeftParen argumentListDeclaration RightParen (Colon explicitReturnType=typeExpression)? (LeftBrace body RightBrace)? ;

genericArgumentListDeclaration : Lt (generitArgumentDeclaration (Comma generitArgumentDeclaration)*)? Gt ;

generitArgumentDeclaration : name=Identifier ;

argumentListDeclaration : (argumentDeclaration (Comma argumentDeclaration)*)? ;

argumentDeclaration: name=Identifier Colon typeExpression;

declaration : valueDeclaration | functionDeclaration ;

declarationList : declaration* ;

body : declarationList return_ ;

return_ : Return expression ;
