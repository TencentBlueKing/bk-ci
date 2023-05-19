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

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BuildApiPermission
import com.tencent.devops.common.web.constant.BuildApiHandleType
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.service.SubPipelineStartUpService
import com.tencent.devops.process.service.builds.PipelineBuildFacadeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildBuildResourceImpl @Autowired constructor(
    private val pipelineBuildFacadeService: PipelineBuildFacadeService,
    private val subPipelineStartUpService: SubPipelineStartUpService,
    private val pipelineUrlBean: PipelineUrlBean
) : BuildBuildResource {

    @BuildApiPermission([BuildApiHandleType.AUTH_CHECK])
    override fun getSingleHistoryBuild(
        projectId: String,
        pipelineId: String,
        buildNum: String,
        channelCode: ChannelCode?
    ): Result<BuildHistory?> {
        return Result(
            data = pipelineBuildFacadeService.getSingleHistoryBuild(
                projectId = projectId,
                pipelineId = pipelineId,
                buildNum = buildNum.toInt(),
                channelCode = channelCode ?: ChannelCode.BS
            )
        )
    }

    @BuildApiPermission([BuildApiHandleType.AUTH_CHECK])
    override fun getLatestSuccessBuild(
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode?
    ): Result<BuildHistory?> {
        return Result(
            data = pipelineBuildFacadeService.getLatestSuccessBuild(
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = channelCode ?: ChannelCode.BS
            )
        )
    }

    @BuildApiPermission([BuildApiHandleType.AUTH_CHECK])
    override fun getBuildDetail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode
    ): Result<ModelDetail> {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        return Result(
            data = pipelineBuildFacadeService.getBuildDetail(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                channelCode = channelCode
            )
        )
    }

    override fun getSubBuildVars(projectId: String, buildId: String, taskId: String): Result<Map<String, String>> {
        return subPipelineStartUpService.getSubVar(projectId = projectId, buildId = buildId, taskId = taskId)
    }

    override fun getBuildDetailUrl(projectId: String, pipelineId: String, buildId: String): Result<String> {
        return Result(pipelineUrlBean.genBuildDetailUrl(projectId, pipelineId, buildId, null, null, true))
    }
}
