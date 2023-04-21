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
 *
 */

package com.tencent.devops.repository.resources.github

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.sdk.github.request.GetRepositoryPermissionsRequest
import com.tencent.devops.common.sdk.github.request.GetRepositoryRequest
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.github.ServiceGithubPermissionResource
import com.tencent.devops.repository.github.service.GithubRepositoryService
import com.tencent.devops.repository.pojo.enums.GithubAccessLevelEnum
import com.tencent.devops.repository.service.github.GithubTokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGithubPermissionResourceImpl @Autowired constructor(
    private val githubTokenService: GithubTokenService,
    private val githubRepositoryService: GithubRepositoryService
) : ServiceGithubPermissionResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceGithubPermissionResourceImpl::class.java)
    }

    override fun isPublicProject(authUserId: String, gitProjectId: String): Result<Boolean> {
        val repository = githubRepositoryService.getRepository(
            request = GetRepositoryRequest(
                repoName = gitProjectId
            ),
            token = githubTokenService.getAccessTokenMustExist(authUserId).accessToken
        )
        return Result(!repository.private)
    }

    override fun isProjectMember(authUserId: String, userId: String, gitProjectId: String): Result<Boolean> {
        return try {
            githubRepositoryService.getRepositoryPermissions(
                request = GetRepositoryPermissionsRequest(
                    repoName = gitProjectId,
                    username = userId
                ),
                token = githubTokenService.getAccessTokenMustExist(authUserId).accessToken
            )
            Result(true)
        } catch (ignore: Exception) {
            logger.error(
                "Fail to get github repository permissions|" +
                    "authUserId:$authUserId|gitProjectId:$gitProjectId|userId:$userId",
                ignore
            )
            Result(false)
        }
    }

    override fun checkUserAuth(
        authUserId: String,
        userId: String,
        gitProjectId: String,
        accessLevel: Int
    ): Result<Boolean> {
        return try {
            val repositoryPermissions = githubRepositoryService.getRepositoryPermissions(
                request = GetRepositoryPermissionsRequest(
                    repoName = gitProjectId,
                    username = userId
                ),
                token = githubTokenService.getAccessTokenMustExist(authUserId).accessToken
            )
            val level = GithubAccessLevelEnum.getGithubAccessLevel(repositoryPermissions.permission).level
            Result(level >= accessLevel)
        } catch (ignore: Exception) {
            logger.error(
                "Fail to get github repository permissions|" +
                    "authUserId:$authUserId|gitProjectId:$gitProjectId|userId:$userId",
                ignore
            )
            Result(false)
        }
    }
}
