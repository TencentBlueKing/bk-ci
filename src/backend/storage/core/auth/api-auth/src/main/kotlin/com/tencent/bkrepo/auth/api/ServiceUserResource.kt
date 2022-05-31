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

package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.constant.AUTH_SERVICE_USER_PREFIX
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.User
import com.tencent.bkrepo.auth.pojo.user.UserInfo
import com.tencent.bkrepo.common.api.constant.AUTH_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@Api(tags = ["SERVICE_USER"], description = "服务-用户接口")
@Primary
@FeignClient(AUTH_SERVICE_NAME, contextId = "ServiceUserResource")
@RequestMapping(AUTH_SERVICE_USER_PREFIX)
interface ServiceUserResource {

    @ApiOperation("创建用户")
    @PostMapping("/create")
    fun createUser(
        @RequestBody request: CreateUserRequest
    ): Response<Boolean>

    @ApiOperation("用户详情")
    @GetMapping("/detail/{uid}")
    fun detail(
        @ApiParam(value = "用户id")
        @PathVariable uid: String
    ): Response<User?>

    @ApiOperation("新增用户所属角色")
    @PostMapping("/role/{uid}/{rid}")
    fun addUserRole(
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户角色id")
        @PathVariable rid: String
    ): Response<User?>

    @ApiOperation("校验用户token")
    @PostMapping("/token")
    fun checkToken(
        @ApiParam(value = "用户id")
        @RequestParam uid: String,
        @ApiParam(value = "用户token")
        @RequestParam token: String
    ): Response<Boolean>

    @ApiOperation("用户info ")
    @GetMapping("/userinfo/{uid}")
    fun userInfoById(
        @PathVariable uid: String
    ): Response<UserInfo?>
}
