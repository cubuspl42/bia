parser grammar BiaParser;

options { tokenVocab = BiaLexer; }

program : body EOF ;

expression : left=expression operator=Multiplication right=expression # binaryOperation
           | left=expression operator=Plus right=expression # binaryOperation
           | left=expression operator=Minus right=expression # binaryOperation
           | left=expression operator=Equals right=expression # equalsOperation
           | LeftParen expression RightParen # parenExpression
           | If guard=expression Then trueBranch=expression Else falseBranch=expression # ifExpression
           | callee=expression LeftParen argument=expression RightParen # callExpression
           | IntLiteral # intLiteral
           | Identifier # reference ;

valueDeclaration : Val name=Identifier Assign initializer=expression ;

functionDeclaration : Def name=Identifier LeftParen argument=Identifier RightParen LeftBrace body RightBrace ;

declaration : valueDeclaration | functionDeclaration ;

body : declaration* return_ ;

return_ : Return expression ;
