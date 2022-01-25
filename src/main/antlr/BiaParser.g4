parser grammar BiaParser;

options { tokenVocab = BiaLexer; }

program : body EOF ;

expression : left=expression operator=Multiplication right=expression # binaryOperation
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
           | If guard=expression Then trueBranch=expression Else falseBranch=expression # ifExpression
           | callee=expression LeftParen callArgumentList RightParen # callExpression
           | IntLiteral # intLiteral
           | Identifier # reference ;

callArgumentList: (expression (Comma expression)*)? ;

valueDeclaration : Val name=Identifier Assign initializer=expression ;

functionDeclaration : Def name=Identifier LeftParen argumentListDeclaration RightParen LeftBrace body RightBrace ;

argumentListDeclaration : (argumentDeclaration (Comma argumentDeclaration)*)? ;

argumentDeclaration: name=Identifier ;

declaration : valueDeclaration | functionDeclaration ;

body : declaration* return_ ;

return_ : Return expression ;
