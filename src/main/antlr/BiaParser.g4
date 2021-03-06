parser grammar BiaParser;

options { tokenVocab = BiaLexer; }

program : topLevelDeclaration* EOF ;

topLevelDeclaration
    : typeAlias # typealiasAlt
    | unionDeclaration # unionDeclarationAlt
    | declaration # declarationAlt
    | singletonDeclaration # singletonDeclarationAlt
    ;

typeExpression
    : NumberType # numberType
    | BooleanType # booleanType
    | BigIntegerType # bigIntegerType
    | genericArgumentListDeclaration? argumentListDeclaration (Colon | ThinArrow) returnType=typeExpression # functionType
    | typeConstructor Lt typeExpression Gt # constructedType
    | typeExpression QuestionMark  # nullableType
    | typeReference # typeReferenceAlt
    | genericArgumentListDeclaration? LeftBrace objectTypeEntryDeclaration (Comma objectTypeEntryDeclaration)* RightBrace # objectType
    ;

typeReference : name=Identifier typeExpressionList? ;

objectTypeEntryDeclaration
    : fieldName=Identifier Colon fieldType=typeExpression ;

typeExpressionList: Lt typeExpression (Comma typeExpression)* Gt ;

expression
    : callableExpression # callableExpressionAlt
    | left=expression operator=Multiplication right=expression # binaryOperation
    | left=expression operator=Division right=expression # binaryOperation
    | left=expression operator=IntegerDivision right=expression # binaryOperation
    | left=expression operator=Reminder right=expression # binaryOperation
    | left=expression operator=Plus right=expression # binaryOperation
    | left=expression operator=Minus right=expression # binaryOperation
    | left=expression operator=Or right=expression # binaryOperation
    | left=expression operator=And right=expression # binaryOperation
    | left=expression operator=Lt right=expression # binaryOperation
    | left=expression operator=Gt right=expression # binaryOperation
    | left=expression operator=Equals right=expression # equalsOperation
    | operator=Not expression # unaryOperation
    | IntLiteral # intLiteral
    | TrueLiteral # trueLiteral
    | FalseLiteral # falseLiteral
    | objectLiteral # objectLiteralAlt
    | If guard=expression Then trueBranch=expression Else falseBranch=expression # ifExpression
    | genericArgumentListDeclaration? argumentListDeclaration (ThinArrow explicitReturnType=typeExpression)? FatArrow body=lambdaBody # lambdaExpression
    | expression Is tagName=Identifier # isExpression
    | expression Hash attachedTagName=Identifier # tagExpression
    | Untag expression # untagExpression
    ;

lambdaBody
    : expression # expressionAlt
    | block # blockAlt
    ;

callableExpression
    : LeftParen expression RightParen # parenExpression
    | callee=callableExpression typeExpressionList? LeftParen callArgumentList RightParen # callExpression
    | self=callableExpression Colon referredCalleeName=Identifier typeExpressionList? # postfixCallExpression
    | referredName=Identifier # referenceExpression
    | matchExpression # matchExpressionAlt
    | callableExpression Dot readFieldName=Identifier # objectFieldRead
    ;

objectLiteral
    : LeftBrace objectLiteralEntry (Comma objectLiteralEntry)* RightBrace ;

objectLiteralEntry
    : assignedFieldName=Identifier Assign initializer=expression ;

matchExpression
    : Match matchee=expression (ThinArrow explicitType=typeExpression)? LeftBrace matchTaggedBranch+ matchElseBranch? RightBrace ;

matchTaggedBranch
    : Case tagName=Identifier FatArrow branch=expression ;

matchElseBranch
    : Else FatArrow branch=expression ;

typeConstructor : ListTypeConstructor # listConstructor
                | SequenceTypeConstructor # sequenceConstructor ;

callArgumentList: (expression (Comma expression)*)? ;

valueDeclaration : Val name=Identifier Assign initializer=expression ;

functionDeclaration : External? Def genericArgumentListDeclaration? name=Identifier argumentListDeclaration (Colon explicitReturnType=typeExpression)? (LeftBrace blockBody RightBrace)? ;

genericArgumentListDeclaration : Lt (generitArgumentDeclaration (Comma generitArgumentDeclaration)*)? Gt ;

generitArgumentDeclaration : name=Identifier ;

argumentListDeclaration
    : LeftParen (argumentDeclaration (Comma argumentDeclaration)*)? RightParen # basicArgumentListDeclaration
    | LeftParen givenName=Identifier Colon typeExpression Ellipsis RightParen # varargArgumentListDeclaration
    ;

argumentDeclaration: name=Identifier Colon typeExpression;

typeAlias : Typealias aliasName=Identifier Assign aliasedType=typeExpression ;

declaration : valueDeclaration | functionDeclaration ;

declarationList : declaration* ;

singletonDeclaration : Singleton name=Identifier ;

block : LeftBrace blockBody RightBrace ;

blockBody : declarationList return_ ;

return_ : Return expression ;

unionDeclaration : Union givenName=Identifier Assign genericArgumentListDeclaration? unionEntryDeclaration (Pipe unionEntryDeclaration)* ;

unionEntryDeclaration : typeReference (Hash explicitTagName=Identifier)? ;
