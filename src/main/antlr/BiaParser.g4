parser grammar BiaParser;

options { tokenVocab = BiaLexer; }

program : root=expression EOF ;

expression : left=expression PLUS right=expression # binaryOperation
           | INTLIT # intLiteral ;
