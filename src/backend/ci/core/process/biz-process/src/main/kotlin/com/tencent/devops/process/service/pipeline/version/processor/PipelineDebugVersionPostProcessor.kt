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

package com.tencent.devops.process.service.pipeline.version.processor

import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * 流水线调试后置处理器
 */
@Service
class PipelineDebugVersionPostProcessor @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBuildDao: PipelineBuildDao,
    private val buildLogPrinter: BuildLogPrinter,
    @Lazy
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao
) : PipelineVersionCreatePostProcessor {

    override fun postProcessAfterVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting
    ) {
        with(context) {
            // 草稿转正式版本,需要把调试记录清空
            val draft2Release = versionAction == PipelineVersionAction.RELEASE_DRAFT &&
                    pipelineResourceVersion.status == VersionStatus.RELEASED
            if (!draft2Release) {
                return
            }
            // #8164 发布后的流水将调试信息清空为0，重新计数，同时取消该版本的调试记录
            pipelineBuildDao.getDebugHistory(dslContext, projectId, pipelineId).forEach { debug ->
                if (!debug.status.isFinish()) {
                    buildLogPrinter.addWarnLine(
                        buildId = debug.buildId,
                        executeCount = debug.executeCount,
                        tag = "",
                        jobId = null,
                        stepId = null,
                        message = ""
                    )
                    pipelineBuildFacadeService.buildManualShutdown(
                        userId = userId, projectId = projectId, pipelineId = pipelineId,
                        buildId = debug.buildId, channelCode = pipelineBasicInfo.channelCode, terminateFlag = true
                    )
                }
            }
            // 查询编排中的基准值，并把调试的版本号刷为基准值
            val debugBuildNo = pipelineResourceVersion.model.getTriggerContainer()
                .buildNo?.buildNo ?: 0
            pipelineBuildSummaryDao.resetDebugInfo(dslContext, projectId, pipelineId, debugBuildNo)
            pipelineBuildDao.clearDebugHistory(dslContext, projectId, pipelineId)
        }
    }
}
