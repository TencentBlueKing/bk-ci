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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.process.dao.PipelineBuildHistoryDao
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.SubPipelineRefTree
import com.tencent.devops.process.pojo.pipeline.SubPipelineStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SubPipelineStatusService @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val redisOperation: RedisOperation,
    private val pipelineBuildHistoryDao: PipelineBuildHistoryDao,
    private var dslContext: DSLContext
) {

    companion object {
        // 子流水线启动过期时间900分钟
        private const val SUBPIPELINE_STATUS_START_EXPIRED = 54000L

        // 子流水线完成过期时间10分钟
        private const val SUBPIPELINE_STATUS_FINISH_EXPIRED = 600L

        // 子流水线递归调用层次
        private const val SUBPIPELINE_CALL_DEPTH = 3
    }

    private val logger = LoggerFactory.getLogger(SubPipelineStatusService::class.java)

    fun onStart(buildId: String) {
        redisOperation.set(
            key = getSubPipelineStatusKey(buildId),
            value = JsonUtil.toJson(
                SubPipelineStatus(status = BuildStatus.RUNNING.name)
            ),
            expiredInSecond = SUBPIPELINE_STATUS_START_EXPIRED
        )
    }

    fun onFinish(event: PipelineBuildFinishBroadCastEvent) {
        with(event) {
            // 不是子流水线启动或者子流水线是异步启动的，不需要缓存状态
            if (triggerType != StartType.PIPELINE.name ||
                redisOperation.get(getSubPipelineStatusKey(buildId)) == null
            ) {
                return
            }

            redisOperation.set(
                key = getSubPipelineStatusKey(buildId),
                value = JsonUtil.toJson(getSubPipelineStatusFromDB(event.projectId, buildId)),
                expiredInSecond = SUBPIPELINE_STATUS_FINISH_EXPIRED
            )
        }
    }

    private fun getSubPipelineStatusFromDB(projectId: String, buildId: String): SubPipelineStatus {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
        return if (buildInfo != null) {
            val status: BuildStatus = when {
                buildInfo.isSuccess() && buildInfo.isStageSuccess() -> BuildStatus.SUCCEED
                buildInfo.isFinish() -> buildInfo.status
                buildInfo.isReadyToRun() -> BuildStatus.RUNNING // QUEUE状态
                buildInfo.isStageSuccess() -> BuildStatus.RUNNING // stage 特性， 未结束，只是卡在Stage审核中
                else -> buildInfo.status
            }
            SubPipelineStatus(
                status = status.name
            )
        } else {
            SubPipelineStatus(
                status = BuildStatus.FAILED.name,
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorMsg = "找不到对应子流水线的构建记录"
            )
        }
    }

    fun getSubPipelineStatus(projectId: String, buildId: String): SubPipelineStatus {
        val subPipelineStatusStr = redisOperation.get(getSubPipelineStatusKey(buildId))
        return if (subPipelineStatusStr.isNullOrBlank()) {
            getSubPipelineStatusFromDB(projectId, buildId)
        } else {
            val redisSubPipelineStatus = JsonUtil.to(subPipelineStatusStr, SubPipelineStatus::class.java)
            // 如果状态完成,子流水线插件就不会再调用,从redis中删除key
            if (BuildStatus.parse(redisSubPipelineStatus.status).isFinish()) {
                redisOperation.delete(getSubPipelineStatusKey(buildId))
            }
            redisSubPipelineStatus
        }
    }

    fun getSubPipelinesStatus(projectId: String, pipelineId: String, buildId: String): SubPipelineRefTree? {
        // 获取状态信息，提取树形结构，避免二次递归查数据库
        val statusKey = "subpipeline:build:$buildId:statusTree"
        val statusInfo = redisOperation.get(statusKey)
        var status: SubPipelineRefTree? = null
        if (statusInfo.isNullOrBlank()) {
            logger.info("首次获取子流水线状态,projectId=[$projectId],pipelineId=[$pipelineId],buildId=[$buildId]")
            val historyRecord = pipelineBuildHistoryDao.findHistoryByParentBuildId(
                dslContext = dslContext,
                parentBuildId = buildId
            )
            status = getSubPipelineByBuildHistory(
                dslContext = dslContext,
                history = historyRecord,
                depth = SUBPIPELINE_CALL_DEPTH,
                base = SubPipelineRefTree(
                    buildId = buildId,
                    pipelineId = pipelineId,
                    projectId = projectId,
                    status = getSubPipelineStatus(
                        projectId = projectId,
                        buildId = buildId
                    ).status
                )
            )
            redisOperation.set(statusKey, JsonUtil.toJson(status!!, false))
        } else {
            logger.info("获取子流水线最新状态,projectId=[$projectId],pipelineId=[$pipelineId],buildId=[$buildId]")
            status = getSubPipelineLastStatus(JsonUtil.to(statusInfo, SubPipelineRefTree::class.java))
        }
        return status
    }

    fun getSubPipelineLastStatus(subPipelines: SubPipelineRefTree): SubPipelineRefTree? {
        if (subPipelines.subPipeline.isEmpty()) {
            return null
        }
        subPipelines.status = getSubPipelineStatus(
            projectId = subPipelines.projectId,
            buildId = subPipelines.buildId
        ).status
        subPipelines.subPipeline.forEach {
            if (it.subPipeline.isNotEmpty()) {
                val subs = getSubPipelineLastStatus(it)
                if (subs != null) {
                    it.subPipeline.add(subs)
                }
            } else {
                it.status = getSubPipelineStatus(
                    projectId = it.projectId,
                    buildId = it.buildId
                ).status
            }
        }
        return subPipelines
    }

    private fun getSubPipelineByBuildHistory(
        dslContext: DSLContext,
        history: Array<out TPipelineBuildHistoryRecord>,
        depth: Int,
        base: SubPipelineRefTree
    ): SubPipelineRefTree? {
        if (history.isEmpty()) {
            return null
        }
        history.forEach {
            val historyRecord = pipelineBuildHistoryDao.findHistoryByParentBuildId(
                dslContext = dslContext,
                parentBuildId = it.buildId
            )
            val subDepth = depth - 1
            // 仍存在下级子流水线调用，则继续递归
            if (historyRecord.isNotEmpty() || subDepth <= 0) {
                val sub = getSubPipelineByBuildHistory(
                    dslContext = dslContext,
                    history = historyRecord,
                    depth = subDepth,
                    base = SubPipelineRefTree(
                        buildId = it.buildId,
                        projectId = it.projectId,
                        pipelineId = it.pipelineId,
                        status = getSubPipelineStatus(
                            projectId = it.projectId,
                            buildId = it.buildId
                        ).status
                    )
                )
                if (sub != null) {
                    base.subPipeline.add(sub)
                }
            } else {
                base.subPipeline.add(
                    SubPipelineRefTree(
                        buildId = it.buildId,
                        projectId = it.projectId,
                        pipelineId = it.pipelineId,
                        status = getSubPipelineStatus(
                            projectId = it.projectId,
                            buildId = it.buildId
                        ).status
                    )
                )
            }
        }
        return base
    }

    private fun getSubPipelineStatusKey(buildId: String): String {
        return "subpipeline:build:$buildId:status"
    }
}
