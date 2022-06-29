package com.tencent.devops.common.expression.expression

interface ITraceWriter {
    fun info(message: String?)
    fun verbose(message: String?)
}
