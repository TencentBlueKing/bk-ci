package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线批量任务明细错误信息-不需要转换的错误信息")
data class PipelineBatchTaskFailedMsg(
    val msg: String
) : PipelineBatchTaskErrorMessage {
    companion object {
        const val classType = "msg"
    }
}
