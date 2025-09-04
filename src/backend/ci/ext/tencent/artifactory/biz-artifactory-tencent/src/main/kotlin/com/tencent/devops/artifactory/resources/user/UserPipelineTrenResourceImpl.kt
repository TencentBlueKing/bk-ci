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

package com.tencent.devops.artifactory.resources.user

import com.tencent.devops.artifactory.api.user.UserPipelineTrendResource
import com.tencent.devops.artifactory.pojo.TrendInfoDto
import com.tencent.devops.artifactory.service.ArtifactoryInfoService
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

@RestResource
class UserPipelineTrenResourceImpl @Autowired constructor(
    private val artifactoryInfoService: ArtifactoryInfoService
) : UserPipelineTrendResource {

    companion object {
        private val TIMEINTERVAL = TimeUnit.DAYS.toSeconds(90)
    }

    override fun constructApkAndIpaTrend(
        pipelineId: String,
        startTime: Long,
        endTime: Long,
        page: Int,
        pageSize: Int
    ): Result<TrendInfoDto> {
        if (pipelineId.isNullOrBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }

        val finishTime = checkTimeInterval(startTime, endTime)

        val result = artifactoryInfoService.queryArtifactoryInfo(pipelineId, startTime, finishTime)
        return Result(result)
    }

    // 查询时间区间最大为3个月
    private fun checkTimeInterval(startTime: Long, endTime: Long): Long {
        val finishTime: Long
        // 区间大于三个月需处理.endtime前推3个月.
        if (endTime - startTime > TIMEINTERVAL) {
            finishTime = endTime - TIMEINTERVAL
        } else {
            finishTime = endTime
        }
        return finishTime
    }
}
