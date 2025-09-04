package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.dao.DcPerformanceOptionsDao
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceOptionsVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DcPerformanceOptionsService constructor(
    private val dslContext: DSLContext,
    private val dcPerformanceOptionsDao: DcPerformanceOptionsDao
) {
    private val logger = LoggerFactory.getLogger(DcPerformanceOptionsService::class.java)

    fun listDcPerformanceConfig(
        userId: String
    ): List<PerformanceOptionsVO> {
        logger.info("$userId list listDcPerformanceConfig.")
        try {
            val list = dcPerformanceOptionsDao.getList(dslContext)

            if (list.size == 0) {
                return emptyList()
            }

            val performanceOptionsVOList = mutableListOf<PerformanceOptionsVO>()
            list.forEach {
                performanceOptionsVOList.add(
                    PerformanceOptionsVO(
                        cpu = it.cpu,
                        memory = it.memory,
                        disk = it.disk,
                        description = it.description
                    )
                )
            }

            return performanceOptionsVOList
        } catch (e: Exception) {
            logger.error("$userId list listDcPerformanceConfig error.", e)
            throw RuntimeException("list listDcPerformanceConfig error.")
        }
    }

    fun createDcPerformanceOptions(userId: String, performanceOptionsVO: PerformanceOptionsVO): Boolean {
        logger.info("$userId create performanceOptionsVO: $performanceOptionsVO")

        try {
            dcPerformanceOptionsDao.create(
                dslContext = dslContext,
                cpu = performanceOptionsVO.cpu,
                memory = performanceOptionsVO.memory,
                disk = performanceOptionsVO.disk,
                description = performanceOptionsVO.description
            )
        } catch (e: Exception) {
            logger.error("$userId add performanceOptionsVO error.", e)
            throw RuntimeException("add performanceOptionsVO error.")
        }

        return true
    }

    fun updateDcPerformanceOptions(userId: String, id: Long, performanceOptionsVO: PerformanceOptionsVO): Boolean {
        logger.info("$userId update performanceOptionsVO: $performanceOptionsVO")

        try {
            dcPerformanceOptionsDao.update(dslContext, id, performanceOptionsVO)
        } catch (e: Exception) {
            logger.error("$userId update performanceOptionsVO error.", e)
            throw RuntimeException("update performanceOptionsVO error.")
        }

        return true
    }

    fun deleteDcPerformanceOptions(userId: String, id: Long): Boolean {
        logger.info("$userId delete performanceOptions id: $id")
        checkParameter(userId, id.toString())
        val result = dcPerformanceOptionsDao.delete(dslContext, id)
        return result == 1
    }

    private fun checkParameter(userId: String, projectId: String) {
        if (projectId.isEmpty()) {
            logger.error("$userId Add failed, projectId is null or ''.")
            throw RuntimeException("Add failed, projectId is null or ''.")
        }
    }
}
