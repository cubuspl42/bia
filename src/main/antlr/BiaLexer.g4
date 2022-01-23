lexer grammar BiaLexer;

// Keywords
If                      : 'if' ;
Then                    : 'then' ;
Else                    : 'else' ;
Val                     : 'val' ;
Return                  : 'return' ;

// Literals
IntLiteral              : '0'|[1-9][0-9]* ;

// Operators
Plus                    : '+' ;
Multiplication          : '*' ;
Equals                  : '==' ;
Assign                  : '=' ;

// Other
Whitespace              : (' ' | '\n') -> skip ;
Identifier              : [a-z]+ ;
LeftParen               : '(' ;
RightParen              : ')' ;
