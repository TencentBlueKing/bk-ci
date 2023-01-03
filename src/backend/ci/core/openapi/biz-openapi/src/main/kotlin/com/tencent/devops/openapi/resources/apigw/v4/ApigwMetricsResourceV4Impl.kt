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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.metrics.api.ServiceMetricsResource
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.openapi.api.apigw.v4.ApigwMetricsResourceV4
import com.tencent.devops.openapi.pojo.ApigwMetricsSummary
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwMetricsResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwMetricsResourceV4 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwMetricsResourceV4Impl::class.java)
    }

    override fun getSummaryInfo(projectId: String, userId: String): Result<ApigwMetricsSummary> {
        logger.info(
            "OPENAPI_METRICS_V4|$userId|get Summary by projectId id|$projectId"
        )
        return Result(
            ApigwMetricsSummary(
                overview = client.get(ServiceMetricsResource::class).queryPipelineSummaryInfo(
                    projectId = projectId,
                    userId = userId,
                    startTime = null,
                    endTime = null
                ).data,
                sumInfo = client.get(ServiceMetricsResource::class).queryPipelineSumInfo(
                    projectId = projectId,
                    userId = userId,
                    baseQueryReq = BaseQueryReqVO()
                ).data
            )
        )
    }
}
