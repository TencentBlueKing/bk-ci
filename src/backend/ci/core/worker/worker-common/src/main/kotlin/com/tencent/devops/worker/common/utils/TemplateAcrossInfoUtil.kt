package com.tencent.devops.worker.common.utils

import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.worker.common.TEMPLATE_ACROSS_INFO_ID
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.process.BuildSDKApi

object TemplateAcrossInfoUtil {

    private val processApi = ApiFactory.create(BuildSDKApi::class)

    fun getAcrossInfo(
        variables: Map<String, String>,
        stepId: String?
    ): BuildTemplateAcrossInfo? {
        return if (variables[TEMPLATE_ACROSS_INFO_ID].isNullOrBlank() || stepId.isNullOrBlank()) {
            null
        } else {
            val result = processApi.getBuildAcrossTemplateInfo(
                templateId = variables[TEMPLATE_ACROSS_INFO_ID]!!
            )
            if (result.isNotOk() || result.data == null) {
                null
            } else {
                result.data?.filter {
                    it.templateType == TemplateAcrossInfoType.STEP &&
                        it.templateInstancesIds.contains(stepId)
                }?.ifEmpty { null }?.first()
            }
        }
    }
}
