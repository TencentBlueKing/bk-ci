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

import com.tencent.bkrepo.auth.exception.RoleUpdateException
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TRole
import com.tencent.bkrepo.auth.pojo.role.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.role.Role
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.role.UpdateRoleRequest
import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.util.IDUtil
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

class RoleServiceImpl constructor(
    private val roleRepository: RoleRepository,
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val mongoTemplate: MongoTemplate
) : RoleService {

    override fun createRole(request: CreateRoleRequest): String? {
        logger.info("create  role  request : [$request] ")
        val role: TRole? = if (request.type == RoleType.REPO) {
            roleRepository.findFirstByRoleIdAndProjectIdAndRepoName(
                request.roleId!!,
                request.projectId,
                request.repoName!!
            )
        } else {
            roleRepository.findFirstByProjectIdAndTypeAndName(
                projectId = request.projectId,
                type = RoleType.PROJECT,
                name = request.name
            )
        }

        role?.let {
            logger.warn("create role [${request.roleId} , ${request.projectId} ]  is exist.")
            return role.id
        }

        val roleId = when (request.type) {
            RoleType.REPO -> request.roleId!!
            RoleType.PROJECT -> findUsableProjectTypeRoleId(request.roleId, request.projectId)
        }

        val result = roleRepository.insert(
            TRole(
                roleId = roleId,
                type = request.type,
                name = request.name,
                projectId = request.projectId,
                repoName = request.repoName,
                admin = request.admin,
                description = request.description
            )
        )
        return result.id
    }

    private fun findUsableProjectTypeRoleId(roleId: String?, projectId: String): String {
        var tempRoleId = roleId ?: "${projectId}_role_${IDUtil.shortUUID()}"
        while (true) {
            val role = roleRepository.findFirstByRoleIdAndProjectId(tempRoleId, projectId)
            if (role == null) return tempRoleId else tempRoleId = "${projectId}_role_${IDUtil.shortUUID()}"
        }
    }

    override fun detail(id: String): Role? {
        logger.debug("get role detail : [$id] ")
        val result = roleRepository.findFirstById(id) ?: return null
        return transfer(result)
    }

    override fun detail(rid: String, projectId: String): Role? {
        logger.debug("get  role  detail rid : [$rid] , projectId : [$projectId] ")
        val result = roleRepository.findFirstByRoleIdAndProjectId(rid, projectId) ?: return null
        return transfer(result)
    }

    override fun detail(rid: String, projectId: String, repoName: String): Role? {
        logger.debug("get  role  detail rid : [$rid] , projectId : [$projectId], repoName: [$repoName]")
        val result = roleRepository.findFirstByRoleIdAndProjectIdAndRepoName(rid, projectId, repoName) ?: return null
        return transfer(result)
    }

    override fun updateRoleInfo(id: String, updateRoleRequest: UpdateRoleRequest): Boolean {
        var roleRecord = true
        with(updateRoleRequest) {
            if (name != null || description != null) {
                val query = Query().addCriteria(Criteria.where(TRole::id.name).`is`(id))
                val update = Update()
                name?.let { update.set(TRole::name.name, name) }
                description?.let { update.set(TRole::description.name, description) }
                val record = mongoTemplate.updateFirst(query, update, TRole::class.java)
                roleRecord = record.modifiedCount == 1L || record.matchedCount == 1L
            }
        }

        val userRecord = updateRoleRequest.userIds?.map { it }?.let { idList ->
            val users = userRepository.findAllByRolesIn(listOf(id))
            userService.removeUserFromRoleBatch(users.map { it.userId }, id)
            userService.addUserToRoleBatch(idList, id)
        } ?: true
        if (roleRecord && userRecord) {
            return true
        } else {
            throw RoleUpdateException("update role failed!")
        }
    }

    override fun listUserByRoleId(id: String): Set<UserResult> {
        val result = mutableSetOf<UserResult>()
        userRepository.findAllByRolesIn(listOf(id)).let { users ->
            for (user in users) {
                result.add(UserResult(user.userId, user.name))
            }
        }
        return result
    }

    override fun listRoleByProject(projectId: String, repoName: String?): List<Role> {
        logger.info("list  role params , projectId : [$projectId], repoName: [$repoName]")
        repoName?.let {
            return roleRepository.findByProjectIdAndRepoNameAndType(projectId, repoName, RoleType.REPO)
                .map { transfer(it) }
        }
        return roleRepository.findByTypeAndProjectId(RoleType.PROJECT, projectId).map { transfer(it) }
    }

    override fun deleteRoleByid(id: String): Boolean {
        logger.info("delete  role  id : [$id]")
        val role = roleRepository.findTRoleById(ObjectId(id))
        if (role == null) {
            logger.warn("delete role [$id ] not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_ROLE_NOT_EXIST)
        } else {
            if (listUserByRoleId(role.id!!).isNotEmpty()) {
                throw ErrorCodeException(AuthMessageCode.AUTH_ROLE_USER_NOT_EMPTY)
            }
            roleRepository.deleteTRolesById(ObjectId(role.id))
        }
        return true
    }

    private fun transfer(tRole: TRole): Role {
        val userList = userRepository.findAllByRolesIn(listOf(tRole.id!!))
        val users = userList.map { it.userId }
        return Role(
            id = tRole.id,
            roleId = tRole.roleId,
            type = tRole.type,
            name = tRole.name,
            projectId = tRole.projectId,
            admin = tRole.admin,
            users = users,
            description = tRole.description
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RoleServiceImpl::class.java)
    }
}
