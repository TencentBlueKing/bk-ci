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

package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.user.UserAuthResourceResource
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.vo.IamGroupInfoVo
import com.tencent.devops.auth.pojo.vo.IamGroupMemberInfoVo
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAuthResourceResourceImpl @Autowired constructor(
    private val permissionResourceService: PermissionResourceService,
    private val permissionResourceGroupService: PermissionResourceGroupService
) : UserAuthResourceResource {
    override fun hasManagerPermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<Boolean> {
        return Result(
            permissionResourceService.hasManagerPermission(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }

    override fun isEnablePermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<Boolean> {
        return Result(
            permissionResourceService.isEnablePermission(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }

    override fun listGroup(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        page: Int,
        pageSize: Int
    ): Result<Pagination<IamGroupInfoVo>> {
        return Result(
            permissionResourceGroupService.listGroup(
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun listUserBelongGroup(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<List<IamGroupMemberInfoVo>> {
        return Result(
            permissionResourceGroupService.listUserBelongGroup(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }

    override fun enable(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<Boolean> {
        return Result(
            permissionResourceService.enableResourcePermission(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }

    override fun disable(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<Boolean> {
        return Result(
            permissionResourceService.disableResourcePermission(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }

    override fun listResources(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceName: String?,
        page: Int,
        pageSize: Int
    ): Result<Pagination<AuthResourceInfo>> {
        return Result(
            permissionResourceService.listResources(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceName = resourceName,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getResource(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<AuthResourceInfo> {
        return Result(
            permissionResourceService.getResource(
                userId = userId,
                projectId = projectId,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        )
    }
}
