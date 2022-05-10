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

package com.tencent.bkrepo.auth.controller

import com.tencent.bkrepo.auth.constant.AUTH_API_PERMISSION_PREFIX
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.resource.OpenPermissionImpl
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping

@RestController
@RequestMapping(AUTH_API_PERMISSION_PREFIX)
class PermissionController @Autowired constructor(
    private val permissionService: PermissionService
) : OpenPermissionImpl() {

    @ApiOperation("创建权限")
    @PostMapping("/create")
    fun createPermission(@RequestBody request: CreatePermissionRequest): Response<Boolean> {
        // todo check request
        return ResponseBuilder.success(permissionService.createPermission(request))
    }

    @ApiOperation("校验权限")
    @PostMapping("/check")
    fun checkPermission(@RequestBody request: CheckPermissionRequest): Response<Boolean> {
        checkRequest(request)
        return ResponseBuilder.success(permissionService.checkPermission(request))
    }

    @ApiOperation("list有权限仓库仓库")
    @GetMapping("/repo/list")
    fun listPermissionRepo(
        @RequestParam projectId: String,
        @RequestParam userId: String,
        @RequestParam appId: String?
    ): Response<List<String>> {
        return ResponseBuilder.success(permissionService.listPermissionRepo(projectId, userId, appId))
    }

    @ApiOperation("list有权限项目")
    @GetMapping("/project/list")
    fun listPermissionProject(@RequestParam userId: String): Response<List<String>> {
        return ResponseBuilder.success(permissionService.listPermissionProject(userId))
    }

    @ApiOperation("权限列表")
    @GetMapping("/list")
    fun listPermission(@RequestParam projectId: String, @RequestParam repoName: String?): Response<List<Permission>> {
        return ResponseBuilder.success(permissionService.listPermission(projectId, repoName))
    }

    @ApiOperation("获取仓库内置权限列表")
    @GetMapping("/list/inrepo")
    fun listRepoBuiltinPermission(
        @RequestParam projectId: String,
        @RequestParam repoName: String
    ): Response<List<Permission>> {
        return ResponseBuilder.success(permissionService.listBuiltinPermission(projectId, repoName))
    }

    @ApiOperation("删除权限")
    @DeleteMapping("/delete/{id}")
    fun deletePermission(@PathVariable id: String): Response<Boolean> {
        return ResponseBuilder.success(permissionService.deletePermission(id))
    }

    @ApiOperation("更新权限include path")
    @PutMapping("/includePath")
    fun updateIncludePermissionPath(@RequestBody request: UpdatePermissionPathRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateIncludePath(request))
    }

    @ApiOperation("更新权限exclude path")
    @PutMapping("/excludePath")
    fun updateExcludePermissionPath(@RequestBody request: UpdatePermissionPathRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateExcludePath(request))
    }

    @ApiOperation("更新权限权限绑定repo")
    @PutMapping("/repo")
    fun updatePermissionRepo(@RequestBody request: UpdatePermissionRepoRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updateRepoPermission(request))
    }

    @ApiOperation("更新权限绑定用户")
    @PutMapping("/user")
    fun updatePermissionUser(@RequestBody request: UpdatePermissionUserRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updatePermissionUser(request))
    }

    @ApiOperation("更新权限绑定角色")
    @PutMapping("/role")
    fun updatePermissionRole(@RequestBody request: UpdatePermissionRoleRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updatePermissionRole(request))
    }

    @ApiOperation("更新权限绑定部门")
    @PutMapping("/department")
    fun updatePermissionDepartment(@RequestBody request: UpdatePermissionDepartmentRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updatePermissionDepartment(request))
    }

    @ApiOperation("更新权限绑定动作")
    @PutMapping("/action")
    fun updatePermissionAction(@RequestBody request: UpdatePermissionActionRequest): Response<Boolean> {
        return ResponseBuilder.success(permissionService.updatePermissionAction(request))
    }
}
