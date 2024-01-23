package com.tencent.devops.process.pojo.trigger

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.utils.I18nUtil.getCodeLanMessage
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线触发事件原因详情-触发匹配失败")
data class PipelineTriggerFailedMatch(
    @ApiModelProperty("匹配失败的插件")
    val elements: List<PipelineTriggerFailedMatchElement>
) : PipelineTriggerReasonDetail {
    companion object {
        const val classType = "match"
    }

    override fun getReasonDetailList(): List<String> {
        return elements.map {
            val i18nReason = JsonUtil.to(it.reasonMsg, I18Variable::class.java).getCodeLanMessage()
            "${it.elementName} | $i18nReason"
        }
    }
}

@ApiModel("流水线触发匹配失败")
data class PipelineTriggerFailedMatchElement(
    @ApiModelProperty("触发插件ID")
    val elementId: String?,
    @ApiModelProperty("触发插件Code")
    val elementAtomCode: String,
    @ApiModelProperty("触发插件名称")
    val elementName: String,
    @ApiModelProperty("触发原因，JSON字符串，便于国际化")
    val reasonMsg: String
)
