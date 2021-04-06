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

package com.tencent.bkrepo.common.security.manager

import com.tencent.bkrepo.auth.api.ServicePermissionResource
import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.security.http.core.HttpAuthProperties
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import org.slf4j.LoggerFactory

/**
 * 权限管理类
 */
open class PermissionManager(
    private val repositoryClient: RepositoryClient,
    private val permissionResource: ServicePermissionResource,
    private val userResource: ServiceUserResource,
    private val httpAuthProperties: HttpAuthProperties
) {

    /**
     * 校验项目权限
     * @param action 动作
     * @param projectId 项目id
     */
    open fun checkProjectPermission(
        action: PermissionAction,
        projectId: String
    ) {
        checkPermission(ResourceType.PROJECT, action, projectId)
    }

    /**
     * 校验仓库权限
     * @param action 动作
     * @param projectId 项目id
     * @param repoName 仓库名称
     */
    fun checkRepoPermission(
        action: PermissionAction,
        projectId: String,
        repoName: String,
        public: Boolean? = null
    ) {
        if (isReadPublicRepo(action, projectId, repoName, public)) {
            return
        }
        checkPermission(ResourceType.REPO, action, projectId, repoName)
    }

    /**
     * 校验节点权限
     * @param action 动作
     * @param projectId 项目id
     * @param repoName 仓库名称
     * @param path 节点路径
     * @param public 仓库是否为public
     */
    fun checkNodePermission(
        action: PermissionAction,
        projectId: String,
        repoName: String,
        path: String,
        public: Boolean? = null
    ) {
        if (isReadPublicRepo(action, projectId, repoName, public)) {
            return
        }
        checkPermission(ResourceType.NODE, action, projectId, repoName, path)
    }

    /**
     * 校验身份
     * @param userId 用户id
     * @param principalType 身份类型
     */
    fun checkPrincipal(userId: String, principalType: PrincipalType) {
        if (!httpAuthProperties.enabled) {
            return
        }
        val platformId = SecurityUtils.getPlatformId()
        checkAnonymous(userId, SecurityUtils.getPlatformId())

        if (principalType == PrincipalType.ADMIN) {
            if (!isAdminUser(userId)) {
                throw PermissionException()
            }
        } else if (principalType == PrincipalType.PLATFORM) {
            if (platformId == null && !isAdminUser(userId)) {
                throw PermissionException()
            }
        }
    }

    fun registerProject(userId: String, projectId: String) {
        permissionResource.registerResource(RegisterResourceRequest(userId, ResourceType.PROJECT, projectId))
    }

    fun registerRepo(userId: String, projectId: String, repoName: String) {
        permissionResource.registerResource(RegisterResourceRequest(userId, ResourceType.PROJECT, projectId, repoName))
    }

    /**
     * 判断是否为public仓库且为READ操作
     */
    private fun isReadPublicRepo(
        action: PermissionAction,
        projectId: String,
        repoName: String,
        public: Boolean? = null
    ): Boolean {
        if (action != PermissionAction.READ) {
            return false
        }
        return public ?: queryRepositoryInfo(projectId, repoName).public
    }

    /**
     * 查询仓库信息
     */
    private fun queryRepositoryInfo(projectId: String, repoName: String): RepositoryInfo {
        return repositoryClient.getRepoInfo(projectId, repoName).data ?: throw RepoNotFoundException(repoName)
    }

    /**
     * 去auth微服务校验资源权限
     */
    private fun checkPermission(
        type: ResourceType,
        action: PermissionAction,
        projectId: String? = null,
        repoName: String? = null,
        path: String? = null
    ) {
        // 判断是否开启认证
        if (!httpAuthProperties.enabled) {
            return
        }
        val userId = SecurityUtils.getUserId()
        val platformId = SecurityUtils.getPlatformId()
        checkAnonymous(userId, SecurityUtils.getPlatformId())

        // 去auth微服务校验资源权限
        val checkRequest = CheckPermissionRequest(
            uid = userId,
            appId = platformId,
            resourceType = type,
            action = action,
            projectId = projectId,
            repoName = repoName,
            path = path
        )
        if (permissionResource.checkPermission(checkRequest).data != true) {
            // 无权限，响应403错误
            throw PermissionException()
        }
        if (logger.isDebugEnabled) {
            logger.debug("User[${SecurityUtils.getPrincipal()}] check permission success.")
        }
    }

    /**
     * 判断是否为管理员
     */
    private fun isAdminUser(userId: String): Boolean {
        return userResource.detail(userId).data?.admin == true
    }

    companion object {

        private val logger = LoggerFactory.getLogger(PermissionManager::class.java)

        /**
         * 检查是否为匿名用户，如果是匿名用户则返回401并提示登录
         */
        private fun checkAnonymous(userId: String, platformId: String?) {
            if (userId == ANONYMOUS_USER && platformId == null) {
                throw AuthenticationException()
            }
        }
    }
}
