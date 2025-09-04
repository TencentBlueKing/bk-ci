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

package com.tencent.devops.remotedev.service

import com.tencent.devops.remotedev.pojo.RemoteDevGitType
import com.tencent.devops.remotedev.pojo.RemoteDevRepository
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongMethod")
class RepositoryService @Autowired constructor(
    private val remoteDevGitTransfer: RemoteDevGitTransfer,
    private val permissionService: PermissionService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryService::class.java)
        private const val defaultPageSize = 20
    }

    // 获取有权限的代码仓库
    fun getAuthorizedGitRepository(
        userId: String,
        search: String?,
        page: Int?,
        pageSize: Int?,
        gitType: RemoteDevGitType
    ): List<RemoteDevRepository> {
        logger.info("$userId get user git repository|$search|$page|$pageSize")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: defaultPageSize
        return permissionService.checkOauthIllegal(userId) {
            remoteDevGitTransfer.load(gitType).getProjectList(
                userId = userId,
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                search = search,
                owned = false,
                minAccessLevel = GitAccessLevelEnum.DEVELOPER
            )
        }
    }

    // 获取代码库的分支
    fun getRepositoryBranch(
        userId: String,
        pathWithNamespace: String,
        gitType: RemoteDevGitType
    ): List<String> {
        logger.info("$userId get git repository branch list|$pathWithNamespace")
        return permissionService.checkOauthIllegal(userId) {
            remoteDevGitTransfer.load(gitType).getProjectBranches(
                userId = userId,
                pathWithNamespace = pathWithNamespace
            ) ?: emptyList()
        }
    }
}
