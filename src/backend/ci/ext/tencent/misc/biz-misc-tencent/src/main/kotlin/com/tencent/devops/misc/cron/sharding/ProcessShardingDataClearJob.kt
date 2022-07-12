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
 *//*


package com.tencent.devops.misc.cron.sharding

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.misc.config.ProcessShardingDataClearConfig
import com.tencent.devops.misc.service.project.TxProjectMiscService
import com.tencent.devops.misc.service.shardingprocess.ProcessShardingDataClearService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Component
class ProcessShardingDataClearJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val processShardingDataClearConfig: ProcessShardingDataClearConfig,
    private val txProjectMiscService: TxProjectMiscService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProcessShardingDataClearJob::class.java)
        private const val LOCK_KEY = "pipelineBuildHistoryDataClear"
        private const val DEFAULT_PAGE_SIZE = 100
        private const val SHARDING_DATA_CLEAR_PROJECT_ID_KEY =
            "sharding:data:clear:project:id"
        private const val SHARDING_DATA_CLEAR_THREAD_SET_KEY =
            "sharding:data:clear:thread:set"
        private var executor: ThreadPoolExecutor? = null
    }

    @PostConstruct
    fun init() {
        logger.info("start init shardingDataClear")
        // 启动的时候删除redis中存储的清理线程集合，防止redis中的线程信息因为服务异常停了无法删除
        redisOperation.delete(SHARDING_DATA_CLEAR_THREAD_SET_KEY, true)
    }

    @Scheduled(initialDelay = 12000, fixedDelay = 15000)
    fun shardingDataClear() {
        if (!processShardingDataClearConfig.switch.toBoolean()) {
            // 如果清理分库历史数据开关关闭，则不清理
            return
        }
        logger.info("shardingDataClear start")
        if (executor == null) {
            // 创建带有边界队列的线程池，防止内存爆掉
            logger.info("shardingDataClear create executor")
            executor = ThreadPoolExecutor(
                processShardingDataClearConfig.maxThreadHandleProjectNum,
                processShardingDataClearConfig.maxThreadHandleProjectNum,
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
            val maxProjectNum = txProjectMiscService.getMaxId() ?: 0L
            // 获取清理数据库冗余数据的线程数量
            val maxThreadHandleProjectNum = processShardingDataClearConfig.maxThreadHandleProjectNum
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
                        key = SHARDING_DATA_CLEAR_THREAD_SET_KEY,
                        item = index.toString(),
                        isDistinguishCluster = true)
                ) {
                    doClearBus(
                        threadNo = index,
                        minThreadProjectPrimaryId = (index - 1) * avgProjectNum,
                        maxThreadProjectPrimaryId = maxThreadProjectPrimaryId
                    )
                }
            }
        } catch (t: Throwable) {
            logger.warn("shardingDataClear failed", t)
        } finally {
            lock.unlock()
        }
    }

    private fun doClearBus(
        threadNo: Int,
        minThreadProjectPrimaryId: Long,
        maxThreadProjectPrimaryId: Long
    ): Future<Boolean> {
        val threadName = "Thread-$threadNo"
        return executor!!.submit(Callable<Boolean> {
            var handleProjectPrimaryId =
                redisOperation.get(key = "$threadName:$SHARDING_DATA_CLEAR_PROJECT_ID_KEY",
                    isDistinguishCluster = true)?.toLong()
            if (handleProjectPrimaryId == null) {
                handleProjectPrimaryId = minThreadProjectPrimaryId
            } else {
                if (handleProjectPrimaryId >= maxThreadProjectPrimaryId) {
                    // 已经清理完全部项目的冗余数据，再重新开始清理
                    redisOperation.delete("$threadName:$SHARDING_DATA_CLEAR_PROJECT_ID_KEY", true)
                    logger.info("shardingDataClear $threadName reStart")
                    return@Callable true
                }
            }
            // 将线程编号存入redis集合
            redisOperation.sadd(SHARDING_DATA_CLEAR_THREAD_SET_KEY,
                threadNo.toString(),
                isDistinguishCluster = true)
            try {
                val maxEveryProjectHandleNum = processShardingDataClearConfig.maxEveryProjectHandleNum
                val channelCodeList = processShardingDataClearConfig.clearChannelCodes.split(",")
                val maxHandleProjectPrimaryId = handleProjectPrimaryId + maxEveryProjectHandleNum
                val projectShardingInfoList = txProjectMiscService.getProjectShardingInfoList(
                    minId = handleProjectPrimaryId,
                    maxId = maxHandleProjectPrimaryId,
                    channelCodeList = channelCodeList
                )
                projectShardingInfoList?.forEach { projectShardingInfo ->
                    // 清理项目的冗余数据
                    clearShardingData(projectShardingInfo.projectId, projectShardingInfo.routingRule)
                }
                // 将当前已处理完的最大项目Id存入redis
                redisOperation.set(
                    key = "$threadName:$SHARDING_DATA_CLEAR_PROJECT_ID_KEY",
                    value = maxHandleProjectPrimaryId.toString(),
                    expired = false,
                    isDistinguishCluster = true
                )
            } catch (ignore: Exception) {
                logger.warn("shardingDataClear doClearBus failed", ignore)
            } finally {
                // 释放redis集合中的线程编号
                redisOperation.sremove(key = SHARDING_DATA_CLEAR_THREAD_SET_KEY,
                    values = threadNo.toString(),
                    isDistinguishCluster = true)
            }
            return@Callable true
        })
    }

    private fun clearShardingData(projectId: String, routingRule: String?) {
        logger.info("processShardingDataClearJob clearShardingData projectId:$projectId,routingRule:$routingRule")
        val clearServiceList = SpringContextUtil.getBeansWithClass(ProcessShardingDataClearService::class.java)
        clearServiceList.forEach { clearService ->
            // 判断数据对应的service是否有权限执行删除逻辑
            val executeFlag = clearService.getExecuteFlag(routingRule)
            if (!executeFlag) {
                return@forEach
            }
            // 获取当前项目下流水线记录的最小主键ID值
            var minId = clearService.getMinPipelineInfoIdByProjectId(projectId)
            val serviceClassName = clearService.javaClass.name
            do {
                logger.info("[$serviceClassName] clearShardingData projectId:$projectId,minId:$minId")
                val pipelineIdList = clearService.getPipelineIdListByProjectId(
                    projectId = projectId,
                    minId = minId,
                    limit = DEFAULT_PAGE_SIZE.toLong()
                )
                if (!pipelineIdList.isNullOrEmpty()) {
                    // 重置minId的值
                    minId = clearService.getPipelineInfoIdByPipelineId(
                        projectId = projectId,
                        pipelineId = pipelineIdList[pipelineIdList.size - 1]
                    ) + 1
                }
                val pipelineNum = pipelineIdList?.size
                logger.info("[$serviceClassName] clearShardingData projectId:$projectId,pipelineNum:$pipelineNum")
                pipelineIdList?.forEach { pipelineId ->
                    clearShardingDataByPipelineId(
                        clearService = clearService,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        routingRule = routingRule
                    )
                }
            } while (pipelineIdList?.size == DEFAULT_PAGE_SIZE)
            // 按项目ID清理分片数据
            clearService.clearShardingDataByProjectId(projectId, routingRule)
        }
    }

    private fun clearShardingDataByPipelineId(
        clearService: ProcessShardingDataClearService,
        projectId: String,
        pipelineId: String,
        routingRule: String?
    ) {
        val totalBuildCount = clearService.getTotalBuildCount(projectId, pipelineId)
        val serviceClassName = clearService.javaClass.name
        logger.info("[$serviceClassName]clearShardingData|$projectId|$pipelineId|totalBuildCount=$totalBuildCount")
        var totalHandleNum = 0
        while (totalHandleNum < totalBuildCount) {
            val pipelineHistoryBuildIdList = clearService.getHistoryBuildIdList(
                projectId = projectId,
                pipelineId = pipelineId,
                totalHandleNum = totalHandleNum,
                handlePageSize = DEFAULT_PAGE_SIZE,
                isCompletelyDelete = true
            )
            doClearShardingDataByBuildId(
                pipelineHistoryBuildIdList = pipelineHistoryBuildIdList,
                clearService = clearService,
                projectId = projectId,
                pipelineId = pipelineId,
                routingRule = routingRule
            )
            totalHandleNum += DEFAULT_PAGE_SIZE
        }
        // 按流水线ID清理分片数据
        clearService.clearShardingDataByPipelineId(projectId, pipelineId, routingRule)
    }

    private fun doClearShardingDataByBuildId(
        pipelineHistoryBuildIdList: List<String>?,
        clearService: ProcessShardingDataClearService,
        projectId: String,
        pipelineId: String,
        routingRule: String?
    ) {
        pipelineHistoryBuildIdList?.forEach { buildId ->
            // 按构建ID清理分片数据
            clearService.clearShardingDataByBuildId(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                routingRule = routingRule
            )
        }
    }
}
*/
