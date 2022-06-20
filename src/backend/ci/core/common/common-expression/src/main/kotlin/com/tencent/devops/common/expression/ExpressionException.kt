package com.tencent.devops.common.expression

import com.tencent.devops.common.expression.expression.ExpressionConstants
import com.tencent.devops.common.expression.expression.ParseExceptionKind
import com.tencent.devops.common.expression.expression.tokens.Token

abstract class ExpressionException(message: String = "") : RuntimeException(message)

class NotSupportedException(override val message: String) : ExpressionException()

class ParseException(
    kind: ParseExceptionKind,
    token: Token?,
    expression: String,
    override var message: String = ""
) : ExpressionException() {
    init {
        val desc = when (kind) {
            ParseExceptionKind.ExceededMaxDepth -> "Exceeded max expression depth ${ExpressionConstants.MAX_DEEP}"
            ParseExceptionKind.ExceededMaxLength -> "Exceeded max expression length ${ExpressionConstants.MAX_LENGTH}"
            ParseExceptionKind.TooFewParameters -> "Too few parameters supplied"
            ParseExceptionKind.TooManyParameters -> "Too many parameters supplied"
            ParseExceptionKind.UnexpectedEndOfExpression -> "Unexpected end of expression"
            ParseExceptionKind.UnexpectedSymbol -> "Unexpected symbol"
            ParseExceptionKind.UnrecognizedFunction -> "Unrecognized function"
            ParseExceptionKind.UnrecognizedNamedValue -> "Unrecognized named-value"
        }

        message = if (token == null) {
            desc
        } else {
            "$desc: '${token.rawValue}'. Located at position ${token.index + 1} within expression:$expression"
        }
    }
}

class InvalidOperationException(override val message: String) : ExpressionException()
