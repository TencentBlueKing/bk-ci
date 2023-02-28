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

package com.tencent.devops.common.auth.api

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.devops.common.auth.api.pojo.AncestorsApiReq
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.EsbCreateApiReq
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.service.IamEsbService
import org.springframework.beans.factory.annotation.Autowired

class BluekingV3ResourceApi @Autowired constructor(
    val iamConfiguration: IamConfiguration,
    val iamEsbService: IamEsbService
) : AuthResourceApi {

    override fun createGrantResource(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String,
        authGroupList: List<BkAuthGroup>?
    ) {
        createResource(
            user = user,
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
    }

    override fun batchCreateResource(
        principalId: String,
        scopeType: String,
        scopeId: String,
        resourceType: AuthResourceType,
        resourceList: List<ResourceRegisterInfo>,
        systemId: AuthServiceCode
    ): Boolean {
        return true
    }

    override fun deleteResource(
        scopeType: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) = Unit

    override fun modifyResource(
        scopeType: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) = Unit

    override fun createResource(
        scopeType: String,
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        createResource(
            user = user,
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
    }

    override fun createResource(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        val ancestors = mutableListOf<AncestorsApiReq>()
        if (resourceType != AuthResourceType.PROJECT) {
            ancestors.add(AncestorsApiReq(
                system = iamConfiguration.systemId,
                id = projectCode,
                type = AuthResourceType.PROJECT.value
            ))
        }
        val iamApiReq = EsbCreateApiReq(
            creator = user,
            name = resourceName,
            id = resourceCode,
            type = resourceType.value,
            system = iamConfiguration.systemId,
            ancestors = ancestors,
            bk_app_code = "",
            bk_app_secret = "",
            bk_username = user
        )
        iamEsbService.createRelationResource(iamApiReq)
    }

    override fun modifyResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) = Unit

    override fun deleteResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) = Unit

    override fun cancelCreateResource(
        userId: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) = Unit

    override fun cancelUpdateResource(
        userId: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) = Unit

    override fun batchCreateResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        user: String,
        resourceList: List<ResourceRegisterInfo>
    ) = Unit
}
