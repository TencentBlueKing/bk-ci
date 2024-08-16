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

package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.PipelineListFacadeService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildPipelineResourceImpl @Autowired constructor(
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildVariableService: BuildVariableService
) : BuildPipelineResource {
    override fun getPipelineNameByIds(projectId: String, pipelineIds: Set<String>): Result<Map<String, String>> {
        logger.info("the method of being done is: getPipelineNameByIds")
        return Result(pipelineListFacadeService.getPipelineNameByIds(
            projectId = projectId, pipelineIds = pipelineIds, filterDelete = true
        ))
    }

    override fun getHistoryBuild(
        currentBuildId: String,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Result<BuildHistoryPage<BuildHistory>> {
        logger.info("the method of being done is: getHistoryBuild")
        val userId = buildVariableService.getVariable(projectId, pipelineId, currentBuildId, "pipeline.start.user.id")
        val result = pipelineBuildFacadeService.getHistoryBuild(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            page = page,
            pageSize = pageSize,
            channelCode = ChannelCode.BS,
            checkPermission = true)
        return Result(result)
    }

    override fun list(
        currentBuildId: String,
        projectId: String,
        pipelineIdListString: String?
    ): Result<List<Pipeline>> {
        logger.info("the method of being done is: list")
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, currentBuildId)
            ?: return Result(emptyList())
        val userId = buildVariableService.getVariable(
            projectId = projectId,
            pipelineId = buildInfo.pipelineId,
            buildId = currentBuildId,
            varName = "pipeline.start.user.id"
        )!!
        val pipelineIdList = pipelineIdListString?.split(",")
        return Result(pipelineListFacadeService.listPipelineInfo(
            userId = userId, projectId = projectId, pipelineIdList = pipelineIdList
        ))
    }

    private val logger = LoggerFactory.getLogger(BuildPipelineResourceImpl::class.java)
}
