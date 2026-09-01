/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.misc.listener

import com.tencent.devops.common.event.listener.EventListener
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildHistoryDataClearEvent
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.misc.config.MiscBuildDataClearConfig
import com.tencent.devops.misc.service.process.PipelineBuildDataClearService
import com.tencent.devops.misc.service.project.ProjectDataClearConfigFactory
import com.tencent.devops.misc.service.project.ProjectMiscService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 流水线构建历史数据清理事件监听器
 *
 * 在构建结束时被触发，对当前流水线即时检查并清理超量/过期的构建记录。
 */
@Component
class PipelineBuildHistoryDataClearListener @Autowired constructor(
    private val miscBuildDataClearConfig: MiscBuildDataClearConfig,
    private val projectMiscService: ProjectMiscService,
    private val pipelineBuildDataClearService: PipelineBuildDataClearService,
    private val redisOperation: RedisOperation
) : EventListener<PipelineBuildHistoryDataClearEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildHistoryDataClearListener::class.java)
        private const val LOCK_KEY_PREFIX = "pipeline_build_history_data_clear_lock:"
        // 流水线级互斥锁过期时间(秒)：避免同一条流水线被并发触发清理，超过该时间后锁自动释放
        private const val LOCK_EXPIRED_SECOND = 600L
    }

    override fun execute(event: PipelineBuildHistoryDataClearEvent) {
        if (!miscBuildDataClearConfig.switch.toBoolean()) {
            return
        }
        val projectId = event.projectId
        val pipelineId = event.pipelineId
        try {
            // 事件驱动场景使用流水线级互斥锁串行化同一条流水线的清理操作，
            // 与 BuildEndControl 派发端的节流互补，避免本地多实例并发或与定时任务并发处理同一流水线
            val lock = RedisLock(
                redisOperation = redisOperation,
                lockKey = "$LOCK_KEY_PREFIX$projectId:$pipelineId",
                expiredTimeInSeconds = LOCK_EXPIRED_SECOND
            )
            if (!lock.tryLock()) {
                logger.info("PipelineBuildHistoryDataClear|$projectId|$pipelineId|lock not acquired, skip")
                return
            }
            try {
                val projectInfo = projectMiscService.getProjectInfoList(
                    projectIdList = listOf(projectId)
                )?.firstOrNull()
                if (projectInfo == null) {
                    logger.warn("PipelineBuildHistoryDataClear|$projectId|$pipelineId|project not found, skip")
                    return
                }
                val projectDataClearConfigService =
                    ProjectDataClearConfigFactory.getProjectDataClearConfigService(projectInfo.channel)
                if (projectDataClearConfigService == null) {
                    logger.info(
                        "PipelineBuildHistoryDataClear|$projectId|$pipelineId|" +
                            "no clear config for channel ${projectInfo.channel}, skip"
                    )
                    return
                }
                val projectDataClearConfig = projectDataClearConfigService.getProjectDataClearConfig()
                logger.info("PipelineBuildHistoryDataClear|$projectId|$pipelineId|event-driven cleanup triggered")
                // 事件驱动路径只做"超量"清理；"过期时间"清理继续由定时任务兜底，避免每次构建结束都全量扫表
                pipelineBuildDataClearService.cleanOverflowPipelineData(
                    pipelineId = pipelineId,
                    projectId = projectId,
                    projectDataClearConfig = projectDataClearConfig
                )
            } finally {
                lock.unlock()
            }
        } catch (ignored: Throwable) {
            logger.warn(
                "PipelineBuildHistoryDataClear|$projectId|$pipelineId|event-driven cleanup failed",
                ignored
            )
        }
    }
}
