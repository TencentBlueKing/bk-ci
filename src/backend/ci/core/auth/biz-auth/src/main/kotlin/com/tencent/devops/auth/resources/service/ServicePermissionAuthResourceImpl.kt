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

package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO
import com.tencent.devops.auth.service.iam.PermissionExtService
import com.tencent.devops.auth.service.iam.PermissionGrantService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePermissionAuthResourceImpl @Autowired constructor(
    val permissionService: PermissionService,
    val permissionExtService: PermissionExtService,
    val permissionGrantService: PermissionGrantService
) : ServicePermissionAuthResource {

    override fun validateUserActionPermission(
        userId: String,
        token: String,
        type: String?,
        action: String
    ): Result<Boolean> {
        return Result(permissionService.validateUserActionPermission(userId, action))
    }

    override fun validateUserResourcePermission(
        userId: String,
        token: String,
        type: String?,
        action: String,
        projectCode: String,
        resourceCode: String?
    ): Result<Boolean> {
        return Result(permissionService.validateUserResourcePermission(userId, action, projectCode, resourceCode))
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        token: String,
        type: String?,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Result<Boolean> {
        return Result(
            permissionService.validateUserResourcePermissionByRelation(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceCode = resourceCode,
                resourceType = resourceType,
                relationResourceType = relationResourceType
            )
        )
    }

    override fun validateUserResourcePermissionByInstance(
        userId: String,
        token: String,
        type: String?,
        action: String,
        projectCode: String,
        resource: AuthResourceInstance
    ): Result<Boolean> {
        return Result(
            permissionService.validateUserResourcePermissionByInstance(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resource = resource
            )
        )
    }

    override fun batchValidateUserResourcePermissionByRelation(
        userId: String,
        token: String,
        type: String?,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?,
        action: List<String>
    ): Result<Boolean> {
        var actionCheckPermission = true
        action.forEach {
            val checkActionPermission = permissionService.validateUserResourcePermissionByRelation(
                userId = userId,
                action = it,
                projectCode = projectCode,
                resourceCode = resourceCode,
                resourceType = resourceType,
                relationResourceType = relationResourceType
            )
            if (!checkActionPermission) {
                actionCheckPermission = false
                return@forEach
            }
        }
        return Result(actionCheckPermission)
    }

    override fun getUserResourceByPermission(
        userId: String,
        token: String,
        type: String?,
        action: String,
        projectCode: String,
        resourceType: String
    ): Result<List<String>> {
        return Result(
            permissionService.getUserResourceByAction(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceType = resourceType
            )
        )
    }

    override fun getUserResourcesByPermissions(
        userId: String,
        token: String,
        type: String?,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Result<Map<AuthPermission, List<String>>> {
        return Result(
            permissionService.getUserResourcesByActions(
                userId = userId,
                actions = actions,
                projectCode = projectCode,
                resourceType = resourceType
            )
        )
    }

    override fun filterUserResourcesByPermissions(
        userId: String,
        token: String,
        type: String?,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resources: List<AuthResourceInstance>
    ): Result<Map<AuthPermission, List<String>>> {
        return Result(
            permissionService.filterUserResourcesByActions(
                userId = userId,
                actions = actions,
                projectCode = projectCode,
                resourceType = resourceType,
                resources = resources
            )
        )
    }

    override fun getUserResourceAndParentByPermission(
        userId: String,
        token: String,
        type: String?,
        action: String,
        projectCode: String,
        resourceType: String
    ): Result<Map<String, List<String>>> {
        return Result(
            permissionService.getUserResourceAndParentByPermission(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceType = resourceType
            )
        )
    }

    override fun resourceCreateRelation(
        userId: String,
        token: String,
        type: String?,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Result<Boolean> {
        return Result(
            permissionExtService.resourceCreateRelation(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName
            )
        )
    }

    override fun resourceModifyRelation(
        token: String,
        type: String?,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Result<Boolean> {
        return Result(
            permissionExtService.resourceModifyRelation(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName
            )
        )
    }

    override fun resourceDeleteRelation(
        token: String,
        type: String?,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Result<Boolean> {
        return Result(
            permissionExtService.resourceDeleteRelation(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }

    override fun resourceCancelRelation(
        userId: String,
        token: String,
        type: String?,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Result<Boolean> {
        return Result(
            permissionExtService.resourceCancelRelation(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }

    override fun grantInstancePermission(
        userId: String,
        token: String,
        projectCode: String,
        grantInstance: GrantInstanceDTO
    ): Result<Boolean> {
        return Result(
            permissionGrantService.grantInstancePermission(
                projectId = projectCode,
                grantInfo = grantInstance
            )
        )
    }
}
