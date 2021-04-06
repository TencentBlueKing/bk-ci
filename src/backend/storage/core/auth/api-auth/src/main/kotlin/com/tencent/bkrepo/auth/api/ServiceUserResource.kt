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

import com.tencent.bkrepo.auth.constant.AUTH_API_USER_PREFIX
import com.tencent.bkrepo.auth.constant.AUTH_SERVICE_USER_PREFIX
import com.tencent.bkrepo.auth.constant.AUTH_USER_PREFIX
import com.tencent.bkrepo.auth.pojo.token.Token
import com.tencent.bkrepo.auth.pojo.token.TokenResult
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToProjectRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToRepoRequest
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.user.User
import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.common.api.constant.AUTH_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Api(tags = ["SERVICE_USER"], description = "服务-用户接口")
@Primary
@FeignClient(AUTH_SERVICE_NAME, contextId = "ServiceUserResource")
@RequestMapping(AUTH_USER_PREFIX, AUTH_API_USER_PREFIX, AUTH_SERVICE_USER_PREFIX)
interface ServiceUserResource {

    @ApiOperation("创建项目用户")
    @PostMapping("/create/project")
    fun createUserToProject(
        @RequestBody request: CreateUserToProjectRequest
    ): Response<Boolean>

    @ApiOperation("创建仓库用户")
    @PostMapping("/create/repo")
    fun createUserToRepo(
        @RequestBody request: CreateUserToRepoRequest
    ): Response<Boolean>

    @ApiOperation("创建用户")
    @PostMapping("/create")
    fun createUser(
        @RequestBody request: CreateUserRequest
    ): Response<Boolean>

    @ApiOperation("用户列表")
    @GetMapping("/list")
    fun listUser(
        @RequestBody rids: List<String>?
    ): Response<List<UserResult>>

    @ApiOperation("用户列表")
    @GetMapping("/listall")
    fun listAllUser(
        @RequestBody rids: List<String>?
    ): Response<List<User>>

    @ApiOperation("删除用户")
    @DeleteMapping("/{uid}")
    fun deleteById(
        @ApiParam(value = "用户id")
        @PathVariable uid: String
    ): Response<Boolean>

    @ApiOperation("用户详情")
    @GetMapping("/detail/{uid}")
    fun detail(
        @ApiParam(value = "用户id")
        @PathVariable uid: String
    ): Response<User?>

    @ApiOperation("更新用户信息")
    @PutMapping("/{uid}")
    fun updateById(
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户更新信息")
        @RequestBody request: UpdateUserRequest
    ): Response<Boolean>

    @ApiOperation("新增用户所属角色")
    @PostMapping("/role/{uid}/{rid}")
    fun addUserRole(
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户角色id")
        @PathVariable rid: String
    ): Response<User?>

    @ApiOperation("删除用户所属角色")
    @DeleteMapping("/role/{uid}/{rid}")
    fun removeUserRole(
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户角色")
        @PathVariable rid: String
    ): Response<User?>

    @ApiOperation("批量新增用户所属角色")
    @PatchMapping("/role/add/{rid}")
    fun addUserRoleBatch(
        @ApiParam(value = "用户角色Id")
        @PathVariable rid: String,
        @ApiParam(value = "用户id集合")
        @RequestBody request: List<String>
    ): Response<Boolean>

    @ApiOperation("批量删除用户所属角色")
    @PatchMapping("/role/delete/{rid}")
    fun deleteUserRoleBatch(
        @ApiParam(value = "用户角色Id")
        @PathVariable rid: String,
        @ApiParam(value = "用户id集合")
        @RequestBody request: List<String>
    ): Response<Boolean>

    @ApiOperation("创建用户token")
    @PostMapping("/token/{uid}")
    fun createToken(
        @ApiParam(value = "用户id")
        @PathVariable uid: String
    ): Response<Token?>

    @ApiOperation("新加用户token")
    @PostMapping("/token/{uid}/{name}")
    fun addUserToken(
        @ApiParam(value = "用户id")
        @PathVariable("uid") uid: String,
        @ApiParam(value = "name")
        @PathVariable("name") name: String,
        @ApiParam(value = "expiredAt", required = false)
        @RequestParam expiredAt: String?,
        @ApiParam(value = "projectId", required = false)
        @RequestParam projectId: String?
    ): Response<Token?>

    @ApiOperation("查询用户token列表")
    @GetMapping("/list/token/{uid}")
    fun listUserToken(
        @ApiParam(value = "用户id")
        @PathVariable("uid") uid: String
    ): Response<List<TokenResult>>

    @ApiOperation("删除用户token")
    @DeleteMapping("/token/{uid}/{name}")
    fun deleteToken(
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户token")
        @PathVariable name: String
    ): Response<Boolean>

    @ApiOperation("校验用户token")
    @GetMapping("/token/{uid}/{token}")
    fun checkUserToken(
        @ApiParam(value = "用户id")
        @PathVariable uid: String,
        @ApiParam(value = "用户token")
        @PathVariable token: String
    ): Response<Boolean>

    @ApiOperation("校验用户token")
    @PostMapping("/login")
    fun loginUser(
        @ApiParam(value = "用户id")
        @RequestParam("uid") uid: String,
        @ApiParam(value = "用户token")
        @RequestParam("token") token: String
    ): Response<Boolean>

    @ApiOperation("获取用户信息")
    @GetMapping("/info")
    fun userInfo(
        @ApiParam(value = "用户id")
        @CookieValue(value = "bkrepo_ticket") bkrepoToken: String?
    ): Response<Map<String, Any>>

    @ApiOperation("校验用户ticket")
    @GetMapping("/verify")
    fun verify(
        @ApiParam(value = "用户id")
        @RequestParam(value = "bkrepo_ticket") bkrepoToken: String?
    ): Response<Map<String, Any>>
}
