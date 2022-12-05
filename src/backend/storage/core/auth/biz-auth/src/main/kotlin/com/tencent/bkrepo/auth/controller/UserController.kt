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

import com.tencent.bkrepo.auth.constant.AUTH_API_USER_PREFIX
import com.tencent.bkrepo.auth.constant.BKREPO_TICKET
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.pojo.enums.AuthPermissionType
import com.tencent.bkrepo.auth.pojo.token.Token
import com.tencent.bkrepo.auth.pojo.token.TokenResult
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToProjectRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToRepoRequest
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.user.User
import com.tencent.bkrepo.auth.pojo.user.UserInfo
import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.auth.resource.OpenResourceImpl
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.util.RequestUtil.buildProjectAdminRequest
import com.tencent.bkrepo.auth.util.RequestUtil.buildRepoAdminRequest
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.http.jwt.JwtAuthProperties
import com.tencent.bkrepo.common.security.util.JwtUtils
import com.tencent.bkrepo.common.security.util.RsaUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiOperation
import javax.servlet.http.Cookie
import org.bouncycastle.crypto.CryptoException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(AUTH_API_USER_PREFIX)
class UserController @Autowired constructor(
    private val userService: UserService,
    private val roleService: RoleService,
    private val jwtProperties: JwtAuthProperties,
    permissionService: PermissionService
) : OpenResourceImpl(permissionService) {

    private val signingKey = JwtUtils.createSigningKey(jwtProperties.secretKey)

    @ApiOperation("创建用户")
    @PostMapping("/create")
    fun createUser(@RequestBody request: CreateUserRequest): Response<Boolean> {
        userService.createUser(request)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("创建仓库用户")
    @PostMapping("/create/project")
    fun createUserToProject(@RequestBody request: CreateUserToProjectRequest): Response<Boolean> {
        checkUserPermission(AuthPermissionType.PROJECT, request.projectId, null)
        userService.createUserToProject(request)
        val createRoleRequest = buildProjectAdminRequest(request.projectId)
        val roleId = roleService.createRole(createRoleRequest)
        userService.addUserToRole(request.userId, roleId!!)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("创建项目用户")
    @PostMapping("/create/repo")
    fun createUserToRepo(@RequestBody request: CreateUserToRepoRequest): Response<Boolean> {
        checkUserPermission(AuthPermissionType.PROJECT, request.projectId, null)
        userService.createUserToRepo(request)
        val createRoleRequest = buildRepoAdminRequest(request.projectId, request.repoName)
        val roleId = roleService.createRole(createRoleRequest)
        userService.addUserToRole(request.userId, roleId!!)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("用户列表")
    @GetMapping("/list")
    fun listUser(@RequestBody rids: List<String>?): Response<List<UserResult>> {
        val result = userService.listUser(rids.orEmpty()).map {
            UserResult(it.userId, it.name)
        }
        return ResponseBuilder.success(result)
    }

    @ApiOperation("权限用户列表")
    @GetMapping("/listall")
    fun listAllUser(@RequestBody rids: List<String>?): Response<List<User>> {
        return ResponseBuilder.success(userService.listUser(rids.orEmpty()))
    }

    @ApiOperation("删除用户")
    @DeleteMapping("/{uid}")
    fun deleteById(@PathVariable uid: String): Response<Boolean> {
        checkUserId(uid)
        userService.deleteById(uid)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("用户详情")
    @GetMapping("/detail/{uid}")
    fun detail(@PathVariable uid: String): Response<User?> {
        checkUserId(uid)
        return ResponseBuilder.success(userService.getUserById(uid))
    }

    @ApiOperation("更新用户信息")
    @PutMapping("/{uid}")
    fun updateById(@PathVariable uid: String, @RequestBody request: UpdateUserRequest): Response<Boolean> {
        checkUserId(uid)
        userService.updateUserById(uid, request)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("新增用户所属角色")
    @PostMapping("/role/{uid}/{rid}")
    fun addUserRole(@PathVariable uid: String, @PathVariable rid: String): Response<User?> {
        val result = userService.addUserToRole(uid, rid)
        return ResponseBuilder.success(result)
    }

    @ApiOperation("删除用户所属角色")
    @DeleteMapping("/role/{uid}/{rid}")
    fun removeUserRole(@PathVariable uid: String, @PathVariable rid: String): Response<User?> {
        val result = userService.removeUserFromRole(uid, rid)
        return ResponseBuilder.success(result)
    }

    @ApiOperation("批量新增用户所属角色")
    @PatchMapping("/role/add/{rid}")
    fun addUserRoleBatch(@PathVariable rid: String, @RequestBody request: List<String>): Response<Boolean> {
        userService.addUserToRoleBatch(request, rid)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("批量删除用户所属角色")
    @PatchMapping("/role/delete/{rid}")
    fun deleteUserRoleBatch(@PathVariable rid: String, @RequestBody request: List<String>): Response<Boolean> {
        userService.removeUserFromRoleBatch(request, rid)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("新加用户token")
    @PostMapping("/token/{uid}/{name}")
    fun addUserToken(
        @PathVariable("uid") uid: String,
        @PathVariable("name") name: String,
        @RequestParam expiredAt: String?,
        @RequestParam projectId: String?
    ): Response<Token?> {
        checkUserId(uid)
        // add user to project first
        projectId?.let {
            val createRoleRequest = buildProjectAdminRequest(projectId)
            val roleId = roleService.createRole(createRoleRequest)
            userService.addUserToRole(uid, roleId!!)
        }
        // add user token
        val result = userService.addUserToken(uid, name, expiredAt)
        return ResponseBuilder.success(result)
    }

    @ApiOperation("查询用户token列表")
    @GetMapping("/list/token/{uid}")
    fun listUserToken(@PathVariable("uid") uid: String): Response<List<TokenResult>> {
        checkUserId(uid)
        val result = userService.listUserToken(uid)
        return ResponseBuilder.success(result)
    }

    @ApiOperation("删除用户token")
    @DeleteMapping("/token/{uid}/{name}")
    fun deleteToken(@PathVariable uid: String, @PathVariable name: String): Response<Boolean> {
        checkUserId(uid)
        val result = userService.removeToken(uid, name)
        return ResponseBuilder.success(result)
    }

    @ApiOperation("校验用户token")
    @PostMapping("/token")
    fun checkToken(@RequestParam uid: String, @RequestParam token: String): Response<Boolean> {
        checkUserId(uid)
        userService.findUserByUserToken(uid, token) ?: return ResponseBuilder.success(false)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("获取公钥")
    @GetMapping("/rsa")
    fun getPublicKey(): Response<String?> {
        return ResponseBuilder.success(RsaUtils.publicKey)
    }

    @ApiOperation("校验用户会话token")
    @PostMapping("/login")
    fun loginUser(@RequestParam("uid") uid: String, @RequestParam("token") token: String): Response<Boolean> {
        val decryptToken: String?
        try {
            decryptToken = RsaUtils.decrypt(token)
        } catch (e: CryptoException) {
            logger.warn("token decrypt failed token [$uid]")
            throw AuthenticationException(messageCode = AuthMessageCode.AUTH_LOGIN_FAILED)
        }

        userService.findUserByUserToken(uid, decryptToken) ?: run {
            logger.info("user not match [$uid]")
            return ResponseBuilder.success(false)
        }
        val ticket = JwtUtils.generateToken(signingKey, jwtProperties.expiration, uid)
        val cookie = Cookie(BKREPO_TICKET, ticket)
        cookie.path = "/"
        cookie.maxAge = 60 * 60 * 24
        HttpContextHolder.getResponse().addCookie(cookie)
        return ResponseBuilder.success(true)
    }

    @ApiOperation("获取用户信息")
    @GetMapping("/info")
    fun userInfo(
        @CookieValue(value = "bkrepo_ticket") bkrepoToken: String?,
        @CookieValue(value = "bk_uid") bkUserId: String?
    ): Response<Map<String, Any>> {
        try {
            bkUserId?.let {
                return ResponseBuilder.success(mapOf("userId" to bkUserId))
            }
            bkrepoToken ?: run {
                throw IllegalArgumentException("ticket can not be null")
            }
            val userId = JwtUtils.validateToken(signingKey, bkrepoToken).body.subject
            val result = mapOf("userId" to userId)
            return ResponseBuilder.success(result)
        } catch (ignored: Exception) {
            logger.warn("validate user token false [$bkrepoToken]")
            throw ErrorCodeException(AuthMessageCode.AUTH_LOGIN_TOKEN_CHECK_FAILED)
        }
    }

    @ApiOperation("校验用户ticket")
    @GetMapping("/verify")
    fun verify(@RequestParam(value = "bkrepo_ticket") bkrepoToken: String?): Response<Map<String, Any>> {
        try {
            bkrepoToken ?: run {
                throw IllegalArgumentException("ticket can not be null")
            }
            val userId = JwtUtils.validateToken(signingKey, bkrepoToken).body.subject
            val result = mapOf("user_id" to userId)
            return ResponseBuilder.success(result)
        } catch (ignored: Exception) {
            logger.warn("validate user token false [$bkrepoToken]")
            throw ErrorCodeException(AuthMessageCode.AUTH_LOGIN_TOKEN_CHECK_FAILED)
        }
    }

    @ApiOperation("用户分页列表")
    @GetMapping("page/{pageNumber}/{pageSize}")
    fun userPage(
        @PathVariable pageNumber: Int,
        @PathVariable pageSize: Int,
        @RequestParam user: String? = null,
        @RequestParam admin: Boolean?,
        @RequestParam locked: Boolean?
    ): Response<Page<UserInfo>> {
        val result = userService.userPage(pageNumber, pageSize, user, admin, locked)
        return ResponseBuilder.success(result)
    }

    @ApiOperation("用户info ")
    @GetMapping("/userinfo/{uid}")
    fun userInfoById(@PathVariable uid: String): Response<UserInfo?> {
        return ResponseBuilder.success(userService.getUserInfoById(uid))
    }

    @ApiOperation("修改用户密码")
    @PutMapping("/update/password/{uid}")
    fun updatePassword(
        @PathVariable uid: String,
        @RequestParam oldPwd: String,
        @RequestParam newPwd: String
    ): Response<Boolean> {
        return ResponseBuilder.success(userService.updatePassword(uid, oldPwd, newPwd))
    }

    @ApiOperation("用户info ")
    @GetMapping("/reset/{uid}")
    fun resetPassword(@PathVariable uid: String): Response<Boolean> {
        return ResponseBuilder.success(userService.resetPassword(uid))
    }

    @ApiOperation("检验系统中是否存在同名userId ")
    @GetMapping("/repeat/{uid}")
    fun repeatUid(@PathVariable uid: String): Response<Boolean> {
        return ResponseBuilder.success(userService.repeatUid(uid))
    }

    @ApiOperation("判断用户是否为项目管理员")
    @GetMapping("/admin/{projectId}")
    fun isProjectAdmin(@PathVariable projectId: String): Response<Boolean> {
        return ResponseBuilder.success(checkProjectAdmin(projectId))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserController::class.java)
    }
}
