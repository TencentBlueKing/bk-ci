/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.misc.cron.process

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.misc.config.MiscBuildDataClearConfig
import com.tencent.devops.misc.pojo.project.ProjectDataClearConfig
import com.tencent.devops.misc.service.artifactory.ArtifactoryDataClearService
import com.tencent.devops.misc.service.dispatch.DispatchDataClearService
import com.tencent.devops.misc.service.plugin.PluginDataClearService
import com.tencent.devops.misc.service.process.ProcessDataClearService
import com.tencent.devops.misc.service.process.ProcessMiscService
import com.tencent.devops.misc.service.process.ProcessRelatedPlatformDataClearService
import com.tencent.devops.misc.service.project.ProjectDataClearConfigFactory
import com.tencent.devops.misc.service.project.ProjectDataClearConfigService
import com.tencent.devops.misc.service.project.ProjectMiscService
import com.tencent.devops.misc.service.quality.QualityDataClearService
import com.tencent.devops.misc.service.repository.RepositoryDataClearService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Component
@Suppress("ALL")
class PipelineBuildHistoryDataClearJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val miscBuildDataClearConfig: MiscBuildDataClearConfig,
    private val projectMiscService: ProjectMiscService,
    private val processMiscService: ProcessMiscService,
    private val processDataClearService: ProcessDataClearService,
    private val repositoryDataClearService: RepositoryDataClearService,
    private val dispatchDataClearService: DispatchDataClearService,
    private val pluginDataClearService: PluginDataClearService,
    private val qualityDataClearService: QualityDataClearService,
    private val artifactoryDataClearService: ArtifactoryDataClearService,
    private val processRelatedPlatformDataClearService: ProcessRelatedPlatformDataClearService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildHistoryDataClearJob::class.java)
        private const val LOCK_KEY = "pipelineBuildHistoryDataClear"
        private const val DEFAULT_PAGE_SIZE = 100
        private const val PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY =
            "pipeline:build:history:data:clear:project:id"
        private const val PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_KEY =
            "pipeline:build:history:data:clear:project:list"
        private const val PIPELINE_BUILD_HISTORY_DATA_CLEAR_THREAD_SET_KEY =
            "pipeline:build:history:data:clear:thread:set"
        private var executor: ThreadPoolExecutor? = null
    }

    @Value("\${process.deletedPipelineStoreDays:30}")
    private val deletedPipelineStoreDays: Long = 30 // 回收站已删除流水线保存天数

    @Value("\${process.clearBaseBuildData:false}")
    private val clearBaseBuildData: Boolean = false // 是否开启清理【被彻底删除的流水线】的基础构建流水数据（建议开启）

    @PostConstruct
    fun init() {
        logger.info("start init pipelineBuildHistoryDataClearJob")
        // 启动的时候删除redis中存储的清理线程集合，防止redis中的线程信息因为服务异常停了无法删除
        redisOperation.delete(PIPELINE_BUILD_HISTORY_DATA_CLEAR_THREAD_SET_KEY, true)
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 12000)
    fun pipelineBuildHistoryDataClear() {
        if (!miscBuildDataClearConfig.switch.toBoolean()) {
            // 如果清理构建历史数据开关关闭，则不清理
            return
        }
        logger.info("pipelineBuildHistoryDataClear start")
        if (executor == null) {
            // 创建带有边界队列的线程池，防止内存爆掉
            logger.info("pipelineBuildHistoryDataClear create executor")
            executor = ThreadPoolExecutor(
                miscBuildDataClearConfig.maxThreadHandleProjectNum,
                miscBuildDataClearConfig.maxThreadHandleProjectNum,
                0L,
                TimeUnit.MILLISECONDS,
                LinkedBlockingQueue(10),
                Executors.defaultThreadFactory(),
                ThreadPoolExecutor.DiscardPolicy()
            )
        }
        val lock = RedisLock(
            redisOperation,
            LOCK_KEY, 3000
        )
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip")
                return
            }
            // 查询project表中的项目数据处理
            val projectIdListConfig = redisOperation.get(
                key = PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_KEY,
                isDistinguishCluster = true)
            // 组装查询项目的条件
            var projectIdList: List<String>? = null
            if (!projectIdListConfig.isNullOrBlank()) {
                projectIdList = projectIdListConfig.split(",")
            }
            val maxProjectNum = if (!projectIdList.isNullOrEmpty()) {
                projectIdList.size.toLong()
            } else {
                projectMiscService.getMaxId(projectIdList) ?: 0L
            }
            // 获取清理项目构建数据的线程数量
            val maxThreadHandleProjectNum = miscBuildDataClearConfig.maxThreadHandleProjectNum
            val avgProjectNum = maxProjectNum / maxThreadHandleProjectNum
            for (index in 1..maxThreadHandleProjectNum) {
                // 计算线程能处理的最大项目主键ID
                val maxThreadProjectPrimaryId = if (index != maxThreadHandleProjectNum) {
                    index * avgProjectNum
                } else {
                    index * avgProjectNum + maxProjectNum % maxThreadHandleProjectNum
                }
                // 判断线程是否正在处理任务，如正在处理则不分配新任务(定时任务12秒执行一次，线程启动到往set集合设置编号耗费时间很短，故不加锁)
                if (!redisOperation.isMember(
                        key = PIPELINE_BUILD_HISTORY_DATA_CLEAR_THREAD_SET_KEY,
                        item = index.toString(),
                        isDistinguishCluster = true)
                ) {
                    doClearBus(
                        threadNo = index,
                        projectIdList = projectIdList,
                        minThreadProjectPrimaryId = (index - 1) * avgProjectNum,
                        maxThreadProjectPrimaryId = maxThreadProjectPrimaryId
                    )
                }
            }
        } catch (ignored: Throwable) {
            logger.warn("pipelineBuildHistoryDataClear failed", ignored)
        } finally {
            lock.unlock()
        }
    }

    private fun doClearBus(
        threadNo: Int,
        projectIdList: List<String>?,
        minThreadProjectPrimaryId: Long,
        maxThreadProjectPrimaryId: Long
    ): Future<Boolean> {
        val threadName = "Thread-$threadNo"
        return executor!!.submit(Callable {
            var handleProjectPrimaryId =
                redisOperation.get(key = "$threadName:$PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY",
                    isDistinguishCluster = true)?.toLong()
            if (handleProjectPrimaryId == null) {
                handleProjectPrimaryId = minThreadProjectPrimaryId
            } else {
                if (handleProjectPrimaryId >= maxThreadProjectPrimaryId) {
                    // 已经清理完全部项目的流水线的过期构建记录，再重新开始清理
                    redisOperation.delete("$threadName:$PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY", true)
                    logger.info("pipelineBuildHistoryDataClear $threadName reStart")
                    return@Callable true
                }
            }
            // 将线程编号存入redis集合
            redisOperation.sadd(PIPELINE_BUILD_HISTORY_DATA_CLEAR_THREAD_SET_KEY,
                threadNo.toString(),
                isDistinguishCluster = true)
            try {
                val maxEveryProjectHandleNum = miscBuildDataClearConfig.maxEveryProjectHandleNum
                var maxHandleProjectPrimaryId = handleProjectPrimaryId.toLong()
                val projectInfoList = if (projectIdList.isNullOrEmpty()) {
                    val channelCodeList = miscBuildDataClearConfig.clearChannelCodes.split(",")
                    maxHandleProjectPrimaryId = handleProjectPrimaryId + maxEveryProjectHandleNum
                    projectMiscService.getProjectInfoList(
                        minId = handleProjectPrimaryId,
                        maxId = maxHandleProjectPrimaryId,
                        channelCodeList = channelCodeList
                    )
                } else {
                    projectMiscService.getProjectInfoList(projectIdList = projectIdList)
                }
                // 根据项目依次查询T_PIPELINE_INFO表中的流水线数据处理
                projectInfoList?.forEach { projectInfo ->
                    val channel = projectInfo.channel
                    // 获取项目对应的流水线数据清理配置类，如果不存在说明无需清理该项目下的构建记录
                    val projectDataClearConfigService =
                        ProjectDataClearConfigFactory.getProjectDataClearConfigService(channel) ?: return@forEach
                    val projectPrimaryId = projectInfo.id
                    if (projectPrimaryId > maxHandleProjectPrimaryId) {
                        maxHandleProjectPrimaryId = projectPrimaryId
                    }
                    val projectId = projectInfo.projectId
                    // 清理流水线构建数据
                    clearPipelineBuildData(projectId, projectDataClearConfigService)
                }
                // 将当前已处理完的最大项目Id存入redis
                redisOperation.set(
                    key = "$threadName:$PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_ID_KEY",
                    value = maxHandleProjectPrimaryId.toString(),
                    expired = false,
                    isDistinguishCluster = true
                )
            } catch (ignored: Throwable) {
                logger.warn("pipelineBuildHistoryDataClear doClearBus failed", ignored)
            } finally {
                // 释放redis集合中的线程编号
                redisOperation.sremove(key = PIPELINE_BUILD_HISTORY_DATA_CLEAR_THREAD_SET_KEY,
                    values = threadNo.toString(),
                    isDistinguishCluster = true)
            }
            return@Callable true
        })
    }

    private fun clearPipelineBuildData(
        projectId: String,
        projectDataClearConfigService: ProjectDataClearConfigService
    ) {
        // 获取当前项目下流水线记录的最小主键ID值
        var minId = processMiscService.getMinPipelineInfoIdByProjectId(projectId)
        do {
            logger.info("pipelineBuildHistoryPastDataClear clearPipelineBuildData projectId:$projectId,minId:$minId")
            val pipelineIdList = processMiscService.getPipelineIdListByProjectId(
                projectId = projectId,
                minId = minId,
                limit = DEFAULT_PAGE_SIZE.toLong()
            )
            if (!pipelineIdList.isNullOrEmpty()) {
                // 重置minId的值
                minId = processMiscService.getPipelineInfoIdByPipelineId(
                    projectId = projectId,
                    pipelineId = pipelineIdList[pipelineIdList.size - 1]
                ) + 1
            }
            val deletePipelineIdList = if (pipelineIdList.isNullOrEmpty()) {
                null
            } else {
                processMiscService.getClearDeletePipelineIdList(
                    projectId = projectId,
                    pipelineIdList = pipelineIdList,
                    gapDays = deletedPipelineStoreDays
                )
            }
            val projectDataClearConfig = projectDataClearConfigService.getProjectDataClearConfig()
            pipelineIdList?.forEach { pipelineId ->
                logger.info("pipelineBuildHistoryPastDataClear start..............")
                val deleteFlag = deletePipelineIdList?.contains(pipelineId) == true
                if (deleteFlag) {
                    // 清理已删除流水线记录
                    cleanDeletePipelineData(pipelineId, projectId)
                } else {
                    // 清理正常流水线记录
                    cleanNormalPipelineData(pipelineId, projectId, projectDataClearConfig)
                }
            }
        } while (pipelineIdList?.size == DEFAULT_PAGE_SIZE)
    }

    private fun cleanNormalPipelineData(
        pipelineId: String,
        projectId: String,
        projectDataClearConfig: ProjectDataClearConfig
    ) {
        // 判断构建记录是否超过系统展示的最大数量，如果超过则需清理超量的数据
        val maxPipelineBuildNum = processMiscService.getMaxPipelineBuildNum(projectId, pipelineId)
        val maxKeepNum = projectDataClearConfig.maxKeepNum
        val maxBuildNum = maxPipelineBuildNum - maxKeepNum
        if (maxBuildNum > 0) {
            logger.info("pipelineBuildHistoryRecentDataClear start.............")
            cleanBuildHistoryData(
                pipelineId = pipelineId,
                projectId = projectId,
                isCompletelyDelete = true,
                maxBuildNum = maxBuildNum.toInt()
            )
        }
        // 根据流水线ID依次查询T_PIPELINE_BUILD_HISTORY表中X个月前的构建记录
        cleanBuildHistoryData(
            pipelineId = pipelineId,
            projectId = projectId,
            isCompletelyDelete = false,
            maxStartTime = projectDataClearConfig.maxStartTime
        )
    }

    private fun cleanDeletePipelineData(pipelineId: String, projectId: String) {
        // 删除已删除流水线构建记录
        cleanBuildHistoryData(
            pipelineId = pipelineId,
            projectId = projectId,
            isCompletelyDelete = true
        )
        // 删除已删除流水线记录
        processDataClearService.clearPipelineData(projectId, pipelineId)
    }

    private fun cleanBuildHistoryData(
        pipelineId: String,
        projectId: String,
        isCompletelyDelete: Boolean,
        maxBuildNum: Int? = null,
        maxStartTime: LocalDateTime? = null
    ) {
        val totalBuildCount = processMiscService.getTotalBuildCount(projectId, pipelineId, maxBuildNum, maxStartTime)
        logger.info("pipelineBuildHistoryDataClear|$projectId|$pipelineId|totalBuildCount=$totalBuildCount")
        var totalHandleNum = processMiscService.getMinPipelineBuildNum(projectId, pipelineId).toInt()
        while (totalHandleNum < totalBuildCount) {
            val cleanBuilds = mutableListOf<String>()
            val pipelineHistoryBuildIdList = processMiscService.getHistoryBuildIdList(
                projectId = projectId,
                pipelineId = pipelineId,
                totalHandleNum = totalHandleNum,
                handlePageSize = DEFAULT_PAGE_SIZE,
                isCompletelyDelete = isCompletelyDelete,
                maxBuildNum = maxBuildNum,
                maxStartTime = maxStartTime
            )
            pipelineHistoryBuildIdList?.forEach { buildId ->
                // 依次删除process表中的相关构建记录(T_PIPELINE_BUILD_HISTORY做为基准表，
                // 为了保证构建流水记录删干净，T_PIPELINE_BUILD_HISTORY记录要最后删)
                if (clearBaseBuildData) {
                    processDataClearService.clearBaseBuildData(projectId, buildId)
                }
                repositoryDataClearService.clearBuildData(buildId)
                if (isCompletelyDelete) {
                    dispatchDataClearService.clearBuildData(buildId)
                    pluginDataClearService.clearBuildData(buildId)
                    qualityDataClearService.clearBuildData(buildId)
                    artifactoryDataClearService.clearBuildData(buildId)
                    processDataClearService.clearOtherBuildData(projectId, pipelineId, buildId)
                    cleanBuilds.add(buildId)
                }
            }
            totalHandleNum += DEFAULT_PAGE_SIZE
            if (cleanBuilds.isNotEmpty()) {
                processRelatedPlatformDataClearService.cleanBuildData(projectId, pipelineId, cleanBuilds)
            }
        }
    }
}
