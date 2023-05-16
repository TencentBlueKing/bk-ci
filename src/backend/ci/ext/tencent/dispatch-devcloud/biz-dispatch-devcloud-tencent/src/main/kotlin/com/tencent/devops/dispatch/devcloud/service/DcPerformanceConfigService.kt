package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.dao.DevcloudPerformanceConfigDao
import com.tencent.devops.dispatch.devcloud.pojo.performance.ListPage
import com.tencent.devops.dispatch.devcloud.pojo.performance.OPPerformanceConfigVO
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceConfigVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DcPerformanceConfigService constructor(
    private val dslContext: DSLContext,
    private val devcloudPerformanceConfigDao: DevcloudPerformanceConfigDao
) {
    private val logger = LoggerFactory.getLogger(DcPerformanceConfigService::class.java)

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

    fun createDcPerformanceConfig(userId: String, opPerformanceConfigVO: OPPerformanceConfigVO): Boolean {
        logger.info("$userId create opPerformanceConfigVO: $opPerformanceConfigVO")
        checkParameter(userId, opPerformanceConfigVO.projectId)

        try {
            devcloudPerformanceConfigDao.createOrUpdate(
                dslContext = dslContext,
                projectId = opPerformanceConfigVO.projectId,
                optionId = opPerformanceConfigVO.optionId
            )
        } catch (e: Exception) {
            logger.error("$userId add performanceConfig error.", e)
            throw RuntimeException("add performanceConfig error.")
        }

        return true
    }

    fun updateDcPerformanceConfig(
        userId: String,
        projectId: String,
        opPerformanceConfigVO: OPPerformanceConfigVO
    ): Boolean {
        logger.info("$userId update performanceConfig: $opPerformanceConfigVO")
        checkParameter(userId, projectId)

        try {
            devcloudPerformanceConfigDao.createOrUpdate(
                dslContext = dslContext,
                projectId = projectId,
                optionId = opPerformanceConfigVO.optionId
            )

            return true
        } catch (e: Exception) {
            logger.error("$userId update performanceConfig error.", e)
            throw RuntimeException("update performanceConfig error")
        }
    }

    fun deleteDcPerformanceConfig(userId: String, projectId: String): Boolean {
        logger.info("$userId delete performanceConfig projectId: $projectId")
        checkParameter(userId, projectId)
        val result = devcloudPerformanceConfigDao.delete(dslContext, projectId)
        return result == 1
    }

    private fun checkParameter(userId: String, projectId: String) {
        if (projectId.isEmpty()) {
            logger.error("$userId Add failed, projectId is null or ''.")
            throw RuntimeException("Add failed, projectId is null or ''.")
        }
    }
}
