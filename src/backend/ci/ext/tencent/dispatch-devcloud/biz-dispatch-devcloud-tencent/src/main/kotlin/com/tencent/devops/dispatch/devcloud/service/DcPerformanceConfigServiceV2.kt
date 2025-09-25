package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.client.DevCloudPerformanceClient
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.PerformanceData
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsV2
import org.springframework.stereotype.Service

@Service
class DcPerformanceConfigServiceV2 constructor(
    private val devCloudPerformanceClient: DevCloudPerformanceClient
) {

    fun getDcPerformanceConfigList(userId: String, projectId: String, pipelineId: String): UserPerformanceOptionsV2 {
        val performanceDataList = devCloudPerformanceClient.getPerformanceList(userId, projectId, pipelineId)
        if (performanceDataList.isEmpty()) {
            return UserPerformanceOptionsV2(DEFAULT_CONFIG_UID, listOf(PerformanceData(
                uid = DEFAULT_CONFIG_UID,
                name = DEFAULT_CONFIG_NAME,
                desc = DEFAULT_CONFIG_DESC
            )))
        }

        return UserPerformanceOptionsV2(performanceDataList.first().uid, performanceDataList)
    }

    companion object {
        private const val DEFAULT_CONFIG_UID = "Standard-S"
        private const val DEFAULT_CONFIG_NAME = "标准型（16核/32G/100G），适用于小型项目编译场景"
        private const val DEFAULT_CONFIG_DESC = "标准型（16核/32G/100G），适用于小型项目编译场景"
    }
}
