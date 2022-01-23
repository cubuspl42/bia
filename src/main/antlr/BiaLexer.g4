lexer grammar BiaLexer;

// Literals
IntLiteral              : '0'|[1-9][0-9]* ;

// Operators
Plus                    : '+' ;
Multiplication          : '*' ;
Equals                  : '==' ;

// Other
Whitespace              : ' ' -> skip ;
LeftParen               : '(' ;
RightParen              : ')' ;
