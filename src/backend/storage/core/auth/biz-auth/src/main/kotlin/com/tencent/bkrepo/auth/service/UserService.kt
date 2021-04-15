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

package com.tencent.bkrepo.auth.service

import com.tencent.bkrepo.auth.pojo.token.Token
import com.tencent.bkrepo.auth.pojo.token.TokenResult
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToProjectRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToRepoRequest
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.user.User

interface UserService {

    fun getUserById(userId: String): User?

    fun createUser(request: CreateUserRequest): Boolean

    fun createUserToProject(request: CreateUserToProjectRequest): Boolean

    fun createUserToRepo(request: CreateUserToRepoRequest): Boolean

    fun listUser(rids: List<String>): List<User>

    fun deleteById(userId: String): Boolean

    fun updateUserById(userId: String, request: UpdateUserRequest): Boolean

    fun addUserToRole(userId: String, roleId: String): User?

    fun addUserToRoleBatch(idList: List<String>, roleId: String): Boolean

    fun removeUserFromRole(userId: String, roleId: String): User?

    fun removeUserFromRoleBatch(idList: List<String>, roleId: String): Boolean

    fun createToken(userId: String): Token?

    fun addUserToken(userId: String, name: String, expiredAt: String?): Token?

    fun listUserToken(userId: String): List<TokenResult>

    fun removeToken(userId: String, name: String): Boolean

    fun findUserByUserToken(userId: String, pwd: String): User?
}
