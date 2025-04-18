package com.tencent.devops.process.pojo.trigger

import com.tencent.devops.common.web.utils.I18nUtil
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线触发事件原因详情-明细说明")
data class PipelineTriggerDetailMessageCode(
    val messageCode: String,
    val params: List<String>? = null
) : PipelineTriggerReasonDetail {
    companion object {
        const val classType = "messageCode"
    }

    override fun getReasonDetailList(): List<String> {
        return listOf(
            I18nUtil.getCodeLanMessage(
                messageCode = messageCode,
                params = params?.toTypedArray()
            )
        )
    }
}
