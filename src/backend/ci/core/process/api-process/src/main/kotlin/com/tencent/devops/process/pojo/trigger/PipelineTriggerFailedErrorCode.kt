package com.tencent.devops.process.pojo.trigger

import com.tencent.devops.common.web.utils.I18nUtil
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线触发事件原因详情-有错误码异常")
data class PipelineTriggerFailedErrorCode(
    val errorCode: String,
    val params: List<String>? = null
) : PipelineTriggerReasonDetail {
    companion object {
        const val classType = "errorCode"
    }

    override fun getReasonDetailList(): List<String> {
        return listOf(
            I18nUtil.getCodeLanMessage(
                messageCode = errorCode,
                params = params?.toTypedArray()
            )
        )
    }
}
