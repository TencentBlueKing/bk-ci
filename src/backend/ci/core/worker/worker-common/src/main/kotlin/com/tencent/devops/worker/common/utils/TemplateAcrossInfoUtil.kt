package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.constant.TEMPLATE_ACROSS_INFO_ID
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.process.BuildSDKApi
import org.slf4j.LoggerFactory

object TemplateAcrossInfoUtil {

    private val logger = LoggerFactory.getLogger(TemplateAcrossInfoUtil::class.java)

    private val processApi = ApiFactory.create(BuildSDKApi::class)

    fun getAcrossInfo(
        variables: Map<String, String>,
        taskId: String?
    ): BuildTemplateAcrossInfo? {
        val tid = variables[TEMPLATE_ACROSS_INFO_ID]
        logger.info("getAcrossInfo tid: $tid taskId: $taskId")

        return if (tid.isNullOrBlank() || taskId.isNullOrBlank()) {
            null
        } else {
            val result = processApi.getBuildAcrossTemplateInfo(
                templateId = tid
            )
            if (result.isNotOk() || result.data == null) {
                null
            } else {
                result.data?.firstOrNull {
                    it.templateType == TemplateAcrossInfoType.STEP &&
                        it.templateInstancesIds.contains(taskId)
                }
            }
        }
    }
}
