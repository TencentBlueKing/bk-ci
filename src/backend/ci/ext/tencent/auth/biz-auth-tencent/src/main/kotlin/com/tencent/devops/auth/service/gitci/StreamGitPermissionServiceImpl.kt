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

import com.tencent.devops.auth.ScmRetryUtils
import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.auth.service.stream.StreamPermissionServiceImpl
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class StreamGitPermissionServiceImpl @Autowired constructor(
    val client: Client,
    val managerService: ManagerService,
    val projectInfoService: GitProjectInfoService
) : StreamPermissionServiceImpl() {
    override fun isPublicProject(projectCode: String, userId: String?): Boolean {
        val gitProjectId = GitCIUtils.getGitCiProjectId(projectCode)
        return projectInfoService.checkProjectPublic(gitProjectId)
    }

    override fun isProjectMember(projectCode: String, userId: String): Pair<Boolean, Boolean> {
        val gitProjectId = GitCIUtils.getGitCiProjectId(projectCode)
        val gitUserId = projectInfoService.getGitUserByRtx(userId, gitProjectId)
        if (gitUserId.isNullOrEmpty()) {
            logger.warn("$userId is not gitCI user")
            return Pair(first = false, second = false)
        }
        return if (checkProjectUser(userId = userId, gitProjectId = gitProjectId)) {
            if (checkDeveloper(gitUserId = gitUserId, gitProjectId = gitProjectId)) {
                Pair(first = true, second = true)
            } else {
                Pair(first = true, second = false)
            }
        } else {
            Pair(first = false, second = false)
        }
    }

    override fun extPermission(
        projectCode: String,
        userId: String,
        action: AuthPermission,
        resourceType: String
    ): Boolean {
        return try {
            val resourceType = AuthResourceType.get(resourceType)
            managerService.isManagerPermission(
                userId = userId,
                projectId = projectCode,
                authPermission = action,
                resourceType = resourceType
            )
        } catch (e: Exception) {
            // 管理员逻辑报错不影响主流程
            logger.warn("reviewManagerCheck change enum fail $projectCode $action $resourceType")
            false
        }
    }

    private fun checkDeveloper(gitUserId: String, gitProjectId: String): Boolean {
        return try {
            val checkResult = ScmRetryUtils.callScm(0, logger) {
                client.getScm(ServiceGitCiResource::class)
                    .checkUserGitAuth(
                        userId = gitUserId,
                        gitProjectId = gitProjectId,
                        accessLevel = 30
                    ).data ?: false
            }
            if (!checkResult) {
                logger.warn("$gitUserId not $gitProjectId developerUp")
            }
            checkResult
        } catch (e: Exception) {
            logger.warn("$gitUserId $gitProjectId checkDeveloper fail $e")
            false
        }
    }

    private fun checkProjectUser(userId: String, gitProjectId: String): Boolean {
        try {
            val projectUser = mutableListOf<String>()
            ScmRetryUtils.callScm(0, logger) {
                client.getScm(ServiceGitCiResource::class).getProjectMembersAll(
                    gitProjectId = gitProjectId,
                    page = 0,
                    pageSize = 100,
                    search = userId
                ).data?.forEach {
                    projectUser.add(it.username)
                }
            }
            if (projectUser.isNotEmpty() && projectUser.contains(userId)) {
                return true
            }
            logger.warn("$gitProjectId $userId is project check fail")
            return false
        } catch (re: RuntimeException) {
            // scm非项目成员会直接报错。 catch异常直接给false
            logger.warn("$userId checkProjectUser $gitProjectId fail")
            return false
        } catch (e: Exception) {
            logger.error("maybe network fail. $e")
            return false
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(StreamGitPermissionServiceImpl::class.java)
    }
}
