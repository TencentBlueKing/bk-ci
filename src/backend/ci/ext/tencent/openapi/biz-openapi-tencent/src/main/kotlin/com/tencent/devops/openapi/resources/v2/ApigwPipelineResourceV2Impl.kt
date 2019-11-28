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
package com.tencent.devops.openapi.resources.v2

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.external.measure.ServiceMeasureResource
import com.tencent.devops.openapi.api.v2.ApigwPipelineResourceV2
import com.tencent.devops.openapi.pojo.BuildStatisticsResponse
import com.tencent.devops.openapi.service.v2.ApigwPipelineServiceV2
import com.tencent.devops.process.api.v2.ServicePipelineResourceV2
import com.tencent.devops.process.pojo.Pipeline
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPipelineResourceV2Impl @Autowired constructor(
    private val client: Client,
    private val apigwPipelineResourceService: ApigwPipelineServiceV2
) : ApigwPipelineResourceV2 {

    override fun getListByOrganizationName(
        userId: String,
        organizationType: String,
        organizationName: String,
        deptName: String?,
        centerName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<Pipeline>> {
        return apigwPipelineResourceService.getListByOrganization(
            userId = userId,
            organizationType = organizationType,
            organizationName = organizationName,
            deptName = deptName,
            centerName = centerName,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getListByOrganizationId(
        userId: String,
        organizationType: String,
        organizationId: Long,
        deptName: String?,
        centerName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<Pipeline>> {
        return apigwPipelineResourceService.getListByOrganizationId(
            userId = userId,
            organizationType = organizationType,
            organizationId = organizationId,
            deptName = deptName,
            centerName = centerName,
            page = page,
            pageSize = pageSize,
            interfaceName = "/v2/pipelines/organizationIds"
        )
    }

    override fun getListByBuildResource(
        userId: String,
        buildResourceType: String,
        buildResourceValue: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<Pipeline>> {
        // 1.直接调Process微服务根据构建资源查流水线接口获取结果返回
        return client.getWithoutRetry(ServicePipelineResourceV2::class).listPipelinesByBuildResource(
            userId = userId,
            buildResourceType = buildResourceType,
            buildResourceValue = buildResourceValue,
            page = page,
            pageSize = pageSize,
            channelCode = ChannelCode.BS
        )
    }

    override fun buildStatistics(
        userId: String,
        organizationType: String,
        organizationId: Int,
        deptName: String?,
        centerName: String?,
        beginTime: String?,
        endTime: String?,
        type: String?
    ): Result<BuildStatisticsResponse> {
        // 1.直接调Measure微服务查询流水线构建统计数据
        return client.getExternalServiceWithoutRetry("measure", ServiceMeasureResource::class).buildStatistics(
            userId = userId,
            organizationType = organizationType,
            organizationId = organizationId,
            deptName = deptName,
            centerName = centerName,
            beginTime = beginTime,
            endTime = endTime,
            type = type
        )
    }
}