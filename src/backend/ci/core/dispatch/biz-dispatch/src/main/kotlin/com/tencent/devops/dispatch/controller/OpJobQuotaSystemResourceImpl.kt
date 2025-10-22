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
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OpJobQuotaSystemResource
import com.tencent.devops.dispatch.pojo.JobQuotaSystem
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.dispatch.service.jobquota.JobQuotaBusinessService
import com.tencent.devops.dispatch.service.jobquota.JobQuotaManagerService
import com.tencent.devops.dispatch.utils.redis.JobQuotaRedisUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpJobQuotaSystemResourceImpl @Autowired constructor(
    private val jobQuotaManagerService: JobQuotaManagerService,
    private val jobQuotaBusinessService: JobQuotaBusinessService,
    private val jobQuotaRedisUtils: JobQuotaRedisUtils
) : OpJobQuotaSystemResource {
    override fun statistics(limit: Int?, offset: Int?): Result<Map<String, Any>> {
        return Result(jobQuotaBusinessService.statistics(limit, offset))
    }

    override fun list(): Result<List<JobQuotaSystem>> {
        return Result(jobQuotaManagerService.listSystemQuota())
    }

    override fun get(jobQuotaVmType: JobQuotaVmType, channelCode: String?): Result<List<JobQuotaSystem>> {
        return if (jobQuotaVmType == JobQuotaVmType.ALL) {
            Result(jobQuotaManagerService.listSystemQuota())
        } else {
            Result(listOf(jobQuotaManagerService.getSystemQuota(
                jobQuotaVmType,
                channelCode ?: ChannelCode.BS.name
            )))
        }
    }

    override fun add(jobQuota: JobQuotaSystem): Result<Boolean> {
        return Result(jobQuotaManagerService.addSystemQuota(jobQuota))
    }

    override fun delete(jobQuotaVmType: JobQuotaVmType, channelCode: String?): Result<Boolean> {
        return Result(jobQuotaManagerService.deleteSystemQuota(
            jobQuotaVmType,
            channelCode ?: ChannelCode.BS.name
        ))
    }

    override fun update(jobQuotaVmType: JobQuotaVmType, jobQuota: JobQuotaSystem): Result<Boolean> {
        return Result(jobQuotaManagerService.updateSystemQuota(jobQuotaVmType, jobQuota))
    }

    override fun restore(vmType: JobQuotaVmType): Result<Boolean> {
        jobQuotaRedisUtils.restoreProjectJobTime(null, vmType)
        return Result(true)
    }
}
