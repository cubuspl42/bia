parser grammar BiaParser;

options { tokenVocab = BiaLexer; }

program : root=expression EOF ;

expression : left=expression operator=Multiplication right=expression # binaryOperation
           | left=expression operator=Plus right=expression # binaryOperation
           | LeftParen expression RightParen # parenExpression
           | IntLiteral # intLiteral ;
