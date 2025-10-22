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

package com.tencent.devops.dispatch.docker.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.docker.api.op.OPDispatchDockerResource
import com.tencent.devops.dispatch.docker.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.DockerIpListPage
import com.tencent.devops.dispatch.docker.pojo.DockerIpUpdateVO
import com.tencent.devops.dispatch.docker.pojo.HostDriftLoad
import com.tencent.devops.dispatch.docker.service.DispatchDockerService

@RestResource
class OPDispatchDockerResourceImpl constructor(
    private val dispatchDockerService: DispatchDockerService
) : OPDispatchDockerResource {

    override fun listDispatchDocker(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<DockerIpListPage<DockerIpInfoVO>> {
        return Result(dispatchDockerService.list(userId, page, pageSize))
    }

    override fun createDispatchDocker(userId: String, dockerIpInfoVOs: List<DockerIpInfoVO>): Result<Boolean> {
        return Result(dispatchDockerService.create(userId, dockerIpInfoVOs))
    }

    override fun updateDispatchDocker(
        userId: String,
        dockerIp: String,
        dockerIpUpdateVO: DockerIpUpdateVO
    ): Result<Boolean> {
        return Result(dispatchDockerService.update(userId, dockerIp, dockerIpUpdateVO))
    }

    override fun updateAllDispatchDockerEnable(userId: String): Result<Boolean> {
        return Result(dispatchDockerService.updateAllDispatchDockerEnable(userId))
    }

    override fun deleteDispatchDocker(userId: String, dockerIp: String): Result<Boolean> {
        return Result(dispatchDockerService.delete(userId, dockerIp))
    }

    override fun removeDockerBuildBinding(userId: String, pipelineId: String, vmSeqId: String): Result<Boolean> {
        return Result(dispatchDockerService.removeDockerBuildBinding(userId, pipelineId, vmSeqId))
    }

    override fun getDockerHostLoadConfig(userId: String): Result<Map<String, DockerHostLoadConfig>> {
        return Result(dispatchDockerService.getDockerHostLoadConfig(userId))
    }

    override fun createDockerHostLoadConfig(
        userId: String,
        dockerHostLoadConfigMap: Map<String, DockerHostLoadConfig>
    ): Result<Boolean> {
        return Result(dispatchDockerService.createDockerHostLoadConfig(userId, dockerHostLoadConfigMap))
    }

    override fun getDockerDriftThreshold(userId: String): Result<Map<String, String>> {
        return Result(dispatchDockerService.getDockerDriftThreshold(userId))
    }

    override fun updateDockerDriftThreshold(userId: String, hostDriftLoad: HostDriftLoad): Result<Boolean> {
        return Result(dispatchDockerService.updateDockerDriftThreshold(userId, hostDriftLoad))
    }
}
