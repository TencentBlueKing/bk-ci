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

import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_ID
import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_NAME
import com.tencent.bkrepo.auth.pojo.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.CreateUserToProjectRequest
import com.tencent.bkrepo.auth.pojo.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.User
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceUserResourceImpl @Autowired constructor(
    private val userService: UserService,
    private val roleService: RoleService
) : ServiceUserResource {

    override fun createUser(request: CreateUserRequest): Response<Boolean> {
        userService.createUser(request)
        return ResponseBuilder.success(true)
    }

    override fun createUserToProject(request: CreateUserToProjectRequest): Response<Boolean> {
        userService.createUserToProject(request)
        val createRoleRequest =
            CreateRoleRequest(PROJECT_MANAGE_ID, PROJECT_MANAGE_NAME, RoleType.PROJECT, request.projectId, null, true)
        val roleId = roleService.createRole(createRoleRequest)
        userService.addUserToRole(request.userId, roleId!!)
        return ResponseBuilder.success(true)
    }

    override fun listUser(rids: List<String>): Response<List<User>> {
        val result = userService.listUser(rids)
        return ResponseBuilder.success(result)
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

    override fun createToken(uid: String): Response<User?> {
        val result = userService.createToken(uid)
        return ResponseBuilder.success(result)
    }

    override fun addUserToken(uid: String, token: String): Response<User?> {
        val result = userService.addUserToken(uid, token)
        return ResponseBuilder.success(result)
    }

    override fun deleteToken(uid: String, token: String): Response<User?> {
        val result = userService.removeToken(uid, token)
        return ResponseBuilder.success(result)
    }

    override fun checkUserToken(uid: String, token: String): Response<Boolean> {
        userService.findUserByUserToken(uid, token) ?: return ResponseBuilder.success(false)
        return ResponseBuilder.success(true)
    }
}
