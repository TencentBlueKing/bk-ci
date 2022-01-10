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

package com.tencent.devops.stream.v2.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.stream.pojo.enums.GitCIProjectType
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.stream.pojo.v2.project.CIInfo
import com.tencent.devops.stream.pojo.v2.project.ProjectCIInfo
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.repository.pojo.enums.GitAccessLevelEnum
import com.tencent.devops.scm.pojo.GitCodeBranchesSort
import com.tencent.devops.scm.pojo.GitCodeProjectsOrder
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.scm.pojo.GitCodeProjectInfo
import com.tencent.devops.stream.pojo.GitCodeProjectSimpleInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class StreamProjectService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val streamScmService: StreamScmService,
    private val oauthService: StreamOauthService,
    private val streamBasicSettingDao: StreamBasicSettingDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamProjectService::class.java)
        private const val STREAM_USER_PROJECT_HISTORY_SET = "stream:user:project:history:set"
        private const val MAX_STREAM_USER_HISTORY_LENGTH = 10
        private const val MAX_STREAM_USER_HISTORY_DAYS = 90L
        private const val STREAM_GIT_PROJECT_LIST_UPDATE_LOCK_PREFIX = "stream:git:projectList:lock:key:"
        private const val STREAM_GIT_PROJECT_LIST_PREFIX = "stream:git:projectList:"
        fun getProjectListKey(userId: String) = STREAM_GIT_PROJECT_LIST_PREFIX + userId
        fun getProjectListLockKey(userId: String) = STREAM_GIT_PROJECT_LIST_UPDATE_LOCK_PREFIX + userId
    }

    fun getProjectList(
        userId: String,
        type: GitCIProjectType?,
        search: String?,
        page: Int?,
        pageSize: Int?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?
    ): Pagination<ProjectCIInfo> {
        val realPage = if (page == null || page <= 0) {
            1
        } else {
            page
        }
        val realPageSize = if (pageSize == null || pageSize <= 0) {
            10
        } else {
            pageSize
        }
        val token = oauthService.getAndCheckOauthToken(userId).accessToken
        val gitProjects = gitProjects(
            token = token,
            userId = userId,
            type = type,
            realPage = realPage,
            realPageSize = realPageSize,
            search = search,
            orderBy = orderBy ?: GitCodeProjectsOrder.UPDATE,
            sort = sort ?: GitCodeBranchesSort.DESC
        )
        if (gitProjects.isNullOrEmpty()) {
            return Pagination(false, emptyList())
        }
        val projectIdMap = streamBasicSettingDao.searchProjectByIds(
            dslContext = dslContext,
            projectIds = gitProjects.map { it.id!! }.toSet()
        ).associateBy { it.id }
        val result = gitProjects.map {
            val project = projectIdMap[it.id]
            // 针对创建流水线异常时，现有代码会创建event记录
            val ciInfo = if (project?.lastCiInfo == null) {
                CIInfo(
                    enableCI = project?.enableCi ?: false,
                    lastBuildId = null,
                    lastBuildStatus = null,
                    lastBuildPipelineId = null,
                    lastBuildMessage = null
                )
            } else {
                JsonUtil.to(project.lastCiInfo, object : TypeReference<CIInfo>() {})
            }
            ProjectCIInfo(
                id = it.id!!,
                projectCode = project?.projectCode,
                public = it.public,
                name = it.name,
                nameWithNamespace = it.pathWithNamespace,
                httpsUrlToRepo = it.httpsUrlToRepo,
                webUrl = it.webUrl,
                avatarUrl = it.avatarUrl,
                description = it.description,
                enableCI = project?.enableCi,
                buildPushedBranches = project?.buildPushedBranches,
                buildPushedPullRequest = project?.buildPushedPullRequest,
                enableMrBlock = project?.enableMrBlock,
                authUserId = project?.enableUserId,
                ciInfo = ciInfo
            )
        }
        return Pagination(
            hasNext = gitProjects.size == realPageSize,
            records = result
        )
    }

    private fun gitProjects(
        token: String,
        userId: String,
        type: GitCIProjectType?,
        search: String?,
        realPage: Int?,
        realPageSize: Int?,
        orderBy: GitCodeProjectsOrder?,
        sort: GitCodeBranchesSort?
    ): List<GitCodeProjectInfo>? {
        val gitProjects = try {
            streamScmService.getProjectList(
                accessToken = token,
                userId = userId,
                page = realPage,
                pageSize = realPageSize,
                search = search,
                orderBy = orderBy ?: GitCodeProjectsOrder.UPDATE,
                sort = sort ?: GitCodeBranchesSort.DESC,
                owned = null,
                minAccessLevel = if (type == GitCIProjectType.MY_PROJECT) {
                    GitAccessLevelEnum.DEVELOPER
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            logger.warn(
                "STREAM|gitProjects|stream scm service is unavailable.|userId=$userId|" +
                    "realPage=$realPage|realPageSize=$realPageSize"
            )
            val res = redisOperation.get(getProjectListKey("$userId-$realPage-$realPageSize"))
            if (res.isNullOrEmpty()) {
                logger.info("STREAM|gitProjects|This does not exist in redis|userId=$userId")
                return null
            }
            return JsonUtil.to(res, object : TypeReference<List<GitCodeProjectInfo>>() {})
        } ?: return null
        // 每次成功访问工蜂接口就刷新redis
        val updateLock = RedisLock(redisOperation, getProjectListLockKey("$userId-$realPage-$realPageSize"), 10)
        updateLock.lock()
        try {
            logger.info("STREAM|gitProjects|update redis|userId=$userId|realPage=$realPage|realPageSize=$realPageSize")
            val newRedisValue = gitProjects.map {
                GitCodeProjectSimpleInfo(
                    id = it.id,
                    pathWithNamespace = it.pathWithNamespace,
                    description = it.description,
                    avatarUrl = it.avatarUrl
                )
            }
            redisOperation.set(
                getProjectListKey("$userId-$realPage-$realPageSize"),
                JsonUtil.toJson(newRedisValue),
                TimeUnit.MINUTES.toSeconds(60)
            )
        } finally {
            updateLock.unlock()
        }
        return gitProjects
    }

    fun addUserProjectHistory(
        userId: String,
        projectId: String
    ) {
        val key = "$STREAM_USER_PROJECT_HISTORY_SET:$userId"
        redisOperation.zadd(key, projectId, LocalDateTime.now().timestamp().toDouble())
        val size = redisOperation.zsize(key) ?: 0
        if (size > MAX_STREAM_USER_HISTORY_LENGTH) {
            // redis zset rank 是从小到大排列所以最新的一般在最后面, 从0起前往后删，有几个删几个
            redisOperation.zremoveRange(key, 0, size - MAX_STREAM_USER_HISTORY_LENGTH - 1)
        }
    }

    fun getUserProjectHistory(
        userId: String,
        size: Long
    ): List<ProjectCIInfo>? {
        val key = "$STREAM_USER_PROJECT_HISTORY_SET:$userId"
        // 先清理3个月前过期数据
        val expiredTime = LocalDateTime.now().timestamp() - TimeUnit.DAYS.toSeconds(MAX_STREAM_USER_HISTORY_DAYS)
        redisOperation.zremoveRangeByScore(key, 0.0, expiredTime.toDouble())
        val gitProjectIds = redisOperation.zrevrange(
            key = key,
            start = 0,
            end = if (size - 1 < 0) {
                0
            } else {
                size - 1
            }
        )?.map { it.removePrefix("git_").toLong() }.let {
            if (it.isNullOrEmpty()) {
                return null
            }
            it
        }
        val settings = streamBasicSettingDao.getBasicSettingList(dslContext, gitProjectIds, null, null)
            .associateBy { it.id }
        val result = mutableListOf<ProjectCIInfo>()
        gitProjectIds.forEach {
            val setting = settings[it] ?: return@forEach
            result.add(
                ProjectCIInfo(
                    id = setting.id,
                    projectCode = setting.projectCode,
                    public = null,
                    name = setting.name,
                    nameWithNamespace = GitCommonUtils.getPathWithNameSpace(setting.gitHttpUrl),
                    httpsUrlToRepo = setting.gitHttpUrl,
                    webUrl = setting.homePage,
                    avatarUrl = setting.gitProjectAvatar,
                    description = setting.gitProjectDesc,
                    enableCI = setting.enableCi,
                    buildPushedBranches = setting.buildPushedBranches,
                    buildPushedPullRequest = setting.buildPushedPullRequest,
                    enableMrBlock = setting.enableMrBlock,
                    authUserId = setting.enableUserId,
                    ciInfo = if (setting.lastCiInfo == null) {
                        CIInfo(
                            enableCI = setting.enableCi ?: false,
                            lastBuildId = null,
                            lastBuildStatus = null,
                            lastBuildPipelineId = null,
                            lastBuildMessage = null
                        )
                    } else {
                        JsonUtil.to(setting.lastCiInfo, object : TypeReference<CIInfo>() {})
                    }
                )
            )
        }
        return result
    }
}
