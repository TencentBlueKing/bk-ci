/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.stream.resources.user

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.user.UserStreamHistoryResource
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.StreamBuildBranch
import com.tencent.devops.stream.pojo.StreamBuildHistory
import com.tencent.devops.stream.pojo.StreamBuildHistorySearch
import com.tencent.devops.stream.service.StreamHistoryService
import com.tencent.devops.stream.util.GitCommonUtils
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStreamHistoryResourceImpl @Autowired constructor(
    private val streamHistoryService: StreamHistoryService,
    private val permissionService: StreamPermissionService
) : UserStreamHistoryResource {

    companion object {
        val logger = LoggerFactory.getLogger(UserStreamHistoryResourceImpl::class.java)
    }

    private val cache: Cache<String, Page<StreamBuildHistory>> = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.SECONDS)
        .build()

    override fun getHistoryBuildList(
        userId: String,
        projectId: String,
        search: StreamBuildHistorySearch?
    ): Result<Page<StreamBuildHistory>> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW
        )
        checkParam(userId)
        val key = "$userId$projectId$search"
        val res = cache.getIfPresent(key)
        if (res != null) {
            logger.info("getHistoryBuildList from cache |$key")
            return Result(res)
        }
        val set = streamHistoryService.getHistoryBuildList(
            userId = userId,
            gitProjectId = gitProjectId,
            search = search
        )
        cache.put(key, set)
        return Result(set)
    }

    override fun getAllBuildBranchList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        keyword: String?
    ): Result<Page<StreamBuildBranch>> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)
        checkParam(userId)
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW
        )
        return Result(
            streamHistoryService.getAllBuildBranchList(
                userId = userId,
                gitProjectId = gitProjectId,
                page = page ?: 1,
                pageSize = pageSize ?: 20,
                keyword = keyword
            )
        )
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
