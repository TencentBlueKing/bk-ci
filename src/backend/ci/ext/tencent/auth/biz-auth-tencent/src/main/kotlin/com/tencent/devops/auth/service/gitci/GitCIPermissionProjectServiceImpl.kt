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

package com.tencent.devops.auth.service.gitci

import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class GitCIPermissionProjectServiceImpl @Autowired constructor(
    val client: Client,
    val projectInfoService: GitCiProjectInfoService
) : PermissionProjectService {

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        val gitProjectId = GitCIUtils.getGitCiProjectId(projectCode)

        val gitProjectMembers = client.getScm(ServiceGitCiResource::class).getProjectMembersAll(
            gitProjectId = gitProjectId,
            page = 0,
            pageSize = 1000,
            search = ""
        ).data
        logger.info("$projectCode project member  $gitProjectMembers")
        if (gitProjectMembers.isNullOrEmpty()) {
            return emptyList()
        }

        return gitProjectMembers.map { it.username }
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        return emptyList()
    }

    override fun getUserProjects(userId: String): List<String> {
        return emptyList()
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        val gitProjectId = GitCIUtils.getGitCiProjectId(projectCode)

        // 判断是否为开源项目, 校验非管理员 若是开源项目直接放行
        if (projectInfoService.checkProjectPublic(gitProjectId)) {
            return true
        }
        return checkProjectUser(userId, gitProjectId, projectCode)
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        return true
    }

    override fun createProjectUser(userId: String, projectCode: String, role: String): Boolean {
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return emptyList()
    }

    fun checkProjectUser(userId: String, gitProjectId: String, projectCode: String): Boolean {
        val gitUserId = projectInfoService.getGitUserByRtx(userId, gitProjectId)
        if (gitUserId.isNullOrEmpty()) {
            logger.warn("$userId is not gitCI user")
            return false
        }
        val projectUser = mutableListOf<String>()
        client.getScm(ServiceGitCiResource::class).getProjectMembersAll(
            gitProjectId = gitProjectId,
            page = 0,
            pageSize = 100,
            search = userId
        ).data?.forEach {
            projectUser.add(it.username)
        }
        if (projectUser.isNotEmpty() && projectUser.contains(userId)) {
            return true
        }
        logger.warn("$projectCode $userId is project check fail")
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(GitCIPermissionProjectServiceImpl::class.java)
    }
}
