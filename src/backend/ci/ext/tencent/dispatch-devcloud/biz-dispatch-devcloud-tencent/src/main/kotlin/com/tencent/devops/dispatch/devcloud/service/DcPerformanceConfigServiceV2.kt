package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.client.DevCloudPerformanceClient
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.PerformanceData
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsV2
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DcPerformanceConfigServiceV2 constructor(
    private val devCloudPerformanceClient: DevCloudPerformanceClient
) {

    fun getDcPerformanceConfigList(
        userId: String,
        projectId: String,
        pipelineId: String,
        templateId: String
    ): UserPerformanceOptionsV2 {
        val performanceDataList = devCloudPerformanceClient.getPerformanceList(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            templateId = templateId
        )
        if (performanceDataList.isEmpty()) {
            return UserPerformanceOptionsV2(DEFAULT_CONFIG_UID, listOf(PerformanceData(
                uid = DEFAULT_CONFIG_UID,
                name = DEFAULT_CONFIG_NAME,
                desc = DEFAULT_CONFIG_DESC
            )))
        }

        return UserPerformanceOptionsV2(performanceDataList.first().uid, performanceDataList)
    }

    fun getDcPerformanceConfigInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        performanceUid: String
    ): PerformanceData {
        val performanceData = devCloudPerformanceClient.getPerformanceInfo(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            performanceUid = performanceUid
        )

        if (performanceData == null || performanceData.name.isBlank() || performanceData.uid.isBlank()) {
            logger.warn("获取性能配置失败，使用默认配置。userId=$userId, projectId=$projectId, pipelineId=$pipelineId, " +
                    "performanceUid=$performanceUid，原因：performanceData为空或name｜uid为空白")
            return PerformanceData(
                uid = DEFAULT_CONFIG_UID,
                name = DEFAULT_CONFIG_NAME,
                desc = DEFAULT_CONFIG_DESC
            )
        }

        return performanceData
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DcPerformanceConfigServiceV2::class.java)
        private const val DEFAULT_CONFIG_UID = "Standard-S"
        private const val DEFAULT_CONFIG_NAME = "标准型（16核/32G/100G），适用于小型项目编译场景"
        private const val DEFAULT_CONFIG_DESC = "标准型（16核/32G/100G），适用于小型项目编译场景"
    }
}
