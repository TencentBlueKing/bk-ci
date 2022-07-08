package com.tencent.devops.common.expression.expression

enum class ParseExceptionKind {
    ExceededMaxDepth,
    ExceededMaxLength,
    TooFewParameters,
    TooManyParameters,
    UnexpectedEndOfExpression,
    UnexpectedSymbol,
    UnrecognizedFunction,
    UnrecognizedNamedValue
}
