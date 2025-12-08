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
 *
 */

package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServiceAuthAuthorizationResource
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationConditionRequest
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationResponse
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthAuthorizationResourceV4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthAuthorizationResourceV4Impl @Autowired constructor(
    val client: Client
) : ApigwAuthAuthorizationResourceV4 {
    companion object {
        val logger = LoggerFactory.getLogger(ApigwAuthAuthorizationResourceV4Impl::class.java)!!
    }

    override fun getResourceAuthorization(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<ResourceAuthorizationResponse> {
        logger.info(
            "OPENAPI_AUTH_AUTHORIZATION_RESOURCE_V4 getResourceAuthorization" +
                "|$appCode|$projectId|$resourceType|$resourceCode"
        )
        return client.get(ServiceAuthAuthorizationResource::class).getResourceAuthorization(
            projectId = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }

    override fun listResourceAuthorization(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        condition: ResourceAuthorizationConditionRequest
    ): Result<SQLPage<ResourceAuthorizationResponse>> {
        logger.info(
            "OPENAPI_AUTH_AUTHORIZATION_RESOURCE_V4 listResourceAuthorization" +
                "|$appCode|$projectId|${condition.resourceType}"
        )
        return client.get(ServiceAuthAuthorizationResource::class).listResourceAuthorization(
            projectId = projectId,
            condition = condition
        )
    }

    override fun batchModifyHandoverFrom(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        resourceAuthorizationHandoverList: List<ResourceAuthorizationHandoverDTO>
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_AUTH_AUTHORIZATION_RESOURCE_V4 batchModifyHandoverFrom" +
                "|$appCode|$projectId|${resourceAuthorizationHandoverList.size}"
        )
        return client.get(ServiceAuthAuthorizationResource::class).batchModifyHandoverFrom(
            projectId = projectId,
            resourceAuthorizationHandoverList = resourceAuthorizationHandoverList
        )
    }
}
