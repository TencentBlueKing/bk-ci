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

package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AUTH_API_PERMISSION_PREFIX
import com.tencent.bkrepo.auth.constant.AUTH_PERMISSION_PREFIX
import com.tencent.bkrepo.auth.constant.AUTH_SERVICE_PERMISSION_PREFIX
import com.tencent.bkrepo.auth.constant.SERVICE_NAME
import com.tencent.bkrepo.auth.pojo.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.Permission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api(tags = ["SERVICE_PERMISSION"], description = "服务-权限接口")
@FeignClient(SERVICE_NAME, contextId = "ServicePermissionResource")
@RequestMapping(AUTH_PERMISSION_PREFIX, AUTH_API_PERMISSION_PREFIX, AUTH_SERVICE_PERMISSION_PREFIX)
interface ServicePermissionResource {

    @ApiOperation("权限列表")
    @GetMapping("/list")
    fun listPermission(
        @ApiParam(value = "权限类型")
        @RequestParam resourceType: ResourceType?,
        @ApiParam(value = "项目ID")
        @RequestParam projectId: String?,
        @ApiParam(value = "仓库名称")
        @RequestParam repoName: String?
    ): Response<List<Permission>>

    @ApiOperation("校验管理员")
    @GetMapping("/checkAdmin/{uid}")
    fun checkAdmin(
        @ApiParam(value = "用户名")
        @PathVariable uid: String
    ): Response<Boolean>

    @ApiOperation("校验权限")
    @PostMapping("/check")
    fun checkPermission(
        @ApiParam(value = "校验权限信息")
        @RequestBody request: CheckPermissionRequest
    ): Response<Boolean>

    @ApiOperation("创建权限")
    @PostMapping("/create")
    fun createPermission(
        @RequestBody request: CreatePermissionRequest
    ): Response<Boolean>

    @ApiOperation("删除权限")
    @DeleteMapping("/delete/{id}")
    fun deletePermission(
        @ApiParam(value = "权限主键ID")
        @PathVariable id: String
    ): Response<Boolean>

    @ApiOperation("更新权限include path")
    @PutMapping("/includePath/{id}")
    fun updateIncludePermissionPath(
        @ApiParam(value = "权限主键ID")
        @PathVariable id: String,
        @RequestBody pathList: List<String>
    ): Response<Boolean>

    @ApiOperation("更新权限exclude path")
    @PutMapping("/excludePath/{id}")
    fun updateExcludePermissionPath(
        @ApiParam(value = "权限主键ID")
        @PathVariable id: String,
        @RequestBody pathList: List<String>
    ): Response<Boolean>

    @ApiOperation("更新权限权限绑定repo")
    @PutMapping("/repo/{id}")
    fun updatePermissionRepo(
        @ApiParam(value = "权限主键ID")
        @PathVariable id: String,
        @RequestBody repoList: List<String>
    ): Response<Boolean>

    @ApiOperation("更新权限绑定用户")
    @PostMapping("/user/{id}/{uid}")
    fun updatePermissionUser(
        @ApiParam(value = "权限主键ID")
        @PathVariable id: String,
        @ApiParam(value = "用户ID")
        @PathVariable uid: String,
        @RequestBody actionList: List<PermissionAction>
    ): Response<Boolean>

    @ApiOperation("删除权限绑定用户")
    @DeleteMapping("/user/{id}/{uid}")
    fun removePermissionUser(
        @ApiParam(value = "权限主键ID")
        @PathVariable id: String,
        @ApiParam(value = "用户ID")
        @PathVariable uid: String
    ): Response<Boolean>

    @ApiOperation("更新权限绑定角色")
    @PostMapping("/role/{id}/{rid}")
    fun updatePermissionRole(
        @ApiParam(value = "权限主键ID")
        @PathVariable id: String,
        @ApiParam(value = "用户ID")
        @PathVariable rid: String,
        @RequestBody actionList: List<PermissionAction>
    ): Response<Boolean>

    @ApiOperation("删除权限绑定角色")
    @DeleteMapping("/role/{id}/{rid}")
    fun removePermissionRole(
        @ApiParam(value = "权限主键ID")
        @PathVariable id: String,
        @ApiParam(value = "角色ID")
        @PathVariable rid: String
    ): Response<Boolean>
}
