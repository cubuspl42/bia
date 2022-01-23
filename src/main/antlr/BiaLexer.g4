lexer grammar BiaLexer;

// Literals
INTLIT              : '0'|[1-9][0-9]* ;

// Operators
PLUS                : '+' ;

// Other
WHITESPACE          : ' ' -> skip ;
