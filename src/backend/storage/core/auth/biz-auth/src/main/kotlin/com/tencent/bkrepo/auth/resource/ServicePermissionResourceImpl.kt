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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.auth.resource

import com.tencent.bkrepo.auth.api.ServicePermissionResource
import com.tencent.bkrepo.auth.pojo.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.Permission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class ServicePermissionResourceImpl @Autowired constructor(
    private val permissionService: PermissionService,
    private val userService: UserService
) : ServicePermissionResource, AbstractPermissionResourceImpl() {

    override fun createPermission(request: CreatePermissionRequest): Response<Boolean> {
        // todo check request
        return ResponseBuilder.success(permissionService.createPermission(request))
    }

    override fun checkAdmin(uid: String): Response<Boolean> {
        // todo check request
        val userInfo = userService.getUserById(uid) ?: return ResponseBuilder.success(false)
        if (!userInfo.admin) {
            return ResponseBuilder.success(false)
        }
        return ResponseBuilder.success(true)
    }

    override fun checkPermission(request: CheckPermissionRequest): Response<Boolean> {
        checkRequest(request)
        return ResponseBuilder.success(permissionService.checkPermission(request))
    }

    override fun listPermission(resourceType: ResourceType?, projectId: String?, repoName: String?): Response<List<Permission>> {
        return ResponseBuilder.success(permissionService.listPermission(resourceType, projectId, repoName))
    }

    override fun deletePermission(id: String): Response<Boolean> {
        return ResponseBuilder.success(permissionService.deletePermission(id))
    }

    override fun updateIncludePermissionPath(id: String, pathList: List<String>): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateIncludePath(id, pathList))
    }

    override fun updateExcludePermissionPath(id: String, pathList: List<String>): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateExcludePath(id, pathList))
    }

    override fun updatePermissionRepo(id: String, repoList: List<String>): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateRepoPermission(id, repoList))
    }

    override fun updatePermissionUser(id: String, uid: String, actionList: List<PermissionAction>): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateUserPermission(id, uid, actionList))
    }

    override fun removePermissionUser(id: String, uid: String): Response<Boolean> {
        return ResponseBuilder.success(permissionService.removeUserPermission(id, uid))
    }

    override fun updatePermissionRole(id: String, rid: String, actionList: List<PermissionAction>): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateRolePermission(id, rid, actionList))
    }

    override fun removePermissionRole(id: String, rid: String): Response<Boolean> {
        return ResponseBuilder.success(permissionService.removeRolePermission(id, rid))
    }
}
