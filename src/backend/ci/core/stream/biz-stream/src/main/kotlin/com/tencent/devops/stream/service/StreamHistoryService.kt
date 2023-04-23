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

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.pojo.StreamBuildBranch
import com.tencent.devops.stream.pojo.StreamBuildHistory
import com.tencent.devops.stream.pojo.StreamBuildHistorySearch
import com.tencent.devops.stream.pojo.StreamGitRequestEventReq
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.stream.util.StreamTriggerMessageUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StreamHistoryService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val streamGitProjectInfoCache: StreamGitProjectInfoCache
) {
    companion object {
        private val logger = LoggerFactory.getLogger(StreamHistoryService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    @Suppress("ComplexMethod")
    fun getHistoryBuildList(
        userId: String,
        gitProjectId: Long,
        search: StreamBuildHistorySearch?
    ): Page<StreamBuildHistory> {
        logger.info("StreamHistoryService|getHistoryBuildList|gitProjectId|$gitProjectId")
        val pageNotNull = search?.page ?: 1
        val pageSizeNotNull = search?.pageSize ?: 10
        val conf = streamBasicSettingService.getStreamBasicSettingAndCheck(gitProjectId)

        val buildIds = if (!search?.status.isNullOrEmpty()) {
            // 如果查询条件有状态信息，需要到引擎里面匹配，拿到buildIds之后再在event build 表里面进行其他条件匹配
            client.get(ServiceBuildResource::class).getBuilds(
                userId = userId,
                projectId = conf.projectCode!!,
                pipelineId = search?.pipelineId,
                buildStatus = search?.status,
                channelCode = channelCode
            ).data
        } else null

        if (buildIds?.isEmpty() == true) {
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0,
                records = emptyList()
            )
        }

        val totalPage = gitRequestEventBuildDao.getRequestEventBuildListMultipleCount(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            branchName = search?.branch,
            sourceGitProjectId = search?.sourceGitProjectId,
            triggerUser = search?.triggerUser,
            pipelineId = search?.pipelineId,
            event = search?.event?.map { it.value }?.toSet(),
            commitMsg = search?.commitMsg,
            buildStatus = null,
            pipelineIds = search?.pipelineIds,
            buildIds = buildIds?.toSet()
        )
        if (totalPage == 0) {
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0,
                records = emptyList()
            )
        }
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page = pageNotNull, pageSize = pageSizeNotNull)
        val gitRequestBuildList = gitRequestEventBuildDao.getRequestEventBuildListMultiple(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            branchName = search?.branch,
            sourceGitProjectId = search?.sourceGitProjectId,
            triggerUser = search?.triggerUser,
            pipelineId = search?.pipelineId,
            event = search?.event?.map { it.value }?.toSet(),
            commitMsg = search?.commitMsg,
            buildStatus = null,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset,
            pipelineIds = search?.pipelineIds,
            buildIds = buildIds?.toSet()
        )
        val builds = gitRequestBuildList.map { it.buildId }.toSet()
        logger.info("StreamHistoryService|getHistoryBuildList|builds|$builds")
        val buildHistoryList =
            client.get(ServiceBuildResource::class).getBatchBuildStatus(conf.projectCode!!, builds, channelCode).data
        if (null == buildHistoryList) {
            logger.info("StreamHistoryService|getHistoryBuildList|empty|gitProjectId|$gitProjectId")
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0,
                records = emptyList()
            )
        }
        val records = mutableListOf<StreamBuildHistory>()
        gitRequestBuildList.forEach {
            val buildHistory = getBuildHistory(
                buildId = it.buildId,
                buildHistoryList = buildHistoryList
            ) ?: return@forEach
            val gitRequestEvent = gitRequestEventDao.getWithEvent(
                dslContext = dslContext,
                id = it.eventId
            ) ?: return@forEach
            // 如果是来自fork库的分支，单独标识
            val realEvent =
                if (gitProjectId == gitRequestEvent.gitProjectId) {
                    val pathWithNamespace = gitRequestEvent.sourceGitProjectId?.let {
                        streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                            gitProjectId = it,
                            useAccessToken = true,
                            userId = conf.enableUserId
                        )
                    }?.pathWithNamespace
                    GitCommonUtils.checkAndGetForkBranch(gitRequestEvent, pathWithNamespace)
                } else {
                    // 当gitProjectId与event的不同时，说明是远程仓库触发的
                    val pathWithNamespace = streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                        gitProjectId = gitRequestEvent.gitProjectId,
                        useAccessToken = true,
                        userId = conf.enableUserId
                    )?.pathWithNamespace
                    GitCommonUtils.checkAndGetRepoBranch(gitRequestEvent, pathWithNamespace)
                }
            val pipeline =
                pipelineResourceDao.getPipelineById(dslContext, gitProjectId, it.pipelineId) ?: return@forEach
            records.add(
                StreamBuildHistory(
                    displayName = pipeline.displayName,
                    pipelineId = pipeline.pipelineId,
                    gitRequestEvent = StreamGitRequestEventReq(realEvent, conf.homepage),
                    buildHistory = buildHistory,
                    reason = StreamTriggerMessageUtils.getEventMessageTitle(realEvent)
                )
            )
        }
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = totalPage.toLong(),
            records = records.sortedByDescending { it.buildHistory?.buildNum }
        )
    }

    fun getProjectLocalBranches(
        projectId: Long,
        branchName: String?,
        page: Int,
        pageSize: Int
    ): List<String> {
        return gitRequestEventBuildDao.getProjectLocalBranches(
            dslContext = dslContext,
            projectId = projectId,
            branchName = branchName,
            pageNotNull = page,
            pageSizeNotNull = pageSize
        )
    }

    @Suppress("LongMethod")
    fun getAllBuildBranchList(
        userId: String,
        gitProjectId: Long,
        page: Int?,
        pageSize: Int?,
        keyword: String?
    ): Page<StreamBuildBranch> {
        logger.info("StreamHistoryService|getAllBuildBranchList|gitProjectId|$gitProjectId")
        val conf = streamBasicSettingService.getStreamBasicSettingAndCheck(gitProjectId)
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val buildBranchList = gitRequestEventBuildDao.getAllBuildBranchList(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            page = null,
            pageSize = null,
            keyword = keyword
        )
        if (buildBranchList.isEmpty()) {
            logger.info("StreamHistoryService|getAllBuildBranchList|empty|gitProjectId|$gitProjectId")
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0,
                records = emptyList()
            )
        }
        // 因为涉及到分组，selectCount无法拿到具体条数，所以拿出来全部查询自己分页
        val firstIndex = (pageNotNull - 1) * pageSizeNotNull
        val lastIndex = if (pageNotNull * pageSizeNotNull > buildBranchList.size) {
            buildBranchList.size
        } else {
            pageNotNull * pageSizeNotNull
        }
        // 过滤掉重复的分支(mr和push会重复)
        val recordsMap = mutableMapOf<String, StreamBuildBranch>()
        // 如果是来自fork库的分支，单独标识
        buildBranchList.subList(firstIndex, lastIndex).forEach {
            val pathWithNamespace = it.sourceGitProjectId?.let { id ->
                streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                    gitProjectId = id,
                    useAccessToken = true,
                    userId = conf.enableUserId
                )
            }?.pathWithNamespace
            val branchName = GitCommonUtils.checkAndGetForkBranchName(
                gitProjectId = it.gitProjectId,
                sourceGitProjectId = it.sourceGitProjectId,
                branch = it.branch,
                pathWithNamespace = pathWithNamespace
            )
            recordsMap[branchName] = StreamBuildBranch(
                branchName = GitCommonUtils.checkAndGetForkBranchName(
                    gitProjectId = it.gitProjectId,
                    sourceGitProjectId = it.sourceGitProjectId,
                    branch = it.branch,
                    pathWithNamespace = pathWithNamespace
                ),
                gitProjectId = it.gitProjectId,
                sourceGitProjectId = it.sourceGitProjectId
            )
        }
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = buildBranchList.size.toLong(),
            records = recordsMap.values.toList()
        )
    }

    private fun getBuildHistory(
        buildId: String,
        buildHistoryList: List<BuildHistory>
    ): BuildHistory? {
        buildHistoryList.forEach { build ->
            if (build.id != buildId) {
                return@forEach
            }
            return build
        }
        return null
    }
}
