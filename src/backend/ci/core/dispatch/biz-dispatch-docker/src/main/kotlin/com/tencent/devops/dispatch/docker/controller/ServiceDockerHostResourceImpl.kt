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

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.docker.api.service.ServiceDockerHostResource
import com.tencent.devops.dispatch.docker.pojo.DockerHostZone
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.SpecialDockerHostVO
import com.tencent.devops.dispatch.docker.service.DispatchDockerService
import com.tencent.devops.dispatch.docker.service.DockerHostBuildService
import com.tencent.devops.dispatch.docker.service.DockerHostZoneTaskService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource@Suppress("ALL")
class ServiceDockerHostResourceImpl @Autowired constructor(
    private val dockerHostBuildService: DockerHostBuildService,
    private val dispatchDockerService: DispatchDockerService,
    private val dockerHostZoneTaskService: DockerHostZoneTaskService
) : ServiceDockerHostResource {
    override fun list(page: Int?, pageSize: Int?): Page<DockerHostZone> {
        checkParams(page, pageSize)
        val realPage = page ?: 1
        val realPageSize = pageSize ?: 20
        val dockerHostList = dockerHostZoneTaskService.list(realPage, realPageSize)
        val count = dockerHostZoneTaskService.count()
        return Page(
            page = realPage,
            pageSize = realPageSize,
            count = count.toLong(),
            records = dockerHostList
        )
    }

    override fun updateContainerId(buildId: String, vmSeqId: Int, containerId: String): Result<Boolean> {
        dockerHostBuildService.updateContainerId(buildId, vmSeqId, containerId)
        return Result(true)
    }

    override fun refresh(dockerIp: String, dockerIpInfoVO: DockerIpInfoVO): Result<Boolean> {
        return Result(dispatchDockerService.updateBuildLessStatus("buildless", dockerIp, dockerIpInfoVO))
    }

    override fun createSpecialDockerHost(
        userId: String,
        specialDockerHostVOs: List<SpecialDockerHostVO>
    ): Result<Boolean> {
        return Result(dockerHostZoneTaskService.create(userId, specialDockerHostVOs))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceDockerHostResourceImpl::class.java)
    }

    fun checkParams(page: Int?, pageSize: Int?) {
        if (page != null && page < 1) {
            throw ParamBlankException("Invalid page")
        }
        if (pageSize != null && pageSize < 1) {
            throw ParamBlankException("Invalid pageSize")
        }
    }
}
