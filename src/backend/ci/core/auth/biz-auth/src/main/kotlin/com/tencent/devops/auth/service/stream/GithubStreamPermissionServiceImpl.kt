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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.auth.service.stream

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.github.ServiceGithubPermissionResource
import com.tencent.devops.stream.api.service.ServiceStreamBasicSettingResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class GithubStreamPermissionServiceImpl @Autowired constructor(
    val client: Client
) : StreamPermissionServiceImpl() {

    private val publicProjectCache =
        CacheBuilder.newBuilder()
            .maximumSize(MAX_SIZE)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build<String, Boolean?>()

    private val projectAuthUserCache =
        CacheBuilder.newBuilder()
            .maximumSize(MAX_SIZE)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build<String, String>()

    private val projectMemberCache =
        CacheBuilder.newBuilder()
            .maximumSize(MAX_SIZE)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build<String, Boolean?>()

    private val projectExecuteCache =
        CacheBuilder.newBuilder()
            .maximumSize(MAX_SIZE)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build<String, Boolean?>()

    override fun isPublicProject(projectCode: String, userId: String?): Boolean {
        if (publicProjectCache.getIfPresent(projectCode) != null) {
            return publicProjectCache.getIfPresent(projectCode)!!
        }

        val authUser = getProjectAuthUser(projectCode, userId!!)
        val publicProject = client.get(ServiceGithubPermissionResource::class)
            .isPublicProject(authUser, getGitProjectId(projectCode)).data
        publicProjectCache.put(projectCode, publicProject!!)
        return publicProject
    }

    override fun isProjectMember(projectCode: String, userId: String): Pair<Boolean, Boolean> {
        val authUser = getProjectAuthUser(projectCode, userId)
        // 是否是项目成员
        val checkProjectMember = checkProjectMemeber(projectCode, userId, authUser)
        if (!checkProjectMember) {
            return Pair(false, false)
        }
        val projectExecute = checkProjectExecutePermission(projectCode, userId, authUser)
        return Pair(checkProjectMember, projectExecute)
    }

    override fun extPermission(
        projectCode: String,
        userId: String,
        action: AuthPermission,
        resourceType: String
    ): Boolean {
        return false
    }

    // 获取github项目在stream内创建auth的用户名（github stream项目的开启人）
    private fun getProjectAuthUser(projectCode: String, userId: String): String {
        if (!projectAuthUserCache.getIfPresent(projectCode).isNullOrEmpty()) {
            return projectAuthUserCache.getIfPresent(projectCode)!!
        }
        val projectInfo = client.get(ServiceStreamBasicSettingResource::class)
            .getStreamConf(projectCode).data
        return if (projectInfo == null) {
            userId
        } else {
            val projectAuthUser = projectInfo.enableUserId
            projectAuthUserCache.put(projectCode, projectAuthUser)
            projectAuthUser
        }
    }

    private fun projectMemberKey(projectCode: String, userId: String): String {
        return projectCode + userId
    }

    private fun checkProjectMemeber(
        projectCode: String,
        userId: String,
        authUser: String
    ): Boolean {
        var projectMember: Boolean?
        projectMember = projectMemberCache.getIfPresent(projectMemberKey(projectCode, userId))
        if (projectMember == null) {
            // 是否是项目成员
            projectMember = client.get(ServiceGithubPermissionResource::class).isProjectMember(
                authUserId = authUser,
                userId = userId,
                gitProjectId = getGitProjectId(projectCode)
            ).data
            projectMemberCache.put(projectMemberKey(projectCode, userId), projectMember!!)
        }
        return projectMember
    }

    private fun checkProjectExecutePermission(
        projectCode: String,
        userId: String,
        authUser: String
    ): Boolean {
        var executePermission: Boolean?
        executePermission = projectExecuteCache.getIfPresent(projectMemberKey(projectCode, userId))
        if (executePermission == null) {
            // 是否有操作权限
            executePermission = client.get(ServiceGithubPermissionResource::class).checkUserAuth(
                authUserId = authUser,
                userId = userId,
                gitProjectId = getGitProjectId(projectCode),
                accessLevel = ACCESSLEVEL
            ).data
            projectMemberCache.put(projectMemberKey(projectCode, userId), executePermission!!)
        }
        return executePermission
    }

    fun getGitProjectId(projectCode: String): String {
        return if (projectCode.contains(GITHUB_PROJECT_PREFIX)) {
            projectCode.substringAfter(GITHUB_PROJECT_PREFIX)
        } else {
            projectCode
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(GithubStreamPermissionServiceImpl::class.java)
        const val MAX_SIZE = 500L
        const val ACCESSLEVEL = 30
        private const val GITHUB_PROJECT_PREFIX = "github_"
    }
}
