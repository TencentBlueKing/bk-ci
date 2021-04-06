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

package com.tencent.bkrepo.auth.service.local

import com.mongodb.BasicDBObject
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.model.TUser
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.PermissionSet
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToProjectRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToRepoRequest
import com.tencent.bkrepo.auth.pojo.user.User
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

abstract class AbstractServiceImpl constructor(
    private val mongoTemplate: MongoTemplate,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {

    fun checkUserExist(userId: String) {
        userRepository.findFirstByUserId(userId) ?: run {
            logger.warn("user [$userId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }
    }

    fun checkUserRoleBind(userId: String, roleId: String): Boolean {
        userRepository.findFirstByUserIdAndRoles(userId, roleId) ?: run {
            logger.warn("user [$userId,$roleId]  not exist.")
            return false
        }
        return true
    }

    // check user is exist
    fun checkUserExistBatch(idList: List<String>) {
        idList.forEach {
            userRepository.findFirstByUserId(it) ?: run {
                logger.warn(" user not  exist.")
                throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
            }
        }
    }

    // check role is exist
    fun checkRoleExist(roleId: String) {
        roleRepository.findFirstById(roleId) ?: run {
            logger.warn(" role not  exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_ROLE_NOT_EXIST)
        }
    }

    fun convCreateProjectUserRequest(request: CreateUserToProjectRequest): CreateUserRequest {
        return CreateUserRequest(
            request.userId,
            request.name,
            request.pwd,
            request.admin,
            request.asstUsers,
            request.group
        )
    }

    fun convCreateRepoUserRequest(request: CreateUserToRepoRequest): CreateUserRequest {
        return CreateUserRequest(
            request.userId,
            request.name,
            request.pwd,
            request.admin,
            request.asstUsers,
            request.group
        )
    }

    fun updatePermissionById(id: String, key: String, value: Any): Boolean {
        val update = Update()
        update.set(key, value)
        val result = mongoTemplate.upsert(buildIdQuery(id), update, TPermission::class.java)
        if (result.matchedCount == 1L) return true
        return false
    }

    fun updatePermissionAction(pId: String, urId: String, actions: List<PermissionAction>, filed: String): Boolean {
        val update = Update()
        var userAction =
            PermissionSet(id = urId, action = actions)
        update.addToSet(filed, userAction)
        val result = mongoTemplate.updateFirst(buildIdQuery(pId), update, TPermission::class.java)
        if (result.matchedCount == 1L) return true
        return false
    }

    fun removePermission(id: String, uid: String, field: String): Boolean {
        val update = Update()
        val s = BasicDBObject()
        s["_id"] = uid
        update.pull(field, s)
        val result = mongoTemplate.updateFirst(buildIdQuery(id), update, TPermission::class.java)
        if (result.modifiedCount == 1L) return true
        return false
    }

    private fun buildIdQuery(id: String): Query {
        return Query.query(Criteria.where("_id").`is`(id))
    }

    fun transferPermission(tPermission: TPermission): Permission {
        return Permission(
            id = tPermission.id,
            resourceType = tPermission.resourceType,
            projectId = tPermission.projectId,
            permName = tPermission.permName,
            repos = tPermission.repos,
            includePattern = tPermission.includePattern,
            excludePattern = tPermission.excludePattern,
            users = tPermission.users,
            roles = tPermission.roles,
            actions = tPermission.actions,
            createBy = tPermission.createBy,
            createAt = tPermission.createAt,
            updatedBy = tPermission.updatedBy,
            updateAt = tPermission.updateAt
        )
    }

    fun transferUser(tUser: TUser): User {
        return User(
            userId = tUser.userId,
            name = tUser.name,
            pwd = tUser.pwd,
            admin = tUser.admin,
            locked = tUser.locked,
            tokens = tUser.tokens,
            roles = tUser.roles
        )
    }

    fun filterRepos(repos: List<String>, originRepoNames: List<String>): List<String> {
        (repos as MutableList).retainAll(originRepoNames)
        return repos
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractServiceImpl::class.java)
    }
}
