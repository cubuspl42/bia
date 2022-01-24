parser grammar BiaParser;

options { tokenVocab = BiaLexer; }

program : body EOF ;

expression : left=expression operator=Multiplication right=expression # binaryOperation
           | left=expression operator=Plus right=expression # binaryOperation
           | left=expression operator=Equals right=expression # equalsOperation
           | LeftParen expression RightParen # parenExpression
           | If guard=expression Then ifTrue=expression Else ifFalse=expression # ifExpression
           | callee=expression LeftParen argument=expression RightParen # callExpression
           | IntLiteral # intLiteral
           | Identifier # reference ;

valueDeclaration : Val identifier=Identifier Assign expression ;

functionDeclaration : Def name=Identifier LeftParen argument=Identifier RightParen LeftBrace body RightBrace ;

declaration : valueDeclaration | functionDeclaration ;

body : declaration+ return_ ;

return_ : Return expression ;
