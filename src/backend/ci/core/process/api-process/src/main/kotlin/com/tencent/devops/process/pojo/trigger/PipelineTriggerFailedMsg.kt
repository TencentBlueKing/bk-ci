package com.tencent.devops.process.pojo.trigger

import io.swagger.annotations.ApiModel

@ApiModel("流水线触发事件原因详情-不需要转换的错误信息")
data class PipelineTriggerFailedMsg(
    private val msg: String
) : PipelineTriggerReasonDetail {
    companion object {
        const val classType = "reason"
    }

    override fun getReasonDetailList(): List<String> {
        return listOf(msg)
    }
}
