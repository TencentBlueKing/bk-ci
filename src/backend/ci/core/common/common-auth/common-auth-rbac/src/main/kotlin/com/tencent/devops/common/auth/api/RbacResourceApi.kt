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
 *
 */

package com.tencent.devops.common.auth.api

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService

class RbacResourceApi(
    private val client: Client,
    private val tokenService: ClientTokenService
) : AuthResourceApi {
    override fun createResource(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            resourceType = RbacAuthUtils.extResourceType(authResourceType = resourceType),
            resourceCode = resourceCode,
            resourceName = resourceName,
            projectCode = projectCode
        )
    }

    override fun batchCreateResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        user: String,
        resourceList: List<ResourceRegisterInfo>
    ) {
        resourceList.forEach { resourceRegisterInfo ->
            client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
                token = tokenService.getSystemToken(null)!!,
                userId = user,
                resourceType = RbacAuthUtils.extResourceType(authResourceType = resourceType),
                resourceCode = resourceRegisterInfo.resourceCode,
                resourceName = resourceRegisterInfo.resourceName,
                projectCode = projectCode
            )
        }
    }

    override fun createGrantResource(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String,
        authGroupList: List<BkAuthGroup>?
    ) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            resourceType = RbacAuthUtils.extResourceType(authResourceType = resourceType),
            resourceCode = resourceCode,
            resourceName = resourceName,
            projectCode = projectCode
        )
    }

    override fun modifyResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenService.getSystemToken(null)!!,
            resourceType = RbacAuthUtils.extResourceType(authResourceType = resourceType),
            resourceCode = resourceCode,
            resourceName = resourceName,
            projectCode = projectCode
        )
    }

    override fun deleteResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenService.getSystemToken(null)!!,
            resourceType = RbacAuthUtils.extResourceType(authResourceType = resourceType),
            resourceCode = resourceCode,
            projectCode = projectCode
        )
    }

    override fun cancelCreateResource(
        userId: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) {
        client.get(ServicePermissionAuthResource::class).resourceCancelRelation(
            userId = userId,
            token = tokenService.getSystemToken(null)!!,
            resourceType = RbacAuthUtils.extResourceType(authResourceType = resourceType),
            resourceCode = resourceCode,
            projectCode = projectCode
        )
    }

    override fun cancelUpdateResource(
        userId: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) {
        client.get(ServicePermissionAuthResource::class).resourceCancelRelation(
            userId = userId,
            token = tokenService.getSystemToken(null)!!,
            resourceType = RbacAuthUtils.extResourceType(authResourceType = resourceType),
            resourceCode = resourceCode,
            projectCode = projectCode
        )
    }

    override fun createResource(
        scopeType: String,
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) = Unit

    override fun modifyResource(
        scopeType: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) = Unit

    override fun deleteResource(
        scopeType: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) = Unit

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
}
