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

import com.tencent.bkrepo.auth.constant.AUTH_API_ROLE_PREFIX
import com.tencent.bkrepo.auth.pojo.role.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.role.Role
import com.tencent.bkrepo.auth.pojo.role.UpdateRoleRequest
import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.auth.resource.OpenResourceImpl
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.util.RequestUtil.buildProjectAdminRequest
import com.tencent.bkrepo.auth.util.RequestUtil.buildRepoAdminRequest
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import java.lang.Exception

@RestController
@RequestMapping(AUTH_API_ROLE_PREFIX)
class RoleController @Autowired constructor(
    private val roleService: RoleService,
    permissionService: PermissionService
) : OpenResourceImpl(permissionService) {

    @ApiOperation("创建角色")
    @PostMapping("/create")
    fun createRole(@RequestBody request: CreateRoleRequest): Response<String?> {
        // todo check request
        val id = roleService.createRole(request)
        return ResponseBuilder.success(id)
    }

    @ApiOperation("创建项目管理员")
    @PostMapping("/create/project/manage/{projectId}")
    fun createProjectManage(@PathVariable projectId: String): Response<String?> {
        val request = buildProjectAdminRequest(projectId)
        val id = roleService.createRole(request)
        return ResponseBuilder.success(id)
    }

    @ApiOperation("创建仓库管理员")
    @PostMapping("/create/repo/manage/{projectId}/{repoName}")
    fun createRepoManage(@PathVariable projectId: String, @PathVariable repoName: String): Response<String?> {
        val request = buildRepoAdminRequest(projectId, repoName)
        val id = roleService.createRole(request)
        return ResponseBuilder.success(id)
    }

    @ApiOperation("删除角色")
    @DeleteMapping("/delete/{id}")
    fun deleteRole(@PathVariable id: String): Response<Boolean> {
        roleService.deleteRoleByid(id)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("根据主键id查询角色详情")
    @GetMapping("/detail/{id}")
    fun detail(@PathVariable id: String): Response<Role?> {
        return ResponseBuilder.success(roleService.detail(id))
    }

    @ApiOperation("根据角色ID与项目Id查询角色")
    @GetMapping("/detail/{rid}/{projectId}")
    fun detailByProject(@PathVariable rid: String, @PathVariable projectId: String): Response<Role?> {
        val result = roleService.detail(rid, projectId)
        return ResponseBuilder.success(result)
    }

    @ApiOperation("根据角色ID与项目Id,仓库名查询角色")
    @GetMapping("/detail/{rid}/{projectId}/{repoName}")
    fun detailByProjectAndRepo(
        @PathVariable rid: String,
        @PathVariable projectId: String,
        @PathVariable repoName: String
    ): Response<Role?> {
        val result = roleService.detail(rid, projectId, repoName)
        return ResponseBuilder.success(result)
    }

    @ApiOperation("根据类型和项目id查询角色")
    @GetMapping("/list")
    fun listRole(@PathVariable projectId: String, @PathVariable repoName: String?): Response<List<Role>> {
        return ResponseBuilder.success(roleService.listRoleByProject(projectId, repoName))
    }

    @ApiOperation("查询用户组下用户列表")
    @GetMapping("/users/{id}")
    fun listUserByRole(@PathVariable id: String): Response<Set<UserResult>> {
        return ResponseBuilder.success(roleService.listUserByRoleId(id))
    }

    @ApiOperation("编辑用户组信息")
    @PutMapping("/{id}")
    @Transactional(rollbackFor = [Exception::class])
    fun updateRoleInfo(
        @PathVariable id: String,
        @RequestBody updateRoleRequest: UpdateRoleRequest
    ): Response<Boolean> {
        return ResponseBuilder.success(roleService.updateRoleInfo(id, updateRoleRequest))
    }
}
