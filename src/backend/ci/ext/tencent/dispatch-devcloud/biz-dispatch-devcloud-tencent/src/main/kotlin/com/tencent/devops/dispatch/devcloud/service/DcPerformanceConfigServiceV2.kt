package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.client.DevCloudPerformanceClient
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
        return UserPerformanceOptionsV2(performanceDataList.first().uid, performanceDataList)
    }
}
