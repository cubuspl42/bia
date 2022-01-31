parser grammar BiaParser;

options { tokenVocab = BiaLexer; }

program : body EOF ;

expression : left=expression operator=Multiplication right=expression # binaryOperation
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
           | callee=expression LeftParen callArgumentList RightParen # callExpression
           | IntLiteral # intLiteral
           | TrueLiteral # trueLiteral
           | FalseLiteral # falseLiteral
           | Identifier # reference
           | If guard=expression Then trueBranch=expression Else falseBranch=expression # ifExpression ;

type : NumberType # numberType
     | BooleanType # booleanType
     | BigIntegerType # bigIntegerType
     | LeftParen argumentListDeclaration RightParen Colon returnType=type # functionType
     | typeConstructor Lt type Gt # constructedType
     | type QuestionMark  # nullableType ;

typeConstructor : ListTypeConstructor # listConstructor
                | SequenceTypeConstructor # sequenceConstructor ;

callArgumentList: (expression (Comma expression)*)? ;

valueDeclaration : Val name=Identifier Assign initializer=expression ;

functionDeclaration : Def name=Identifier LeftParen argumentListDeclaration RightParen (Colon explicitReturnType=type)? LeftBrace body RightBrace ;

externalFunctionDeclaration : External Def name=Identifier LeftParen argumentListDeclaration RightParen Colon returnType=type;

argumentListDeclaration : (argumentDeclaration (Comma argumentDeclaration)*)? ;

argumentDeclaration: name=Identifier Colon type;

declaration : valueDeclaration | functionDeclaration | externalFunctionDeclaration ;

declarationList : declaration* ;

body : declarationList return_ ;

return_ : Return expression ;
