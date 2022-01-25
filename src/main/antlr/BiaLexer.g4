lexer grammar BiaLexer;

// Keywords
If                      : 'if' ;
Then                    : 'then' ;
Else                    : 'else' ;
Val                     : 'val' ;
Return                  : 'return' ;
Def                     : 'def' ;
Or                      : 'or' ;

// Literals
IntLiteral              : '0'|[1-9][0-9]* ;

// Operators
Plus                    : '+' ;
Minus                   : '-' ;
Multiplication          : '*' ;
Reminder                : '%' ;
Equals                  : '==' ;
Assign                  : '=' ;

// Other
Whitespace              : (' ' | '\n') -> skip ;
LineComment             : '//' ~[\r\n]* -> skip ;
Identifier              : [a-z]+ ;
LeftParen               : '(' ;
RightParen              : ')' ;
LeftBrace               : '{' ;
RightBrace              : '}' ;
Comma                   : ',' ;
