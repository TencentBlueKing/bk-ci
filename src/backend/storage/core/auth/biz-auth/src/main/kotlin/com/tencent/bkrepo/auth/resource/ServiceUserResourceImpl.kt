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

import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.auth.constant.BKREPO_TICKET
import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_ID
import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_NAME
import com.tencent.bkrepo.auth.constant.REPO_MANAGE_ID
import com.tencent.bkrepo.auth.constant.REPO_MANAGE_NAME
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.role.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.token.Token
import com.tencent.bkrepo.auth.pojo.token.TokenResult
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToProjectRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToRepoRequest
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.user.User
import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.http.jwt.JwtAuthProperties
import com.tencent.bkrepo.common.security.util.JwtUtils
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.Cookie

@RestController
class ServiceUserResourceImpl @Autowired constructor(
    private val userService: UserService,
    private val roleService: RoleService,
    private val permissionService: PermissionService,
    private val jwtProperties: JwtAuthProperties
) : ServiceUserResource {

    private val signingKey = JwtUtils.createSigningKey(jwtProperties.secretKey)

    override fun createUser(request: CreateUserRequest): Response<Boolean> {
        userService.createUser(request)
        return ResponseBuilder.success(true)
    }

    override fun createUserToProject(request: CreateUserToProjectRequest): Response<Boolean> {
        val userId = SecurityUtils.getUserId()
        // check 用户权限,非匿名用户
        if (ANONYMOUS_USER != userId) {
            val checkRequest =
                CheckPermissionRequest(
                    uid = userId,
                    resourceType = ResourceType.SYSTEM,
                    action = PermissionAction.WRITE
                )
            if (!permissionService.checkPermission(checkRequest)) {
                logger.warn("check user permission error [$checkRequest]")
                throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_FAILED)
            }
        }

        userService.createUserToProject(request)
        val createRoleRequest =
            CreateRoleRequest(
                PROJECT_MANAGE_ID,
                PROJECT_MANAGE_NAME,
                RoleType.PROJECT,
                request.projectId,
                null,
                true
            )
        val roleId = roleService.createRole(createRoleRequest)
        userService.addUserToRole(request.userId, roleId!!)
        return ResponseBuilder.success(true)
    }

    override fun createUserToRepo(request: CreateUserToRepoRequest): Response<Boolean> {
        val userId = SecurityUtils.getUserId()
        // check 用户权限,非匿名用户
        if (ANONYMOUS_USER != userId) {
            val checkRequest =
                CheckPermissionRequest(
                    uid = userId,
                    resourceType = ResourceType.PROJECT,
                    action = PermissionAction.WRITE,
                    projectId = request.projectId
                )
            if (!permissionService.checkPermission(checkRequest)) {
                logger.warn("check user permission error [$checkRequest]")
                throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_FAILED)
            }
        }
        userService.createUserToRepo(request)
        val createRoleRequest =
            CreateRoleRequest(
                REPO_MANAGE_ID,
                REPO_MANAGE_NAME,
                RoleType.REPO,
                request.projectId,
                request.repoName,
                true
            )
        val roleId = roleService.createRole(createRoleRequest)
        userService.addUserToRole(request.userId, roleId!!)
        return ResponseBuilder.success(true)
    }

    override fun listUser(rids: List<String>?): Response<List<UserResult>> {
        val result = userService.listUser(rids.orEmpty()).map {
            UserResult(it.userId, it.name)
        }
        return ResponseBuilder.success(result)
    }

    override fun listAllUser(rids: List<String>?): Response<List<User>> {
        return ResponseBuilder.success(userService.listUser(rids.orEmpty()))
    }

    override fun deleteById(uid: String): Response<Boolean> {
        userService.deleteById(uid)
        return ResponseBuilder.success(true)
    }

    override fun detail(uid: String): Response<User?> {
        return ResponseBuilder.success(userService.getUserById(uid))
    }

    override fun updateById(uid: String, request: UpdateUserRequest): Response<Boolean> {
        userService.updateUserById(uid, request)
        return ResponseBuilder.success(true)
    }

    override fun addUserRole(uid: String, rid: String): Response<User?> {
        val result = userService.addUserToRole(uid, rid)
        return ResponseBuilder.success(result)
    }

    override fun removeUserRole(uid: String, rid: String): Response<User?> {
        val result = userService.removeUserFromRole(uid, rid)
        return ResponseBuilder.success(result)
    }

    override fun addUserRoleBatch(rid: String, request: List<String>): Response<Boolean> {
        userService.addUserToRoleBatch(request, rid)
        return ResponseBuilder.success(true)
    }

    override fun deleteUserRoleBatch(rid: String, request: List<String>): Response<Boolean> {
        userService.removeUserFromRoleBatch(request, rid)
        return ResponseBuilder.success(true)
    }

    override fun createToken(uid: String): Response<Token?> {
        val result = userService.createToken(uid)
        return ResponseBuilder.success(result)
    }

    override fun addUserToken(uid: String, name: String, expiredAt: String?, projectId: String?): Response<Token?> {
        // add user to project first
        projectId?.let {
            val createRoleRequest =
                CreateRoleRequest(
                    PROJECT_MANAGE_ID,
                    PROJECT_MANAGE_NAME,
                    RoleType.PROJECT,
                    projectId,
                    null,
                    true
                )
            val roleId = roleService.createRole(createRoleRequest)
            userService.addUserToRole(uid, roleId!!)
        }
        // add user token
        val result = userService.addUserToken(uid, name, expiredAt)
        return ResponseBuilder.success(result)
    }

    override fun listUserToken(uid: String): Response<List<TokenResult>> {
        val result = userService.listUserToken(uid)
        return ResponseBuilder.success(result)
    }

    override fun deleteToken(uid: String, name: String): Response<Boolean> {
        val result = userService.removeToken(uid, name)
        return ResponseBuilder.success(result)
    }

    override fun checkUserToken(uid: String, token: String): Response<Boolean> {
        userService.findUserByUserToken(uid, token) ?: return ResponseBuilder.success(false)
        return ResponseBuilder.success(true)
    }

    override fun loginUser(uid: String, token: String): Response<Boolean> {
        userService.findUserByUserToken(uid, token) ?: run {
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

    override fun userInfo(bkrepoToken: String?): Response<Map<String, Any>> {
        try {
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

    override fun verify(bkrepoToken: String?): Response<Map<String, Any>> {
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

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceUserResourceImpl::class.java)
    }
}
