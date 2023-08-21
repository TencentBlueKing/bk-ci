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

package com.tencent.devops.dispatch.docker.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.docker.api.builds.DispatchBuildDockerHostResource
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceOptionsVO
import com.tencent.devops.dispatch.docker.service.DispatchDockerService
import com.tencent.devops.dispatch.docker.service.DispatchDockerHostBuildService
import com.tencent.devops.dispatch.docker.service.DockerResourceOptionsService
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class DispatchBuildDockerHostResourceImpl @Autowired constructor(
    private val dispatchDockerHostBuildService: DispatchDockerHostBuildService,
    private val dispatchDockerService: DispatchDockerService,
    private val dockerResourceOptionsService: DockerResourceOptionsService
) : DispatchBuildDockerHostResource {

    override fun getResourceConfig(pipelineId: String, vmSeqId: String): Result<DockerResourceOptionsVO> {
        return Result(dockerResourceOptionsService.getDockerResourceConfig(pipelineId, vmSeqId))
    }

    override fun getQpcGitProjectList(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        poolNo: Int
    ): Result<List<String>> {
        return Result(dispatchDockerHostBuildService.getQpcGitProjectList(projectId, buildId, vmSeqId, poolNo))
    }

    override fun log(buildId: String, red: Boolean, message: String, tag: String?, jobId: String?): Result<Boolean>? {
        dispatchDockerHostBuildService.log(buildId, red, message, tag, jobId)
        return Result(0, "success")
    }

    override fun postLog(
        buildId: String,
        red: Boolean,
        message: String,
        tag: String?,
        jobId: String?
    ): Result<Boolean>? {
        dispatchDockerHostBuildService.log(buildId, red, message, tag, jobId)
        return Result(0, "success")
    }

    override fun getPublicImages(): Result<List<ImageRepoInfo>> {
        return dispatchDockerHostBuildService.getPublicImage()
    }

    override fun refresh(dockerIp: String, dockerIpInfoVO: DockerIpInfoVO): Result<Boolean> {
        return Result(dispatchDockerService.updateDockerIpLoad("dockerhost", dockerIp, dockerIpInfoVO))
    }
}
