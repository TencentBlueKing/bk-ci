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

package com.tencent.bkrepo.auth.service.local

import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.Permission
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "local")
class PermissionServiceImpl @Autowired constructor(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val mongoTemplate: MongoTemplate
) : PermissionService, AbstractServiceImpl(mongoTemplate, userRepository, roleRepository) {

    override fun deletePermission(id: String): Boolean {
        permissionRepository.deleteById(id)
        return true
    }

    override fun listPermission(resourceType: ResourceType?, projectId: String?, repoName: String?): List<Permission> {
        logger.info("list  permission resourceType : [$resourceType], projectId: [$projectId], repoName: [$repoName]")

        return if (resourceType == null && projectId == null && repoName == null) {
            permissionRepository.findAll().map { transferPermission(it) }
        } else if (projectId == null && resourceType != null) {
            permissionRepository.findByResourceType(resourceType).map { transferPermission(it) }
        } else if (projectId != null && resourceType != null && repoName == null) {
            permissionRepository.findByResourceTypeAndProjectId(resourceType, projectId).map { transferPermission(it) }
        } else if (projectId != null && resourceType != null && repoName != null) {
            permissionRepository.findByResourceTypeAndProjectIdAndRepos(resourceType, projectId, repoName)
                .map { transferPermission(it) }
        } else {
            emptyList()
        }
    }

    override fun createPermission(request: CreatePermissionRequest): Boolean {
        logger.info("create  permission request : [$request]")
        // todo check request
        val permission = permissionRepository.findOneByPermNameAndProjectIdAndResourceType(
            request.permName,
            request.projectId,
            request.resourceType
        )
        permission?.let {
            logger.warn("create permission  [$request] is exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_DUP_PERMNAME)
        }
        val result = permissionRepository.insert(
            TPermission(
                resourceType = request.resourceType,
                projectId = request.projectId,
                permName = request.permName,
                repos = request.repos,
                includePattern = request.includePattern,
                excludePattern = request.excludePattern,
                users = request.users,
                roles = request.roles,
                createBy = request.createBy,
                createAt = LocalDateTime.now(),
                updatedBy = request.updatedBy,
                updateAt = LocalDateTime.now()
            )
        )
        result.id?.let {
            return true
        }
        return false
    }

    override fun updateIncludePath(id: String, path: List<String>): Boolean {
        logger.info("update include path id : [$id] ,path : [$path]")
        checkPermissionExist(id)
        return updatePermissionById(id, TPermission::includePattern.name, path)
    }

    override fun updateExcludePath(id: String, path: List<String>): Boolean {
        logger.info("update exclude path id : [$id] ,path :[$path]")
        checkPermissionExist(id)
        return updatePermissionById(id, TPermission::excludePattern.name, path)
    }

    override fun updateRepoPermission(id: String, repos: List<String>): Boolean {
        logger.info("update repo permission  id : [$id] ,repos : [$repos]")
        checkPermissionExist(id)
        return updatePermissionById(id, TPermission::repos.name, repos)
    }

    override fun updateUserPermission(id: String, uid: String, actions: List<PermissionAction>): Boolean {
        logger.info("update user permission  id : [$id] ,uid : [$uid], actions: [$actions] ")
        checkPermissionExist(id)
        checkUserExist(uid)

        val userQuery = Query.query(Criteria.where("_id").`is`(id).and("users.id").`is`(uid))
        val userResult = mongoTemplate.findOne(userQuery, TPermission::class.java)
        userResult?.let {
            val query = Query.query(Criteria.where("_id").`is`(id).and("users._id").`is`(uid))
            val update = Update()
            update.set("users.$.action", actions)
            val result = mongoTemplate.updateFirst(query, update, TPermission::class.java)
            if (result.modifiedCount == 1L) return true
            logger.warn("update user permission  [$id] , user [$uid] exist .")
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_PERMISSION_EXIST)
        }
        return updatePermissionAction(id, uid, actions, TPermission::users.name)
    }

    override fun removeUserPermission(id: String, uid: String): Boolean {
        logger.info("remove user permission  id : [$id] ,uid : [$uid]")
        checkPermissionExist(id)
        return removePermission(id, uid, TPermission::users.name)
    }

    override fun updateRolePermission(id: String, rid: String, actions: List<PermissionAction>): Boolean {
        logger.info("update role permission  id : [$id] ,rid : [$rid], actions: [$actions] ")
        checkPermissionExist(id)
        checkRoleExist(rid)

        val roleQuery = Query.query(Criteria.where("_id").`is`(id).and("roles.id").`is`(rid))
        val roleResult = mongoTemplate.findOne(roleQuery, TPermission::class.java)
        roleResult?.let {
            logger.warn("add role permission [$id] role [$rid]   exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_ROLE_PERMISSION_EXIST)
        }

        return updatePermissionAction(id, rid, actions, TPermission::roles.name)
    }

    override fun removeRolePermission(id: String, rid: String): Boolean {
        logger.info("remove role permission  id : [$id] ,rid : [$rid] ")
        checkPermissionExist(id)
        return removePermission(id, rid, TPermission::roles.name)
    }

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.info("check permission  request : [$request] ")
        val user = userRepository.findFirstByUserId(request.uid) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }
        if (user.admin || !request.appId.isNullOrBlank()) return true
        val roles = user.roles

        // check project admin
        if (roles.isNotEmpty() && request.projectId != null && request.resourceType == ResourceType.PROJECT) {
            roles.forEach {
                val role = roleRepository.findFirstByIdAndProjectIdAndType(it, request.projectId!!, RoleType.PROJECT)
                if (role != null && role.admin) return true
            }
        }

        // check repo admin
        if (roles.isNotEmpty() && request.projectId != null && request.resourceType == ResourceType.REPO) {
            roles.forEach {
                // check project admin first
                val pRole = roleRepository.findFirstByIdAndProjectIdAndType(it, request.projectId!!, RoleType.PROJECT)
                if (pRole != null && pRole.admin) return true
                // check repo admin then
                val rRole = roleRepository.findFirstByIdAndProjectIdAndTypeAndRepoName(
                    it,
                    request.projectId!!,
                    RoleType.REPO,
                    request.repoName!!
                )
                if (rRole != null && rRole.admin) return true
            }
        }

        // check repo permission
        val criteria = Criteria()
        var celeriac = criteria.orOperator(
            Criteria.where("users._id").`is`(request.uid).and("users.action").`is`(request.action.toString()),
            Criteria.where("roles._id").`in`(roles).and("users.action").`is`(request.action.toString())
        ).and(TPermission::resourceType.name).`is`(request.resourceType.toString())
        if (request.resourceType != ResourceType.SYSTEM) {
            celeriac = celeriac.and(TPermission::projectId.name).`is`(request.projectId)
        }
        if (request.resourceType == ResourceType.REPO) {
            celeriac = celeriac.and(TPermission::repos.name).`is`(request.repoName)
        }
        val query = Query.query(celeriac)
        val result = mongoTemplate.count(query, TPermission::class.java)
        if (result != 0L) return true
        return false
    }

    private fun checkPermissionExist(pId: String) {
        permissionRepository.findFirstById(pId) ?: run {
            logger.warn("update permission repos [$pId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_NOT_EXIST)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionServiceImpl::class.java)
    }
}
