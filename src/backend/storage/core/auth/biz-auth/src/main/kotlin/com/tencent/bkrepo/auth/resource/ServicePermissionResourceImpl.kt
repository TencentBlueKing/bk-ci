/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.resource

import com.tencent.bkrepo.auth.api.ServicePermissionResource
import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.ListRepoPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
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

    override fun listRepoPermission(request: ListRepoPermissionRequest): Response<List<String>> {
        checkRequest(request)
        return ResponseBuilder.success(permissionService.listRepoPermission(request))
    }

    override fun listPermission(projectId: String, repoName: String?): Response<List<Permission>> {
        return ResponseBuilder.success(permissionService.listPermission(projectId, repoName))
    }

    override fun listRepoBuiltinPermission(projectId: String, repoName: String): Response<List<Permission>> {
        return ResponseBuilder.success(permissionService.listBuiltinPermission(projectId, repoName))
    }

    override fun deletePermission(id: String): Response<Boolean> {
        return ResponseBuilder.success(permissionService.deletePermission(id))
    }

    override fun updateIncludePermissionPath(request: UpdatePermissionPathRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateIncludePath(request))
    }

    override fun updateExcludePermissionPath(request: UpdatePermissionPathRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateExcludePath(request))
    }

    override fun updatePermissionRepo(request: UpdatePermissionRepoRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateRepoPermission(request))
    }

    override fun updatePermissionUser(request: UpdatePermissionUserRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updatePermissionUser(request))
    }

    override fun updatePermissionRole(request: UpdatePermissionRoleRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updatePermissionRole(request))
    }

    override fun updatePermissionDepartment(request: UpdatePermissionDepartmentRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updatePermissionDepartment(request))
    }

    override fun updatePermissionAction(request: UpdatePermissionActionRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updatePermissionAction(request))
    }

    override fun registerResource(request: RegisterResourceRequest): Response<Boolean> {
        permissionService.registerResource(request)
        return ResponseBuilder.success()
    }
}
