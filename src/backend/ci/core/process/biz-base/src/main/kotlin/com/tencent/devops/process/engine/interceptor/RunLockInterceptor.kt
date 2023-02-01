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

package com.tencent.devops.process.engine.interceptor

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_LOCK
import com.tencent.devops.process.engine.pojo.Response
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 运行时锁定
 *
 * @version 1.0
 */
@Component
class RunLockInterceptor @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService
) : PipelineInterceptor {

    override fun execute(task: InterceptData): Response<BuildStatus> {
        val projectId = task.pipelineInfo.projectId
        val pipelineId = task.pipelineInfo.pipelineId
        val setting = task.setting
        val runLockType = setting?.runLockType
        val concurrencyGroup = setting?.concurrencyGroup
        return checkRunLock(runLockType, projectId, pipelineId, concurrencyGroup)
    }

    fun checkRunLock(
        runLockType: PipelineRunLockType?,
        projectId: String,
        pipelineId: String,
        concurrencyGroup: String?
    ): Response<BuildStatus> {
        val result: Response<BuildStatus> = if (checkLock(runLockType)) {
            Response(ERROR_PIPELINE_LOCK.toInt(), "当前流水线已被锁定，无法执行，请解锁后重试")
        } else if (runLockType == PipelineRunLockType.SINGLE || runLockType == PipelineRunLockType.SINGLE_LOCK) {
            val buildSummaryRecord = pipelineRuntimeService.getBuildSummaryRecord(projectId, pipelineId)
            return if (buildSummaryRecord?.runningCount ?: 0 >= 1) {
                logger.info("[$pipelineId] 当前流水线已设置为同时只能运行一个构建任务，开始排队！")
                Response(BuildStatus.QUEUE)
            } else {
                Response(BuildStatus.RUNNING)
            }
        } else if (runLockType == PipelineRunLockType.GROUP_LOCK) {
            val concurrencyGroupNotNull = concurrencyGroup ?: pipelineId
            val concurrencyGroupRunningCount = concurrencyGroupNotNull.let {
                var size = pipelineRuntimeService.getBuildInfoListByConcurrencyGroup(
                    projectId = projectId,
                    concurrencyGroup = it,
                    status = listOf(BuildStatus.RUNNING)
                ).size

                // #8143 兼容旧流水线版本 TODO 待模板设置补上漏洞，后期下掉 # 8143
                if (it == pipelineId) {
                    size += pipelineRuntimeService.getBuildInfoListByConcurrencyGroupNull(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        status = listOf(BuildStatus.RUNNING)
                    ).size
                }
                return@let size
            } ?: 0
            if (concurrencyGroupRunningCount >= 1) {
                logger.info("[$pipelineId] 当前互斥组[$concurrencyGroup]同时只能运行一个构建任务，开始排队！")
                Response(BuildStatus.QUEUE)
            } else {
                Response(BuildStatus.RUNNING)
            }
        } else {
            Response(BuildStatus.RUNNING)
        }
        logger.info("[$pipelineId] run lock check result: $result")
        return result
    }

    private fun checkLock(runLockType: PipelineRunLockType?): Boolean {
        return runLockType == PipelineRunLockType.LOCK
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RunLockInterceptor::class.java)
    }
}
