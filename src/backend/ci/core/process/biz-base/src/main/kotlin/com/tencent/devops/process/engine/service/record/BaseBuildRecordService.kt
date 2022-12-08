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

package com.tencent.devops.process.engine.service.record

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Suppress("LongParameterList", "MagicNumber")
@Service
class BaseBuildRecordService(
    private val dslContext: DSLContext,
    private val buildRecordModelDao: BuildRecordModelDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation
) {

    protected fun update(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        cancelUser: String? = null,
        operation: String = "",
        refreshOperation: () -> Unit
    ) {
        val watcher = Watcher(id = "updateRecord#$buildId#$operation")
        var message = "nothing"
        val lock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "process.build.record.lock.$buildId.$executeCount",
            expiredTimeInSeconds = ExpiredTimeInSeconds
        )
        try {
            watcher.start("lock")
            lock.lock()

            watcher.start("getRecord")
            val record = buildRecordModelDao.getRecord(
                dslContext, projectId, pipelineId, buildId, executeCount
            ) ?: run {
                message = "Will not update"
                return
            }

            watcher.start("refreshOperation")
            refreshOperation()
            watcher.stop()

            watcher.start("updatePipelineRecord")
            val (change, finalStatus) = takeBuildStatus(record, buildStatus)
            if (!change) {
                message = "Will not update"
                return
            }
            buildRecordModelDao.updateRecord(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = executeCount,
                buildStatus = finalStatus,
                modelVar = emptyMap(), // 暂时没有变量，保留修改可能
                cancelUser = cancelUser // 系统行为导致的取消状态(仅当在取消状态时，还没有设置过取消人，才默认为System)
                    ?: if (buildStatus.isCancel() && record.cancelUser.isNullOrBlank()) "System" else null,
                startTime = null,
                endTime = null,
                timestamps = null,
                timeCost = null
            )

            watcher.start("dispatchEvent")
            pipelineDetailChangeEvent(projectId, pipelineId, buildId, record.startUser)
            message = "Will not update"
        } catch (ignored: Throwable) {
            message = ignored.message ?: ""
            logger.warn("[$buildId]| Fail to update the build detail: ${ignored.message}", ignored)
        } finally {
            lock.unlock()
            watcher.stop()
            logger.info("[$buildId|$buildStatus]|$operation|update_detail_record| $message")
            LogUtils.printCostTimeWE(watcher)
        }
        return
    }

    private fun takeBuildStatus(
        record: BuildRecordModel,
        buildStatus: BuildStatus
    ): Pair<Boolean, BuildStatus> {
        val oldStatus = BuildStatus.parse(record.status)
        return if (!oldStatus.isFinish()) {
            (oldStatus != buildStatus) to buildStatus
        } else {
            false to oldStatus
        }
    }

    protected fun pipelineDetailChangeEvent(
        projectId: String,
        pipelineId: String,
        buildId: String,
        startUser: String
    ) {
        pipelineEventDispatcher.dispatch(
            PipelineBuildWebSocketPushEvent(
                source = "pauseTask",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = startUser,
                buildId = buildId,
                refreshTypes = RefreshType.DETAIL.binary
            )
        )
    }

    companion object {
        fun refreshOperation(f: () -> Int): Int = f()
        private const val ExpiredTimeInSeconds: Long = 10
        private val logger = LoggerFactory.getLogger(BaseBuildRecordService::class.java)
    }
}
