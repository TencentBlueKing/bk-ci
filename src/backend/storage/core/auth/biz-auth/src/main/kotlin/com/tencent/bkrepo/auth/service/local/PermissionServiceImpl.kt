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

import com.tencent.bkrepo.auth.constant.AUTH_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_ADMIN
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_USER
import com.tencent.bkrepo.auth.constant.AUTH_BUILTIN_VIEWER
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TPermission
import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.ListRepoPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest
import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.time.LocalDateTime
import java.util.stream.Collectors

open class PermissionServiceImpl constructor(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val mongoTemplate: MongoTemplate,
    private val repositoryClient: RepositoryClient
) : PermissionService, AbstractServiceImpl(mongoTemplate, userRepository, roleRepository) {

    override fun deletePermission(id: String): Boolean {
        logger.info("delete  permission  repoName: [$id]")
        permissionRepository.deleteById(id)
        return true
    }

    override fun listPermission(projectId: String, repoName: String?): List<Permission> {
        logger.debug("list  permission  projectId: [$projectId], repoName: [$repoName]")
        repoName?.let {
            return permissionRepository.findByResourceTypeAndProjectIdAndRepos(ResourceType.REPO, projectId, repoName)
                .map { transferPermission(it) }
        }
        return permissionRepository.findByResourceTypeAndProjectId(ResourceType.PROJECT, projectId)
            .map { transferPermission(it) }
    }

    override fun listBuiltinPermission(projectId: String, repoName: String): List<Permission> {
        logger.debug("list  builtin permission  projectId: [$projectId], repoName: [$repoName]")
        val repoAdmin = getOnePermission(projectId, repoName, AUTH_BUILTIN_ADMIN, listOf(PermissionAction.MANAGE))
        val repoUser = getOnePermission(
            projectId,
            repoName,
            AUTH_BUILTIN_USER,
            listOf(PermissionAction.WRITE, PermissionAction.READ, PermissionAction.DELETE, PermissionAction.UPDATE)
        )
        val repoViewer = getOnePermission(projectId, repoName, AUTH_BUILTIN_VIEWER, listOf(PermissionAction.READ))
        return listOf(repoAdmin, repoUser, repoViewer).map { transferPermission(it) }
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

    override fun updateIncludePath(request: UpdatePermissionPathRequest): Boolean {
        logger.info("update include path request :[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::includePattern.name, path)
        }
    }

    override fun updateExcludePath(request: UpdatePermissionPathRequest): Boolean {
        logger.info("update exclude path request :[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::excludePattern.name, path)
        }
    }

    override fun updateRepoPermission(request: UpdatePermissionRepoRequest): Boolean {
        logger.info("update repo permission request :  [$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::repos.name, repos)
        }
    }

    override fun updatePermissionUser(request: UpdatePermissionUserRequest): Boolean {
        logger.info("update permission user request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::users.name, userId)
        }
    }

    override fun updatePermissionRole(request: UpdatePermissionRoleRequest): Boolean {
        logger.info("update permission role request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::roles.name, rId)
        }
    }

    override fun updatePermissionDepartment(request: UpdatePermissionDepartmentRequest): Boolean {
        logger.info("update  permission department request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::departments.name, departmentId)
        }
    }

    override fun updatePermissionAction(request: UpdatePermissionActionRequest): Boolean {
        logger.info("update permission action request:[$request]")
        with(request) {
            checkPermissionExist(permissionId)
            return updatePermissionById(permissionId, TPermission::actions.name, actions)
        }
    }

    override fun checkPermission(request: CheckPermissionRequest): Boolean {
        logger.debug("check permission  request : [$request] ")
        val user = userRepository.findFirstByUserId(request.uid) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }

        // check user admin permission
        if (user.admin || !request.appId.isNullOrBlank()) return true

        // check role project admin
        if (checkProjectAdmin(request, user.roles)) return true

        // check role repo admin
        if (checkRepoAdmin(request, user.roles)) return true

        // check repo action action
        return checkRepoAction(request, user.roles)
    }

    private fun checkProjectAdmin(request: CheckPermissionRequest, roles: List<String>): Boolean {
        if (roles.isNotEmpty() && request.projectId != null) {
            roles.forEach {
                val role = roleRepository.findFirstByIdAndProjectIdAndType(it, request.projectId!!, RoleType.PROJECT)
                if (role != null && role.admin) return true
            }
        }
        return false
    }

    private fun checkRepoAdmin(request: CheckPermissionRequest, roles: List<String>): Boolean {
        // check role repo admin
        if (roles.isNotEmpty() && request.projectId != null && request.repoName != null) {
            roles.forEach {
                val rRole = roleRepository.findFirstByIdAndProjectIdAndTypeAndRepoName(
                    it,
                    request.projectId!!,
                    RoleType.REPO,
                    request.repoName!!
                )
                if (rRole != null && rRole.admin) return true
            }
        }
        return false
    }

    private fun checkRepoAction(request: CheckPermissionRequest, roles: List<String>): Boolean {
        with(request) {
            projectId?.let {
                var celeriac = buildCheckActionQuery(projectId!!, uid, action, resourceType, roles)
                if (request.resourceType == ResourceType.REPO) {
                    celeriac = celeriac.and(TPermission::repos.name).`is`(request.repoName)
                }
                val query = Query.query(celeriac)
                val result = mongoTemplate.count(query, TPermission::class.java)
                if (result != 0L) return true
            }
            return false
        }
    }

    override fun listRepoPermission(request: ListRepoPermissionRequest): List<String> {
        logger.debug("list repo permission  request : [$request] ")
        if (request.repoNames.isNullOrEmpty()) return emptyList()
        val user = userRepository.findFirstByUserId(request.uid) ?: run {
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }
        if (user.admin || !request.appId.isNullOrBlank()) {
            // 查询该项目下的所有仓库并过滤返回
            val repoList = repositoryClient.listRepo(request.projectId).data?.map { it.name } ?: emptyList()
            return filterRepos(repoList, request.repoNames)
        }
        val roles = user.roles

        // check project admin
        if (roles.isNotEmpty() && request.resourceType == ResourceType.PROJECT) {
            return listProjectPermissions(roles, request)
        }

        val reposList = mutableListOf<String>()
        // check repo admin
        if (roles.isNotEmpty() && request.resourceType == ResourceType.REPO) {
            return listRepoPermissions(roles, request, reposList)
        }

        // check repo permission
        with(request) {
            val celeriac = buildCheckActionQuery(projectId, uid, action, request.resourceType, roles)
            val query = Query.query(celeriac)
            val result = mongoTemplate.find(query, TPermission::class.java)
            val permissionRepoList = result.stream().flatMap { it.repos.stream() }.collect(Collectors.toList())
            reposList.addAll(permissionRepoList)
            return filterRepos(reposList, request.repoNames)
        }
    }

    private fun listRepoPermissions(
        roles: List<String>,
        request: ListRepoPermissionRequest,
        reposList: MutableList<String>
    ): List<String> {
        roles.forEach { role ->
            // check project admin first
            val pRole = roleRepository.findFirstByIdAndProjectIdAndType(role, request.projectId, RoleType.PROJECT)
            if (pRole != null && pRole.admin) {
                val repoList = repositoryClient.listRepo(request.projectId).data?.map { it.name } ?: emptyList()
                return filterRepos(repoList, request.repoNames)
            }
            // check repo admin then
            val rRole = roleRepository.findFirstByIdAndProjectIdAndType(
                role,
                request.projectId,
                RoleType.REPO
            )
            if (rRole != null && rRole.admin) reposList.add(rRole.repoName!!)
        }
        return emptyList()
    }

    private fun listProjectPermissions(roles: List<String>, request: ListRepoPermissionRequest): List<String> {
        roles.forEach { role ->
            val tRole = roleRepository.findFirstByIdAndProjectIdAndType(role, request.projectId, RoleType.PROJECT)
            if (tRole != null && tRole.admin) {
                val repoList = repositoryClient.listRepo(request.projectId).data?.map { it.name } ?: emptyList()
                return filterRepos(repoList, request.repoNames)
            }
        }
        return emptyList()
    }

    override fun registerResource(request: RegisterResourceRequest) {
        return
    }

    private fun checkPermissionExist(pId: String) {
        permissionRepository.findFirstById(pId) ?: run {
            logger.warn("update permission repos [$pId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_PERMISSION_NOT_EXIST)
        }
    }

    private fun getOnePermission(
        projectId: String,
        repoName: String,
        permName: String,
        actions: List<PermissionAction>
    ): TPermission {
        permissionRepository.findOneByProjectIdAndReposAndPermNameAndResourceType(
            projectId,
            repoName,
            permName,
            ResourceType.REPO
        ) ?: run {
            val request =
                TPermission(
                    projectId = projectId,
                    repos = listOf(repoName),
                    permName = permName,
                    actions = actions,
                    resourceType = ResourceType.REPO,
                    createAt = LocalDateTime.now(),
                    updateAt = LocalDateTime.now(),
                    createBy = AUTH_ADMIN,
                    updatedBy = AUTH_ADMIN
                )
            logger.info("permission not exist, create [$request]")
            permissionRepository.insert(request)
        }
        return permissionRepository.findOneByProjectIdAndReposAndPermNameAndResourceType(
            projectId,
            repoName,
            permName,
            ResourceType.REPO
        )!!
    }

    private fun buildCheckActionQuery(
        projectId: String,
        uid: String,
        action: PermissionAction,
        resourceType: ResourceType,
        roles: List<String>
    ): Criteria {
        val criteria = Criteria()
        var celeriac = criteria.orOperator(
            Criteria.where(TPermission::users.name).`is`(uid),
            Criteria.where(TPermission::roles.name).`in`(roles)
        ).and(TPermission::resourceType.name).`is`(resourceType.toString()).and(TPermission::users.name)
            .`is`(action.toString())
        if (resourceType != ResourceType.SYSTEM) {
            celeriac = celeriac.and(TPermission::projectId.name).`is`(projectId)
        }
        return celeriac
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionServiceImpl::class.java)
    }
}
