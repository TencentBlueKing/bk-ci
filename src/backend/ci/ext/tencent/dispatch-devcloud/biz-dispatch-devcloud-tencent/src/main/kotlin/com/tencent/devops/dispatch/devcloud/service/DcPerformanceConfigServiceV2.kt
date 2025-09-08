package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.client.DevCloudPerformanceClient
import com.tencent.devops.dispatch.devcloud.dao.DcPerformanceOptionsDao
import com.tencent.devops.dispatch.devcloud.dao.DevcloudPerformanceConfigDao
import com.tencent.devops.dispatch.devcloud.pojo.performance.*
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class DcPerformanceConfigServiceV2 constructor(
    private val dslContext: DSLContext,
    private val devcloudPerformanceConfigDao: DevcloudPerformanceConfigDao,
    private val devCloudPerformanceClient: DevCloudPerformanceClient
) {
    private val logger = LoggerFactory.getLogger(DcPerformanceConfigServiceV2::class.java)

    @Value("\${devCloud.cpu}")
    var cpu: Int = 32

    @Value("\${devCloud.memory}")
    var memory: String = "65535M"

    @Value("\${devCloud.disk}")
    var disk: String = "500G"

    fun listDcPerformanceConfig(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): ListPage<PerformanceConfigVO> {
        logger.info("$userId list performanceConfigList.")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        try {
            val list = devcloudPerformanceConfigDao.getList(dslContext, pageNotNull, pageSizeNotNull)
            val count = devcloudPerformanceConfigDao.getCount(dslContext)

            if (list == null || list.size == 0 || count == 0L) {
                return ListPage(pageNotNull, pageSizeNotNull, count, emptyList())
            }

            val performanceConfigVOList = mutableListOf<PerformanceConfigVO>()
            list.forEach {
                performanceConfigVOList.add(
                    PerformanceConfigVO(
                        projectId = it["PROJECT_ID"] as String,
                        cpu = it["CPU"] as Int,
                        memory = (it["MEMORY"] as Int).toString() + "M",
                        disk = (it["DISK"] as Int).toString() + "G",
                        description = it["DESCRIPTION"] as String
                    )
                )
            }

            return ListPage(pageNotNull, pageSizeNotNull, count, performanceConfigVOList)
        } catch (e: Exception) {
            logger.error("$userId list performanceConfigList error.", e)
            throw RuntimeException("list performanceConfigList error.")
        }
    }

    fun getDcPerformanceConfigList(userId: String, projectId: String, pipelineId: String): UserPerformanceOptionsV2 {
        val performanceDataList = devCloudPerformanceClient.getPerformanceList(userId, projectId, pipelineId)
        val performanceMaps = performanceDataList.stream().collect(Collectors.toMap({ it.uid }, { it }))

        return UserPerformanceOptionsV2(performanceDataList.first().uid, performanceMaps)
    }

    private fun checkParameter(userId: String, projectId: String) {
        if (projectId.isEmpty()) {
            logger.error("$userId Add failed, projectId is null or ''.")
            throw RuntimeException("Add failed, projectId is null or ''.")
        }
    }
}
