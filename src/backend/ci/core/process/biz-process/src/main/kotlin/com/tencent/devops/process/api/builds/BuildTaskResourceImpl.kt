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

package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.pojo.task.PipelineBuildTaskInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildTaskResourceImpl @Autowired constructor(
    private val pipelineTaskService: PipelineTaskService,
    private val taskBuildRecordService: TaskBuildRecordService
) : BuildTaskResource {

    override fun getAllBuildTask(projectId: String, buildId: String): Result<List<PipelineBuildTaskInfo>> {
        return Result(pipelineTaskService.getAllBuildTaskInfo(projectId, buildId))
    }

    override fun reportExternalLink(
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String,
        executeCount: Int?,
        link: String
    ): Result<Boolean> {
        val externalLink = link.trim()
        if (!externalLink.startsWith("http://") && !externalLink.startsWith("https://")) {
            LOG.warn("REPORT_EXTERNAL_LINK_INVALID|$buildId|$taskId|$externalLink")
            return Result(false)
        }
        if (externalLink.length > MAX_EXTERNAL_LINK_LENGTH) {
            LOG.warn("REPORT_EXTERNAL_LINK_TOO_LONG|$buildId|$taskId")
            return Result(false)
        }
        taskBuildRecordService.updateTaskRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = executeCount ?: 1,
            taskVar = mapOf(Element::externalLink.name to externalLink),
            buildStatus = null,
            operation = "reportExternalLink#$taskId"
        )
        return Result(true)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(BuildTaskResourceImpl::class.java)
        private const val MAX_EXTERNAL_LINK_LENGTH = 1024
    }
}
