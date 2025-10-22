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

package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OpJobQuotaProjectResource
import com.tencent.devops.dispatch.pojo.JobQuotaProject
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.service.jobquota.JobQuotaManagerService
import com.tencent.devops.dispatch.utils.redis.JobQuotaRedisUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpJobQuotaProjectResourceImpl @Autowired constructor(
    private val jobQuotaManagerService: JobQuotaManagerService,
    private val jobQuotaRedisUtils: JobQuotaRedisUtils
) : OpJobQuotaProjectResource {
    override fun list(projectId: String?): Result<List<JobQuotaProject>> {
        return Result(jobQuotaManagerService.listProjectQuota(projectId))
    }

    override fun get(
        projectId: String,
        vmType: JobQuotaVmType,
        channelCode: String
    ): Result<JobQuotaProject> {
        return Result(jobQuotaManagerService.getProjectQuota(projectId, vmType, channelCode))
    }

    override fun add(projectId: String, jobQuota: JobQuotaProject): Result<Boolean> {
        return Result(jobQuotaManagerService.addProjectQuota(projectId, jobQuota))
    }

    override fun delete(projectId: String, vmType: JobQuotaVmType, channelCode: String): Result<Boolean> {
        return Result(jobQuotaManagerService.deleteProjectQuota(projectId, vmType, channelCode))
    }

    override fun update(projectId: String, vmType: JobQuotaVmType, jobQuota: JobQuotaProject): Result<Boolean> {
        return Result(jobQuotaManagerService.updateProjectQuota(projectId, vmType, jobQuota))
    }

    override fun restoreProjectRunningJobs(
        projectId: String,
        vmType: JobQuotaVmType,
        createTime: String,
        channelCode: String
    ): Result<Boolean> {
        jobQuotaManagerService.clearRunningJobs(projectId, vmType, createTime, channelCode)
        return Result(true)
    }
}
