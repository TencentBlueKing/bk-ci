package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线批量任务明细错误信息-有错误码异常")
data class PipelineBatchTaskFailedErrorCode(
    val errorCode: String,
    val params: List<String>? = null
) : PipelineBatchTaskErrorMessage {
    companion object {
        const val classType = "errorCode"
    }
}
