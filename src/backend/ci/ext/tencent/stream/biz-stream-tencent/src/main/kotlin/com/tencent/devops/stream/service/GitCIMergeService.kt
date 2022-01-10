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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.pojo.GitCIBuildHistory
import com.tencent.devops.stream.pojo.GitMergeHistory
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.StreamBasicSettingService
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Suppress("NestedBlockDepth", "ComplexMethod")
@Service
class GitCIMergeService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val streamBasicSettingService: StreamBasicSettingService,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val pipelineResourceDao: GitPipelineResourceDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIMergeService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getMergeBuildList(userId: String, gitProjectId: Long, page: Int?, pageSize: Int?): Page<GitMergeHistory> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        logger.info("get merge build list, gitProjectId: $gitProjectId")
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId)
            ?: throw CustomException(Response.Status.FORBIDDEN, "项目未开启Stream，无法查询")
        val mergeList = gitRequestEventDao.getMergeRequestList(
            dslContext = dslContext,
            gitProjectId = gitProjectId,
            page = pageNotNull,
            pageSize = pageSizeNotNull
        )
        if (mergeList.isEmpty()) {
            logger.info("Get merge request build list return empty, gitProjectId: $gitProjectId")
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0L,
                records = emptyList()
            )
        }

        val mergeHistoryMap = mutableMapOf<Long, GitMergeHistory>()
        mergeList.forEach { event ->
            val mrId = event.mergeRequestId ?: return@forEach
            // 如果是来自fork库的分支，单独标识
            val realEvent = GitCommonUtils.checkAndGetForkBranch(event, client)

            val mergeHistory = GitMergeHistory(
                id = realEvent.id ?: return@forEach,
                gitProjectId = gitProjectId,
                mergeRequestId = mrId,
                mrTitle = realEvent.mrTitle!!,
                branch = realEvent.branch,
                targetBranch = realEvent.targetBranch!!,
                extensionAction = realEvent.extensionAction,
                operationKind = realEvent.operationKind,
                commitTimeStamp = realEvent.commitTimeStamp,
                totalCommitCount = realEvent.totalCommitCount,
                userId = realEvent.userId,
                description = realEvent.description
            )
            val mergeBuildsList = gitRequestEventBuildDao.getRequestBuildsByEventId(dslContext, realEvent.id!!)
            logger.info("Get merge build list mergeBuildsList: $mergeBuildsList, gitProjectId: $gitProjectId")
            val builds = mergeBuildsList.map { it.buildId }.toSet()
            val buildList = client.get(ServiceBuildResource::class).getBatchBuildStatus(
                projectId = conf.projectCode!!,
                buildId = builds,
                channelCode = channelCode
            ).data
            if (buildList?.isEmpty() == false) {
                logger.info("Get merge build history list buildHistoryList: $buildList, gitProjectId: $gitProjectId")
                val records = mutableListOf<GitCIBuildHistory>()
                mergeBuildsList.forEach nextBuild@{
                    try {
                        val history = getBuildHistory(buildList, it.buildId ?: return@nextBuild)
                        val pipeline = pipelineResourceDao.getPipelineById(dslContext, gitProjectId, it.pipelineId)
                            ?: return@nextBuild
                        records.add(GitCIBuildHistory(
                            displayName = pipeline.displayName,
                            pipelineId = pipeline.pipelineId,
                            gitRequestEvent = realEvent,
                            buildHistory = history
                        ))
                    } catch (e: Exception) {
                        logger.error("Load gitProjectId: ${it.gitProjectId}, " +
                            "eventId: ${it.eventId}, pipelineId: ${it.pipelineId} failed with error: ", e)
                        return@nextBuild
                    }
                }
                mergeHistory.buildRecords = records
            } else {
                logger.info("Get branch build history list return empty, gitProjectId: $gitProjectId")
                return@forEach
            }
            if (mergeHistoryMap.containsKey(mrId)) {
                mergeHistoryMap[mrId]?.buildRecords?.addAll(mergeHistory.buildRecords ?: return@forEach)
            } else {
                mergeHistoryMap[mrId] = mergeHistory
            }
        }
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = mergeHistoryMap.size.toLong(),
            records = mergeHistoryMap.values.toList()
        )
    }

    private fun getBuildHistory(buildHistoryList: List<BuildHistory>, buildIdIt: String): BuildHistory? {
        buildHistoryList.forEach {
            if (it.id == buildIdIt) {
                return it
            }
        }
        return null
    }
}
