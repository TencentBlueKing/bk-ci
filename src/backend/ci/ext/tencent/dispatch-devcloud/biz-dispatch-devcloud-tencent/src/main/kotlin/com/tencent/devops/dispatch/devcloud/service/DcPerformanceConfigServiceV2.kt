package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.client.DevCloudPerformanceClient
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.PerformanceData
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsV2
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DcPerformanceConfigServiceV2 constructor(
    private val devCloudPerformanceClient: DevCloudPerformanceClient
) {
    private val logger = LoggerFactory.getLogger(DcPerformanceConfigServiceV2::class.java)

    @Value("\${devCloud.cpu}")
    var cpu: Int = 32

    @Value("\${devCloud.memory}")
    var memory: String = "65535M"

    @Value("\${devCloud.disk}")
    var disk: String = "500G"

    fun getDcPerformanceConfigList(userId: String, projectId: String, pipelineId: String): UserPerformanceOptionsV2 {
        val performanceDataList = devCloudPerformanceClient.getPerformanceList(userId, projectId, pipelineId)
        if (performanceDataList.isEmpty()) {
            return UserPerformanceOptionsV2("Standard-S", listOf(PerformanceData(
                uid = "Standard-S",
                name = "标准型（16核/32G/100G），适用于小型项目编译场景",
                desc = "标准型（16核/32G/100G），适用于小型项目编译场景"
            )))
        }

        return UserPerformanceOptionsV2(performanceDataList.first().uid, performanceDataList)
    }
}
