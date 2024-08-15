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
import com.tencent.devops.misc.service.process.PipelineBuildHistoryDataClearService
import com.tencent.devops.misc.service.project.ProjectMiscService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Component
@Suppress("ALL")
class PipelineBuildHistoryDataClearJob @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val miscBuildDataClearConfig: MiscBuildDataClearConfig,
    private val projectMiscService: ProjectMiscService,
    private val pipelineBuildHistoryDataClearService: PipelineBuildHistoryDataClearService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildHistoryDataClearJob::class.java)
        private const val LOCK_KEY = "pipelineBuildHistoryDataClear"
        private const val PIPELINE_BUILD_HISTORY_DATA_CLEAR_PROJECT_LIST_KEY =
            "pipeline:build:history:data:clear:project:list"
        private const val PIPELINE_BUILD_HISTORY_DATA_CLEAR_THREAD_SET_KEY =
            "pipeline:build:history:data:clear:thread:set"
        private var executor: ThreadPoolExecutor? = null
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
                isDistinguishCluster = true
            )
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
                        isDistinguishCluster = true
                    )
                ) {
                    pipelineBuildHistoryDataClearService.doClearBus(
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
}
