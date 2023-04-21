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

package com.tencent.devops.stream.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamCIInfo
import com.tencent.devops.stream.pojo.StreamProjectCIInfo
import com.tencent.devops.stream.pojo.StreamProjectGitInfo
import com.tencent.devops.stream.pojo.StreamProjectSimpleInfo
import com.tencent.devops.stream.pojo.enums.StreamProjectType
import com.tencent.devops.stream.pojo.enums.StreamProjectsOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc
import com.tencent.devops.stream.util.GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.max

@Service
class StreamProjectService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val streamGitTransferService: StreamGitTransferService
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
        type: StreamProjectType?,
        search: String?,
        page: Int?,
        pageSize: Int?,
        orderBy: StreamProjectsOrder?,
        sort: StreamSortAscOrDesc?
    ): Pagination<StreamProjectCIInfo> {
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

        val gitProjects = gitProjects(
            userId = userId,
            type = type,
            realPage = realPage,
            realPageSize = realPageSize,
            search = search,
            orderBy = orderBy ?: StreamProjectsOrder.UPDATE,
            sort = sort ?: StreamSortAscOrDesc.DESC
        )
        if (gitProjects.isNullOrEmpty()) {
            return Pagination(false, emptyList())
        }
        val projectIdMap = streamBasicSettingDao.searchProjectByIds(
            dslContext = dslContext,
            projectIds = gitProjects.map { it.id }.toSet()
        ).associateBy { it.id }
        val result = gitProjects.map {
            val project = projectIdMap[it.id]
            // 针对创建流水线异常时，现有代码会创建event记录
            val streamCiInfo = if (project?.lastCiInfo == null) {
                StreamCIInfo(
                    enableCI = project?.enableCi ?: false,
                    lastBuildId = null,
                    lastBuildStatus = null,
                    lastBuildPipelineId = null,
                    lastBuildMessage = null
                )
            } else {
                JsonUtil.to(project.lastCiInfo, object : TypeReference<StreamCIInfo>() {})
            }
            StreamProjectCIInfo(
                id = it.id,
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
                ciInfo = streamCiInfo
            )
        }
        return Pagination(
            hasNext = gitProjects.size == realPageSize,
            records = result
        )
    }

    private fun gitProjects(
        userId: String,
        type: StreamProjectType?,
        search: String?,
        realPage: Int,
        realPageSize: Int,
        orderBy: StreamProjectsOrder?,
        sort: StreamSortAscOrDesc?
    ): List<StreamProjectGitInfo>? {
        return if (search.isNullOrBlank()) {
            val cacheList = cacheProjectList(userId)
            if (cacheList.isEmpty()) {
                logger.info("STREAM|gitProjects|This does not exist in redis|userId=$userId")
                return null
            }
            val start = ((realPage - 1) * realPageSize).takeIf { it < cacheList.size && it >= 0 } ?: cacheList.size
            val end = (realPage * realPageSize).takeIf { it < cacheList.size && it >= 0 } ?: cacheList.size
            cacheList.subList(start, end).map { StreamProjectGitInfo(it) }
        } else {
            streamGitTransferService.getProjectList(
                userId = userId,
                page = realPage,
                pageSize = realPageSize,
                search = search,
                orderBy = orderBy ?: StreamProjectsOrder.ACTIVITY,
                sort = sort ?: StreamSortAscOrDesc.DESC,
                owned = null,
                minAccessLevel = if (type == StreamProjectType.MY_PROJECT) {
                    GitAccessLevelEnum.DEVELOPER
                } else {
                    null
                }
            )
        }
    }

    /**
     *  只会在首次访问getProjectList 时缓存一次.
     */
    fun cacheProjectList(userId: String): List<StreamProjectSimpleInfo> {
        val res = redisOperation.get(getProjectListKey(userId))
        if (res.isNullOrEmpty()) {
            logger.info("STREAM|gitProjects|This does not exist in redis, so create it|userId=$userId")
            val projectList = mutableListOf<StreamProjectSimpleInfo>()
            var page = 1
            do {
                val list = streamGitTransferService.getProjectList(
                    userId = userId,
                    page = page,
                    pageSize = 75,
                    search = null,
                    orderBy = StreamProjectsOrder.ACTIVITY,
                    sort = StreamSortAscOrDesc.DESC,
                    owned = null,
                    minAccessLevel = GitAccessLevelEnum.DEVELOPER
                ) ?: emptyList()
                val settings = streamBasicSettingDao.searchProjectByIds(
                    dslContext = dslContext,
                    projectIds = list.map { it.id }.toSet()
                ).associateBy { it.id }
                list.map { item ->
                    projectList.add(
                        StreamProjectSimpleInfo(
                            id = item.id,
                            pathWithNamespace = item.pathWithNamespace,
                            description = item.description,
                            avatarUrl = item.avatarUrl,
                            enabledCi = settings[item.id]?.enableCi ?: false,
                            projectCode = settings[item.id]?.projectCode,
                            public = item.public,
                            name = item.name,
                            httpsUrlToRepo = item.httpsUrlToRepo,
                            webUrl = item.webUrl
                        )
                    )
                }
                page += 1
            } while (list.isNotEmpty() && page < 3)
            if (projectList.isEmpty()) {
                return emptyList()
            }
            val updateLock = RedisLock(redisOperation, getProjectListLockKey(userId), 10)
            updateLock.lock()
            try {
                logger.info("STREAM|gitProjects|update redis|userId=$userId")
                redisOperation.set(
                    getProjectListKey(userId),
                    JsonUtil.toJson(projectList),
                    TimeUnit.MINUTES.toSeconds(1440)
                )
            } finally {
                updateLock.unlock()
            }
            return projectList
        } else {
            return JsonUtil.to(res, object : TypeReference<List<StreamProjectSimpleInfo>>() {})
        }
    }

    fun addUserProjectHistory(userId: String, projectId: String) {
        val key = "$STREAM_USER_PROJECT_HISTORY_SET:$userId"
        redisOperation.zadd(key, projectId, LocalDateTime.now().timestamp().toDouble())
        val size = redisOperation.zsize(key) ?: 0
        if (size > MAX_STREAM_USER_HISTORY_LENGTH) {
            // redis zset rank 是从小到大排列所以最新的一般在最后面, 从0起前往后删，有几个删几个
            redisOperation.zremoveRange(key, 0, size - MAX_STREAM_USER_HISTORY_LENGTH - 1)
        }
        // 连续90天未访问应该让该Key失效，因各种原因可能不再访问这项服务，所以不应该永久保留Key成为无用脏数据
        redisOperation.expire(key, expiredInSecond = TimeUnit.DAYS.toSeconds(MAX_STREAM_USER_HISTORY_DAYS))
    }

    fun getUserProjectHistory(userId: String, size: Long): List<StreamProjectCIInfo>? {
        val key = "$STREAM_USER_PROJECT_HISTORY_SET:$userId"
        // 先清理3个月前过期数据
        val expiredTime = LocalDateTime.now().timestamp() - TimeUnit.DAYS.toSeconds(MAX_STREAM_USER_HISTORY_DAYS)
        redisOperation.zremoveRangeByScore(key, 0.0, expiredTime.toDouble())

        val gitProjectIds = redisOperation.zrevrange(
            key = key,
            start = 0,
            end = max(size - 1, 0)
        )?.map { GitCommonUtils.getGitProjectId(it) }

        if (gitProjectIds.isNullOrEmpty()) { // 防止let里的return产生的理解成本：到底是在let闭包返回，还是从当前函数返回
            return null
        }

        val settings = streamBasicSettingDao.getBasicSettingList(dslContext, gitProjectIds, null, null)
            .associateBy { it.id }
        val result = mutableListOf<StreamProjectCIInfo>()
        gitProjectIds.forEach {
            val setting = settings[it] ?: return@forEach
            result.add(
                StreamProjectCIInfo(
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
                        StreamCIInfo(
                            enableCI = setting.enableCi ?: false,
                            lastBuildId = null,
                            lastBuildStatus = null,
                            lastBuildPipelineId = null,
                            lastBuildMessage = null
                        )
                    } else {
                        JsonUtil.to(setting.lastCiInfo, object : TypeReference<StreamCIInfo>() {})
                    }
                )
            )
        }
        return result
    }
}
