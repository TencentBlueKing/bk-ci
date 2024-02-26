package com.tencent.devops.process.pojo.trigger

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.web.utils.I18nUtil.getCodeLanMessage
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线触发事件原因详情-兼容历史数据")
class PipelineTriggerFailedFix(
    private val reasonDetailList: List<String>?
) : PipelineTriggerReasonDetail {

    companion object {
        const val classType = "fix"
    }

    /**
     * reasonDetail资格字段设计的时候是一个数组类型,存在两种格式
     *  1. [ "流水线: 流水线队列满" ]
     *  2. [
     *     "{"elementId" : "","elementAtomCode" : "","elementName" : "","reasonMsg" : "{\"code\":\"\",\"params\":[]}"}"
     *     ]
     *  在做pac的时候发现需要兼容更多的字段，所以修改成PipelineTriggerReasonDetail对象,但是需要对历史数据做兼容
     */
    override fun getReasonDetailList(): List<String>? {
        val objectMapper = JsonUtil.getObjectMapper()
        return when {
            reasonDetailList.isNullOrEmpty() -> null
            else -> try {
                reasonDetailList.map {
                    val jsonNode = objectMapper.readTree(it)
                    if (jsonNode.isObject) {
                        val reasonDetail = JsonUtil.to(it, PipelineTriggerFailedMatchElement::class.java)
                        // 国际化触发失败原因
                        val i18nReason = JsonUtil.to(
                            json = reasonDetail.reasonMsg,
                            typeReference = object : TypeReference<I18Variable>() {}
                        ).getCodeLanMessage()
                        // 详情格式： {{触发器名称}}|{{国际化后的触发失败原因}}
                        "${reasonDetail.elementName} | $i18nReason"
                    } else {
                        it
                    }
                }
            } catch (ignored: Exception) {
                reasonDetailList
            }
        }
    }
}
