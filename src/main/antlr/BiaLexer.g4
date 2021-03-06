lexer grammar BiaLexer;

// Keywords
If                      : 'if' ;
Then                    : 'then' ;
Else                    : 'else' ;
Val                     : 'val' ;
Return                  : 'return' ;
Def                     : 'def' ;
Or                      : 'or' ;
And                     : 'and' ;
Not                     : 'not' ;
External                : 'external' ;
Typealias               : 'typealias' ;
Union                   : 'union' ;
Is                      : 'is' ;
Untag                   : 'untag' ;
Match                   : 'match' ;
Case                    : 'case' ;
Singleton               : 'singleton' ;

// Built-in types / type constructors
NumberType              : 'Number' ;
BooleanType             : 'Boolean' ;
BigIntegerType          : 'BigInteger' ;
ListTypeConstructor     : 'List' ;
SequenceTypeConstructor : 'Sequence' ;

// Literals
IntLiteral              : '0'|[1-9][0-9]* ;
TrueLiteral            : 'true' ;
FalseLiteral            : 'false' ;

// Arrows
ThinArrow               : '->' ;
FatArrow                : '=>' ;

// Operators
Plus                    : '+' ;
Minus                   : '-' ;
Multiplication          : '*' ;
Division                : '/' ;
IntegerDivision         : '~/' ;
Reminder                : '%' ;
Equals                  : '==' ;
Assign                  : '=' ;
Lt                      : '<' ;
Gt                      : '>' ;

// Other
Whitespace              : (' ' | '\n') -> skip ;
LineComment             : '//' ~[\r\n]* -> skip ;
Identifier              : [a-zA-Z] ([a-zA-Z1-9] | ':')* ;
LeftParen               : '(' ;
RightParen              : ')' ;
LeftBrace               : '{' ;
RightBrace              : '}' ;
Comma                   : ',' ;
Colon                   : ':' ;
QuestionMark            : '?' ;
Ellipsis                : '...' ;
Dot                     : '.' ;
Pipe                    : '|' ;
Hash                    : '#' ;
