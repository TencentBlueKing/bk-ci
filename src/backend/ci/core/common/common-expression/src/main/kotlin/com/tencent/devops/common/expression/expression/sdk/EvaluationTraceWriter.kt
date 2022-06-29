package com.tencent.devops.common.expression.expression.sdk

import com.tencent.devops.common.expression.expression.ITraceWriter

class EvaluationTraceWriter(
    val mTrace: ITraceWriter?
) : ITraceWriter {
    override fun info(message: String?) {
        mTrace?.info(message)
    }

    override fun verbose(message: String?) {
        mTrace?.verbose(message)
    }
}
