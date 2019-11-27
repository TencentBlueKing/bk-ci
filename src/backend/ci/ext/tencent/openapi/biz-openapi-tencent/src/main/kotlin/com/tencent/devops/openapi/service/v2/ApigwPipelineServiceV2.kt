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
package com.tencent.devops.openapi.service.v2

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.ERROR_OPENAPI_INNER_SERVICE_FAIL
import com.tencent.devops.openapi.exception.MicroServiceInvokeFailure
import com.tencent.devops.process.api.v2.ServiceProjectPipelineResource
import com.tencent.devops.process.pojo.Pipeline
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/9/1
 * @Version 1.0
 */
@Service
class ApigwPipelineServiceV2(
    private val client: Client,
    private val organizationProjectService: OrganizationProjectService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwPipelineServiceV2::class.java)
    }

    fun getListByOrganizationId(
        userId: String,
        organizationType: String,
        organizationId: Long,
        deptName: String?,
        centerName: String?,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "ApigwPipelineServiceV2"
    ): Result<Page<Pipeline>> {
        logger.info("$interfaceName:getListByOrganizationId:Input($userId,$organizationType,$organizationId,$deptName,$centerName,$page,$pageSize)")
        val projectIds = organizationProjectService.getProjectIdsByOrganizationTypeAndId(
            userId = userId,
            organizationType = organizationType,
            organizationId = organizationId,
            deptName = deptName,
            centerName = centerName
        )
        return getPipelinesByProjectIds(
            userId = userId,
            projectIds = projectIds,
            page = page,
            pageSize = pageSize
        )
    }

    fun getListByOrganization(
        userId: String,
        organizationType: String,
        organizationName: String,
        deptName: String?,
        centerName: String?,
        page: Int?,
        pageSize: Int?,
        interfaceName: String? = "ApigwPipelineServiceV2"
    ): Result<Page<Pipeline>> {
        logger.info("$interfaceName:getListByOrganization:Input($userId,$organizationType,$organizationName,$deptName,$centerName,$page,$pageSize)")
        // 1.根据组织信息获取所有项目
        val projectIds = organizationProjectService.getProjectIdsByOrganizationTypeAndName(
            userId = userId,
            organizationType = organizationType,
            organizationName = organizationName,
            deptName = deptName,
            centerName = centerName,
            interfaceName = interfaceName
        )

        // 2.根据所有项目Id获取对应流水线
        return getPipelinesByProjectIds(
            userId = userId,
            projectIds = projectIds,
            page = page,
            pageSize = pageSize
        )
    }

    fun getPipelinesByProjectIds(
        userId: String,
        projectIds: Set<String>,
        page: Int?,
        pageSize: Int?
    ): Result<Page<Pipeline>> {
        val pipelinesResult = client.getWithoutRetry(ServiceProjectPipelineResource::class).listPipelinesByProjectIds(
            userId = userId,
            page = if (page == null || page <= 0) 1 else page,
            pageSize = if (pageSize == null || pageSize <= 0) 20 else pageSize,
            projectIds = projectIds
        )
        val resultStr = JsonUtil.toJson(pipelinesResult)
        if (pipelinesResult.isNotOk()) {
            val serviceInfo = "process:ServiceProjectPipelineResource:listPipelinesByProjectIds"
            throw MicroServiceInvokeFailure(
                serviceInterface = serviceInfo,
                message = "projectsResult=$resultStr",
                errorCode = ERROR_OPENAPI_INNER_SERVICE_FAIL,
                params = arrayOf(serviceInfo)
            )
        }
        return pipelinesResult
    }
}