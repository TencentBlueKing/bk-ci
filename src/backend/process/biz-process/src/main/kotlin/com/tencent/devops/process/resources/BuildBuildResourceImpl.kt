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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.BuildBuildResource
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineVMBuildService
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildBuildResourceImpl @Autowired constructor(
    private val vmBuildService: PipelineVMBuildService,
    private val buildService: PipelineBuildService
) : BuildBuildResource {

    override fun setStarted(buildId: String, vmSeqId: String, vmName: String): Result<BuildVariables> {
        checkParam(buildId, vmSeqId, vmName)
        return Result(vmBuildService.buildVMStarted(buildId, vmSeqId, vmName))
    }

//    override fun setPluginStarted(buildId: String, vmSeqId: String, vmName: String): Result<BuildVariables> {
//        checkParam(buildId, vmSeqId, vmName)
//        return Result(vmBuildService.pluginStart(buildId, vmSeqId, vmName))
//    }

    override fun claimTask(buildId: String, vmSeqId: String, vmName: String): Result<BuildTask> {
        checkParam(buildId, vmSeqId, vmName)
        return Result(vmBuildService.buildClaimTask(buildId, vmSeqId, vmName))
    }

    override fun completeTask(
        buildId: String,
        vmSeqId: String,
        vmName: String,
        result: BuildTaskResult
    ): Result<Boolean> {
        checkParam(buildId, vmSeqId, vmName)
        vmBuildService.buildCompleteTask(buildId, vmSeqId, vmName, result)
        return Result(true)
    }

    override fun endTask(buildId: String, vmSeqId: String, vmName: String): Result<Boolean> {
        checkParam(buildId, vmSeqId, vmName)
        return Result(vmBuildService.buildEndTask(buildId, vmSeqId, vmName))
    }

    override fun heartbeat(buildId: String, vmSeqId: String, vmName: String): Result<Boolean> {
        checkParam(buildId, vmSeqId, vmName)
        return Result(vmBuildService.heartbeat(buildId, vmSeqId, vmName))
    }

    override fun getSingleHistoryBuild(
        projectId: String,
        pipelineId: String,
        buildNum: String,
        channelCode: ChannelCode?
    ): Result<BuildHistory?> {
        val history = buildService.getSingleHistoryBuild(
            projectId, pipelineId,
            buildNum.toInt(), channelCode ?: ChannelCode.BS
        )
        return Result(history)
    }

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
            buildService.getBuildDetail(
                projectId, pipelineId, buildId, channelCode,
                ChannelCode.isNeedAuth(channelCode)
            )
        )
    }

    private fun checkParam(buildId: String, vmSeqId: String, vmName: String) {
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (vmSeqId.isBlank()) {
            throw ParamBlankException("Invalid vmSeqId")
        }
        if (vmName.isBlank()) {
            throw ParamBlankException("Invalid vmName")
        }
    }
}
