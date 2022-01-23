parser grammar BiaParser;

options { tokenVocab = BiaLexer; }

program : root=expression EOF ;

expression : left=expression operator=Multiplication right=expression # binaryOperation
           | left=expression operator=Plus right=expression # binaryOperation
           | left=expression operator=Equals right=expression # equalsOperation
           | LeftParen expression RightParen # parenExpression
           | If guard=expression Then ifTrue=expression Else ifFalse=expression # ifExpression
           | IntLiteral # intLiteral ;
