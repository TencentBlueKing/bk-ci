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

package com.tencent.devops.stream.v1.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.service.StreamScmService
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.stream.v1.components.V1StreamGitProjectInfoCache
import com.tencent.devops.stream.v1.dao.V1GitPipelineResourceDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventBuildDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventNotBuildDao
import com.tencent.devops.stream.v1.dao.V1StreamUserMessageDao
import com.tencent.devops.stream.v1.pojo.V1GitCIBuildHistory
import com.tencent.devops.stream.v1.pojo.V1GitRequestEventReq
import com.tencent.devops.stream.v1.pojo.V1GitRequestHistory
import com.tencent.devops.stream.v1.utils.V1GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Suppress("ComplexMethod", "NestedBlockDepth")
@Service
class V1GitCIRequestService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val streamBasicSettingService: V1StreamBasicSettingService,
    private val gitRequestEventDao: V1GitRequestEventDao,
    private val gitRequestEventBuildDao: V1GitRequestEventBuildDao,
    private val streamUserMessageDao: V1StreamUserMessageDao,
    private val gitRequestEventNotBuildDao: V1GitRequestEventNotBuildDao,
    private val pipelineResourceDao: V1GitPipelineResourceDao,
    private val streamGitProjectInfoCache: V1StreamGitProjectInfoCache,
    private val streamScmService: StreamScmService,
    private val streamGitConfig: StreamGitConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(V1GitCIRequestService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getRequestList(userId: String, gitProjectId: Long, page: Int?, pageSize: Int?): Page<V1GitRequestHistory> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        logger.info("get request list, gitProjectId: $gitProjectId")
        val conf = streamBasicSettingService.getGitCIConf(gitProjectId)
            ?: throw CustomException(Response.Status.FORBIDDEN, "项目未开启Stream，无法查询")
        val projectId = GitCommonUtils.getCiProjectId(gitProjectId, streamGitConfig.getScmType())
        val count = streamUserMessageDao.selectMessageCount(
            dslContext = dslContext,
            projectId = projectId,
            messageType = null,
            haveRead = null
        ).toLong()
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        val messageData = streamUserMessageDao.getMessages(
            dslContext = dslContext,
            projectId = projectId,
            messageType = null,
            haveRead = null,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        )
        val requestList = gitRequestEventDao.getRequestsById(
            dslContext = dslContext,
            requestIds = messageData?.map { it.messageId.toInt() }?.toSet() ?: emptySet(),
            hasEvent = false
        )
        if (requestList.isEmpty() || count == 0L) {
            logger.info("Get request build list return empty, gitProjectId: $gitProjectId")
            return Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = 0L,
                records = emptyList()
            )
        }
        val resultList = mutableListOf<V1GitRequestHistory>()
        requestList.forEach { event ->
            // 如果是来自fork库的分支，单独标识
            val realEvent =
                if (gitProjectId == event.gitProjectId) {
                    val gitProjectInfoCache = event.sourceGitProjectId?.let {
                        lazy {
                            streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                                gitProjectId = it,
                                useAccessToken = true,
                                getProjectInfo = streamScmService::getProjectInfoRetry
                            )
                        }
                    }
                    V1GitCommonUtils.checkAndGetForkBranch(event, gitProjectInfoCache)
                } else {
                    // 当gitProjectId与event的不同时，说明是远程仓库触发的
                    val pathWithNamespace = streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                        gitProjectId = event.gitProjectId,
                        useAccessToken = true,
                        getProjectInfo = streamScmService::getProjectInfoRetry
                    )?.pathWithNamespace
                    V1GitCommonUtils.checkAndGetRepoBranch(event, pathWithNamespace)
                }

            val requestHistory = V1GitRequestHistory(
                id = realEvent.id ?: return@forEach,
                gitProjectId = gitProjectId,
                commitId = realEvent.commitId,
                commitMsg = realEvent.commitMsg,
                branch = realEvent.branch,
                objectKind = realEvent.objectKind,
                commitTimeStamp = realEvent.commitTimeStamp,
                userId = realEvent.userId,
                description = realEvent.description,
                targetBranch = realEvent.targetBranch,
                mrTitle = realEvent.mrTitle,
                operationKind = realEvent.operationKind,
                mergeRequestId = realEvent.mergeRequestId,
                totalCommitCount = realEvent.totalCommitCount,
                buildRecords = mutableListOf()
            )

            // 已触发的所有记录
            val buildsList = gitRequestEventBuildDao.getRequestBuildsByEventId(dslContext, realEvent.id!!, gitProjectId)
            logger.info("Get build list requestBuildsList: $buildsList, gitProjectId: $gitProjectId")
            val builds = buildsList.map { it.buildId }.toSet()
            val buildList = client.get(ServiceBuildResource::class).getBatchBuildStatus(
                projectId = conf.projectCode!!,
                buildId = builds,
                channelCode = channelCode
            ).data
            if (buildList?.isEmpty() == false) {
                logger.info("V1GitCIRequestService|getRequestList|history list|$buildList|gitProjectId|$gitProjectId")
                val records = mutableListOf<V1GitCIBuildHistory>()
                buildsList.forEach nextBuild@{
                    try {
                        val history = getBuildHistory(buildList, it.buildId ?: return@nextBuild)
                        val pipeline = pipelineResourceDao.getPipelineById(dslContext, gitProjectId, it.pipelineId)
                            ?: return@nextBuild
                        records.add(
                            V1GitCIBuildHistory(
                                displayName = pipeline.displayName,
                                pipelineId = pipeline.pipelineId,
                                gitRequestEvent = V1GitRequestEventReq(realEvent),
                                buildHistory = history,
                                reason = TriggerReason.TRIGGER_SUCCESS.name,
                                reasonDetail = null
                            )
                        )
                    } catch (e: Exception) {
                        logger.warn(
                            "V1GitCIRequestService|getRequestList|gitProjectId: ${it.gitProjectId}, " +
                                "eventId: ${it.eventId}, pipelineId: ${it.pipelineId} failed with error: ",
                            e
                        )
                        return@nextBuild
                    }
                }
                requestHistory.buildRecords.addAll(records)
            } else {
                logger.info("Get branch build history list return empty, gitProjectId: $gitProjectId")
            }
            // -------

            // 未触发的所有记录
            val noBuildList = gitRequestEventNotBuildDao.getRequestNoBuildsByEventId(
                dslContext = dslContext,
                eventId = event.id!!,
                gitProjectId = gitProjectId
            )
            logger.info("Get no build list requestBuildsList: $noBuildList, gitProjectId: $gitProjectId")
            val records = mutableListOf<V1GitCIBuildHistory>()

            // 取所有记录的非空流水线ID，查出对应流水线
            val pipelineIds: List<String> = noBuildList.filter {
                !it.pipelineId.isNullOrBlank()
            }.map { it.pipelineId }
            val pipelineMap = pipelineResourceDao.getPipelinesInIds(dslContext, gitProjectId, pipelineIds).map {
                it.pipelineId to it
            }.toMap()

            noBuildList.forEach nextBuild@{
                val pipeline = if (it.pipelineId.isNullOrBlank()) null else pipelineMap[it.pipelineId]
                records.add(
                    V1GitCIBuildHistory(
                        displayName = pipeline?.displayName,
                        pipelineId = pipeline?.pipelineId,
                        gitRequestEvent = V1GitRequestEventReq(event),
                        buildHistory = null,
                        reason = it.reason,
                        reasonDetail = it.reasonDetail
                    )
                )
            }
            requestHistory.buildRecords.addAll(records)
            // -------

            resultList.add(requestHistory)
        }
        return Page(
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            count = count,
            records = resultList
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
