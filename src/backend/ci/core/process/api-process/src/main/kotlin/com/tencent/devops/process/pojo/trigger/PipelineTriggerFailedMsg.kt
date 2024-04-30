package com.tencent.devops.process.pojo.trigger

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线触发事件原因详情-不需要转换的错误信息")
data class PipelineTriggerFailedMsg(
    val msg: String
) : PipelineTriggerReasonDetail {
    companion object {
        const val classType = "msg"
    }

    override fun getReasonDetailList(): List<String> {
        return listOf(msg)
    }
}
