lexer grammar BiaLexer;

// Keywords
If                      : 'if' ;
Then                    : 'then' ;
Else                    : 'else' ;

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
