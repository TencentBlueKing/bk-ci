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

package com.tencent.devops.metrics.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.metrics.api.UserPipelineStageResource
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.checkDateInterval
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.getEndDateTime
import com.tencent.devops.metrics.utils.QueryParamCheckUtil.getStartDateTime
import com.tencent.devops.metrics.service.PipelineStageManageService
import com.tencent.devops.metrics.pojo.dto.QueryPipelineOverviewDTO
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.StageTrendSumInfoVO
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineStageResourceImpl @Autowired constructor(
    private val pipelineStageManageService: PipelineStageManageService
) : UserPipelineStageResource {
    override fun queryPipelineStageTrendInfo(
        projectId: String,
        userId: String,
        baseQueryReq: BaseQueryReqVO?
    ): Result<List<StageTrendSumInfoVO>> {
        val queryReq = baseQueryReq ?: BaseQueryReqVO()
        if (queryReq.startTime.isNullOrBlank()) { queryReq.startTime = getStartDateTime() }
        if (queryReq.endTime.isNullOrBlank()) { queryReq.endTime = getEndDateTime() }
        checkDateInterval(queryReq.startTime!!, queryReq.endTime!!)
        return Result(
            pipelineStageManageService.queryPipelineStageTrendInfo(
                QueryPipelineOverviewDTO(
                    projectId = projectId,
                    userId = userId,
                    baseQueryReq = queryReq
                )
            )
        )
    }
}
