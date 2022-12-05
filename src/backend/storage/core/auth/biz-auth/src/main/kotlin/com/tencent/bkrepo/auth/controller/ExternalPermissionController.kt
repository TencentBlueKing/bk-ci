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

package com.tencent.bkrepo.auth.controller

import com.tencent.bkrepo.auth.constant.AUTH_API_EXT_PERMISSION_PREFIX
import com.tencent.bkrepo.auth.pojo.externalPermission.CreateExtPermissionRequest
import com.tencent.bkrepo.auth.pojo.externalPermission.ExternalPermission
import com.tencent.bkrepo.auth.pojo.externalPermission.ListExtPermissionOption
import com.tencent.bkrepo.auth.pojo.externalPermission.UpdateExtPermissionRequest
import com.tencent.bkrepo.auth.service.ExternalPermissionService
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("用户-外部权限接口")
@RestController
@RequestMapping(AUTH_API_EXT_PERMISSION_PREFIX)
class ExternalPermissionController(
    private val externalPermissionService: ExternalPermissionService
) {

    @ApiOperation("创建外部权限")
    @PostMapping
    fun createExtPermission(
        @RequestBody request: CreateExtPermissionRequest
    ): Response<Void> {
        externalPermissionService.createExtPermission(request)
        return ResponseBuilder.success()
    }

    @ApiOperation("更新外部权限")
    @PutMapping
    fun updateExtPermission(
        @RequestBody request: UpdateExtPermissionRequest
    ): Response<Void> {
        externalPermissionService.updateExtPermission(request)
        return ResponseBuilder.success()
    }

    @ApiOperation("删除外部权限")
    @DeleteMapping("/{id}")
    fun deleteExtPermission(
        @PathVariable id: String
    ): Response<Void> {
        externalPermissionService.deleteExtPermission(id)
        return ResponseBuilder.success()
    }

    @ApiOperation("分页查询外部权限")
    @GetMapping
    fun listExtPermissionPage(
        listExtPermissionOption: ListExtPermissionOption
    ): Response<Page<ExternalPermission>> {
        return ResponseBuilder.success(externalPermissionService.listExtPermissionPage(listExtPermissionOption))
    }
}
