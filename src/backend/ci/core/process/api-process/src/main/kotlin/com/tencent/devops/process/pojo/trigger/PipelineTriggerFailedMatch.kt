package com.tencent.devops.process.pojo.trigger

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.utils.I18nUtil.getCodeLanMessage
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线触发事件原因详情-触发匹配失败")
data class PipelineTriggerFailedMatch(
    @get:Schema(title = "匹配失败的插件")
    val elements: List<PipelineTriggerFailedMatchElement>
) : PipelineTriggerReasonDetail {
    companion object {
        const val classType = "match"
    }

    override fun getReasonDetailList(): List<String> {
        return elements.filter { it.reasonMsg.isNotBlank() }.map {
            val i18nReason = JsonUtil.to(it.reasonMsg, I18Variable::class.java).getCodeLanMessage()
            "${it.elementName} | $i18nReason"
        }
    }
}

@Schema(title = "流水线触发匹配失败")
data class PipelineTriggerFailedMatchElement(
    @get:Schema(title = "触发插件ID")
    val elementId: String?,
    @get:Schema(title = "触发插件Code")
    val elementAtomCode: String,
    @get:Schema(title = "触发插件名称")
    val elementName: String,
    @get:Schema(title = "触发原因，JSON字符串，便于国际化")
    val reasonMsg: String
)
