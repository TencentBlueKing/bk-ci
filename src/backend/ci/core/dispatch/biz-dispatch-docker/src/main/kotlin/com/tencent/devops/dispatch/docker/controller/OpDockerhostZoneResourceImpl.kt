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
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.docker.api.op.OpDockerHostZoneResource
import com.tencent.devops.dispatch.docker.pojo.DockerHostZoneWithPage
import com.tencent.devops.dispatch.docker.pojo.SpecialDockerHostVO
import com.tencent.devops.dispatch.docker.service.DockerHostZoneTaskService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpDockerhostZoneResourceImpl @Autowired constructor(
    private val dockerHostZoneTaskService: DockerHostZoneTaskService
) : OpDockerHostZoneResource {
    override fun create(hostIp: String, zone: Zone, remark: String?): Result<Boolean> {
        dockerHostZoneTaskService.create(hostIp, zone.toString(), remark)
        return Result(true)
    }

    override fun delete(hostIp: String): Result<Boolean> {
        dockerHostZoneTaskService.delete(hostIp)
        return Result(true)
    }

    override fun list(page: Int, pageSize: Int): Result<DockerHostZoneWithPage> {
        return Result(DockerHostZoneWithPage(
            total = dockerHostZoneTaskService.count(),
            data = dockerHostZoneTaskService.list(page, pageSize)
        ))
    }

    override fun enable(hostIp: String, enable: Boolean): Result<Boolean> {
        dockerHostZoneTaskService.enable(hostIp, enable)
        return Result(true)
    }

    override fun listSpecialDockerHost(userId: String): Result<List<SpecialDockerHostVO>> {
        return Result(dockerHostZoneTaskService.listSpecialDockerHosts(userId))
    }

    override fun createSpecialDockerHost(
        userId: String,
        specialDockerHostVOs: List<SpecialDockerHostVO>
    ): Result<Boolean> {
        return Result(dockerHostZoneTaskService.create(userId, specialDockerHostVOs))
    }

    override fun updateSpecialDockerHost(userId: String, specialDockerHostVO: SpecialDockerHostVO): Result<Boolean> {
        return Result(dockerHostZoneTaskService.update(userId, specialDockerHostVO))
    }

    override fun deleteSpecialDockerHost(userId: String, projectId: String): Result<Boolean> {
        return Result(dockerHostZoneTaskService.delete(userId, projectId))
    }
}
