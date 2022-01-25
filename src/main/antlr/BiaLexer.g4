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
Lt                      : '<' ;
Gt                      : '<' ;

// Other
Whitespace              : (' ' | '\n') -> skip ;
LineComment             : '//' ~[\r\n]* -> skip ;
Identifier              : ([a-zA-Z] | ':')+ ;
LeftParen               : '(' ;
RightParen              : ')' ;
LeftBrace               : '{' ;
RightBrace              : '}' ;
Comma                   : ',' ;
