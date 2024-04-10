package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.dao.DcPerformanceOptionsDao
import com.tencent.devops.dispatch.devcloud.dao.DevcloudPerformanceConfigDao
import com.tencent.devops.dispatch.devcloud.pojo.performance.ListPage
import com.tencent.devops.dispatch.devcloud.pojo.performance.OPPerformanceConfigVO
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceConfigVO
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceMap
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DcPerformanceConfigService constructor(
    private val dslContext: DSLContext,
    private val devcloudPerformanceConfigDao: DevcloudPerformanceConfigDao,
    private val dcPerformanceOptionsDao: DcPerformanceOptionsDao
) {
    private val logger = LoggerFactory.getLogger(DcPerformanceConfigService::class.java)

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

    fun getDcPerformanceConfigList(userId: String, projectId: String): UserPerformanceOptionsVO {
        val projectPerformance = devcloudPerformanceConfigDao.getByProjectId(dslContext, projectId)

        val memoryG = (memory.dropLast(1).toInt() / 1024).toString() + "G"
        var default = "0"
        val performanceMaps = mutableListOf<PerformanceMap>()
        if (projectPerformance != null) {
            val cpuInt = projectPerformance["CPU"] as Int
            val memoryInt = projectPerformance["MEMORY"] as Int
            val diskInt = projectPerformance["DISK"] as Int

            val optionList = dcPerformanceOptionsDao.getOptionsList(dslContext, cpuInt, memoryInt, diskInt)
            if (optionList.size == 0) {
                performanceMaps.add(
                    PerformanceMap(
                        id = "0",
                        performanceConfigVO = PerformanceConfigVO(
                            projectId = projectId,
                            cpu = cpu,
                            memory = memoryG,
                            disk = disk,
                            description = "Basic"
                        )
                    )
                )

                return UserPerformanceOptionsVO(default, true, performanceMaps)
            }

            optionList.forEach {
                if (it.memory == memory.dropLast(1).toInt()) {
                    default = it.id.toString()
                }
                performanceMaps.add(
                    PerformanceMap(
                        id = it.id.toString(),
                        performanceConfigVO = PerformanceConfigVO(
                            projectId = projectId,
                            cpu = it.cpu,
                            memory = "${it.memory / 1024}G",
                            disk = "${it.disk}G",
                            description = it.description
                        )
                    )
                )
            }

            // 若没有application默认的配置，默认第一个
            if (default == "0") {
                default = optionList[0].id.toString()
            }
        } else {
            performanceMaps.add(
                PerformanceMap(
                    id = "0",
                    performanceConfigVO = PerformanceConfigVO(
                        projectId = projectId,
                        cpu = cpu,
                        memory = memoryG,
                        disk = disk,
                        description = "Basic"
                    )
                )
            )
        }

        return UserPerformanceOptionsVO(default, true, performanceMaps)
    }

    private fun checkParameter(userId: String, projectId: String) {
        if (projectId.isEmpty()) {
            logger.error("$userId Add failed, projectId is null or ''.")
            throw RuntimeException("Add failed, projectId is null or ''.")
        }
    }
}
