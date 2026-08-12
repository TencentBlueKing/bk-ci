package com.tencent.devops.process.pojo.trigger

import com.tencent.devops.common.web.utils.I18nUtil
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线触发事件原因详情-流水线转换失败明细")
data class PipelineTransferFailed(
    @get:Schema(title = "外层错误码（如 YAML_NOT_VALID）")
    val errorCode: String,
    @get:Schema(title = "外层错误码参数")
    val params: List<String>? = null,
    @get:Schema(title = "逐条校验失败明细")
    val details: List<PipelineTriggerValidateDetail>? = null,
    @get:Schema(title = "标题与首条明细的拼接符号")
    val titleSeparator: String = ":"
) : PipelineTriggerReasonDetail {

    companion object {
        const val classType = "pipelineTransfer"
    }

    /**
     * 渲染规则：
     * - 无 [details]：仅返回外层错误码渲染后的一行文本。
     * - 有 [details]：外层错误码作为标题与"首条明细"拼合作为首行，其余明细独立成行。
     *   有 [details] 时忽略 [params]，避免与展开的 details 内容重复。
     */
    override fun getReasonDetailList(): List<String> {
        if (details.isNullOrEmpty()) {
            return listOf(
                I18nUtil.getCodeLanMessage(
                    messageCode = errorCode,
                    params = params?.toTypedArray()
                )
            )
        }
        val header = I18nUtil.getCodeLanMessage(
            messageCode = errorCode,
            params = arrayOf("")
        ).trimEnd()
        val renderedDetails = details.map { detail ->
            I18nUtil.getCodeLanMessage(
                messageCode = detail.messageCode,
                params = detail.params?.toTypedArray()
            )
        }
        return buildList {
            add("$header$titleSeparator${renderedDetails.first()}")
            if (renderedDetails.size > 1) {
                addAll(renderedDetails.drop(1))
            }
        }
    }
}
